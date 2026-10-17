package amba.dojo.pulse

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import amba.dojo.pulse.ui.theme.Dojo
import amba.dojo.pulse.ui.theme.DojoPulseTheme
import amba.dojo.pulse.ui.theme.MonoLabel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val backgroundColor = Dojo.Bg
    val buttonColor = Dojo.Red
    val textColor = Color.White
    val clientId = stringResource(id = R.string.default_web_client_id)

    val auth = remember {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth?.signInWithCredential(credential)?.addOnCompleteListener { authTask ->
                    isLoading = false
                    if (authTask.isSuccessful) {
                        errorMessage = "Signed in successfully via Google!"
                        onLoginSuccess()
                    } else {
                        errorMessage = authTask.exception?.message ?: "Google Firebase Authentication failed"
                    }
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Google Sign-In failed: ${e.message}"
            }
        } else {
            isLoading = false
        }
    }

    val onGoogle: () -> Unit = {
        isLoading = true
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Error initiating Google Sign In: ${e.message}"
        }
    }

    val onSubmit: () -> Unit = {
        if (email.isNotEmpty() && password.isNotEmpty()) {
            isLoading = true
            if (auth != null) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            errorMessage = "Signed in successfully!"
                            onLoginSuccess()
                        } else {
                            errorMessage = task.exception?.message ?: "Authentication failed"
                        }
                    }
            } else {
                isLoading = false
                errorMessage = "Firebase not initialized. Check google-services.json"
            }
        } else {
            errorMessage = "Please enter email and password"
        }
    }

    val fieldShape = RoundedCornerShape(10.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Dojo.Surface,
        unfocusedContainerColor = Dojo.Surface,
        focusedTextColor = Dojo.Text,
        unfocusedTextColor = Dojo.Text,
        focusedBorderColor = Dojo.Red,
        unfocusedBorderColor = Dojo.Border,
        cursorColor = Dojo.Red
    )
    val fieldText = TextStyle(fontFamily = Dojo.Mono, fontSize = 13.sp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Dojo.RedDeep)
                    .border(1.dp, Dojo.Red.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SportsMma, contentDescription = null, tint = Dojo.RedSoft, modifier = Modifier.size(38.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Text("DOJO", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("PULSE", color = Dojo.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            MonoLabel("TACTICAL STRIKING TELEMETRY", size = 9, color = Dojo.Dim)

            Spacer(modifier = Modifier.height(48.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                MonoLabel("FIGHTER ID / EMAIL", size = 9)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    textStyle = fieldText,
                    colors = fieldColors,
                    placeholder = { Text("alex.chen@combatsport.io", color = Dojo.Dim, fontFamily = Dojo.Mono, fontSize = 13.sp) },
                    trailingIcon = { Icon(Icons.Outlined.Email, contentDescription = "Email Icon", tint = Dojo.Red, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                MonoLabel("CIPHER PIN / PASSWORD", size = 9)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    textStyle = fieldText,
                    colors = fieldColors,
                    visualTransformation = PasswordVisualTransformation(),
                    trailingIcon = { Icon(Icons.Default.VpnKey, contentDescription = "Password Icon", tint = Dojo.Muted, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Dojo.RedSoft,
                    fontSize = 12.sp,
                    fontFamily = Dojo.Mono
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSubmit() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = textColor, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = "AUTHENTICATION  →",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        fontFamily = Dojo.Mono
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onGoogle() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Dojo.Border),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Dojo.Surface, contentColor = Dojo.Text),
                enabled = !isLoading
            ) {
                Text("G", color = Dojo.Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Login With Google", color = Dojo.Text, fontSize = 12.sp, fontFamily = Dojo.Mono)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonoLabel("Forgot Password?", size = 10)
                Row {
                    MonoLabel("No account? ", size = 10)
                    MonoLabel(
                        "Register",
                        size = 10,
                        color = Dojo.Red,
                        bold = true,
                        modifier = Modifier.clickable { onNavigateToRegister() }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    DojoPulseTheme {
        LoginPage(onLoginSuccess = {}, onNavigateToRegister = {})
    }
}