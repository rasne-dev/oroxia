package com.oroxia.launcher.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oroxia.launcher.data.local.entity.AppEntity
import com.oroxia.launcher.domain.categorizer.FolderSuggestion
import com.oroxia.launcher.ui.common.AppIcon
import com.oroxia.launcher.ui.theme.AccentPurple
import com.oroxia.launcher.ui.theme.AccentTeal
import com.oroxia.launcher.ui.theme.SurfaceDark
import com.oroxia.launcher.ui.theme.SurfaceVariantDark
import com.oroxia.launcher.ui.theme.TextPrimary
import com.oroxia.launcher.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedFolderWithApps by remember { mutableStateOf<FolderWithApps?>(null) }
    val sheetState = rememberModalBottomSheetState()

    androidx.activity.compose.BackHandler(enabled = selectedFolderWithApps != null) {
        selectedFolderWithApps = null
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            HomeBottomBar(
                onOpenDrawer = onOpenDrawer,
                onAutoOrganize = { viewModel.autoOrganizeAll() },
                onRefresh = { viewModel.refreshApps(force = true) },
                onOpenSettings = onOpenSettings,
                isScanning = uiState.isScanning
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header: Digital Clock & Date
            HomeHeader()

            // Suggestions Carousel / Banner
            AnimatedVisibility(visible = uiState.suggestions.isNotEmpty()) {
                val currentSuggestion = uiState.suggestions.firstOrNull()
                if (currentSuggestion != null) {
                    SuggestionCard(
                        suggestion = currentSuggestion,
                        onAccept = { viewModel.acceptSuggestion(currentSuggestion) },
                        onDismiss = { viewModel.dismissSuggestion(currentSuggestion) }
                    )
                }
            }

            // Folders and Apps Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section: Smart Folders
                if (uiState.foldersWithApps.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) {
                        Text(
                            text = "AKILLI KLASÖRLER",
                            color = AccentTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(uiState.foldersWithApps, key = { "folder_${it.folder.id}" }) { folderWithApps ->
                        FolderGridItem(
                            folderWithApps = folderWithApps,
                            onClick = { selectedFolderWithApps = folderWithApps }
                        )
                    }
                }

                // Section: Uncategorized / Individual Apps
                if (uiState.uncategorizedApps.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) {
                        Text(
                            text = "DİĞER UYGULAMALAR (${uiState.uncategorizedApps.size})",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(uiState.uncategorizedApps, key = { "app_${it.packageName}" }) { app ->
                        AppGridItem(
                            app = app,
                            onClick = { viewModel.launchApp(context, app.packageName) }
                        )
                    }
                }
            }
        }

        // Folder Expanded Sheet
        selectedFolderWithApps?.let { folderWithApps ->
            ModalBottomSheet(
                onDismissRequest = { selectedFolderWithApps = null },
                sheetState = sheetState,
                containerColor = SurfaceDark
            ) {
                FolderExpandedContent(
                    folderWithApps = folderWithApps,
                    onAppClick = { app ->
                        viewModel.launchApp(context, app.packageName)
                        selectedFolderWithApps = null
                    }
                )
            }
        }
    }
}

@Composable
fun HomeHeader(modifier: Modifier = Modifier) {
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    var currentDate by remember {
        mutableStateOf(SimpleDateFormat("EEEE, d MMMM", Locale("tr")).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d MMMM", Locale("tr")).format(now)
            kotlinx.coroutines.delay(1000L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 12.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentTime,
            color = TextPrimary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp
        )
        Text(
            text = currentDate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr")) else it.toString() },
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SuggestionCard(
    suggestion: FolderSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AccentPurple.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Akıllı Klasör Önerisi",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = suggestion.reason,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yoksay", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Klasöre Ekle", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FolderGridItem(
    folderWithApps: FolderWithApps,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceVariantDark)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            // 2x2 grid preview of apps inside folder
            val previewApps = folderWithApps.apps.take(4)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    previewApps.getOrNull(0)?.let { AppIcon(it.packageName, size = 20.dp) }
                    previewApps.getOrNull(1)?.let { AppIcon(it.packageName, size = 20.dp) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    previewApps.getOrNull(2)?.let { AppIcon(it.packageName, size = 20.dp) }
                    previewApps.getOrNull(3)?.let { AppIcon(it.packageName, size = 20.dp) }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = folderWithApps.folder.name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppGridItem(
    app: AppEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(app.packageName, size = 52.dp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.appName,
            color = TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FolderExpandedContent(
    folderWithApps: FolderWithApps,
    onAppClick: (AppEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = AccentPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = folderWithApps.folder.name,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${folderWithApps.apps.size} uygulama",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(folderWithApps.apps, key = { it.packageName }) { app ->
                AppGridItem(
                    app = app,
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

@Composable
fun HomeBottomBar(
    onOpenDrawer: () -> Unit,
    onAutoOrganize: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .background(SurfaceDark.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onRefresh, enabled = !isScanning) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = AccentTeal)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = "Yeniden Tara", tint = TextPrimary)
            }
        }

        IconButton(onClick = onAutoOrganize) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Otomatik Klasörle", tint = AccentTeal)
        }

        // Center: App Drawer
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(AccentPurple, CircleShape)
                .clickable { onOpenDrawer() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Apps, contentDescription = "Tüm Uygulamalar", tint = Color.White)
        }

        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Ayarlar", tint = TextPrimary)
        }
    }
}
