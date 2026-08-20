package com.example.volumeup.ui.calllog

import android.app.Application
import android.provider.CallLog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.volumeup.model.CallLogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallLogViewModel(application: Application) : AndroidViewModel(application) {

    private val _callLogs = MutableLiveData<List<CallLogItem>>()
    val callLogs: LiveData<List<CallLogItem>> = _callLogs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadCallLogs() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val logs = queryCallLogsFromProvider()
            withContext(Dispatchers.Main) {
                _callLogs.value = logs
                _isLoading.value = false
            }
        }
    }

    private fun queryCallLogsFromProvider(): List<CallLogItem> {
        val logs = mutableListOf<CallLogItem>()

        val cursor = getApplication<Application>().contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use { c ->
            val idIdx = c.getColumnIndex(CallLog.Calls._ID)
            val numberIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = c.getColumnIndex(CallLog.Calls.DURATION)

            while (c.moveToNext()) {
                val id = if (idIdx >= 0) c.getString(idIdx) else ""
                val number = if (numberIdx >= 0) c.getString(numberIdx) ?: "" else ""
                val name = if (nameIdx >= 0) c.getString(nameIdx) else null
                val type = if (typeIdx >= 0) c.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                val duration = if (durationIdx >= 0) c.getLong(durationIdx) else 0L

                if (number.isNotEmpty()) {
                    logs.add(
                        CallLogItem(
                            id = id,
                            number = number,
                            name = name,
                            type = type,
                            date = date,
                            duration = duration
                        )
                    )
                }
            }
        }
        return logs
    }
}
