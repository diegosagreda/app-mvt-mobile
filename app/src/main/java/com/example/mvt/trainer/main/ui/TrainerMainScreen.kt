package com.example.mvt.trainer.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.trainer.navigation.TrainerDestination
import com.example.mvt.trainer.navigation.TrainerNavGraph
import com.example.mvt.ui.theme.*
import com.example.mvt.ui.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerMainScreen(
    navController: NavController, // Root NavController
    userViewModel: UserViewModel
) {
    val trainerNavController = rememberNavController()
    val backStackEntry by trainerNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val user by userViewModel.user.collectAsState()

    LaunchedEffect(Unit) {
        userViewModel.loadUserInfo()
    }

    Scaffold(
        modifier = Modifier.background(AppBackground),
        containerColor = AppBackground,
        topBar = {
            TrainerHeader(
                profilePhotoUrl = user?.foto_url,
                currentRoute = currentRoute,
                onProfileClick = {
                    if (currentRoute == TrainerDestination.SETTINGS) {
                        trainerNavController.popBackStack()
                    } else {
                        trainerNavController.navigate(TrainerDestination.SETTINGS) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        },
        bottomBar = {
            TrainerBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    if (currentRoute != destination) {
                        trainerNavController.navigate(destination) {
                            popUpTo(trainerNavController.graph.startDestinationId) {
                                saveState = true
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        TrainerNavGraph(
            navController = trainerNavController,
            rootNavController = navController as NavHostController,
            user = user,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainerHeader(
    profilePhotoUrl: String?,
    currentRoute: String?,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
        navigationIcon = {
            IconButton(onClick = { /* Home Click */ }) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = { },
        actions = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "Mensajes",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (currentRoute == TrainerDestination.SETTINGS) PrimaryBlue.copy(alpha = 0.2f) else AppSurface)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePhotoUrl.isNullOrBlank()) {
                        Image(
                            painter = painterResource(id = R.drawable.iconografia_02_svg),
                            contentDescription = "Cuenta",
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        AsyncImage(
                            model = profilePhotoUrl,
                            contentDescription = "Cuenta",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun TrainerBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = AppSurface,
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column {
            NavigationBar(
                containerColor = AppSurface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.height(76.dp)
            ) {
                TrainerNavigationItem(
                    selected = currentRoute == TrainerDestination.HOME,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Inicio"
                        )
                    },
                    label = "Inicio",
                    onClick = {
                        onNavigate(TrainerDestination.HOME)
                    }
                )

                TrainerNavigationItem(
                    selected = currentRoute == TrainerDestination.PROFILE ||
                            currentRoute == TrainerDestination.PERSONAL_INFO ||
                            currentRoute == TrainerDestination.SETTINGS,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil"
                        )
                    },
                    label = "Perfil",
                    onClick = {
                        onNavigate(TrainerDestination.PROFILE)
                    }
                )
            }

            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(
                    WindowInsets.navigationBars
                )
            )
        }
    }
}

@Composable
private fun RowScope.TrainerNavigationItem(
    selected: Boolean,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = {
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.SemiBold
                    }
                )
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = Color.White,
            unselectedIconColor = AppTextSecondary,
            unselectedTextColor = AppTextSecondary,
            indicatorColor = PrimaryBlue.copy(alpha = 0.22f)
        )
    )
}
