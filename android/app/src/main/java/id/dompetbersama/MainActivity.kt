package id.dompetbersama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.dompetbersama.ui.DompetAppUi
import id.dompetbersama.ui.theme.DompetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DompetTheme { DompetAppUi() }
        }
    }
}
