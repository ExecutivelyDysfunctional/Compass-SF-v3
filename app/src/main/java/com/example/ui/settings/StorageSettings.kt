package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.data.Resource
import com.example.data.RmpLocation
import com.example.data.Visit
import com.example.data.Task

@Composable
fun StorageSettingsPage(
    viewModel: CompassViewModel,
    resources: List<Resource> = emptyList(),
    rmpLocations: List<RmpLocation> = emptyList(),
    visits: List<Visit> = emptyList(),
    tasks: List<Task> = emptyList(),
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onOpenPasteJson: () -> Unit = {},
    onOpenPhotoAudit: () -> Unit = {},
    onOpenReseedConfirm: () -> Unit = {}
) {
    val hiddenResources = remember(resources) { resources.filter { it.hidden } }
    val hiddenRmp = remember(rmpLocations) { rmpLocations.filter { it.hidden } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Granular JSON Backup & Export", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val exportSel = viewModel.exportSelection.value
                    BackupCheckboxRow("Starred Favorites", exportSel.includeFavorites) {
                        viewModel.setExportEntitySelected("favorites", it)
                    }
                    BackupCheckboxRow("Personal Notes", exportSel.includeNotes) {
                        viewModel.setExportEntitySelected("notes", it)
                    }
                    BackupCheckboxRow("Visit Logs & Field Reviews", exportSel.includeVisits) {
                        viewModel.setExportEntitySelected("visits", it)
                    }
                    BackupCheckboxRow("Day Checklist Tasks", exportSel.includeTasks) {
                        viewModel.setExportEntitySelected("tasks", it)
                    }
                    BackupCheckboxRow("Custom Saved Places", exportSel.includeCustomPlaces) {
                        viewModel.setExportEntitySelected("custom", it)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup File (.json)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onImportClick,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100)
                        ) {
                            Text("Open JSON File", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenPasteJson,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist100)
                        ) {
                            Text("Paste Raw Text", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔄 Baseline Database Re-seed", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Restores all SF civic directory entries and EBT hot meal spots to initial baseline state.",
                        color = Mist400,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onOpenReseedConfirm,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon500),
                        border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Re-seed Civic Database", fontSize = 12.sp)
                    }
                }
            }
        }

        if (hiddenResources.isNotEmpty() || hiddenRmp.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Ink900),
                    border = BorderStroke(1.dp, Ink700),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hidden Items (${hiddenResources.size + hiddenRmp.size})", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        hiddenResources.forEach { res ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(res.name, color = Mist100, fontSize = 12.sp)
                                TextButton(onClick = { viewModel.toggleResourceHidden(res) }) {
                                    Text("Unhide", color = Beacon500, fontSize = 11.sp)
                                }
                            }
                        }

                        hiddenRmp.forEach { rmp ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${rmp.name} (EBT)", color = Mist100, fontSize = 12.sp)
                                TextButton(onClick = { viewModel.toggleRmpHidden(rmp) }) {
                                    Text("Unhide", color = Beacon500, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupCheckboxRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (enabled) (if (checked) Mist100 else Mist300) else Mist400.copy(alpha = 0.5f),
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
        )
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = Beacon500,
                checkmarkColor = Ink950,
                uncheckedColor = Mist400
            ),
            modifier = Modifier.size(24.dp)
        )
    }
}
