package zhb.company.bgvoz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import zhb.company.bgvoz.ui.DeparturesScreen
import zhb.company.bgvoz.ui.theme.BgVozTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BgVozTheme {
                val vm: TimetableViewModel = viewModel()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DeparturesScreen(
                        state = vm.state,
                        onRefresh = vm::refresh,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
