package com.digikhata.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.digikhata.ui.theme.DigiRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun digiTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = DigiRed,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White
)
