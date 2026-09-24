package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*

@Composable
fun AppearanceSettingsPage(
    viewModel: CompassViewModel
) {
    val activeMode = viewModel.currentThemeMode.value
    val activeAccent = viewModel.currentAccentColor.value
    val activeFontScale = viewModel.currentFontScale.value
    val highContrast = viewModel.highContrastEnabled.value
    val compactListing = viewModel.compactListingEnabled.value
    val activeAppIcon = viewModel.currentAppIconKey.value

    val iconChoices = listOf(
        "classic" to "Compass Rose",
        "beacon" to "Beacon Pulse",
        "bridge" to "Golden Gate",
        "lantern" to "Lantern Light",
        "wayfinder" to "Wayfinder"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP ANCHORED LIVE PREVIEW CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(
                    if (highContrast) 2.dp else 1.dp,
                    if (highContrast) Beacon500 else Ink700
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("appearance_live_preview_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("✨ LIVE PREVIEW", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Beacon500, letterSpacing = 1.sp)
                        }

                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(12.dp))
                                .border(1.dp, Ink700, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "${activeMode.displayName} • ${activeAccent.displayName} • ${activeFontScale.displayName}",
                                color = Mist100,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Ink800),
                        border = BorderStroke(1.dp, if (highContrast) Beacon500 else Ink700),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "GLIDE — Daily Free Meals",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Mist100
                                )
                                Text("❤️", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "330 Ellis St • Tenderloin • Breakfast 8am, Lunch 12pm",
                                color = Mist400,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Beacon500, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Food & Dining", color = OnAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Emerald500, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Open Now", color = OnAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor)
                        ) {
                            Text("Primary Action", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Ink700),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist200)
                        ) {
                            Text("Secondary Action", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Atmosphere / Theme Modes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Atmosphere & Theme Mode", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Select UI theme tone", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = activeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectThemeMode(mode) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectThemeMode(mode) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                                )
                                Column {
                                    Text(mode.displayName, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                                    Text(mode.subtitle, color = Mist400, fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Highlight Accent Colors
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Highlight Accent Colors", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Choose highlight color for buttons, badges, & indicators", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    AppAccentColor.entries.forEach { accent ->
                        val isSelected = activeAccent == accent
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectAccentColor(accent) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectAccentColor(accent) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                                )
                                Text(accent.displayName, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(accent.primary, CircleShape)
                                    .border(1.dp, Mist100.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Text Scale
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Text Scale & Readability", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Adjust font size for street legibility", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppFontScale.entries.forEach { scale ->
                            val isSelected = activeFontScale == scale
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Beacon500 else Ink800)
                                    .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectFontScale(scale) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    scale.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) OnAccentColor else Mist100
                                )
                            }
                        }
                    }
                }
            }
        }

        // Display Density & High Contrast
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Display Density & Contrast", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("High Contrast Mode", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Enforce strong borders & high-visibility text", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = highContrast,
                            onCheckedChange = { viewModel.toggleHighContrast(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Ink800)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Compact Directory Cards", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Show more listings per screen height", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = compactListing,
                            onCheckedChange = { viewModel.toggleCompactListing(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }

        // App Launcher Icon Branding
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Custom App Launcher Branding", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Text("Select your preferred icon aesthetic", color = Mist400, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    iconChoices.forEach { (key, name) ->
                        val isSelected = activeAppIcon == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectAppIcon(key) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectAppIcon(key) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                                )
                                Text(name, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Reset
        item {
            OutlinedButton(
                onClick = { viewModel.resetThemingToDefaults() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon500),
                border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Appearance Defaults", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
