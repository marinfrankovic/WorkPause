package com.worksched

// Modified for WorkPause: remove the externally callable production test hook.
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.worksched.ui.MainScreen
import com.worksched.ui.theme.WorkSchedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WorkSchedTheme {
                MainScreen()
            }
        }
    }
}
