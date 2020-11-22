package com.namehillsoftware.projectblue.playback.engine.preparation;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

public interface BuildPreparedPlaybackQueueFeeder {
	IPlayableFilePreparationSourceProvider build(Library library);
}
