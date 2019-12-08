package com.lasthopesoftware.bluewater.client.connection.okhttp;

import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier;
import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.resources.CachedSingleThreadExecutor;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class OkHttpFactory implements ProvideOkHttpClients {

	private static final CreateAndHold<ExecutorService> executor = new Lazy<>(CachedSingleThreadExecutor::new);

	private static final CreateAndHold<OkHttpClient.Builder> lazyCommonBuilder = new AbstractSynchronousLazy<OkHttpClient.Builder>() {
		@Override
		protected OkHttpClient.Builder create() {
			return new OkHttpClient.Builder()
				.addNetworkInterceptor(chain -> {
					final Request.Builder requestBuilder = chain.request().newBuilder().addHeader("Connection", "close");
					return chain.proceed(requestBuilder.build());
				})
				.cache(null)
				.readTimeout(3, TimeUnit.MINUTES)
				.connectTimeout(5, TimeUnit.SECONDS)
				.dispatcher(new Dispatcher(executor.getObject()));
		}
	};

	private static final CreateAndHold<OkHttpFactory> lazyHttpFactory = new Lazy<>(OkHttpFactory::new);

	public static OkHttpFactory getInstance() {
		return lazyHttpFactory.getObject();
	}

	private OkHttpFactory() {}

	@Override
	public OkHttpClient getOkHttpClient(IUrlProvider urlProvider) {
		return lazyCommonBuilder.getObject()
			.addNetworkInterceptor(chain -> {
				final Request.Builder requestBuilder = chain.request().newBuilder();

				final String authCode = urlProvider.getAuthCode();

				if (authCode != null && !authCode.isEmpty())
					requestBuilder.addHeader("Authorization", "basic " + urlProvider.getAuthCode());

				return chain.proceed(requestBuilder.build());
			})
			.sslSocketFactory(getSslSocketFactory(urlProvider), getTrustManager(urlProvider))
			.hostnameVerifier(getHostnameVerifier(urlProvider))
			.build();
	}

	private static SSLSocketFactory getSslSocketFactory(IUrlProvider urlProvider) {
		final SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		try {
			sslContext.init(null, new TrustManager[] { getTrustManager(urlProvider) }, null);
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		}

		return sslContext.getSocketFactory();
	}

	private static X509TrustManager getTrustManager(IUrlProvider urlProvider) {
		final TrustManagerFactory trustManagerFactory;
		try {
			trustManagerFactory = TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		try {
			trustManagerFactory.init((KeyStore) null);
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		}

		final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
		if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
			throw new IllegalStateException("Unexpected default trust managers:"
				+ Arrays.toString(trustManagers));
		}

		final X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
		return urlProvider.getCertificateFingerprint().length == 0
			? trustManager
			: new SelfSignedTrustManager(urlProvider.getCertificateFingerprint(), trustManager);
	}

	private static HostnameVerifier getHostnameVerifier(IUrlProvider urlProvider) {
		final HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

		try {
			return urlProvider.getCertificateFingerprint().length == 0
				? defaultHostnameVerifier
				: new AdditionalHostnameVerifier(new URL(urlProvider.getBaseUrl()).getHost(), defaultHostnameVerifier);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
