package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings // wait, or import everything
import androidx.compose.material.icons.filled.*
import com.example.ui.*

@Composable
fun SettingsHubContent(
    viewModel: CompassViewModel,
    onSelectCategory: (SettingsSubPage) -> Unit
) {
    val activeMode = viewModel.currentThemeMode.value
    val activeAccent = viewModel.currentAccentColor.value
    val activeFontScale = viewModel.currentFontScale.value
    val demographicPresets = viewModel.demographicPresets.value
    val mobilityMode = viewModel.accessibilityMobilityMode.value
    val dietaryPresets = viewModel.dietaryPresets.value
    val startupRoute = viewModel.startupScreenRoute.value
    val defaultAnchor = viewModel.defaultNeighborhoodAnchorId.value
    val aiStyle = viewModel.aiNavigatorStyle.value
    val aiOffline = viewModel.aiOfflineOnly.value
    val incognito = viewModel.incognitoSearchMode.value
    val cacheBytes = viewModel.totalPhotoCacheBytes.value

    val subPages = listOf(
        Triple(
            SettingsSubPage.APPEARANCE,
            "${activeMode.displayName} • ${activeAccent.displayName} • ${viewModel.currentAppIconKey.value.uppercase()}",
            "Live preview card, color themes, font scaling & launcher icon switcher"
        ),
        Triple(
            SettingsSubPage.WORKSPACE,
            "Start: ${startupRoute.replaceFirstChar { it.uppercase() }} • Anchor: ${defaultAnchor.replaceFirstChar { it.uppercase() }}",
            "Startup screen, bottom navigation bar active items & neighborhood anchor"
        ),
        Triple(
            SettingsSubPage.PERSONALIZATION,
            "${demographicPresets.size} Presets • ${if (mobilityMode) "Mobility On" else "Standard"} • ${dietaryPresets.size} Dietary",
            "Primary need priorities, demographic filters, dietary rules & mobility mode"
        ),
        Triple(
            SettingsSubPage.SEARCH,
            "Incognito: ${if (incognito) "ENABLED" else "Off"} • Physical Address Filters",
            "Open-only search defaults, non-location helpline filters & query history"
        ),
        Triple(
            SettingsSubPage.MAP,
            "Transit, Neighborhoods & Landmarks Layers",
            "Vector map overlays, marker clustering density & reduced motion"
        ),
        Triple(
            SettingsSubPage.AI,
            "${aiStyle.title} • ${if (aiOffline) "Offline Gemini" else "Online Gemini"}",
            "Guide persona tone, online/offline Gemini mode, API key & server test"
        ),
        Triple(
            SettingsSubPage.DATA,
            "Photo Cache: ${(cacheBytes / 1024 / 1024)}MB • Feed Toggles & JSON Backup",
            "Civic feed toggles, offline photo cache, factory re-seed & JSON export/import"
        ),
        Triple(
            SettingsSubPage.ABOUT,
            "Compass SF v3.0.0 • 100% Local Privacy",
            "App version info, open data sources, local storage guarantee & emergency hotlines"
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(subPages) { (page, summaryText, description) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink900),
                border = BorderStroke(1.dp, Ink700),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCategory(page) }
                    .testTag("settings_category_${page.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Beacon500.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.title,
                            tint = Beacon500,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(page.emoji, fontSize = 14.sp)
                            Text(
                                page.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Mist100
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Box(
                            modifier = Modifier
                                .background(Ink800, RoundedCornerShape(6.dp))
                                .border(1.dp, Ink700, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                summaryText,
                                color = Beacon400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            description,
                            color = Mist400,
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open ${page.title}",
                        tint = Mist400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
