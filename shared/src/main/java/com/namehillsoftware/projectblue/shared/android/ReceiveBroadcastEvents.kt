package com.namehillsoftware.projectblue.shared.android

import android.content.Context
import android.content.Intent

fun interface ReceiveBroadcastEvents {
	fun onReceive(context: Context, intent: Intent)
}
