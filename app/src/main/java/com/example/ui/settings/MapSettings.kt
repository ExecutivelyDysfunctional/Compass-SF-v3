package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*

@Composable
fun MapSettingsPage(
    viewModel: CompassViewModel
) {
    val transit = viewModel.mapLayerTransit.value
    val neighborhoods = viewModel.mapLayerNeighborhoods.value
    val landmarks = viewModel.mapLayerLandmarks.value
    val clustering = viewModel.mapClusteringMode.value
    val reducedMotion = viewModel.mapReducedMotion.value

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
                    Text("🗺️ Map Visual Layers", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Transit Lines & Stops", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Show Muni bus, BART, and light rail corridors", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = transit,
                            onCheckedChange = { viewModel.toggleMapLayerTransit(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Neighborhood Boundaries", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Highlight Tenderloin, Mission, SoMa & Civic Center boundaries", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = neighborhoods,
                            onCheckedChange = { viewModel.toggleMapLayerNeighborhoods(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("City Landmarks", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Display key reference points like City Hall, BART stations & parks", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = landmarks,
                            onCheckedChange = { viewModel.toggleMapLayerLandmarks(it) },
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
                    Text("Density & Motion Settings", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Pin Clustering Mode", fontSize = 12.sp, color = Mist300, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    MapClusteringMode.entries.forEach { mode ->
                        val isSelected = clustering == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Beacon500.copy(alpha = 0.18f) else Ink800)
                                .border(1.dp, if (isSelected) Beacon500 else Ink700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectMapClusteringMode(mode) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mode.displayName, color = Mist100, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectMapClusteringMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = Beacon500)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reduced Motion", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 13.sp)
                            Text("Disable map pan & zoom animations for reduced visual stress", color = Mist400, fontSize = 11.sp)
                        }
                        Switch(
                            checked = reducedMotion,
                            onCheckedChange = { viewModel.toggleMapReducedMotion(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Beacon500)
                        )
                    }
                }
            }
        }
    }
}
