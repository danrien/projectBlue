package com.lasthopesoftware.bluewater.client.stored.library.items.files.system;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class MediaQueryCursorProvider implements IMediaQueryCursorProvider, ImmediateResponse<Map<String, String>, Cursor> {

	private static final String mediaDataQuery = MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%' ";
	private static final String[] mediaQueryProjection = { MediaStore.Audio.Media.DATA };

	private final Context context;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;

	public MediaQueryCursorProvider(Context context, CachedFilePropertiesProvider cachedFilePropertiesProvider) {
		if (context == null)
			throw new IllegalArgumentException("Context cannot be null");

		this.context = context;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
	}

	@Override
	public Promise<Cursor> getMediaQueryCursor(ServiceFile serviceFile) {
		return
			cachedFilePropertiesProvider
				.promiseFileProperties(serviceFile)
				.then(this);
	}

	@Override
	public Cursor respond(Map<String, String> fileProperties) throws Exception {
		final String originalFilename = fileProperties.get(FilePropertiesProvider.FILENAME);
		if (originalFilename == null)
			throw new IOException("The filename property was not retrieved. A connection needs to be re-established.");

		final String filename = originalFilename.substring(originalFilename.lastIndexOf('\\') + 1, originalFilename.lastIndexOf('.'));

		final StringBuilder querySb = new StringBuilder(mediaDataQuery);
		appendAnd(querySb);

		final ArrayList<String> params = new ArrayList<>(5);
		params.add(filename);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ARTIST, fileProperties.get(FilePropertiesProvider.ARTIST));
		appendAnd(querySb);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ALBUM, fileProperties.get(FilePropertiesProvider.ALBUM));
		appendAnd(querySb);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TITLE, fileProperties.get(FilePropertiesProvider.NAME));
		appendAnd(querySb);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TRACK, fileProperties.get(FilePropertiesProvider.TRACK));

		return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaQueryProjection, querySb.toString(), params.toArray(new String[params.size()]), null);
	}

	private static StringBuilder appendPropertyFilter(final StringBuilder querySb, final ArrayList<String> params, final String key, final String value) {
		querySb.append(' ').append(key).append(' ');

		if (value != null) {
			querySb.append(" = ? ");
			params.add(value);
		} else {
			querySb.append(" IS NULL ");
		}

		return querySb;
	}

	private static StringBuilder appendAnd(final StringBuilder querySb) {
		return querySb.append(" AND ");
	}
}
