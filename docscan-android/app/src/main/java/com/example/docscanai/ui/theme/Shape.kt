package com.example.docscanai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val DocScanShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // chips, small badges
    small      = RoundedCornerShape(10.dp),  // text fields, small cards
    medium     = RoundedCornerShape(14.dp),  // dialogs, bottom sheets
    large      = RoundedCornerShape(20.dp),  // cards, panels
    extraLarge = RoundedCornerShape(28.dp),  // scan CTA, large surfaces
)
