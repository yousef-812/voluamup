package com.bdaey.voluamup.model

data class CallLogItem(
    val id: String,
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long
)
