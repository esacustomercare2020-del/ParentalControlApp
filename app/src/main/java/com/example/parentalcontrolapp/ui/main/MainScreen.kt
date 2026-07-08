package com.example.parentalcontrolapp.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.parentalcontrolapp.ParentalControlService
import com.example.parentalcontrolapp.SecurityManager
import com.example.parentalcontrolapp.data.DefaultDataRepository
import com.example.parentalcontrolapp.theme.ParentalControlAppTheme

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var isServiceEnabled by remember { mutableStateOf(false) }
  var isUnlocked by remember { mutableStateOf(SecurityManager.isParentAuthenticated) }
  var pinText by remember { mutableStateOf("") }
  var isPinError by remember { mutableStateOf(false) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        isServiceEnabled = isAccessibilityServiceEnabled(context, ParentalControlService::class.java)
        isUnlocked = SecurityManager.isParentAuthenticated
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  if (!isUnlocked) {
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF0D0D11))
        .padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Console Locked",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      Text(
        text = "Enter Parent PIN to manage shield settings",
        fontSize = 14.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 32.dp)
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24))
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          OutlinedTextField(
            value = pinText,
            onValueChange = {
              if (it.length <= 8) {
                pinText = it
                isPinError = false
              }
            },
            label = { Text("Parent PIN", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = isPinError,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Color(0xFFC62828),
              unfocusedBorderColor = Color.DarkGray,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
          )

          if (isPinError) {
            Text(
              text = "Invalid PIN. Please try again.",
              color = Color(0xFFF44336),
              fontSize = 12.sp,
              modifier = Modifier.padding(bottom = 16.dp)
            )
          } else {
            Text(
              text = "Default PIN is 1234",
              color = Color.DarkGray,
              fontSize = 12.sp,
              modifier = Modifier.padding(bottom = 16.dp)
            )
          }

          Button(
            onClick = {
              if (SecurityManager.authenticate(pinText, context)) {
                isUnlocked = true
                pinText = ""
              } else {
                isPinError = true
                pinText = ""
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Unlock Console", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  } else {
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF0D0D11))
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Guardian Shield",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = "Parental Control Console",
            fontSize = 13.sp,
            color = Color.Gray
          )
        }
        
        IconButton(
          onClick = {
            SecurityManager.lock()
            isUnlocked = false
          }
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFC62828).copy(alpha = 0.15f),
            contentColor = Color(0xFFEF5350)
          ) {
            Text(
              text = "Lock",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 28.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isServiceEnabled) Color(0xFF1E291E) else Color(0xFF291E1E)
        )
      ) {
        Column(
          modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = if (isServiceEnabled) "Shield Active" else "Shield Deactivated",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isServiceEnabled) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Text(
            text = if (isServiceEnabled) {
              "Accessibility Service is running. YouTube and Instagram are currently blocked."
            } else {
              "Accessibility Service is required to monitor foreground applications and block restricted apps."
            },
            fontSize = 13.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
          )

          Button(
            onClick = {
              val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
              context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isServiceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
          ) {
            Text(
              text = if (isServiceEnabled) "Configure Service" else "Enable Accessibility",
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.Start
      ) {
        Text(
          text = "Restricted Applications",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      AppStatusRow(appName = "Instagram", packageName = "com.instagram.android", isBlocked = isServiceEnabled)
      Spacer(modifier = Modifier.height(10.dp))
      AppStatusRow(appName = "YouTube", packageName = "com.google.android.youtube", isBlocked = isServiceEnabled)
    }
  }
}

@Composable
fun AppStatusRow(appName: String, packageName: String, isBlocked: Boolean) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24))
  ) {
    Row(
      modifier = Modifier
        .padding(14.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = appName,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = packageName,
          fontSize = 11.sp,
          color = Color.Gray
        )
      }
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isBlocked) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFF37474F),
        contentColor = if (isBlocked) Color(0xFF81C784) else Color(0xFFB0BEC5)
      ) {
        Text(
          text = if (isBlocked) "Blocked" else "Idle",
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
  val expectedComponentName = "${context.packageName}/${service.name}"
  val enabledServicesSetting = Settings.Secure.getString(
    context.contentResolver,
    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
  ) ?: return false

  val colonSplitter = TextUtils.SimpleStringSplitter(':')
  colonSplitter.setString(enabledServicesSetting)
  while (colonSplitter.hasNext()) {
    val componentName = colonSplitter.next()
    if (componentName.equals(expectedComponentName, ignoreCase = true)) {
      return true
    }
  }
  return false
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  ParentalControlAppTheme {
    MainScreen(onItemClick = {})
  }
}
