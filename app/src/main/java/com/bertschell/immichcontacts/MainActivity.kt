package com.bertschell.immichcontacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bertschell.immichcontacts.ui.navigation.ImmichContactsApp
import com.bertschell.immichcontacts.ui.theme.ImmichContactsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImmichContactsTheme {
                ImmichContactsApp()
            }
        }
    }
}