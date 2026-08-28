package com.loukatech.mbote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MastaIcon(
    isSelected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    if (isSelected) {
        Icon(
            imageVector = Icons.Filled.Diversity3,
            contentDescription = "Masta",
            tint = tint,
            modifier = modifier.size(size)
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.Diversity3,
            contentDescription = "Masta",
            tint = tint,
            modifier = modifier.size(size)
        )
    }
}
