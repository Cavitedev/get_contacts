package com.cavitedev.get_contacts.permissions

fun interface ErrorCallback {
    fun onError(errorCode: String?, errorDescription: String?)
}