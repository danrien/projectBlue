package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.IDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class ExtractorMediaSourceFactoryProvider {

	private final Context context;
	private final Library library;
	private final Cache cache;

	private static final CreateAndHold<ExtractorsFactory> extractorsFactory = new Lazy<>(() -> Mp3Extractor.FACTORY);

	private final CreateAndHold<ExtractorMediaSource.Factory> lazyFileExtractorFactory = new AbstractSynchronousLazy<ExtractorMediaSource.Factory>() {
		@Override
		protected ExtractorMediaSource.Factory create() {
			final ExtractorMediaSource.Factory factory = new ExtractorMediaSource.Factory(new FileDataSourceFactory());
			factory.setMinLoadableRetryCount(ExtractorMediaSource.DEFAULT_MIN_LOADABLE_RETRY_COUNT_LIVE);
			factory.setExtractorsFactory(extractorsFactory.getObject());
			return factory;
		}
	};

	private final CreateAndHold<ExtractorMediaSource.Factory> lazyRemoteExtractorFactory = new AbstractSynchronousLazy<ExtractorMediaSource.Factory>() {
		@Override
		protected ExtractorMediaSource.Factory create() throws NoSuchAlgorithmException, KeyManagementException {
			final X509TrustManager trustManager = new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			};

			final SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { trustManager }, null);
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			final OkHttpDataSourceFactory httpDataSourceFactory = new OkHttpDataSourceFactory(
				new OkHttpClient.Builder()
					.readTimeout(45, TimeUnit.SECONDS)
					.retryOnConnectionFailure(false)
					.sslSocketFactory(sslSocketFactory, trustManager)
					.build(),
				Util.getUserAgent(context, context.getString(R.string.app_name)),
				null);

			final String authKey = library.getAuthKey();

			if (authKey != null && !authKey.isEmpty())
				httpDataSourceFactory.getDefaultRequestProperties().set("Authorization", "basic " + authKey);

			final CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(
				cache,
				httpDataSourceFactory);

			final ExtractorMediaSource.Factory factory = new ExtractorMediaSource.Factory(cacheDataSourceFactory);
			factory.setMinLoadableRetryCount(ExtractorMediaSource.DEFAULT_MIN_LOADABLE_RETRY_COUNT_LIVE);
			factory.setExtractorsFactory(extractorsFactory.getObject());
			return factory;
		}
	};

	public ExtractorMediaSourceFactoryProvider(Context context, Library library, IDiskCacheDirectoryProvider diskCacheDirectory, Cache cache) {
		this.context = context;
		this.library = library;
		this.cache = cache;
	}

	public ExtractorMediaSource.Factory getFactory(Uri uri) {
		return uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)
			? lazyFileExtractorFactory.getObject()
			: lazyRemoteExtractorFactory.getObject();
	}
}
