package com.lasthopesoftware.bluewater.client.browsing.items.media.image.GivenAServiceFile.WithoutAnImage;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.RemoteImageAccess;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingTheImageBytes {

	private static byte[] imageBytes;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();
		fakeConnectionProvider.mapResponse(
			p -> new FakeConnectionResponseTuple(500, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\r\n<Response Status=\"Failure\"/>\r\n".getBytes()),
			"File/GetImage", "File=31", "Type=Full", "Pad=1", "Format=jpg", "FillTransparency=ffffff");

		final RemoteImageAccess memoryCachedImageAccess = new RemoteImageAccess(
			new FakeLibraryConnectionProvider(
				new HashMap<LibraryId, IConnectionProvider>() {
					{
						put(new LibraryId(21), fakeConnectionProvider);
					}
				}
			)
		);

		imageBytes = new FuturePromise<>(memoryCachedImageAccess.promiseImageBytes(
			new LibraryId(21), new ServiceFile(31))).get();
	}

	@Test
	public void thenTheBytesAreEmpty() {
		assertThat(imageBytes).isEmpty();
	}
}
