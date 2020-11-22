package com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.IProvideCaches
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.resources.scheduling.ParsingScheduler
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.projectblue.shared.promises.extensions.toPromise
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.*

class DiskCacheImageAccess(private val sourceImages: GetRawImages, private val imageCacheKeys: LookupImageCacheKey, private val caches: IProvideCaches) : GetRawImages {

	companion object {
		private val logger = LoggerFactory.getLogger(DiskCacheImageAccess::class.java)

		fun getBytesFromFiles(file: File): ByteArray {
			try {
				FileInputStream(file).use { fis ->
					ByteArrayOutputStream().use { buffer ->
						IOUtils.copy(fis, buffer)
						return buffer.toByteArray()
					}
				}
			} catch (e: FileNotFoundException) {
				logger.error("Could not find cached file.", e)
				return ByteArray(0)
			} catch (e: IOException) {
				logger.error("Error reading cached file.", e)
				return ByteArray(0)
			}
		}
	}

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
					caches.promiseCache(libraryId)
						.eventually { cache ->
							cache?.promiseCachedFile(uniqueKey)
								?.eventually { imageFile ->
									if (imageFile != null) QueuedPromise(ImageDiskCacheWriter(imageFile), ParsingScheduler.instance().scheduler)
									else Promise.empty<ByteArray?>()
								}
								?.eventually { bytes ->
									bytes?.toPromise() ?: sourceImages.promiseImageBytes(libraryId, serviceFile)
										.then {
											cache.put(uniqueKey, it).excuse { ioe -> logger.error("Error writing cached file!", ioe) }

											it
										}
								}
								?: Promise.empty()
						}
				}
			promiseProxy.proxy(promisedBytes)
		}
	}

	private class ImageDiskCacheWriter(private val imageCacheFile: File) : MessageWriter<ByteArray> {
		override fun prepareMessage(): ByteArray = getBytesFromFiles(imageCacheFile)
	}
}
