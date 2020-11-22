package com.namehillsoftware.projectblue.shared.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

class DefaultImageProvider(private val context: Context) {
	fun promiseFileBitmap(): Promise<Bitmap> = promiseFillerBitmap(context)

	companion object {
		private val defaultImageAccessExecutor = lazy { CachedSingleThreadExecutor() }
		private lateinit var fillerBitmap: Bitmap

		private fun promiseFillerBitmap(context: Context) =
			if (Companion::fillerBitmap.isInitialized) Promise(getBitmapCopy(fillerBitmap))
			else QueuedPromise(MessageWriter {
				if (!Companion::fillerBitmap.isInitialized) {
					fillerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.wave_background)
					val dm = context.resources.displayMetrics
					val maxSize = dm.heightPixels.coerceAtLeast(dm.widthPixels)
					fillerBitmap = Bitmap.createScaledBitmap(fillerBitmap, maxSize, maxSize, false)
				}
				getBitmapCopy(fillerBitmap)
			}, defaultImageAccessExecutor.value)

		private fun getBitmapCopy(src: Bitmap): Bitmap = src.copy(src.config, false)
	}
}
