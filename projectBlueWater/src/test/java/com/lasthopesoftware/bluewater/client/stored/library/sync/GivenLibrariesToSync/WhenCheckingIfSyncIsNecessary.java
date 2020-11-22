package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenLibrariesToSync;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncChecker;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenCheckingIfSyncIsNecessary {

	private static boolean isSyncNeeded;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final ILibraryProvider fakeLibraryProvider = new FakeLibraryProvider(
			new Library().setId(3),
			new Library().setId(10));

		final SyncChecker syncChecker = new SyncChecker(
			fakeLibraryProvider,
			(l) -> {
				switch (l.getId()) {
					case 3: return new Promise<>(Arrays.asList(new ServiceFile(1), new ServiceFile(18)));
					case 10: return new Promise<>(Arrays.asList(new ServiceFile(3), new ServiceFile(6)));
					default: return new Promise<>(Collections.emptyList());
				}
			});
		isSyncNeeded = new FuturePromise<>(syncChecker.promiseIsSyncNeeded()).get();
	}

	@Test
	public void thenSyncIsNeeded() {
		assertThat(isSyncNeeded).isTrue();
	}
}
