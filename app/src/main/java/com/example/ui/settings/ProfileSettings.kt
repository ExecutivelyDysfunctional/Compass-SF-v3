package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.data.DemographicPreset
import com.example.data.DietaryPreset

@Composable
fun ProfileSettingsPage(
    viewModel: CompassViewModel
) {
    val demographicPresets = viewModel.demographicPresets.value
    val mobilityMode = viewModel.accessibilityMobilityMode.value
    val dietaryPresets = viewModel.dietaryPresets.value
    val excludeNonLocation = viewModel.excludeNonLocationResources.value

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
                    Text("Demographic Presets", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Highlight services catering to specific populations", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    DemographicPreset.entries.forEach { preset ->
                        val isSelected = demographicPresets.contains(preset)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleDemographicPreset(preset) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(preset.icon, fontSize = 16.sp)
                                Text(preset.title, color = Mist100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleDemographicPreset(preset) },
                                colors = CheckboxDefaults.colors(checkedColor = Beacon500)
                            )
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
                    Text("Accessibility & Mobility", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wheelchair & Mobility Access Only", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Only surface locations with verified ramp or elevator access", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = mobilityMode,
                            onCheckedChange = { viewModel.toggleAccessibilityMobilityMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
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
                    Text("Dietary Requirements", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    DietaryPreset.entries.forEach { preset ->
                        val isSelected = dietaryPresets.contains(preset)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleDietaryPreset(preset) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(preset.icon, fontSize = 16.sp)
                                Text(preset.title, color = Mist100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleDietaryPreset(preset) },
                                colors = CheckboxDefaults.colors(checkedColor = Beacon500)
                            )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exclude Non-Location Helplines", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Hide phone-only or online resources when browsing physical spots", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = excludeNonLocation,
                            onCheckedChange = { viewModel.toggleExcludeNonLocationResources(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.clearAllPresets() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                border = BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear All Presets & Preferences", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
