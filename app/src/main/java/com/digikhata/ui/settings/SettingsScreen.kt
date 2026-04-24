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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Context
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.auth.AuthRepository
import com.digikhata.data.auth.DigiUser
import com.digikhata.data.entity.Business
import com.digikhata.data.export.BackupExporter
import com.digikhata.data.export.BackupImporter
import com.digikhata.data.export.ImportResult
import android.net.Uri as AndroidUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.compose.ui.res.stringResource
import com.digikhata.BuildConfig
import com.digikhata.R
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.LocaleManager

private const val GITHUB_URL = "https://github.com/MianTaha0/digikhata-clone"

@HiltViewModel
class SettingsViewModel @Inject constructor(
    authRepo: AuthRepository,
    private val active: ActiveBookHolder,
    private val exporter: BackupExporter,
    private val importer: BackupImporter
) : ViewModel() {
    val currentUser: StateFlow<DigiUser?> = authRepo.currentUser
    val business: StateFlow<Business?> = active.active.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        null
    )

    fun exportBackup(context: Context, onDone: (File?) -> Unit) {
        val biz = business.value
        if (biz == null) { onDone(null); return }
        viewModelScope.launch {
            val file = try {
                withContext(Dispatchers.IO) { exporter.export(context, biz) }
            } catch (_: Throwable) { null }
            onDone(file)
        }
    }

    fun importBackup(context: Context, uri: AndroidUri, onDone: (ImportResult?) -> Unit) {
        val biz = business.value
        if (biz == null) { onDone(null); return }
        viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        importer.importZip(input, biz.id)
                    }
                }
            } catch (_: Throwable) { null }
            onDone(result)
        }
    }
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
    val business by vm.business.collectAsState()
    var exporting by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var showLangPicker by remember { mutableStateOf(false) }
    var currentLang by remember { mutableStateOf(LocaleManager.current()) }

    val pickZip = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: AndroidUri? ->
        if (uri != null) {
            importing = true
            vm.importBackup(context, uri) { result ->
                importing = false
                importResult = result
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            SectionHeader(stringResource(R.string.settings_section_sync))
            SettingsRow(
                icon = Icons.Default.Cloud,
                title = stringResource(R.string.settings_cloud_sync),
                subtitle = if (currentUser != null)
                    stringResource(R.string.settings_signed_in_as, currentUser?.phoneNumber ?: "")
                else
                    stringResource(R.string.settings_sign_in_to_sync_sub),
                onClick = {
                    if (currentUser != null) onProfile() else onSignIn()
                }
            )
            SettingsRow(
                icon = Icons.Default.Download,
                title = stringResource(R.string.settings_export),
                subtitle = when {
                    exporting -> stringResource(R.string.settings_export_preparing)
                    business == null -> stringResource(R.string.settings_export_no_book)
                    else -> stringResource(R.string.settings_export_sub)
                },
                onClick = {
                    if (!exporting && business != null) {
                        exporting = true
                        vm.exportBackup(context) { file ->
                            exporting = false
                            if (file != null) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    context.packageName + ".provider",
                                    file
                                )
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(send, "Share backup"))
                            }
                        }
                    }
                }
            )
            SettingsRow(
                icon = Icons.Default.Restore,
                title = stringResource(R.string.settings_restore),
                subtitle = when {
                    importing -> stringResource(R.string.settings_restore_importing)
                    business == null -> stringResource(R.string.settings_export_no_book)
                    else -> stringResource(R.string.settings_restore_sub)
                },
                onClick = {
                    if (!importing && business != null) {
                        pickZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                    }
                }
            )

            SectionHeader(stringResource(R.string.settings_section_preferences))
            SettingsRow(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language),
                subtitle = when (currentLang) {
                    LocaleManager.Lang.SYSTEM -> stringResource(R.string.settings_language_system)
                    LocaleManager.Lang.ENGLISH -> stringResource(R.string.settings_language_english)
                    LocaleManager.Lang.URDU -> stringResource(R.string.settings_language_urdu)
                    LocaleManager.Lang.HINDI -> stringResource(R.string.settings_language_hindi)
                },
                onClick = { showLangPicker = true }
            )

            SectionHeader(stringResource(R.string.settings_section_about))
            SettingsRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_version),
                subtitle = "${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"
            )
            SettingsRow(
                icon = Icons.Default.Code,
                title = stringResource(R.string.settings_source_github),
                subtitle = "MianTaha0/digikhata-clone",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()))
                }
            )
            SettingsRow(
                icon = Icons.Default.Share,
                title = stringResource(R.string.settings_share_app),
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
                title = stringResource(R.string.settings_rate_app),
                subtitle = stringResource(R.string.settings_rate_app_sub),
                comingSoon = true
            )

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.settings_made_with),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    importResult?.let { r ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { importResult = null },
            title = { Text(stringResource(R.string.settings_import_complete)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_import_clients, r.clientsImported))
                    Text(stringResource(R.string.settings_import_transactions, r.transactionsImported))
                    Text(stringResource(R.string.settings_import_cash, r.cashImported))
                    Text(stringResource(R.string.settings_import_expenses, r.expensesImported))
                    if (r.errors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.settings_import_skipped, r.errors.size),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { importResult = null }) {
                    Text(stringResource(R.string.settings_ok))
                }
            }
        )
    }

    if (showLangPicker) {
        val options = listOf(
            LocaleManager.Lang.SYSTEM to stringResource(R.string.settings_language_system),
            LocaleManager.Lang.ENGLISH to stringResource(R.string.settings_language_english),
            LocaleManager.Lang.URDU to stringResource(R.string.settings_language_urdu),
            LocaleManager.Lang.HINDI to stringResource(R.string.settings_language_hindi),
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLangPicker = false },
            title = { Text(stringResource(R.string.settings_language_picker_title)) },
            text = {
                Column {
                    options.forEach { (lang, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentLang = lang
                                    LocaleManager.apply(lang)
                                    showLangPicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = currentLang == lang,
                                onClick = {
                                    currentLang = lang
                                    LocaleManager.apply(lang)
                                    showLangPicker = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showLangPicker = false }) {
                    Text(stringResource(R.string.settings_ok))
                }
            }
        )
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
