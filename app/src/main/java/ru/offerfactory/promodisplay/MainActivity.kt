package ru.offerfactory.promodisplay

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Context.DEVICE_POLICY_SERVICE
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ru.offerfactory.promodisplay.auto_boot.AdminReceiver
import ru.offerfactory.promodisplay.ui.theme.PromoDisplayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PromoDisplayTheme {
                PromoDisplayScreen()
            }
        }

        enableAutoStart(this)
    }
}

private fun enableAutoStart(context: Context) {
    with(context) {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = AdminReceiver.getComponentName(this)

        if (!dpm.isDeviceOwnerApp(packageName)) return

        dpm.setLockTaskPackages(admin, arrayOf(packageName))
    }
}


@Composable
fun PromoDisplayScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Promo Display")
    }
}

@Preview(showBackground = true)
@Composable
fun PromoDisplayScreenPreview() {
    PromoDisplayTheme {
        PromoDisplayScreen()
    }
}
