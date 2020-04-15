package com.lasthopesoftware.bluewater.client.browsing.items.media.image.raw

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.IProvideCaches
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.LookupImageCacheKey
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider
import com.lasthopesoftware.resources.scheduling.ParsingScheduler
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.*

class DiskCacheImageAccess(private val imageCacheKeys: LookupImageCacheKey, private val caches: IProvideCaches, private val selectedLibraryIdentifierProvider: ISelectedLibraryIdentifierProvider, connectionProvider: IConnectionProvider) : RemoteImageAccess(connectionProvider) {
	override fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray> {
		return Promise(ImageOperator(serviceFile))
	}

	inner class ImageOperator internal constructor(private val serviceFile: ServiceFile) : MessengerOperator<ByteArray> {
		override fun send(messenger: Messenger<ByteArray>) {
			val promisedCacheKey = imageCacheKeys.promiseImageCacheKey(serviceFile);

			val cancellationProxy = CancellationProxy()
			messenger.cancellationRequested(cancellationProxy)
			cancellationProxy.doCancel(promisedCacheKey)

			val promiseProxy = PromiseProxy(messenger)
			val promisedBytes = promisedCacheKey
				.eventually { uniqueKey ->
					caches.promiseCache(selectedLibraryIdentifierProvider.selectedLibraryId)
						.eventually { cache ->
							cache.promiseCachedFile(uniqueKey)
								.eventually { imageFile -> QueuedPromise(ImageDiskCacheWriter(imageFile), ParsingScheduler.instance().scheduler) }
								.eventually { bytes ->
									if (bytes.isNotEmpty()) Promise(bytes)
									else super@DiskCacheImageAccess.promiseImageBytes(serviceFile)
										.then {
											cache.put(uniqueKey, it)
												.excuse { ioe -> logger.error("Error writing cached file!", ioe) }

											it
										}
								}
						}
				}
			promiseProxy.proxy(promisedBytes)
		}
	}

	private class ImageDiskCacheWriter internal constructor(private val imageCacheFile: File) : MessageWriter<ByteArray> {
		override fun prepareMessage(): ByteArray {
			return getBytesFromFiles(imageCacheFile)
		}
	}

	companion object {
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

		private val logger = LoggerFactory.getLogger(DiskCacheImageAccess::class.java)
	}
}
