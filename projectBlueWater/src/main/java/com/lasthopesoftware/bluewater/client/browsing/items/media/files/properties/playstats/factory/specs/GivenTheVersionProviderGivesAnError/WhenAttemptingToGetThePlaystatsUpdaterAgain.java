package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.specs.GivenTheVersionProviderGivesAnError;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.specs.FakeFilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider;
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenAttemptingToGetThePlaystatsUpdaterAgain {

	private static ExecutionException exception;
	private static IPlaystatsUpdate updater;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();

		final IProgramVersionProvider programVersionProvider = mock(IProgramVersionProvider.class);
		when(programVersionProvider.promiseServerVersion())
			.thenReturn(new Promise<>(new Exception(":(")))
			.thenReturn(new Promise<>(new SemanticVersion(22, 0, 0)));

		final FakeFilePropertiesContainer fakeFilePropertiesContainer = new FakeFilePropertiesContainer();
		final PlaystatsUpdateSelector playstatsUpdateSelector = new PlaystatsUpdateSelector(
			fakeConnectionProvider,
			new SessionFilePropertiesProvider(fakeConnectionProvider, fakeFilePropertiesContainer),
			new FilePropertiesStorage(fakeConnectionProvider, fakeFilePropertiesContainer),
			programVersionProvider);

		try {
			new FuturePromise<>(playstatsUpdateSelector.promisePlaystatsUpdater()).get();
		} catch (ExecutionException e) {
			exception = e;
		}

		updater = new FuturePromise<>(playstatsUpdateSelector.promisePlaystatsUpdater()).get();
	}

	@Test
	public void thenTheExceptionIsThrown() {
		assertThat(exception).isNotNull();
	}

	@Test
	public void thenThePlayedFilePlaystatsUpdaterIsGiven() {
		assertThat(updater).isInstanceOf(PlayedFilePlayStatsUpdater.class);
	}
}
