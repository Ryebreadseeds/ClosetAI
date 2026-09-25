package com.ryebreadseeds.closetai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ryebreadseeds.closetai.ui.ClosetViewModel
import com.ryebreadseeds.closetai.ui.ClosetViewModelFactory
import com.ryebreadseeds.closetai.ui.nav.ClosetNavHost
import com.ryebreadseeds.closetai.ui.theme.ClosetAiTheme
import com.ryebreadseeds.closetai.ui.theme.ClosetColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ClosetAiApp
        setContent {
            ClosetAiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClosetColors.Ink
                ) {
                    val vm: ClosetViewModel = viewModel(
                        factory = ClosetViewModelFactory(app)
                    )
                    ClosetNavHost(viewModel = vm)
                }
            }
        }
    }
}
