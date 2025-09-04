package com.example.cardemulator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BluetoothVMFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BluetoothVM(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}