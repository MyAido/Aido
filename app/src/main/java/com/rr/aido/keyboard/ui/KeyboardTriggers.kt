package com.rr.aido.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.aido.data.models.Preprompt

@Composable
fun KeyboardTriggers(
    triggers: List<Preprompt>,
    onTriggerClick: (String) -> Unit,
    onBackClick: () -> Unit,
    isDark: Boolean
) {
    val backgroundColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF2F2F2)
    val itemBackgroundColor = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.Gray else Color.DarkGray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp) // Consistent height
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onBackClick() }
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Triggers",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        if (triggers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No triggers available.\nAdd them in Aido settings.",
                    color = secondaryTextColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(triggers) { preprompt ->
                    TriggerItemRow(
                        preprompt = preprompt,
                        onClick = { onTriggerClick(preprompt.trigger) },
                        backgroundColor = itemBackgroundColor,
                        textColor = textColor,
                        secondaryColor = secondaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun TriggerItemRow(
    preprompt: Preprompt,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    secondaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = preprompt.trigger,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = preprompt.instruction,
            color = secondaryColor,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
