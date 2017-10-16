package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.IDiskFileCachePersistence;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.ILazy;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CachedFileOutputStream implements Closeable {

	private static final ExecutorService cachedFileWriteExecutor = Executors.newCachedThreadPool();

	private final String uniqueKey;
	private final File file;
	private final IDiskFileCachePersistence diskFileCachePersistence;
	private final ILazy<FileOutputStream> lazyFileOutputStream = new AbstractSynchronousLazy<FileOutputStream>() {
		@Override
		protected FileOutputStream initialize() throws Exception {
			return new FileOutputStream(file);
		}
	};

	public CachedFileOutputStream(String uniqueKey, File file, IDiskFileCachePersistence diskFileCachePersistence) {
		this.uniqueKey = uniqueKey;
		this.file = file;
		this.diskFileCachePersistence = diskFileCachePersistence;
	}

	public Promise<CachedFileOutputStream> promiseWrite(byte[] buffer, int offset, int length) {
		return new QueuedPromise<>(() -> {
			lazyFileOutputStream.getObject().write(buffer, offset, length);
			return this;
		}, cachedFileWriteExecutor);
	}

	public Promise<CachedFileOutputStream> flush() {
		return new QueuedPromise<>(() -> {
			if (lazyFileOutputStream.isInitialized())
				lazyFileOutputStream.getObject().flush();

			return this;
		}, cachedFileWriteExecutor);
	}

	public Promise<CachedFile> commitToCache() {
		return diskFileCachePersistence.putIntoDatabase(uniqueKey, file);
	}

	@Override
	public void close() throws IOException {
		if (lazyFileOutputStream.isInitialized())
			lazyFileOutputStream.getObject().close();
	}
}
