package com.oroxia.launcher.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oroxia.launcher.OroxiaApplication
import com.oroxia.launcher.ui.theme.AccentPurple
import com.oroxia.launcher.ui.theme.AccentTeal
import com.oroxia.launcher.ui.theme.SurfaceDark
import com.oroxia.launcher.ui.theme.SurfaceVariantDark
import com.oroxia.launcher.ui.theme.TextPrimary
import com.oroxia.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as OroxiaApplication
    val prefs = app.preferencesRepository
    val coroutineScope = rememberCoroutineScope()

    val currentApiKey by prefs.geminiApiKeyFlow.collectAsState(initial = "")
    val autoFolderPlacement by prefs.autoFolderPlacementFlow.collectAsState(initial = false)
    val autoCategorize by prefs.autoCategorizeEnabledFlow.collectAsState(initial = true)

    var inputApiKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var keySavedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = TextPrimary)
            }
            Text(
                text = "Ayarlar",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Default Launcher Setting Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = AccentTeal)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Varsayılan Başlatıcı (Launcher)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Oroxia'yı varsayılan ana ekranınız yaparak tüm akıllı klasörleri yönetin.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Varsayılan Yap")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gemini 2.0 Flash API Key Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = AccentPurple)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Gemini 2.0 Flash API Anahtarı", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Kategorilendirme için kullanılır. local.properties veya buradan tanımlanabilir.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputApiKey,
                    onValueChange = { inputApiKey = it },
                    label = { Text("API Anahtarı") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            prefs.setGeminiApiKey(inputApiKey.trim())
                            keySavedMessage = "API anahtarı başarıyla kaydedildi."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kaydet", color = SurfaceDark)
                }

                keySavedMessage?.let {
                    Text(it, color = AccentTeal, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Folder Placement Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tam Otomatik Klasörleme", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Açıkken yeni uygulamaları onay istemeden doğrudan klasöre ekler. Kapalıyken size öneri sunar.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoFolderPlacement,
                        onCheckedChange = { coroutineScope.launch { prefs.setAutoFolderPlacement(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentTeal, checkedTrackColor = AccentPurple)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Yapay Zeka ile Kategorileme", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Gemini 2.0 Flash kullanarak akıllı sınıflandırma yapar.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoCategorize,
                        onCheckedChange = { coroutineScope.launch { prefs.setAutoCategorizeEnabled(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentTeal, checkedTrackColor = AccentPurple)
                    )
                }
            }
        }
    }
}
