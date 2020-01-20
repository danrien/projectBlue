package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.nowplaying;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface INowPlayingFileProvider {
	Promise<ServiceFile> getNowPlayingFile();
}
