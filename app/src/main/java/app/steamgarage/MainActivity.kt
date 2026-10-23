package app.steamgarage

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import app.steamgarage.ui.AppRoot
import app.steamgarage.ui.navigation.LaunchExtras
import app.steamgarage.ui.navigation.toNavRequest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // При пересоздании экрана (поворот) интент тот же — второй раз по нему не переходим.
        if (savedInstanceState == null) handleIntent(intent)
        setContent { AppRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val request = intent.toNavRequest() ?: return
        appContainer.navRequests.value = request
        // Кнопка «Заменено» не закрывает уведомление сама — убираем его, раз приложение открыто по нему.
        intent.getIntExtra(LaunchExtras.EXTRA_NOTIFICATION_ID, 0).takeIf { it != 0 }?.let {
            NotificationManagerCompat.from(this).cancel(it)
        }
    }
}
