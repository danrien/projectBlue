package com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache

import android.content.Context
import androidx.collection.LruCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ImageDiskFileCacheFactory
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.RemoteImageAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.projectblue.shared.promises.extensions.toPromise

class MemoryCachedImageAccess(private val sourceImages: GetRawImages, private val imageCacheKeys: LookupImageCacheKey) : GetRawImages {

	companion object {
		private const val MAX_MEMORY_CACHE_SIZE = 10

		private lateinit var instance: MemoryCachedImageAccess

		@Synchronized
		@JvmStatic
		fun getInstance(context: Context): MemoryCachedImageAccess {
			if (::instance.isInitialized) return instance

			val libraryConnectionProvider = LibraryConnectionProvider.get(context)
			val filePropertiesCache = FilePropertyCache.getInstance()
			val imageCacheKeyLookup = ImageCacheKeyLookup(CachedFilePropertiesProvider(
				libraryConnectionProvider,
				filePropertiesCache,
				FilePropertiesProvider(libraryConnectionProvider, filePropertiesCache)))

			instance = MemoryCachedImageAccess(
				DiskCacheImageAccess(
					RemoteImageAccess(
						libraryConnectionProvider),
					imageCacheKeyLookup,
					ImageDiskFileCacheFactory.getInstance(context)),
				imageCacheKeyLookup)

			return instance
		}
	}

	private val syncObject = Object()

	private val imageMemoryCache = LruCache<String, ByteArray>(MAX_MEMORY_CACHE_SIZE)

	@Volatile
	private var currentCacheAccessPromise = Promise(ByteArray(0))

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> =
		Promise(ImageOperator(libraryId, serviceFile))

	inner class ImageOperator internal constructor(private val libraryId: LibraryId, private val serviceFile: ServiceFile) : MessengerOperator<ByteArray> {
		override fun send(messenger: Messenger<ByteArray>) {
			val promisedCacheKey = imageCacheKeys.promiseImageCacheKey(libraryId, serviceFile)

            val cancellationProxy = CancellationProxy()
			messenger.cancellationRequested(cancellationProxy)
			cancellationProxy.doCancel(promisedCacheKey)

			val promiseProxy = PromiseProxy(messenger)
			val promisedBytes = promisedCacheKey
				.eventually { uniqueKey ->
					val cachedBytes = imageMemoryCache[uniqueKey]
					if (cachedBytes != null && cachedBytes.isNotEmpty()) cachedBytes.toPromise()
					else synchronized(syncObject) {
						currentCacheAccessPromise = currentCacheAccessPromise
							.then({ imageMemoryCache[uniqueKey] }, { imageMemoryCache[uniqueKey] })
							.eventually { bytes ->
								bytes?.toPromise() ?:
									sourceImages.promiseImageBytes(libraryId, serviceFile)
										.then {
											imageMemoryCache.put(uniqueKey, it)
											it
										}
							}

						currentCacheAccessPromise
					}
				}
			promiseProxy.proxy(promisedBytes)
		}
	}
}
