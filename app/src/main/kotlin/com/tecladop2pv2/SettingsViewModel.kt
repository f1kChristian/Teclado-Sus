package com.tecladop2pv2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tecladop2pv2.core.model.KeyboardCustomization
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TecladoApplication
    val customization = app.customization.customization.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KeyboardCustomization())
    val records = app.history.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val filteredRecords = app.history.observeFiltered().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pendingCount = app.history.observePendingCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun update(transform: (KeyboardCustomization) -> KeyboardCustomization) {
        viewModelScope.launch { app.customization.update(transform) }
    }
    fun resetCustomization() { viewModelScope.launch { app.customization.reset() } }
    fun delete(id: String) { viewModelScope.launch { app.history.delete(id) } }
    fun deleteAll() { viewModelScope.launch { app.history.deleteAll() } }
}
