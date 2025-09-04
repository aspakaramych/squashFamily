package com.example.cardemulator

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BluetoothVM(private val context: Context) : ViewModel() {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

    private val _receivedMessage = MutableStateFlow("")
    val receivedMessage: StateFlow<String> = _receivedMessage

    private val _sentMessage = MutableStateFlow("")
    val sentMessage: StateFlow<String> = _sentMessage

    private val _connectionStatus = MutableStateFlow("Не подключен")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private var bluetoothGatt: BluetoothGatt? = null

    private val SERVICE_UUID = UUID.fromString("A1B2C3D4-E5F6-A7B8-C9D0-E1F2A3B4C5D6")
    private val CHARACTERISTIC_UUID = UUID.fromString("A1B2C3D4-E5F6-A7B8-C9D0-E1F2A3B4C5D7")

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            // Ошибка 1 и 2: Требуется проверка разрешений для bluetoothLeScanner.stopScan() и device.connectGatt()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.bluetoothLeScanner.stopScan(this)
            }

            result?.device?.let { device ->
                Log.d("BluetoothViewModel", "Найдено устройство: ${device.name ?: "N/A"}")
                _connectionStatus.value = "Подключение к ${device.name ?: "устройству"}..."

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device.connectGatt(context, false, gattCallback)
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BluetoothViewModel", "Подключено к GATT серверу.")
                _connectionStatus.value = "Подключен"
                // Ошибка 3: Требуется проверка разрешения
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt?.discoverServices()
                }
                bluetoothGatt = gatt
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BluetoothViewModel", "Отключено от GATT сервера.")
                _connectionStatus.value = "Отключен"
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    // Ошибка 4: Требуется проверка разрешения
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.setCharacteristicNotification(characteristic, true)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            // Ошибка 5: Безопасный вызов на nullable объекте
            if (characteristic?.uuid == CHARACTERISTIC_UUID) {
                // Ошибка 6: Запрещено использовать .value напрямую
                val receivedData = characteristic!!.value
                val message = String(receivedData)
                Log.d("BluetoothViewModel", "Получено сообщение: $message")
                // Ошибка 7: Обновление LiveData (MutableStateFlow) должно быть на главном потоке. Используем viewModelScope.launch
                viewModelScope.launch {
                    _receivedMessage.value = message
                }
                // Ошибка 8: Nullable gatt
                sendResponse(gatt)
            }
        }
    }

    fun startScan() {
        // Ошибка 1: Требуется проверка разрешения
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            _connectionStatus.value = "Сканирование..."
            bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        } else {
            _connectionStatus.value = "Разрешения для сканирования не предоставлены."
        }
    }

    private fun sendResponse(gatt: BluetoothGatt?) {
        val service = gatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
        if (characteristic != null) {
            val response = "Ответ от Android"
            characteristic.value = response.toByteArray()
            // Ошибка 9: Требуется проверка разрешения
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.writeCharacteristic(characteristic)
                _sentMessage.value = response
                Log.d("BluetoothViewModel", "Отправлено сообщение: $response")
            }
        }
    }
}