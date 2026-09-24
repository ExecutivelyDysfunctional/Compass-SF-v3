package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*

@Composable
fun SearchSettingsPage(
    viewModel: CompassViewModel
) {
    val excludeNonLoc = viewModel.excludeNonLocationResources.value
    val openOnly = viewModel.askOpenOnly.value
    val incognito = viewModel.incognitoSearchMode.value

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
                    Text("🔍 Directory Search & Filtering Preferences", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exclude Non-Location Helplines", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Hide phone-only / website-only resources that lack physical SF street addresses", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = excludeNonLoc,
                            onCheckedChange = { viewModel.toggleExcludeNonLocationResources(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Open-Now Filter", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Automatically filter search results to currently open services", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = openOnly,
                            onCheckedChange = { viewModel.askOpenOnly.value = it },
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
                    Text("🕵️ Ephemeral Search Privacy", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Incognito Search Mode", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Do not save recent search queries to local search log", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = incognito,
                            onCheckedChange = { viewModel.toggleIncognitoSearchMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.clearRecentSearches() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Mist200),
                        border = BorderStroke(1.dp, Ink700),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Recent Search Queries History", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
