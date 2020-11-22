package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import android.content.Context
import android.os.Handler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.BestMatchUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources

class PreparedPlaybackQueueFeederBuilder(
        private val context: Context,
        private val handler: Handler,
        private val mediaSourceProvider: com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources,
        private val bestMatchUriProvider: BestMatchUriProvider) : BuildPreparedPlaybackQueueFeeder {

	override fun build(library: Library): IPlayableFilePreparationSourceProvider =
            com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider(
                    context,
                    handler,
                    mediaSourceProvider,
                    bestMatchUriProvider)
}
