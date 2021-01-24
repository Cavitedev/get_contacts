//MIT License
//
//        Copyright (c) 2020 Baseflow
//
//        Permission is hereby granted, free of charge, to any person obtaining a copy
//        of this software and associated documentation files (the "Software"), to deal
//        in the Software without restriction, including without limitation the rights
//        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//        copies of the Software, and to permit persons to whom the Software is
//        furnished to do so, subject to the following conditions:
//
//        The above copyright notice and this permission notice shall be included in all
//        copies or substantial portions of the Software.
//
//        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//        SOFTWARE.
package com.cavitedev.get_contacts.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener
import java.util.*

class PermissionManager {
    fun interface ActivityRegistry {
        fun addListener(handler: PluginRegistry.ActivityResultListener?)
    }

    fun interface PermissionRegistry {
        fun addListener(handler: RequestPermissionsResultListener?)
    }

    fun interface RequestPermissionsSuccessCallback {
        fun onSuccess(results: Map<Int, Int>)
    }


    private var ongoing = false
    fun requestPermissions(
            permissions: List<Int>,
            activity: Activity?,
            activityRegistry: ActivityRegistry,
            permissionRegistry: PermissionRegistry,
            successCallback: RequestPermissionsSuccessCallback,
            errorCallback: ErrorCallback) {
        if (ongoing) {
            errorCallback.onError(
                    "PermissionHandler.PermissionManager",
                    "A request for permissions is already running, please wait for it to finish before doing another request (note that you can request multiple permissions at the same time).")
            return
        }
        if (activity == null) {
            Log.d(PermissionConstants.LOG_TAG, "Unable to detect current Activity.")
            errorCallback.onError(
                    "PermissionHandler.PermissionManager",
                    "Unable to detect current Android Activity.")
            return
        }
        val requestResults: MutableMap<Int, Int> = HashMap()
        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            @PermissionConstants.PermissionStatus val permissionStatus = determinePermissionStatus(permission, activity, activity)
            if (permissionStatus == PermissionConstants.PERMISSION_STATUS_GRANTED) {
                if (!requestResults.containsKey(permission)) {
                    requestResults[permission] = PermissionConstants.PERMISSION_STATUS_GRANTED
                }
                continue
            }
            val names: List<String> = PermissionUtils.getManifestNames(activity, permission)

            // check to see if we can find manifest names
            // if we can't add as unknown and continue
            if (names.isEmpty()) {
                if (!requestResults.containsKey(permission)) {
                    requestResults[permission] = PermissionConstants.PERMISSION_STATUS_NOT_DETERMINED
                }
                continue
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permission == PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS) {
                activityRegistry.addListener(
                        ActivityResultListener(successCallback)
                )
                val packageName = activity.packageName
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                activity.startActivityForResult(intent, PermissionConstants.PERMISSION_CODE_IGNORE_BATTERY_OPTIMIZATIONS)
            } else {
                permissionsToRequest.addAll(names)
            }
        }
        val requestPermissions = permissionsToRequest.toTypedArray()
        if (permissionsToRequest.size > 0) {
            permissionRegistry.addListener(
                    RequestPermissionsListener(
                            activity,
                            requestResults
                    ) { results: Map<Int, Int> ->
                        ongoing = false
                        successCallback.onSuccess(results)
                    }
            )
            ongoing = true
            ActivityCompat.requestPermissions(
                    activity,
                    requestPermissions,
                    PermissionConstants.PERMISSION_CODE)
        } else {
            ongoing = false
            if (requestResults.isNotEmpty()) {
                successCallback.onSuccess(requestResults)
            }
        }
    }

    @PermissionConstants.PermissionStatus
    private fun determinePermissionStatus(
            @PermissionConstants.PermissionGroup permission: Int,
            context: Context,
            activity: Activity?): Int {
        val names = PermissionUtils.getManifestNames(context, permission)

        //if no permissions were found then there is an issue and permission is not set in Android manifest
        if (names.isEmpty()) {
            Log.d(PermissionConstants.LOG_TAG, "No permissions found in manifest for: $permission")
            return PermissionConstants.PERMISSION_STATUS_NOT_DETERMINED
        }
        val targetsMOrHigher = context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.M
        for (name in names) {
            // Only handle them if the client app actually targets a API level greater than M.
            if (targetsMOrHigher) {
                if (permission == PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS) {
                    val packageName = context.packageName
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    // PowerManager.isIgnoringBatteryOptimizations has been included in Android M first.
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (pm.isIgnoringBatteryOptimizations(packageName)) {
                            PermissionConstants.PERMISSION_STATUS_GRANTED
                        } else {
                            PermissionConstants.PERMISSION_STATUS_DENIED
                        }
                    } else {
                        PermissionConstants.PERMISSION_STATUS_RESTRICTED
                    }
                }
                val permissionStatus = ContextCompat.checkSelfPermission(context, name)
                if (permissionStatus == PackageManager.PERMISSION_DENIED) {
                    return if (!PermissionUtils.getRequestedPermissionBefore(context, name)) {
                        PermissionConstants.PERMISSION_STATUS_NOT_DETERMINED
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            PermissionUtils.isNeverAskAgainSelected(activity, name)) {
                        PermissionConstants.PERMISSION_STATUS_NEVER_ASK_AGAIN
                    } else {
                        PermissionConstants.PERMISSION_STATUS_DENIED
                    }
                } else if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                    return PermissionConstants.PERMISSION_STATUS_DENIED
                }
            }
        }
        return PermissionConstants.PERMISSION_STATUS_GRANTED
    }


    @VisibleForTesting
    internal class ActivityResultListener @VisibleForTesting constructor(private val callback: RequestPermissionsSuccessCallback) : PluginRegistry.ActivityResultListener {
        // There's no way to unregister permission listeners in the v1 embedding, so we'll be called
        // duplicate times in cases where the user denies and then grants a permission. Keep track of if
        // we've responded before and bail out of handling the callback manually if this is a repeat
        // call.
        private var alreadyCalled = false
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
            if (alreadyCalled || requestCode != PermissionConstants.PERMISSION_CODE_IGNORE_BATTERY_OPTIMIZATIONS) {
                return false
            }
            alreadyCalled = true
            val status = if (resultCode == Activity.RESULT_OK) PermissionConstants.PERMISSION_STATUS_GRANTED else PermissionConstants.PERMISSION_STATUS_DENIED
            val results = HashMap<Int, Int>()
            results[PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS] = status
            callback.onSuccess(results)
            return true
        }
    }

    @VisibleForTesting
    internal class RequestPermissionsListener @VisibleForTesting constructor(
            private val activity: Activity,
            private val requestResults: MutableMap<Int, Int>,
            private val callback: RequestPermissionsSuccessCallback) : RequestPermissionsResultListener {
        // There's no way to unregister permission listeners in the v1 embedding, so we'll be called
        // duplicate times in cases where the user denies and then grants a permission. Keep track of if
        // we've responded before and bail out of handling the callback manually if this is a repeat
        // call.
        private var alreadyCalled = false
        override fun onRequestPermissionsResult(id: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
            if (alreadyCalled || id != PermissionConstants.PERMISSION_CODE) {
                return false
            }
            alreadyCalled = true
            for (i in permissions.indices) {
                val permissionName = permissions[i]
                @PermissionConstants.PermissionGroup val permission = PermissionUtils.parseManifestName(permissionName)
                if (permission == PermissionConstants.PERMISSION_GROUP_UNKNOWN) continue
                val result = grantResults[i]

                if (!requestResults.containsKey(permission)) {
                    requestResults[permission] = PermissionUtils.toPermissionStatus(activity, permissionName, result)
                }
                PermissionUtils.updatePermissionShouldShowStatus(activity, permission)
            }
            callback.onSuccess(requestResults)
            return true
        }
    }
}