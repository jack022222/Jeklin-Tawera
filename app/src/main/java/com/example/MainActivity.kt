package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.PosDatabase
import com.example.data.repository.PosRepository
import com.example.ui.KasirKuApp
import com.example.ui.PosViewModel
import com.example.ui.theme.KasirKuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PosDatabase.getInstance(applicationContext)
        val repository = PosRepository(database.posDao())

        setContent {
            KasirKuTheme {
                val posViewModel: PosViewModel = viewModel(
                    factory = PosViewModel.provideFactory(repository)
                )
                KasirKuApp(viewModel = posViewModel)
            }
        }
    }
}
