package com.namehillsoftware.projectblue.playback.file.volume;

import com.namehillsoftware.handoff.promises.Promise;

public interface ProvideMaxFileVolume {
	Promise<Float> promiseMaxFileVolume(ServiceFile serviceFile);
}
