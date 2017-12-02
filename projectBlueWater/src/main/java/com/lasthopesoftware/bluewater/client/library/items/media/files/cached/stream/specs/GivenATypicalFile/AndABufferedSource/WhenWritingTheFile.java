package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.specs.GivenATypicalFile.AndABufferedSource;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.IDiskFileCachePersistence;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.CachedFileOutputStream;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import okio.Buffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenWritingTheFile {

	private static final File file = new File("test");
	private static final byte[] bytes = new byte[2000000];

	private static final byte[] bytesWritten = new byte[2000000];

	static {
		file.deleteOnExit();
		new Random().nextBytes(bytes);
	}

	@BeforeClass
	public static void before() throws InterruptedException {
		final CachedFileOutputStream cachedFileOutputStream = new CachedFileOutputStream("unique-test", file, mock(IDiskFileCachePersistence.class));
		final Buffer buffer = new Buffer();
		buffer.write(bytes);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		cachedFileOutputStream.promiseWrite(buffer)
			.then(w -> {
				try (final FileInputStream fis = new FileInputStream(file)) {
					fis.read(bytesWritten, 0, bytesWritten.length);
				}
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheBytesAreWrittenCorrectly() {
		assertThat(bytesWritten).isEqualTo(bytes);
	}
}
