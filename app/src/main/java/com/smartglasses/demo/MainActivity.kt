package com.smartglasses.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartglasses.demo.ui.theme.SmartGlassesTheme
import com.smartglasses.demo.navigation.SmartGlassesNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartGlassesTheme {
                SmartGlassesNavHost()
            }
        }
    }
}
