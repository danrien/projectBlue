package com.lasthopesoftware.bluewater.client.connection.trust.specs.GivenAMismatchedHostname;

import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class WhenVerifyingTheHostname {

	private static final HostnameVerifier defaultHostnameVerifier = mock(HostnameVerifier.class);

	private static boolean isHostnameValid;

	@BeforeClass
	public static void before() {
		final AdditionalHostnameVerifier additionalHostnameVerifier = new AdditionalHostnameVerifier("my-test-host-name", defaultHostnameVerifier);
		isHostnameValid = additionalHostnameVerifier.verify("my-other-test-host-name", null);
	}

	@Test
	public void thenTheHostnameIsValid() {
		assertThat(isHostnameValid).isFalse();
	}

	@Test
	public void thenTheDefaultHostnameVerifierIsAlsoAsked() {
		verify(defaultHostnameVerifier, atLeastOnce()).verify("my-other-test-host-name", null);
	}
}
