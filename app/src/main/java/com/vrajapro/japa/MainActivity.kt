package com.vrajapro.japa

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.vrajapro.japa.ui.JapaScreen
import com.vrajapro.japa.ui.JapaTheme

// AppCompatActivity (rather than ComponentActivity) so AppCompatDelegate can
// switch the app language at runtime on all supported Android versions.
class MainActivity : AppCompatActivity() {

    private val viewModel: JapaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            JapaTheme {
                JapaScreen(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForeground()
    }

    override fun onStop() {
        viewModel.onAppBackground()
        super.onStop()
    }

    /** Volume-up counts a mantra, volume-down undoes one (like ↑ / ↓ on the web app). */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!viewModel.state.value.showSessionDialog) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> { if (event.repeatCount == 0) viewModel.countManual(); return true }
                KeyEvent.KEYCODE_VOLUME_DOWN -> { if (event.repeatCount == 0) viewModel.decrement(); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
