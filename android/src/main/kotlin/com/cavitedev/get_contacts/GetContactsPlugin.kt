package com.cavitedev.get_contacts

import android.app.Activity
import android.content.Context
import com.cavitedev.get_contacts.contacts.MethodCallServiceHandler
import com.cavitedev.get_contacts.permissions.PermissionManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

/** GetContactsPlugin */
class GetContactsPlugin : FlutterPlugin, ActivityAware {

    private var methodCallHandler: MethodCallServiceHandler? = null
    private var methodChannel: MethodChannel? = null


    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {

        startListening(
                binding.applicationContext,
                binding.binaryMessenger
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        stopListening()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        startListeningToActivity(
                binding.activity,
                { listener: PluginRegistry.ActivityResultListener? -> binding.addActivityResultListener(listener!!) },
                { listener: PluginRegistry.RequestPermissionsResultListener? -> binding.addRequestPermissionsResultListener(listener!!) }
        )
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        stopListeningToActivity()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    private fun startListening(applicationContext: Context, messenger: BinaryMessenger) {
        this.methodChannel = MethodChannel(
                messenger,
                "com.cavitedev.get_contacts")
        methodCallHandler = MethodCallServiceHandler(applicationContext,PermissionManager())

        methodChannel!!.setMethodCallHandler(methodCallHandler)

    }

    private fun stopListening() {
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        methodCallHandler = null
    }

    private fun startListeningToActivity(
            activity: Activity,
            activityRegistry: PermissionManager.ActivityRegistry,
            permissionRegistry: PermissionManager.PermissionRegistry
    ) {

        methodCallHandler?.activity = activity
        methodCallHandler?.activityRegistry = activityRegistry
        methodCallHandler?.permissionRegistry = permissionRegistry


    }

    private fun stopListeningToActivity() {
        methodCallHandler?.activity = null
        methodCallHandler?.activityRegistry = null
        methodCallHandler?.permissionRegistry = null

    }
}
