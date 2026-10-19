package com.github.leemoon228.messanger_lab3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.github.leemoon228.messanger_lab3.ui.theme.Messanger_lab3Theme

class MainActivity : ComponentActivity() {
    val chatAPIVM: ChatAPIVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Messanger_lab3Theme {
                Scaffold(
                    modifier =Modifier.fillMaxSize()
                ) { innerPadding ->
                    ChatScreen(chatAPIVM, Modifier.padding(innerPadding))
                }

            }
        }
    }
}
