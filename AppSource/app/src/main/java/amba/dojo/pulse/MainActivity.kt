package amba.dojo.pulse

import android.os.Bundle
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import amba.dojo.pulse.ui.theme.DojoPulseTheme
import amba.dojo.pulse.ui.theme.ArenasScreen // Pastikan ini di-import
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoPulseTheme {
                val navController = rememberNavController()

                // Determine starting destination
                val auth = FirebaseAuth.getInstance()
                val startDestination = if (auth.currentUser != null) "dashboard" else "login"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0.dp)
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = startDestination) {
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
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    },
                                    onStartTraining = {
                                        navController.navigate("training")
                                    },
                                    onNavigateTutorial = {
                                        navController.navigate("tutorial")
                                    },
                                    onNavigateToArenas = {
                                        navController.navigate("arenas")
                                    }
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