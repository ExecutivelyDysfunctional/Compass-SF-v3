package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.data.AiNavigatorStyle

@Composable
fun AiSettingsPage(
    viewModel: CompassViewModel
) {
    val aiStyle = viewModel.aiNavigatorStyle.value
    val aiOffline = viewModel.aiOfflineOnly.value
    val customKey = viewModel.aiCustomApiKey.value
    val customEndpoint = viewModel.aiCustomEndpoint.value
    val connectionStatus = viewModel.aiConnectionStatus.value
    val isTesting = viewModel.isTestingAiConnection.value

    var keyInput by remember(customKey) { mutableStateOf(customKey) }
    var endpointInput by remember(customEndpoint) { mutableStateOf(customEndpoint) }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🤖", fontSize = 20.sp)
                        Text("AI Navigator & Gemini API Integration", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Compass SF uses online Gemini AI as its primary experience to deliver intelligent recommendations, natural language queries, and street flyer parsing. If offline or without an API key, local keyword matching acts as a graceful fallback.",
                        color = Mist300,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Response Style Persona
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Guide Persona & Response Style", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                    Text("Customize how the AI assistant frames recommendations and guidance", color = Mist400, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    AiNavigatorStyle.entries.forEach { style ->
                        val isSelected = aiStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setAiNavigatorStyle(style) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(style.title, fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                                Text(style.description, color = Mist400, fontSize = 11.sp)
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setAiNavigatorStyle(style) },
                                colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Online / Offline Control
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connectivity & Offline Mode Control", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Force Offline-Only Mode", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Never contact Gemini API; force local keyword search fallback", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = aiOffline,
                            onCheckedChange = { viewModel.setAiOfflineOnly(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }

        // Custom API Key & Endpoint
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Custom API Key & Server Endpoint", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 14.sp)
                    Text("Optional custom Gemini API credentials or server proxy", color = Mist400, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            viewModel.setAiCustomApiKey(it)
                        },
                        label = { Text("Custom Gemini API Key", fontSize = 12.sp) },
                        placeholder = { Text("Enter AI Studio or Gemini API key", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Beacon500,
                            unfocusedBorderColor = Ink700,
                            focusedTextColor = Mist100,
                            unfocusedTextColor = Mist100
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = {
                            endpointInput = it
                            viewModel.setAiCustomEndpoint(it)
                        },
                        label = { Text("Custom API Base Endpoint", fontSize = 12.sp) },
                        placeholder = { Text("https://generativelanguage.googleapis.com", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Beacon500,
                            unfocusedBorderColor = Ink700,
                            focusedTextColor = Mist100,
                            unfocusedTextColor = Mist100
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.testAiConnection() },
                        enabled = !isTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnAccentColor, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Connection...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Gemini API Connection", fontSize = 12.sp)
                        }
                    }

                    if (!connectionStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Ink800, RoundedCornerShape(6.dp))
                                .border(1.dp, Ink700, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(connectionStatus, color = Mist100, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Reset
        item {
            OutlinedButton(
                onClick = {
                    viewModel.resetAiSettings()
                    keyInput = ""
                    endpointInput = ""
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Beacon500),
                border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset AI Settings to Defaults", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
