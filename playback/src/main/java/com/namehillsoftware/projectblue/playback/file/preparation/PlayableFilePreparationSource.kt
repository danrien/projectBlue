package com.namehillsoftware.projectblue.playback.file.preparation

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface PlayableFilePreparationSource {
	fun promisePreparedPlaybackFile(serviceFile: ServiceFile, preparedAt: Long): Promise<PreparedPlayableFile>
}
