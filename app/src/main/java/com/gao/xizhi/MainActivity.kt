package com.gao.xizhi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gao.xizhi.data.AppDatabase
import com.gao.xizhi.ui.XizhiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.get(this)
        setContent { XizhiApp(db) }
    }
}
