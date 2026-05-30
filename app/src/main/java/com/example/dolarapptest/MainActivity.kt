package com.example.dolarapptest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
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
