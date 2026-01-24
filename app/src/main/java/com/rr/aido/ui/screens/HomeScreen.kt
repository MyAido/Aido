package com.rr.aido.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.aido.R
import com.rr.aido.utils.AccessibilityUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayground: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToPreprompts: () -> Unit,
    onNavigateToSpecialCommands: () -> Unit,
    onNavigateToTextShortcuts: () -> Unit,
    onNavigateToFeaturesSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Status states - Initialize with actual values to prevent flickering
    var isAccessibilityEnabled by remember { mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context)) }
    var isKeyboardEnabled by remember { mutableStateOf(AccessibilityUtils.isAidoKeyboardEnabled(context)) }
    var isKeyboardActive by remember { mutableStateOf(AccessibilityUtils.isAidoKeyboardActive(context)) }

    // Refresh status when app comes to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
                isKeyboardEnabled = AccessibilityUtils.isAidoKeyboardEnabled(context)
                isKeyboardActive = AccessibilityUtils.isAidoKeyboardActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Modern Gradient Background (Same as SettingsScreen)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                item {
                    Column {
                        HomeTopBar(onSettingsClick = onNavigateToSettings)
                        HomeHeader()
                    }
                }

                // Status Dashboard
                item {
                    StatusDashboard(
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isKeyboardActive = isKeyboardActive,
                        context = context
                    )
                }

                // Features Showcase
                item {
                    FeaturesShowcaseSection(
                        onNavigateToSpecialCommands = onNavigateToSpecialCommands,
                        onNavigateToPreprompts = onNavigateToPreprompts,
                        onNavigateToTextShortcuts = onNavigateToTextShortcuts,
                        onNavigateToFeaturesSettings = onNavigateToFeaturesSettings
                    )
                }

                // Quick Actions
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        QuickActionsGrid(onNavigateToPlayground, onNavigateToChat)
                    }
                }

                // Support
                item {
                    SupportSection(context)
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    // Animated alpha for pulsing effect
    val infiniteTransition = rememberInfiniteTransition(label = "title_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title with gradient and animation
        Text(
            text = "Aido",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 40.sp,
                letterSpacing = 1.sp,
                lineHeight = 40.sp,
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                )
            ),
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        )

        // Settings Button
        Surface(
            onClick = onSettingsClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Hello there,",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ready to type?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusDashboard(
    isAccessibilityEnabled: Boolean,
    isKeyboardActive: Boolean,
    context: android.content.Context
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Accessibility Status
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Accessibility",
                isActive = isAccessibilityEnabled,
                icon = Icons.Outlined.Build,
                onClick = {
                     val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                     context.startActivity(intent)
                }
            )

            // Keyboard Status
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Keyboard",
                isActive = isKeyboardActive,
                icon = Icons.Outlined.Keyboard,
                onClick = {
                    AccessibilityUtils.showKeyboardPicker(context)
                }
            )
        }
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    isActive: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    // Glassmorphic styling similar to Settings PremiumStatusCard
    val containerColor = if (isActive)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    else
        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)

    val contentColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) "Active" else "Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }

            // Icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun FeaturesShowcaseSection(
    onNavigateToSpecialCommands: () -> Unit,
    onNavigateToPreprompts: () -> Unit,
    onNavigateToTextShortcuts: () -> Unit,
    onNavigateToFeaturesSettings: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "My Triggers",
                    description = "Custom commands (@fixg...)",
                    icon = Icons.Outlined.Create,
                    onClick = onNavigateToPreprompts,
                    iconTint = MaterialTheme.colorScheme.primary
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Special Commands",
                    description = "Smart Reply, Tone Rewrite...",
                    icon = Icons.Default.AutoAwesome,
                    onClick = onNavigateToSpecialCommands,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                 FeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Text Shortcuts",
                    description = "!mail -> snippets",
                    icon = Icons.Outlined.Keyboard,
                    onClick = onNavigateToTextShortcuts,
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Visual Feedback",
                    description = "AI processing effects",
                    icon = Icons.Default.Bolt,
                    onClick = onNavigateToFeaturesSettings,
                    iconTint = MaterialTheme.colorScheme.error // Or another color
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: Color
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) // Glass feel relative to background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Top Left
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }

            // Text Bottom
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onPlaygroundClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Playground Card
        QuickActionCard(
            modifier = Modifier.weight(1f),
            title = "Playground",
            subtitle = "Test prompts",
            icon = Icons.Default.Star,
            onClick = onPlaygroundClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        // Chat Card
        QuickActionCard(
            modifier = Modifier.weight(1f),
            title = "AI Chat",
            subtitle = "Ask anything",
            icon = Icons.Default.Send,
            onClick = onChatClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun SupportSection(context: android.content.Context) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Community & Support",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Telegram Button
            Button(
                onClick = {
                      try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/MyAido"))
                            context.startActivity(intent)
                        } catch (e: Exception) { }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Telegram", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Support Dev Button
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://buymeacoffee.com/myaido"))
                        context.startActivity(intent)
                    } catch (e: Exception) { }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFDD00).copy(alpha = 0.5f))
            ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFDD00), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Support", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Play Store Link
         Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.rr.aido"))
                        context.startActivity(intent)
                    } catch (e: Exception) { }
                },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "If you like, get it from Play Store",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You are using the Community Version",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
