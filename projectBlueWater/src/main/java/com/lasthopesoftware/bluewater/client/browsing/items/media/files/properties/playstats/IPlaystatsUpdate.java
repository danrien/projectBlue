package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface IPlaystatsUpdate {
	Promise<?> promisePlaystatsUpdate(ServiceFile serviceFile);
}
