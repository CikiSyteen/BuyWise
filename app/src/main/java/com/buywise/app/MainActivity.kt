package com.buywise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.buywise.app.data.local.AppDatabase
import com.buywise.app.data.local.PreferencesManager
import com.buywise.app.data.repository.AssessmentRepository
import com.buywise.app.ui.navigation.BuyWiseNavGraph
import com.buywise.app.ui.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferencesManager = PreferencesManager(applicationContext)
        val repository = AssessmentRepository(AppDatabase.get(this).assessmentRecordDao())

        // 首帧前读取一次配置：已填过月薪直达评估页，否则先进设置页
        lifecycleScope.launch {
            val profile = preferencesManager.profileFlow.first()
            val startDestination =
                if (profile.isValid) Screen.Assessment.route else Screen.Settings.route

            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        BuyWiseNavGraph(
                            preferencesManager = preferencesManager,
                            repository = repository,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}
