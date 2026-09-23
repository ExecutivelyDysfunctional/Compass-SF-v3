package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

/**
 * Live Street Alert Banner displayed in the Now Screen.
 * Evaluates real-time closing cutoffs, severe weather notices, and impending reminders.
 */
@Composable
fun StreetAlertsBanner(
    viewModel: CompassViewModel,
    onNavigateToFind: (String) -> Unit = {},
    onOpenReminderDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allResources by viewModel.allResources.collectAsState()
    val allReminders by viewModel.allReminders.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    val mealCutoffsEnabled by viewModel.alertsMealCutoffsEnabled
    val leadMinutes by viewModel.alertsLeadMinutes
    val weatherShelterEnabled by viewModel.alertsWeatherShelterEnabled

    // Compute live alerts
    val alerts = remember(
        allResources,
        allReminders,
        allTasks,
        mealCutoffsEnabled,
        leadMinutes,
        weatherShelterEnabled
    ) {
        StreetAlertEngine.evaluateActiveAlerts(
            allResources = allResources,
            allReminders = allReminders,
            allTasks = allTasks,
            alertsMealCutoffsEnabled = mealCutoffsEnabled,
            leadMinutes = leadMinutes,
            alertsWeatherShelterEnabled = weatherShelterEnabled
        )
    }

    if (alerts.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        alerts.forEach { alert ->
            val bgColor = when (alert.severity) {
                "critical" -> Rose500.copy(alpha = 0.18f)
                "warning" -> Amber500.copy(alpha = 0.18f)
                else -> Beacon500.copy(alpha = 0.15f)
            }
            val borderColor = when (alert.severity) {
                "critical" -> Rose500
                "warning" -> Amber500
                else -> Beacon500
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("street_alert_card_${alert.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(borderColor.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(alert.icon, fontSize = 16.sp)
                            }
                            Column {
                                Text(
                                    text = alert.title,
                                    color = Mist100,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                alert.resourceName?.let { rName ->
                                    Text(
                                        text = rName,
                                        color = Beacon400,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        alert.cutoffTime?.let { cutoff ->
                            Box(
                                modifier = Modifier
                                    .background(borderColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, borderColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = cutoff,
                                    color = Mist100,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = alert.message,
                        color = Mist200,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (alert.category == "food" || alert.category == "meal") {
                            TextButton(
                                onClick = { onNavigateToFind("food") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                            ) {
                                Text("View Open Meals →", color = Beacon400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = {
                                NotificationHelper.sendNotification(
                                    context = context,
                                    channelId = NotificationHelper.CHANNEL_MEALS,
                                    title = alert.title,
                                    message = alert.message,
                                    bigText = "${alert.title}\n${alert.message}\nResource: ${alert.resourceName ?: "SF Street Service"}"
                                )
                                Toast.makeText(context, "Alert pushed to notification shade", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(0.5.dp, Ink700),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(13.dp), tint = Mist300)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Notify Me", color = Mist300, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Morning Street Briefing Card for the Now Screen.
 */
@Composable
fun MorningBriefingCard(
    viewModel: CompassViewModel,
    modifier: Modifier = Modifier
) {
    val allResources by viewModel.allResources.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val morningBriefingEnabled by viewModel.alertsMorningBriefingEnabled
    val anchorNeighborhood by viewModel.defaultNeighborhoodAnchorId

    if (!morningBriefingEnabled) return

    val briefing = remember(allResources, allTasks, anchorNeighborhood) {
        StreetAlertEngine.generateMorningBriefing(
            allResources = allResources,
            allTasks = allTasks,
            anchorNeighborhood = anchorNeighborhood
        )
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Ink900),
        border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("morning_briefing_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Beacon500.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧭", fontSize = 15.sp)
                    }
                    Column {
                        Text(
                            text = "MORNING STREET BRIEFING",
                            color = Beacon500,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = briefing.headline,
                            color = Mist100,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle briefing details",
                        tint = Mist400
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = briefing.weatherSummary,
                color = Mist300,
                fontSize = 11.5.sp
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = Ink800)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Key Meal Programs Today:",
                        color = Beacon400,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    briefing.openKeyServices.take(4).forEach { svc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${svc.name}",
                                color = Mist100,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = svc.hoursText,
                                color = Mist400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (briefing.urgentTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Checklist Reminders (${briefing.urgentTasks.size}):",
                            color = Amber500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        briefing.urgentTasks.take(3).forEach { task ->
                            Text(
                                text = "⏱️ ${task.title}",
                                color = Mist200,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Add / Edit Street Reminder Dialog.
 */
@Composable
fun AddStreetReminderDialog(
    viewModel: CompassViewModel,
    onDismiss: () -> Unit,
    prefillResource: Resource? = null
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(if (prefillResource != null) "${prefillResource.name} Cutoff" else "") }
    var description by remember { mutableStateOf(if (prefillResource != null) "Closing time alert for ${prefillResource.name}" else "") }
    var reminderType by remember { mutableStateOf(if (prefillResource?.category == "food") "meal" else "custom") }
    var targetTimeText by remember { mutableStateOf(if (prefillResource != null && prefillResource.hoursText.isNotBlank()) prefillResource.hoursText else "1:30 PM") }
    var selectedLeadMinutes by remember { mutableIntStateOf(30) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission required to trigger street sound alerts.", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ink900,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = Beacon500)
                Text(
                    text = if (prefillResource != null) "Set Service Alert" else "New Street Reminder",
                    color = Beacon500,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title", color = Mist400) },
                    placeholder = { Text("e.g. St. Anthony's Lunch Closes", color = Mist400) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Beacon500,
                        unfocusedBorderColor = Ink700,
                        focusedTextColor = Mist100,
                        unfocusedTextColor = Mist100
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details & Notes", color = Mist400) },
                    placeholder = { Text("e.g. Dining line closes at 1:30 PM", color = Mist400) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Beacon500,
                        unfocusedBorderColor = Ink700,
                        focusedTextColor = Mist100,
                        unfocusedTextColor = Mist100
                    )
                )

                // Category selector
                Text("Category", fontSize = 12.sp, color = Mist300, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val types = listOf("meal" to "🍱 Meal Cutoff", "task" to "📋 Task / Errand", "custom" to "⏰ Custom")
                    types.forEach { (typeId, label) ->
                        val isSelected = reminderType == typeId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Beacon500 else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(6.dp))
                                .clickable { reminderType = typeId }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Ink950 else Mist200,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Target time text
                OutlinedTextField(
                    value = targetTimeText,
                    onValueChange = { targetTimeText = it },
                    label = { Text("Target Closing / Appointment Time", color = Mist400) },
                    placeholder = { Text("e.g. 1:30 PM", color = Mist400) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Beacon500,
                        unfocusedBorderColor = Ink700,
                        focusedTextColor = Mist100,
                        unfocusedTextColor = Mist100
                    )
                )

                // Lead Time Selector
                Text("Alert Lead Time (Advance Notice)", fontSize = 12.sp, color = Mist300, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val options = listOf(15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "60 min")
                    options.forEach { (minutes, label) ->
                        val isSelected = selectedLeadMinutes == minutes
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Beacon500 else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(6.dp))
                                .clickable { selectedLeadMinutes = minutes }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Ink950 else Mist200,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !NotificationHelper.hasNotificationPermission(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        viewModel.addReminder(
                            title = title.trim(),
                            description = description.trim(),
                            reminderType = reminderType,
                            resourceId = prefillResource?.id,
                            resourceName = prefillResource?.name ?: title.trim(),
                            targetTimeText = targetTimeText.trim(),
                            leadMinutes = selectedLeadMinutes
                        )
                        Toast.makeText(context, "Street reminder created!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Please provide a title", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = Ink950),
                modifier = Modifier.testTag("save_reminder_button")
            ) {
                Text("Save Reminder", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Ink700),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist200)
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Reminders & Alerts Management Section in the Settings Screen.
 */
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
