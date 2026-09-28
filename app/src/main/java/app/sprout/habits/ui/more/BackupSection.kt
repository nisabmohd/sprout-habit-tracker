package app.sprout.habits.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.sprout.habits.R
import app.sprout.habits.data.backup.BackupException
import app.sprout.habits.data.backup.BackupFile
import app.sprout.habits.data.backup.BackupManager
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** "Export to file" and "Import from file" rows, through the system file picker (no storage permission). */
@Composable
fun BackupRows(backup: BackupManager, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<BackupFile?>(null) }
    var choosingFormat by remember { mutableStateOf(false) }

    fun write(uri: Uri?, block: suspend (java.io.OutputStream) -> Unit, done: String) {
        uri ?: return
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { block(it) }
                }
            }
            onMessage(if (result.isSuccess) done else context.getString(R.string.save_failed))
        }
    }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        write(uri, { backup.exportJson(it) }, context.getString(R.string.backup_saved))
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        write(uri, { backup.exportCsv(it) }, context.getString(R.string.csv_saved))
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                pending = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { BackupManager.parse(it) }
                }
            } catch (e: BackupException) {
                onMessage(context.backupError(e))
            } catch (e: java.io.IOException) {
                onMessage(context.getString(R.string.read_failed))
            }
        }
    }

    SettingsRow(stringResource(R.string.export_to_file), stringResource(R.string.export_desc), icon = R.drawable.ic_download, onClick = { choosingFormat = true })
    CardDivider()
    SettingsRow(
        stringResource(R.string.import_from_file),
        stringResource(R.string.import_desc),
        icon = R.drawable.ic_upload,
        onClick = { import.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
    )

    if (choosingFormat) {
        AlertDialog(
            onDismissRequest = { choosingFormat = false },
            title = { Text(stringResource(R.string.export_to_file)) },
            text = { Text(stringResource(R.string.export_dialog_text)) },
            confirmButton = {
                TextButton(onClick = { choosingFormat = false; exportJson.launch("sprout-backup-${LocalDate.now()}.json") }) { Text(stringResource(R.string.json_backup)) }
            },
            dismissButton = {
                TextButton(onClick = { choosingFormat = false; exportCsv.launch("sprout-${LocalDate.now()}.csv") }) { Text(stringResource(R.string.csv)) }
            },
        )
    }

    pending?.let { file ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.replace_data_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.replace_data_text,
                        pluralStringResource(R.plurals.habit_count, file.habits.size, file.habits.size),
                        pluralStringResource(R.plurals.logged_day_count, file.entries.size, file.entries.size),
                        pluralStringResource(R.plurals.note_count, file.notes.size, file.notes.size),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        try {
                            backup.restore(file)
                            onMessage(context.getString(R.string.backup_restored))
                        } catch (e: BackupException) {
                            onMessage(context.backupError(e))
                        } catch (e: android.database.sqlite.SQLiteException) {
                            onMessage(context.getString(R.string.restore_failed))
                        }
                    }
                }) { Text(stringResource(R.string.replace)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun android.content.Context.backupError(e: BackupException): String = when (e.reason) {
    BackupException.Reason.DAMAGED -> getString(R.string.backup_error_damaged)
    BackupException.Reason.NOT_SPROUT -> getString(R.string.backup_error_not_sprout)
    BackupException.Reason.NEWER -> getString(R.string.backup_error_newer)
    BackupException.Reason.BAD_DATE -> getString(R.string.backup_error_date, e.detail)
}
