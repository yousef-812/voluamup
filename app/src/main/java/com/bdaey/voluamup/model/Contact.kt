package com.bdaey.voluamup.model

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val emailAddress: String? = null,
    val photoUri: String? = null
)
