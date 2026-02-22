package com.example.mvt.ui.components.summary

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.data.firebase.models.Routine
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "RoutineStatusSelector"

// ================================================================
//  COMPONENTE COMPLETO
// ================================================================
@Composable
fun RoutineStatusSelector(
    routine: Routine,
    modifier: Modifier = Modifier,
    onEstadoActualizado: ((String) -> Unit)? = null   // callback opcional
) {
    Log.d(TAG, "🔵 Composable iniciado → estado inicial = '${routine.estado}', id = ${routine.id}")

    val estados = listOf(
        "Pendiente",
        "Realizada",
        "Parcial",
        "No_realizada"
    )

    // Estado interno controlado
    var selected by remember {
        mutableStateOf(routine.estado)
    }

    Log.d(TAG, "🟣 Recompose → selected = '$selected'")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {

        Text(
            text = "Estado de la rutina",
            fontSize = 14.sp,
            color = Color.White.copy(0.85f),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            estados.chunked(2).forEach { fila ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fila.forEach { estado ->

                        EstadoChip(
                            label = estado,
                            selected = (selected == estado),
                            modifier = Modifier.weight(1f),
                            highlightColor = getEstadoColor(estado),
                        ) {
                            Log.d(TAG, "🟠 Click en '$estado'")

                            selected = estado   // ← actualiza UI inmediata
                            Log.d(TAG, "🟢 Estado interno cambiado → '$selected'")

                            actualizarEstadoFirebase(
                                id = routine.id,
                                estado = estado
                            ) {
                                onEstadoActualizado?.invoke(estado)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================================================================
//  CHIP FUTURISTA
// ================================================================
@Composable
private fun EstadoChip(
    label: String,
    selected: Boolean,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                brush = if (selected)
                    Brush.verticalGradient(
                        colors = listOf(
                            highlightColor.copy(alpha = 0.45f),
                            highlightColor.copy(alpha = 0.30f)
                        )
                    )
                else
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (selected) highlightColor else Color.White.copy(0.4f),
                        shape = RoundedCornerShape(50)
                    )
            )

            Text(
                text = cleanStateLabel(label),
                fontSize = 14.sp,
                color = if (selected) Color.White else Color.White.copy(0.7f)
            )
        }
    }
}

// ================================================================
//  COLORES OFICIALES DE ESTADO
// ================================================================
private fun getEstadoColor(estado: String): Color {
    return when (estado) {
        "Realizada"    -> Color(0xFF77DD77)
        "Parcial"      -> Color(0xFFFFCA99)
        "No_realizada" -> Color(0xFFFF6961)
        "Pendiente"    -> Color(0xFFE5DDE6)
        else           -> Color.White.copy(alpha = 0.2f)
    }
}

private fun cleanStateLabel(label: String): String =
    when (label) {
        "No_realizada" -> "No realizada"
        else -> label
    }

// ================================================================
//  FIREBASE UPDATE + LOGS
// ================================================================
private fun actualizarEstadoFirebase(
    id: String,
    estado: String,
    onSuccess: () -> Unit
) {
    Log.d(TAG, "🔵 Actualizando Firebase → id='$id', estado='$estado'")

    if (id.isEmpty()) {
        Log.e(TAG, "❌ ERROR → ID VACÍO. No se actualizará en Firestore.")
        return
    }

    FirebaseFirestore.getInstance()
        .collection("rutinas")
        .document(id)
        .update("estado", estado)
        .addOnSuccessListener {
            Log.d(TAG, "✅ Firebase actualizado → estado='$estado'")
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "❌ ERROR al actualizar Firebase → ${e.message}")
        }
}
