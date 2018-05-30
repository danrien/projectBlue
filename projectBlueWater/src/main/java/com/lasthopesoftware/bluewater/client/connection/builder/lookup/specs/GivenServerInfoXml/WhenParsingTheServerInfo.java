package com.lasthopesoftware.bluewater.client.connection.builder.lookup.specs.GivenServerInfoXml;

import com.lasthopesoftware.bluewater.client.connection.builder.lookup.RequestServerInfoXml;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenParsingTheServerInfo {

	private static ServerInfo serverInfo;

	@BeforeClass
	public static void before() throws XmlParseException, ExecutionException, InterruptedException {
		final RequestServerInfoXml serverInfoXml = mock(RequestServerInfoXml.class);
		when(serverInfoXml.promiseServerInfoXml(any()))
			.thenReturn(new Promise<>(Xmlwise.createXml(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
					"<Response Status=\"OK\">\n" +
					"<keyid>gooPc</keyid>\n" +
					"<ip>108.491.23.154</ip>\n" +
					"<port>52199</port>\n" +
					"<localiplist>169.254.72.216,192.168.1.50</localiplist>\n" +
					"<certificate_fingerprint>746E06046B44CED35658F300DB2D08A799DEBC7E</certificate_fingerprint>\n" +
					"<https_port>52200</https_port>\n" +
				"</Response>")));

		final ServerLookup serverLookup = new ServerLookup(serverInfoXml);
		serverInfo = new FuturePromise<>(serverLookup.promiseServerInformation(new Library())).get();
	}

	@Test
	public void thenTheRemoteIpIsCorrect() {
		assertThat(serverInfo.getRemoteIp()).isEqualTo("108.491.23.154");
	}

	@Test
	public void thenTheLocalIpsAreCorrect() {
		assertThat(serverInfo.getLocalIps()).contains("169.254.72.216", "192.168.1.50");
	}

	@Test
	public void thenTheHttpPortIsCorrect() {
		assertThat(serverInfo.getHttpPort()).isEqualTo(52199);
	}

	@Test
	public void thenTheHttpsPortIsCorrect() {
		assertThat(serverInfo.getHttpsPort()).isEqualTo(52200);
	}

	@Test
	public void thenTheCertificateFingerprintIsCorrect() {
		assertThat(serverInfo.getCertificateFingerprint()).isEqualToIgnoringCase("746E06046B44CED35658F300DB2D08A799DEBC7E");
	}
}
