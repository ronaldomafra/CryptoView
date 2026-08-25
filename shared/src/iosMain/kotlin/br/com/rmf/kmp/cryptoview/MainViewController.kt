package br.com.rmf.kmp.cryptoview

import androidx.compose.ui.window.ComposeUIViewController
import br.com.rmf.kmp.cryptoview.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
