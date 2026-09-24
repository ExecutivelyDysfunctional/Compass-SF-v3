package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*

@Composable
fun NavigationSettingsPage(
    viewModel: CompassViewModel
) {
    val defaultTab = viewModel.startupScreenRoute.value
    val resources = viewModel.allResources.collectAsState(initial = emptyList()).value
    val rmpLocations = viewModel.allRmpLocations.collectAsState(initial = emptyList()).value

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
                    Text("🧭 Workspace Navigation", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Startup Screen", color = Mist300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink800, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink700, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("Default startup route: $defaultTab", color = Mist100, fontSize = 12.sp)
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
                    Text("Civic Data Sources", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    DataSourceToggleCard(
                        title = "SF OpenData Directory",
                        description = "Verified community shelters, meal sites, and clinics",
                        count = resources.size,
                        badge = "Primary Feed",
                        checked = true,
                        onCheckedChange = {}
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DataSourceToggleCard(
                        title = "EBT Restaurant Meals Program (RMP)",
                        description = "CalFresh hot food vendors in San Francisco",
                        count = rmpLocations.size,
                        badge = "CalFresh RMP",
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.resetWorkspaceNavigationToDefaults() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon500),
                border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Navigation Defaults", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DataSourceToggleCard(
    title: String,
    description: String,
    count: Int,
    badge: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (checked) Ink950 else Ink900, RoundedCornerShape(8.dp))
            .border(1.dp, if (checked) Ink800 else Ink700.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (checked) Mist100 else Mist400
                )
                Box(
                    modifier = Modifier
                        .background(if (checked) Beacon500.copy(alpha = 0.15f) else Ink800, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        badge,
                        color = if (checked) Beacon400 else Mist400,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 11.sp, color = Mist400)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$count entries available in directory", fontSize = 10.sp, color = if (checked) Emerald500 else Mist400)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Beacon500,
                checkedTrackColor = Beacon500.copy(alpha = 0.3f),
                uncheckedThumbColor = Mist400,
                uncheckedTrackColor = Ink800
            )
        )
    }
}
