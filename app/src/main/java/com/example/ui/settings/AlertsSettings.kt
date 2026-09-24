package com.example.ui.settings

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.data.NotificationHelper

@Composable
fun StreetRemindersSettingsSection(
    viewModel: CompassViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allReminders by viewModel.allReminders.collectAsState()
    val mealCutoffsEnabled by viewModel.alertsMealCutoffsEnabled
    val leadMinutes by viewModel.alertsLeadMinutes
    val morningBriefingEnabled by viewModel.alertsMorningBriefingEnabled
    val weatherShelterEnabled by viewModel.alertsWeatherShelterEnabled
    val soundVibrationEnabled by viewModel.alertsSoundVibrationEnabled

    var showAddDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled! Street alerts will sound.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied. Notifications will only appear in-app.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Ink900),
        border = BorderStroke(1.dp, Ink700),
        modifier = modifier
            .fillMaxWidth()
            .testTag("street_alerts_settings_section")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Beacon500.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ALERTS", color = Beacon400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }

                    Text(
                        "Reminders & Street Alerts",
                        fontWeight = FontWeight.Bold,
                        color = Beacon500,
                        fontSize = 15.sp
                    )
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Beacon500, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add custom reminder", tint = Ink950, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Timely push notifications and audio alerts for closing meal lines, shelter queues, and daily schedule briefings.",
                color = Mist400,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Check Permission Banner if not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !NotificationHelper.hasNotificationPermission(context)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Amber500.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, Amber500.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔔", fontSize = 14.sp)
                        Text(
                            "Allow notification permission to receive audio reminders",
                            fontSize = 11.sp,
                            color = Amber500
                        )
                    }
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Ink950),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 28.dp)
                    ) {
                        Text("Allow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Toggle 1: Meal Cutoff Alerts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Meal & Intake Cutoff Alerts", color = Mist100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Warn before lunch lines and intake lotteries close", color = Mist400, fontSize = 11.sp)
                }
                Switch(
                    checked = mealCutoffsEnabled,
                    onCheckedChange = { viewModel.setAlertsMealCutoffs(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Beacon500,
                        checkedTrackColor = Beacon500.copy(alpha = 0.3f),
                        uncheckedThumbColor = Mist400,
                        uncheckedTrackColor = Ink800
                    )
                )
            }

            // Lead time selector
            if (mealCutoffsEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Warning Lead Time:", color = Mist300, fontSize = 11.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(15, 30, 45, 60).forEach { min ->
                            val isSelected = leadMinutes == min
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) Beacon500 else Ink800)
                                    .clickable { viewModel.setAlertsLeadMinutes(min) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${min}m",
                                    color = if (isSelected) Ink950 else Mist300,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Ink800, modifier = Modifier.padding(vertical = 8.dp))

            // Toggle 2: Morning Street Briefing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Morning Navigator Digest", color = Mist100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Daily 8:00 AM summary of open meal sites & schedule", color = Mist400, fontSize = 11.sp)
                }
                Switch(
                    checked = morningBriefingEnabled,
                    onCheckedChange = { viewModel.setAlertsMorningBriefing(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Beacon500,
                        checkedTrackColor = Beacon500.copy(alpha = 0.3f),
                        uncheckedThumbColor = Mist400,
                        uncheckedTrackColor = Ink800
                    )
                )
            }

            HorizontalDivider(color = Ink800, modifier = Modifier.padding(vertical = 8.dp))

            // Toggle 3: Severe Weather & Cold Shelter Alerts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Severe Weather & Shelter Advisories", color = Mist100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Cold night shelter activations and rain drops", color = Mist400, fontSize = 11.sp)
                }
                Switch(
                    checked = weatherShelterEnabled,
                    onCheckedChange = { viewModel.setAlertsWeatherShelter(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Beacon500,
                        checkedTrackColor = Beacon500.copy(alpha = 0.3f),
                        uncheckedThumbColor = Mist400,
                        uncheckedTrackColor = Ink800
                    )
                )
            }

            HorizontalDivider(color = Ink800, modifier = Modifier.padding(vertical = 8.dp))

            // Toggle 4: Sound & Vibration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sound & Vibration", color = Mist100, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Enable haptic buzz and system notification sound", color = Mist400, fontSize = 11.sp)
                }
                Switch(
                    checked = soundVibrationEnabled,
                    onCheckedChange = { viewModel.setAlertsSoundVibration(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Beacon500,
                        checkedTrackColor = Beacon500.copy(alpha = 0.3f),
                        uncheckedThumbColor = Mist400,
                        uncheckedTrackColor = Ink800
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Alert Trigger Buttons
            Text("Send Instant Test Alerts:", color = Beacon400, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.testSendMealAlert(context)
                        Toast.makeText(context, "Test Meal alert triggered!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Ink800, contentColor = Mist100),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("🍱 Test Meal", fontSize = 11.sp, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = {
                        viewModel.testSendMorningBriefing(context)
                        Toast.makeText(context, "Test Morning Briefing triggered!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Ink800, contentColor = Mist100),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("🧭 Briefing", fontSize = 11.sp, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = {
                        viewModel.testSendTaskReminder(context)
                        Toast.makeText(context, "Test Task Reminder triggered!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Ink800, contentColor = Mist100),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("📋 Task", fontSize = 11.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Reminders List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Active Street Reminders (${allReminders.size})",
                    color = Mist100,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                if (allReminders.isEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.seedDefaultStreetReminders()
                            Toast.makeText(context, "Loaded common SF meal cutoff reminders", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Seed Common Deadlines", color = Beacon400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (allReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Ink800, RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No custom street reminders configured.", color = Mist400, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap '+' above or 'Seed Common Deadlines' to preload St. Anthony's and Glide meal alerts.", color = Mist400, fontSize = 11.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allReminders.forEach { rem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Ink800, RoundedCornerShape(8.dp))
                                .border(1.dp, if (rem.enabled) Ink700 else Ink800, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = rem.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (rem.enabled) Mist100 else Mist400
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(if (rem.enabled) Beacon500.copy(alpha = 0.2f) else Ink900, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = rem.targetTimeText,
                                            color = if (rem.enabled) Beacon400 else Mist400,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (rem.description.isNotBlank()) {
                                    Text(
                                        text = rem.description,
                                        fontSize = 11.sp,
                                        color = Mist400,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = rem.enabled,
                                    onCheckedChange = { viewModel.toggleReminder(rem) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Beacon500,
                                        checkedTrackColor = Beacon500.copy(alpha = 0.3f),
                                        uncheckedThumbColor = Mist400,
                                        uncheckedTrackColor = Ink900
                                    )
                                )
                                IconButton(
                                    onClick = { viewModel.deleteReminder(rem) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Mist400, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStreetReminderDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}
