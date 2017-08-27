package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.messenger.promises.Promise;

public interface INowPlayingFileProvider {
	Promise<ServiceFile> getNowPlayingFile();
}
