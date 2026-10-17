package amba.dojo.pulse

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import amba.dojo.pulse.ui.theme.DojoPulseTheme
import amba.dojo.pulse.ui.theme.ArenasScreen // Pastikan ini di-import
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            DojoPulseTheme {
                val navController = rememberNavController()
                val navigateTab: (String) -> Unit = { route ->
                    navController.navigate(route) {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                // Determine starting destination
                val auth = FirebaseAuth.getInstance()
                val startDestination = if (auth.currentUser != null) "dashboard" else "login"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0.dp)
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            enterTransition = { fadeIn(animationSpec = tween(200)) },
                            exitTransition = { fadeOut(animationSpec = tween(200)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
                            popExitTransition = { fadeOut(animationSpec = tween(200)) }
                        ) {
                            composable("login") {
                                LoginPage(
                                    onLoginSuccess = {
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    onNavigateToRegister = {
                                        navController.navigate("register")
                                    }
                                )
                            }
                            composable("register") {
                                RegisterPage(
                                    onRegisterSuccess = {
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    onNavigateToLogin = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            composable("dashboard") {
                                DashboardPage(
                                    onNavigate = navigateTab,
                                    onStartTraining = { navController.navigate("training") },
                                    onOpenWorkout = { id -> navController.navigate("workout/$id") }
                                )
                            }
                            composable("latihan") {
                                LatihanScreen(
                                    onNavigate = navigateTab,
                                    onOpenTutorial = { navController.navigate("tutorial") },
                                    onOpenWorkout = { id -> navController.navigate("workout/$id") },
                                    onOpenArenas = { navController.navigate("arenas") }
                                )
                            }
                            composable("progres") {
                                ProgresScreen(
                                    onNavigate = navigateTab,
                                    onNextCourse = { navController.navigate("latihan") }
                                )
                            }
                            composable("aktivitas") {
                                AktivitasScreen(
                                    onNavigate = navigateTab,
                                    onStartTraining = { navController.navigate("training") }
                                )
                            }
                            composable("profil") {
                                ProfilScreen(
                                    onNavigate = navigateTab,
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("workout/{id}") { entry ->
                                WorkoutDetailScreen(
                                    workoutId = entry.arguments?.getString("id") ?: "striking",
                                    onNavigateBack = { navController.popBackStack() },
                                    onStartRound = { navController.navigate("training") }
                                )
                            }
                            composable("arenas") {
                                ArenasScreen(
                                    onNavigateToDashboard = {
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    },
                                    onStartTraining = {
                                        navController.navigate("training")
                                    }
                                )
                            }
                            composable("training") {
                                amba.dojo.pulse.vision.TrainingScreen(
                                    onFinishTraining = {
                                        navController.popBackStack()
                                    },
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onOpenTutorial = {
                                        navController.navigate("tutorial")
                                    }
                                )
                            }
                            composable("tutorial") {
                                BoxingTutorialScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onStartDrill = {
                                        navController.navigate("training") {
                                            popUpTo("tutorial") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}