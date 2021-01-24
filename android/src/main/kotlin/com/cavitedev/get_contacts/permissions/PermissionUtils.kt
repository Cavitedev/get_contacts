package com.cavitedev.get_contacts.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.*

object PermissionUtils {
    @PermissionConstants.PermissionGroup
    fun parseManifestName(permission: String?): Int {
        return when (permission) {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.GET_ACCOUNTS -> PermissionConstants.PERMISSION_GROUP_CONTACTS
            else -> PermissionConstants.PERMISSION_GROUP_UNKNOWN
        }
    }

    fun getManifestNames(context: Context?, @PermissionConstants.PermissionGroup permission: Int): List<String> {
        val permissionNames = ArrayList<String>()
        when (permission) {
            PermissionConstants.PERMISSION_GROUP_CONTACTS -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_CONTACTS)) permissionNames.add(Manifest.permission.READ_CONTACTS)
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.WRITE_CONTACTS)) permissionNames.add(Manifest.permission.WRITE_CONTACTS)
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.GET_ACCOUNTS)) permissionNames.add(Manifest.permission.GET_ACCOUNTS)
            }
        }
        return permissionNames
    }

    private fun hasPermissionInManifest(context: Context?, confirmedPermissions: ArrayList<String>, permission: String): Boolean {
        try {
            for (r in confirmedPermissions) {
                if (r == permission) {
                    return true
                }
            }
            if (context == null) {
                Log.d(PermissionConstants.LOG_TAG, "Unable to detect current Activity or App Context.")
                return false
            }
            val info = context
                    .packageManager
                    .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            if (info == null) {
                Log.d(PermissionConstants.LOG_TAG, "Unable to get Package info, will not be able to determine permissions to request.")
                return false
            }
            val requestedPermissions = arrayOf(*info.requestedPermissions)
            for (r in requestedPermissions) {
                if (r == permission) {
                    return true
                }
            }
        } catch (ex: Exception) {
            Log.d(PermissionConstants.LOG_TAG, "Unable to check manifest for permission: ", ex)
        }
        return false
    }

    @PermissionConstants.PermissionStatus
    fun toPermissionStatus(activity: Activity?, permissionName: String?, grantResult: Int): Int {
        return if (grantResult == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isNeverAskAgainSelected(activity, permissionName)) PermissionConstants.PERMISSION_STATUS_NEVER_ASK_AGAIN else PermissionConstants.PERMISSION_STATUS_DENIED
        } else PermissionConstants.PERMISSION_STATUS_GRANTED
    }

    fun updatePermissionShouldShowStatus(activity: Activity?, @PermissionConstants.PermissionGroup permission: Int) {
        if (activity == null) {
            return
        }
        val names = getManifestNames(activity, permission)
        if (names.isEmpty()) {
            return
        }
        for (name in names) {
            setRequestedPermission(activity, name)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun isNeverAskAgainSelected(activity: Activity?, name: String?): Boolean {
        return if (activity == null) {
            false
        } else neverAskAgainSelected(activity, name)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun neverAskAgainSelected(activity: Activity, permission: String?): Boolean {
        val hasRequestedPermissionBefore = getRequestedPermissionBefore(activity, permission)
        val shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission!!)
        return hasRequestedPermissionBefore && !shouldShowRequestPermissionRationale
    }

    private fun setRequestedPermission(context: Context, permission: String?) {
        val genPrefs = context.getSharedPreferences("GENERIC_PREFERENCES", Context.MODE_PRIVATE)
        val editor = genPrefs.edit()
        editor.putBoolean(permission, true)
        editor.apply()
    }

    fun getRequestedPermissionBefore(context: Context, permission: String?): Boolean {
        val genPrefs = context.getSharedPreferences("GENERIC_PREFERENCES", Context.MODE_PRIVATE)
        return genPrefs.getBoolean(permission, false)
    }
}