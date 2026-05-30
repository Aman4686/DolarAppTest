package com.example.dolarapptest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dolarapptest.ui.feature.claude.ExchangeScreenAI
import com.example.dolarapptest.ui.feature.exchange.ExchangeScreen
import com.example.dolarapptest.ui.theme.DolarAppTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DolarAppTestTheme {
                Surface {
                    ExchangeScreen()
                }
            }
        }
    }
}
