package com.example.mvt.welcome.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.welcome.data.WelcomeRepository
import com.example.mvt.welcome.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val RemoteOperationTimeoutMillis = 20_000L

class WelcomeViewModel(private val repository: WelcomeRepository = WelcomeRepository()) : ViewModel() {
    private val _state = MutableStateFlow<WelcomeUiState>(WelcomeUiState.Loading)
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()
    private val _selectedPhoto = MutableStateFlow<Uri?>(null)
    val selectedPhoto: StateFlow<Uri?> = _selectedPhoto.asStateFlow()
    private var observer: Job? = null
    private var uid: String = ""
    var hasStartedWelcomeSession: Boolean = false
        private set

    fun start(userId: String) {
        if (userId.isBlank() || (uid == userId && observer?.isActive == true)) return
        uid = userId
        observer?.cancel()
        observer = viewModelScope.launch {
            repository.observe(userId).collect { remote ->
                if (remote.step == WelcomeStep.COMPLETED) {
                    _state.value = WelcomeUiState.Ready(remote)
                    return@collect
                }
                _state.value = WelcomeUiState.Loading
                runCatching {
                    withTimeout(RemoteOperationTimeoutMillis) {
                        repository.loadStep(userId, remote)
                    }
                }
                    .onSuccess { _state.value = WelcomeUiState.Ready(it) }
                    .onFailure { _state.value = WelcomeUiState.Error(it.friendlyMessage()) }
            }
        }
    }

    fun retry() { val current = uid; uid = ""; start(current) }

    fun markWelcomeSessionStarted() {
        hasStartedWelcomeSession = true
    }

    fun updatePersonal(transform: (WelcomePersonalForm) -> WelcomePersonalForm) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        _state.value = ready.copy(personal = transform(ready.personal), validation = WelcomeValidation(), message = null)
    }

    fun validatePersonalStage(stage: Int): Boolean {
        val ready = _state.value as? WelcomeUiState.Ready ?: return false
        val keys = when (stage) {
            0 -> setOf("firstName", "lastName", "birthDate", "gender", "phonePrefix", "phone")
            1 -> setOf("height", "weight", "minHeartRate", "maxHeartRate")
            else -> setOf("subjectiveLevel", "heartRateMonitor")
        }
        val validation = WelcomeValidation(validatePersonal(ready.personal).errors.filterKeys { it in keys })
        _state.value = ready.copy(validation = validation)
        return validation.isValid
    }

    fun updateGoal(transform: (WelcomeGoalForm) -> WelcomeGoalForm) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        _state.value = ready.copy(goal = transform(ready.goal), validation = WelcomeValidation(), message = null)
    }

    fun savePersonal() {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        val validation = validatePersonal(ready.personal)
        if (!validation.isValid) { _state.value = ready.copy(validation = validation); return }
        execute(ready) {
            _selectedPhoto.value?.let { repository.uploadProfilePhoto(uid, it) }
            repository.savePersonal(uid, ready.personal)
            _selectedPhoto.value = null
        }
    }

    fun selectPhoto(uri: Uri?) { _selectedPhoto.value = uri }

    fun selectPlan(plan: WelcomePlan) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        _state.value = ready.copy(
            selectedPlanId = plan.id,
            validation = WelcomeValidation(),
            message = null
        )
    }

    fun continueWithSelectedPlan() {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        val plan = ready.snapshot.plans.firstOrNull { it.id == ready.selectedPlanId }
        if (plan == null) {
            _state.value = ready.copy(
                validation = WelcomeValidation(
                    mapOf("selectedPlan" to "Selecciona un plan para continuar.")
                ),
                message = null
            )
            return
        }
        if (plan.price > 0) {
            _state.value = ready.copy(message = "El plan ${plan.name} avanzará cuando el pago sea confirmado.")
            return
        }
        execute(ready) { repository.selectFreePlan(uid) }
    }

    fun saveGoal(editingId: String?, onSuccess: () -> Unit) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        val validation = validateGoal(ready.goal, requireAvailableDays = false)
        if (!validation.isValid) { _state.value = ready.copy(validation = validation); return }
        mutateObjectives(ready, {
            if (editingId == null) repository.addGoal(uid, ready.goal)
            else repository.updateGoal(uid, editingId, ready.goal)
        }, onSuccess)
    }

    fun deleteGoal(id: String) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        mutateObjectives(ready, { repository.deleteGoal(uid, id) })
    }

    fun updateAvailableDay(day: String) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        val selected = ready.selectedDays
        if (day !in selected && selected.size >= 4) return
        _state.value = ready.copy(
            selectedDays = if (day in selected) selected - day else selected + day,
            validation = WelcomeValidation(),
            message = null
        )
    }

    fun continueFromObjectives() {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        val paidPlan = ready.snapshot.planName.isNotBlank() &&
            !ready.snapshot.planName.contains("bronce", ignoreCase = true)
        val errors = buildMap {
            if (ready.snapshot.goals.isEmpty()) put("goals", "Registra al menos un objetivo para continuar.")
            if (paidPlan && ready.selectedDays.size != 4) {
                val missingDays = (4 - ready.selectedDays.size).coerceAtLeast(0)
                put(
                    "days",
                    if (missingDays == 1) {
                        "Te falta 1 día por seleccionar. Debes completar 4 días disponibles."
                    } else {
                        "Te faltan $missingDays días por seleccionar. Debes completar 4 días disponibles."
                    }
                )
            }
        }
        if (errors.isNotEmpty()) {
            _state.value = ready.copy(validation = WelcomeValidation(errors), message = null)
            return
        }
        execute(ready) { repository.completeObjectives(uid, ready.selectedDays) }
    }

    fun selectFreeTrainingPlan(plan: FreeTrainingPlan) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        _state.value = ready.copy(selectedFreePlanId = plan.id, selectedTrainerId = null, previewFreePlanId = null)
    }

    fun previewFreeTrainingPlan(plan: FreeTrainingPlan) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        _state.value = ready.copy(previewFreePlanId = plan.id, message = null)
    }

    fun closeFreeTrainingPlanPreview() {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        _state.value = ready.copy(previewFreePlanId = null)
    }

    fun enrollFreeTrainingPlan(plan: FreeTrainingPlan) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        execute(ready.copy(selectedFreePlanId = plan.id, previewFreePlanId = null)) {
            repository.enrollFreePlan(uid, plan)
        }
    }

    fun selectTrainer(trainer: WelcomeTrainer) {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        _state.value = ready.copy(
            selectedTrainerId = trainer.id.takeUnless { it == ready.selectedTrainerId },
            selectedFreePlanId = null
        )
    }

    fun completeFinalSelection() {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        if (ready.snapshot.planName.contains("bronce", ignoreCase = true)) {
            val plan = ready.snapshot.freePlans.firstOrNull { it.id == ready.selectedFreePlanId }
            if (plan == null) { _state.value = ready.copy(message = "Selecciona un plan de entrenamiento."); return }
            execute(ready) { repository.enrollFreePlan(uid, plan) }
        } else {
            val trainer = ready.snapshot.trainers.firstOrNull { it.id == ready.selectedTrainerId }
            if (trainer == null) { _state.value = ready.copy(message = "Selecciona un entrenador."); return }
            execute(ready) { repository.requestTrainer(uid, trainer) }
        }
    }

    fun goBack() {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        execute(ready) { repository.goBack(uid, ready.snapshot.step) }
    }

    fun clearMessage() {
        val ready = _state.value as? WelcomeUiState.Ready ?: return
        _state.value = ready.copy(message = null)
    }

    private fun execute(ready: WelcomeUiState.Ready, action: suspend () -> Unit) {
        if (ready.isSaving) return
        _state.value = ready.copy(isSaving = true, message = null)
        viewModelScope.launch {
            runCatching {
                withTimeout(RemoteOperationTimeoutMillis) { action() }
            }.onSuccess {
                val current = _state.value as? WelcomeUiState.Ready ?: return@onSuccess
                if (current.isSaving) _state.value = current.copy(isSaving = false)
            }.onFailure { error ->
                val current = _state.value as? WelcomeUiState.Ready ?: ready
                _state.value = current.copy(isSaving = false, message = error.friendlyMessage())
            }
        }
    }

    private fun mutateObjectives(
        ready: WelcomeUiState.Ready,
        action: suspend () -> Unit,
        onSuccess: () -> Unit = {}
    ) {
        if (ready.isSaving) return
        _state.value = ready.copy(isSaving = true, message = null)
        viewModelScope.launch {
            runCatching {
                withTimeout(RemoteOperationTimeoutMillis) {
                    action()
                    repository.loadStep(uid, ready.snapshot)
                }
            }.onSuccess { refreshed ->
                _state.value = ready.copy(
                    snapshot = refreshed,
                    goal = WelcomeGoalForm(),
                    selectedDays = refreshed.availableDays,
                    validation = WelcomeValidation(),
                    isSaving = false,
                    message = null
                )
                onSuccess()
            }.onFailure { error ->
                _state.value = ready.copy(isSaving = false, message = error.friendlyMessage())
            }
        }
    }
}

private fun Throwable.friendlyMessage() = when (this) {
    is TimeoutCancellationException -> "La operación está tardando más de lo esperado. Revisa tu conexión e inténtalo nuevamente."
    else -> message?.takeIf(String::isNotBlank)
        ?: "No fue posible guardar los cambios. Revisa tu conexión e inténtalo nuevamente."
}
