package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.support.annotation.Nullable;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;

/**
 * Created by david on 11/6/16.
 */

public class PositionedFile {
	public final int playlistPosition;
	public final File file;

	public PositionedFile(int playlistPosition, File file) {
		this.playlistPosition = playlistPosition;
		this.file = file;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof PositionedFile)) return false;

		final PositionedFile other = (PositionedFile)obj;

		return
			playlistPosition == other.playlistPosition &&
			file.equals(other.file);
	}
}
