package com.lasthopesoftware.bluewater.client.library.items.media.audio.specs.GivenACachedFileIsNotAvailable;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.audio.CachedAudioFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access.ICachedFilesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.RemoteFileUriProvider;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenProvidingTheUri {

	private static Uri cachedFileUri;

	@BeforeClass
	public static void before() {
		final RemoteFileUriProvider remoteFileUriProvider = mock(RemoteFileUriProvider.class);
		when(remoteFileUriProvider.promiseFileUri(any()))
			.thenReturn(Promise.empty());

		final ICachedFilesProvider cachedFilesProvider = mock(ICachedFilesProvider.class);
		when(cachedFilesProvider.promiseCachedFile("http://a-url/"));

		final CachedAudioFileUriProvider cachedAudioFileUriProvider =
			new CachedAudioFileUriProvider(
				remoteFileUriProvider,
				cachedFilesProvider);

		cachedAudioFileUriProvider
			.promiseFileUri(new ServiceFile(10))
			.then(uri -> cachedFileUri = uri);
	}

	@Test
	public void thenTheUriIsEmpty() {
		assertThat(cachedFileUri).isNull();
	}
}
