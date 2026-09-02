package com.mate.focus

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Mate asks to be a device administrator for exactly one reason: Android
 * refuses to uninstall an app while its admin is active. It requests no
 * policies, so it cannot wipe, lock, or read anything.
 */
class MateDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Turning this off is the first step to deleting Mate.\n\n" +
            Motivation.blockLine()

    override fun onDisabled(context: Context, intent: Intent) {
        Prefs.init(context)
        Prefs.noteProtectionDropped()
    }

    companion object {
        fun component(context: Context) =
            ComponentName(context, MateDeviceAdminReceiver::class.java)

        fun isActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as? DevicePolicyManager ?: return false
            return dpm.isAdminActive(component(context))
        }

        /** The system screen that explains the request and asks for consent. */
        fun enableIntent(context: Context): Intent =
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
                .putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "So that Mate cannot be deleted on impulse. It asks for no " +
                        "other powers - it cannot wipe your phone, lock it, or see " +
                        "your data.",
                )

        fun release(context: Context) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as? DevicePolicyManager ?: return
            if (dpm.isAdminActive(component(context))) {
                dpm.removeActiveAdmin(component(context))
            }
        }
    }
}
