package com.cavitedev.get_contacts.permissions

import androidx.annotation.IntDef

internal object PermissionConstants {
    const val LOG_TAG = "permissions_handler"
    const val PERMISSION_CODE = 24
    const val PERMISSION_CODE_IGNORE_BATTERY_OPTIMIZATIONS = 5672353

    //PERMISSION_GROUP
    const val PERMISSION_GROUP_CONTACTS = 2
    const val PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS = 15
    const val PERMISSION_GROUP_UNKNOWN = 19


    //PERMISSION_STATUS
    const val PERMISSION_STATUS_DENIED = 0
    const val PERMISSION_STATUS_GRANTED = 1
    const val PERMISSION_STATUS_RESTRICTED = 2
    const val PERMISSION_STATUS_NOT_DETERMINED = 3
    const val PERMISSION_STATUS_NEVER_ASK_AGAIN: Int = 4

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(PERMISSION_GROUP_CONTACTS,PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS, PERMISSION_GROUP_UNKNOWN)
    internal annotation class PermissionGroup

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(PERMISSION_STATUS_DENIED, PERMISSION_STATUS_GRANTED, PERMISSION_STATUS_RESTRICTED, PERMISSION_STATUS_NOT_DETERMINED, PERMISSION_STATUS_NEVER_ASK_AGAIN)
    internal annotation class PermissionStatus

}