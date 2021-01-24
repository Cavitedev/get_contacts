package com.cavitedev.get_contacts.contacts


import android.app.Activity
import android.content.Context
import com.cavitedev.get_contacts.contacts.ContactsService.fetchContacts
import com.cavitedev.get_contacts.permissions.PermissionConstants
import com.cavitedev.get_contacts.permissions.PermissionConstants.PERMISSION_GROUP_CONTACTS
import com.cavitedev.get_contacts.permissions.PermissionManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import kotlinx.coroutines.runBlocking

class MethodCallServiceHandler(private val context: Context,
                               private val permissionManager: PermissionManager
) : MethodCallHandler {


    var activity: Activity? = null

    var activityRegistry: PermissionManager.ActivityRegistry? = null

    var permissionRegistry: PermissionManager.PermissionRegistry? = null


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {


        if (call.method == "getContacts") {

            permissionManager.requestPermissions(
                    listOf(PERMISSION_GROUP_CONTACTS),
                    activity,
                    activityRegistry!!,
                    permissionRegistry!!,
                    { resultPermission: Map<Int, Int> ->

                        onPermissionResult(resultPermission, result)


                    },
                    { errorCode: String?, errorDescription: String? ->
                        result.error(errorCode, errorDescription, "App needs READ_CONTACT permissions")
                    })


        } else {
            result.notImplemented()
        }
    }

    private fun onPermissionResult(resultPermission: Map<Int, Int>, result: MethodChannel.Result) {
        when (resultPermission[PERMISSION_GROUP_CONTACTS]) {
            PermissionConstants.PERMISSION_STATUS_GRANTED -> {
                runBlocking {
                    val contacts = fetchContacts(context)
                    val json = Contact.multipleToJson(contacts)
                    result.success(json.toString())
                }
            }
            PermissionConstants.PERMISSION_STATUS_DENIED ->
                result.error("DENIED PERMISSIONS", "READ_CONTACT PERMISSION WAS DENIED", "PLUGIN REQUIRES READ_CONTACTS PERMISSION TO RETRIEVE ANDROID CONTACTS")
            PermissionConstants.PERMISSION_STATUS_NEVER_ASK_AGAIN ->
                result.error("DENIED PERMISSIONS PERMANENTLY", "READ_CONTACT PERMISSION WAS DENIED", "PLUGIN REQUIRES READ_CONTACTS PERMISSION TO RETRIEVE ANDROID CONTACTS")

            else ->
                result.error("UNKNOWN","","")
        }
    }

}