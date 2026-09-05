package com.buywise.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 极简 ViewModel 工厂，用于把 PreferencesManager 注入 ViewModel。
 * 后续若接入 Hilt，直接替换为 @HiltViewModel 即可。
 */
class BuyWiseViewModelFactory(
    private val creator: () -> ViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
