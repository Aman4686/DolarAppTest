package com.example.dolarapptest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
private fun OverlayLoading() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}