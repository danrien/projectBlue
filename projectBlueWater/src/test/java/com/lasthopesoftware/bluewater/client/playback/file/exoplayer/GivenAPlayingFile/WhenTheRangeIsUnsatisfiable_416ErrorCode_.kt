package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import android.net.Uri
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException
import com.lasthopesoftware.any
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenTheRangeIsUnsatisfiable_416ErrorCode_ {

	companion object {
		private var exoPlayerException: ExoPlayerException? = null
		private var eventListener: Player.EventListener? = null
		private var isComplete = false

		@JvmStatic
		@BeforeClass
		@Throws(InterruptedException::class, TimeoutException::class, ExecutionException::class)
		fun before() {
			val mockExoPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockExoPlayer.setPlayWhenReady(ArgumentMatchers.anyBoolean())).thenReturn(mockExoPlayer.toPromise())
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(50L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation ->
				eventListener = invocation.getArgument(0)
				mockExoPlayer.toPromise()
			}.`when`(mockExoPlayer).addListener(any())
			val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val promisedFuture = FuturePromise(exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
				.eventually { obj: PlayingFile -> obj.promisePlayedFile() }
				.then(
					{ isComplete = true },
					{ e ->
						if (e is ExoPlayerException) {
							exoPlayerException = e
						}
						isComplete = false
					}))
			eventListener!!.onPlayerError(ExoPlaybackException.createForSource(InvalidResponseCodeException(416, "", HashMap(), DataSpec(Uri.EMPTY))))
			promisedFuture[1, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenPlaybackCompletes() {
		AssertionsForClassTypes.assertThat(isComplete).isTrue
	}

	@Test
	fun thenNoPlaybackErrorOccurs() {
		AssertionsForClassTypes.assertThat(exoPlayerException).isNull()
	}
}
