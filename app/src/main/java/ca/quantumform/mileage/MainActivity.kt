package ca.quantumform.mileage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.quantumform.mileage.core.AppEntitlement
import ca.quantumform.mileage.core.MileageAppStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QfMileageApp()
        }
    }
}

@Composable
private fun QfMileageApp() {
    val status = MileageAppStatus(
        displayName = "QF Mileage",
        version = "0.1.0",
        entitlement = AppEntitlement.FreeWithAds
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = status.displayName, style = MaterialTheme.typography.headlineMedium)
                Text(text = "Android MVP scaffold is ready for trips, backups, exports, and review workflows.")
                Text(text = "Version ${status.version} · ${status.entitlement.label}")
            }
        }
    }
}
