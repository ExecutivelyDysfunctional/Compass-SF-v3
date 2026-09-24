package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*

@Composable
fun PrivacyInfoSection() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Ink900),
        border = BorderStroke(1.dp, Ink700),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🛡️ Local-First Privacy Protection", fontWeight = FontWeight.Bold, color = Mist100, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "All personal data—including starred favorites, notes, checklists, search queries, and alerts—is stored entirely on your local device. No accounts, no clouds, and absolutely no remote tracking.",
                color = Mist300,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}
