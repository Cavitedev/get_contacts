package com.cavitedev.get_contacts.core

object StringManipulator {

    private val isPhoneRegex : Regex = Regex("[^0-9+]+")

    fun toJoinedPhoneString(str : String) : String {

        return str.split(isPhoneRegex).joinToString("")

    }
    

}