package com.rr.aido.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.aido.data.models.AiProvider
import com.rr.aido.data.models.TriggerMethod

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSaveSettings: (AiProvider, String, String, String, String, String, TriggerMethod) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    // Settings State
    var selectedProvider by remember { mutableStateOf(AiProvider.GEMINI) }
    var apiKey by remember { mutableStateOf("") }
    var customApiUrl by remember { mutableStateOf("https://api.openai.com/v1/") }
    var customApiKey by remember { mutableStateOf("") }
    var customModelName by remember { mutableStateOf("gpt-4o-mini") }
    var triggerMethod by remember { mutableStateOf(TriggerMethod.ACCESSIBILITY) }

    val steps = listOf(
        "Welcome",
        "Provider",
        "Setup",
        "How to Use",
        "Permissions"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            OnboardingBottomBar(
                currentStep = currentStep,
                totalSteps = steps.size,
                onNext = {
                    if (currentStep < steps.size - 1) {
                        currentStep++
                    } else {
                        onSaveSettings(
                            selectedProvider,
                            apiKey,
                            "gemini-2.5-flash-lite", // Default model
                            customApiUrl,
                            customApiKey,
                            customModelName,
                            triggerMethod
                        )
                        onComplete()
                    }
                },
                onBack = {
                    if (currentStep > 0) {
                        currentStep--
                    }
                },
                canProceed = when(currentStep) {
                    2 -> if (selectedProvider == AiProvider.GEMINI) apiKey.isNotEmpty() else customApiKey.isNotEmpty()
                    else -> true
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                steps.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (index == currentStep) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= currentStep) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            AnimatedContent(
                targetState = currentStep,
                label = "OnboardingStep"
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (step) {
                        0 -> WelcomeStep()
                        1 -> ProviderStep(
                            selectedProvider = selectedProvider,
                            onProviderSelected = { selectedProvider = it }
                        )
                        2 -> SetupStep(
                            provider = selectedProvider,
                            apiKey = apiKey,
                            onApiKeyChange = { apiKey = it },
                            customApiUrl = customApiUrl,
                            onCustomUrlChange = { customApiUrl = it },
                            customApiKey = customApiKey,
                            onCustomApiKeyChange = { customApiKey = it },
                            customModelName = customModelName,
                            onCustomModelChange = { customModelName = it }
                        )
                            
                        3 -> TutorialStep()
                        4 -> {
                            val context = LocalContext.current
                            PermissionsStep(
                                onEnableClick = {
                                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                onLaterClick = {
                                    onSaveSettings(
                                        selectedProvider,
                                        apiKey,
                                        "gemini-2.5-flash-lite",
                                        customApiUrl,
                                        customApiKey,
                                        customModelName,
                                        triggerMethod
                                    )
                                    onComplete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.SmartToy,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary
                        )
                    ),
                    shape = CircleShape
                )
                .padding(24.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
        
        Text(
            text = "Welcome to Aido",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Your AI-powered writing assistant that works in any app. Type smarter, faster, and better.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProviderStep(
    selectedProvider: AiProvider,
    onProviderSelected: (AiProvider) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Choose Intelligence",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Select which AI provider you want to use to power Aido.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        ProviderCard(
            title = "Google Gemini",
            description = "Fast, capable, and free tier available. Recommended for most users.",
            isSelected = selectedProvider == AiProvider.GEMINI,
            onClick = { onProviderSelected(AiProvider.GEMINI) }
        )

        ProviderCard(
            title = "Custom (OpenAI Compatible)",
            description = "Use your own endpoint (OpenAI, LocalAI, Ollama, etc).",
            isSelected = selectedProvider == AiProvider.CUSTOM,
            onClick = { onProviderSelected(AiProvider.CUSTOM) }
        )
    }
}

@Composable
private fun ProviderCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SetupStep(
    provider: AiProvider,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    customApiUrl: String,
    onCustomUrlChange: (String) -> Unit,
    customApiKey: String,
    onCustomApiKeyChange: (String) -> Unit,
    customModelName: String,
    onCustomModelChange: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Setup Connection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (provider == AiProvider.GEMINI) {
            Text(
                text = "Enter your Google Gemini API Key.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null) }
            )
            
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get Gemini API Key")
            }
        } else {
            // Custom Provider Inputs
             Text(
                text = "Configure your custom OpenAI-compatible endpoint.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedTextField(
                value = customApiUrl,
                onValueChange = onCustomUrlChange,
                label = { Text("API URL (Base URL)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://api.openai.com/v1/") }
            )
            
            OutlinedTextField(
                value = customApiKey,
                onValueChange = onCustomApiKeyChange,
                label = { Text("API Key (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null) }
            )
            
            OutlinedTextField(
                value = customModelName,
                onValueChange = onCustomModelChange,
                label = { Text("Model Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("gpt-4o") }
            )
        }
    }
}

@Composable
private fun TutorialStep() {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "How to use Aido",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Using Aido is simple. Just type and let AI handle the rest.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Step 1: Type
        TutorialCard(
            step = "1",
            title = "Type anywhere",
            description = "Open any app (WhatsApp, Email, etc.) and type your text."
        ) {
            Text(
                text = "Sorry I cant come|",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface 
            )
        }

        // Step 2: Trigger
        TutorialCard(
            step = "2",
            title = "Use a Trigger",
            description = "Add a command like @fixg or @tone at the end."
        ) {
            Row {
                Text(
                    text = "Sorry I cant come ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface 
                )
                Text(
                    text = "@fixg",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary 
                )
            }
        }
        
        // Step 3: Magic
        TutorialCard(
            step = "3",
            title = "Watch the Magic",
            description = "Aido will instantly replace your text with the improved version."
        ) {
             Text(
                text = "I apologize, but I won't be able to make it.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface 
            )
        }
    }
}

@Composable
private fun TutorialCard(
    step: String,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Simulation Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                content()
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionsStep(
    onEnableClick: () -> Unit,
    onLaterClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
         Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                .padding(16.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        
        Text(
            text = "Enable Aido",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "To work in any app, Aido needs Accessibility Service content.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.Security, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Aido only processes text when you explicitly use commands. Your data stays private.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Primary Action
        Button(
            onClick = onEnableClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Enable Service",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Secondary Action
        TextButton(
            onClick = onLaterClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Maybe Later",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    canProceed: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStep > 0) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
        
        Button(
            onClick = onNext,
            enabled = canProceed,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (currentStep == totalSteps - 1) "Finish" else "Next",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            if (currentStep < totalSteps - 1) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            } else {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
