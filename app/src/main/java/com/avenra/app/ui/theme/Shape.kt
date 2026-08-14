package com.avenra.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Approved Shape Token Radius Specifications
val ButtonShape = RoundedCornerShape(40.dp)
val CardShape = RoundedCornerShape(15.dp)
val InputShape = RoundedCornerShape(8.dp)
val ChipShape = RoundedCornerShape(20.dp)

val DesignShapes = Shapes(
    small = InputShape,
    medium = CardShape,
    large = ButtonShape,
)
