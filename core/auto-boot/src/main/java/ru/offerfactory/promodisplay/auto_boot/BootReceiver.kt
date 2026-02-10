package ru.offerfactory.promodisplay.auto_boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val packageManager = context?.packageManager
            val launchIntent =
                packageManager?.getLaunchIntentForPackage("ru.offerfactory.promodisplay")
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(launchIntent)
        }
    }
}