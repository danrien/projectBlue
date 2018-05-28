package com.lasthopesoftware.bluewater.client.connection.builder;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlScanner implements BuildUrlProviders {

	private final TestConnections connectionTester;

	public UrlScanner(TestConnections connectionTester) {
		this.connectionTester = connectionTester;
	}

	@Override
	public Promise<IUrlProvider> promiseBuiltUrlProvider(Library library) {
		if (library == null)
			return new Promise<>(new IllegalArgumentException("The library cannot be null"));

		if (library.getAccessCode() == null)
			return new Promise<>(new IllegalArgumentException("The access code cannot be null"));


		final MediaServerUrlProvider mediaServerUrlProvider;
		try {
			mediaServerUrlProvider = new MediaServerUrlProvider(
				library.getAuthKey(),
				parseAccessCode(library.getAccessCode()));
		} catch (MalformedURLException e) {
			return new Promise<>(e);
		}

		return connectionTester.promiseIsConnectionPossible(new ConnectionProvider(mediaServerUrlProvider))
			.then(isValid -> mediaServerUrlProvider);
	}

	private static URL parseAccessCode(String accessCode) throws MalformedURLException {
		final String[] urlParts = accessCode.split(":", 2);
		final int port =
			urlParts.length > 1 && isPositiveInteger(urlParts[1])
				? Integer.parseInt(urlParts[1])
				: 80;
		return new URL("http", urlParts[0], port, "");
	}

	private static boolean isPositiveInteger(String string) {
		for (final char c : string.toCharArray())
			if (!Character.isDigit(c)) return false;

		return true;
	}
}
