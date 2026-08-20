package com.example.volumeup.model

data class CallLogItem(
    val id: String,
    val number: String,
    val name: String?,
    val type: Int, // CallLog.Calls.INCOMING_TYPE, OUTGOING_TYPE, MISSED_TYPE
    val date: Long,
    val duration: Long
)
