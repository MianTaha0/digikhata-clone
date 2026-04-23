package com.digikhata.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.digikhata.data.auth.AuthRepository
import com.digikhata.data.auth.DigiUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.digikhata.BuildConfig
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiRed

private const val GITHUB_URL = "https://github.com/MianTaha0/digikhata-clone"

@HiltViewModel
class SettingsViewModel @Inject constructor(
    authRepo: AuthRepository
) : ViewModel() {
    val currentUser: StateFlow<DigiUser?> = authRepo.currentUser
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit = {},
    onProfile: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by vm.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = digiTopBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            SectionHeader("Sync & Backup")
            SettingsRow(
                icon = Icons.Default.Cloud,
                title = "Cloud Sync",
                subtitle = if (currentUser != null)
                    "Signed in as ${currentUser?.phoneNumber ?: ""}"
                else
                    "Sign in to sync across devices",
                onClick = {
                    if (currentUser != null) onProfile() else onSignIn()
                }
            )
            SettingsRow(
                icon = Icons.Default.Download,
                title = "Export data",
                subtitle = "Save a backup to device storage",
                comingSoon = true
            )
            SettingsRow(
                icon = Icons.Default.Restore,
                title = "Restore from backup",
                subtitle = "Import data from a previous backup",
                comingSoon = true
            )

            SectionHeader("Preferences")
            SettingsRow(
                icon = Icons.Default.Language,
                title = "Language",
                subtitle = "English",
                comingSoon = true
            )

            SectionHeader("About")
            SettingsRow(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"
            )
            SettingsRow(
                icon = Icons.Default.Code,
                title = "Source on GitHub",
                subtitle = "MianTaha0/digikhata-clone",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()))
                }
            )
            SettingsRow(
                icon = Icons.Default.Share,
                title = "Share this app",
                onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "DigiKhata clone — $GITHUB_URL")
                    }
                    context.startActivity(Intent.createChooser(share, "Share"))
                }
            )
            SettingsRow(
                icon = Icons.Default.Star,
                title = "Rate the app",
                subtitle = "Open the Play Store listing",
                comingSoon = true
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "Made with Jetpack Compose",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = DigiRed,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    comingSoon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickable = onClick != null && !comingSoon
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable { onClick!!() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DigiRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = DigiRed, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (comingSoon) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (comingSoon) {
            Text(
                "Soon",
                style = MaterialTheme.typography.labelSmall,
                color = DigiRed,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DigiRed.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
}
