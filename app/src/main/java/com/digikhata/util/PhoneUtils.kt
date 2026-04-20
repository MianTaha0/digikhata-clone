package com.digikhata.util

object PhoneUtils {
    fun cleanPhone(phone: String): String = phone.filter { it.isDigit() || it == '+' }
}
