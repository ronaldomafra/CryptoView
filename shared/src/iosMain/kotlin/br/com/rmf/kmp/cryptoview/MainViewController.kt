package br.com.rmf.kmp.cryptoview

import androidx.compose.ui.window.ComposeUIViewController
import br.com.rmf.kmp.cryptoview.di.initKoin
import br.com.rmf.kmp.cryptoview.di.iosSecurityModule
import br.com.rmf.kmp.cryptoview.security.PlatformApiKeyCipher
import platform.UIKit.UIViewController

fun MainViewController(platformApiKeyCipher: PlatformApiKeyCipher): UIViewController {
    initKoin(listOf(iosSecurityModule(platformApiKeyCipher)))
    return ComposeUIViewController { App() }
}
