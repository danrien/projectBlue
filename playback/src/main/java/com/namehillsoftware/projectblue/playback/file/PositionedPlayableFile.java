package com.namehillsoftware.projectblue.playback.file;

import androidx.annotation.NonNull;

import com.namehillsoftware.projectblue.playback.file.volume.ManagePlayableFileVolume;

public final class PositionedPlayableFile implements Comparable<PositionedPlayableFile> {
	private final PlayableFile playbackHandler;
	private final ManagePlayableFileVolume playableFileVolumeManager;
	private final PositionedFile positionedFile;

	public PositionedPlayableFile(int playlistPosition, PlayableFile playbackHandler, ManagePlayableFileVolume playableFileVolumeManager, ServiceFile serviceFile) {
		this(playbackHandler, playableFileVolumeManager, new PositionedFile(playlistPosition, serviceFile));
	}

	public PositionedPlayableFile(PlayableFile playbackHandler, ManagePlayableFileVolume playableFileVolumeManager, PositionedFile positionedFile) {
		this.playableFileVolumeManager = playableFileVolumeManager;
		this.positionedFile = positionedFile;
		this.playbackHandler = playbackHandler;
	}

	public int getPlaylistPosition() {
		return positionedFile.getPlaylistPosition();
	}

	public ServiceFile getServiceFile() {
		return positionedFile.getServiceFile();
	}

	public PlayableFile getPlayableFile() {
		return playbackHandler;
	}

	public ManagePlayableFileVolume getPlayableFileVolumeManager() {
		return playableFileVolumeManager;
	}

	@Override
	public int compareTo(@NonNull PositionedPlayableFile other) {
		return positionedFile.compareTo(other.positionedFile);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PositionedPlayableFile && compareTo((PositionedPlayableFile)obj) == 0;
	}

	@Override
	public int hashCode() {
		return positionedFile.hashCode();
	}

	public PositionedFile asPositionedFile() {
		return positionedFile;
	}
}
