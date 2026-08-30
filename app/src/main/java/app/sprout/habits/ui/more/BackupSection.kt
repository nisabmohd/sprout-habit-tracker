package app.sprout.habits.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.sprout.habits.data.backup.BackupException
import app.sprout.habits.data.backup.BackupFile
import app.sprout.habits.data.backup.BackupManager
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Export to JSON or CSV, and import JSON, through the system file picker (no storage permission). */
@Composable
fun BackupSection(backup: BackupManager, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<BackupFile?>(null) }

    fun write(uri: Uri?, block: suspend (java.io.OutputStream) -> Unit, done: String) {
        uri ?: return
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { block(it) }
                }
            }
            onMessage(if (result.isSuccess) done else "Couldn't save the file.")
        }
    }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        write(uri, { backup.exportJson(it) }, "Backup saved")
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        write(uri, { backup.exportCsv(it) }, "CSV saved")
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                pending = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { BackupManager.parse(it) }
                }
            } catch (e: BackupException) {
                onMessage(e.message ?: "Couldn't read the file.")
            } catch (e: java.io.IOException) {
                onMessage("Couldn't read the file.")
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Backup & restore", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Save everything to a file you keep, or restore from one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { exportJson.launch("sprout-backup-${LocalDate.now()}.json") }, modifier = Modifier.weight(1f)) {
                Text("Export")
            }
            OutlinedButton(onClick = { import.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }, modifier = Modifier.weight(1f)) {
                Text("Import")
            }
        }
        OutlinedButton(onClick = { exportCsv.launch("sprout-${LocalDate.now()}.csv") }, modifier = Modifier.fillMaxWidth()) {
            Text("Export CSV for spreadsheets")
        }
    }

    pending?.let { file ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Replace your data?") },
            text = {
                Text(
                    "This backup has ${count(file.habits.size, "habit")}, ${count(file.entries.size, "logged day")} and " +
                        "${count(file.notes.size, "note")}. " +
                        "Importing replaces everything in Sprout now.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        try {
                            backup.restore(file)
                            onMessage("Backup restored")
                        } catch (e: BackupException) {
                            onMessage(e.message ?: "Couldn't restore the backup.")
                        } catch (e: android.database.sqlite.SQLiteException) {
                            onMessage("Couldn't restore the backup.")
                        }
                    }
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
}

private fun count(n: Int, noun: String) = if (n == 1) "1 $noun" else "$n ${noun}s"
