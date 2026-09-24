package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*

@Composable
fun AboutHelpSettingsPage(
    viewModel: CompassViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Beacon500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Explore, contentDescription = null, tint = Beacon500, modifier = Modifier.size(32.dp))
                        Column {
                            Text("Compass SF", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 18.sp)
                            Text("v3.0.0 • Local Personal Civic Navigator", color = Beacon400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Designed specifically for San Francisco residents, unhoused community members, social workers, and street navigators. Provides fast, offline-ready access to daily essential services with intelligent AI search.",
                        color = Mist300,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
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
                    Text("📊 Open Civic Data Sources", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• ShelterTech SF Service Guide — Community-verified shelters, meals & hygiene", color = Mist300, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• DataSF Open Data Portal — City & County of San Francisco public facilities", color = Mist300, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• CalFresh RMP — Verified EBT hot food restaurant vendor directory", color = Mist300, fontSize = 12.sp)
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
                    Text("🛡️ Privacy & Storage Architecture", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "100% Private & Local-Only. Your notes, checklist tasks, search history, and relevance profile never leave your phone. There are no remote user accounts or profile tracking.",
                        color = Mist300,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
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
                    Text("🚨 Emergency & Street Hotlines", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    HotlineRow("988 Suicide & Crisis Lifeline", "988", "24/7 free & confidential emotional support")
                    Spacer(modifier = Modifier.height(8.dp))
                    HotlineRow("SF City Services (311)", "311", "San Francisco non-emergency city services")
                    Spacer(modifier = Modifier.height(8.dp))
                    HotlineRow("211 Bay Area Help", "211", "Community health and human services referral")
                }
            }
        }
    }
}

@Composable
fun HotlineRow(title: String, phone: String, desc: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink800, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Mist100, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Mist400, fontSize = 10.5.sp)
        }
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Beacon500, contentColor = OnAccentColor),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("Call $phone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
