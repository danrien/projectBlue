package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AudioRenderingEventListener
implements
	AudioRendererEventListener {

	private static final Logger logger = LoggerFactory.getLogger(AudioRenderingEventListener.class);

	private DecoderCounters audioDecoderCounters;
	private int audioSessionId;
	private Format audioFormat;

	// AudioRendererEventListener implementation
	@Override
	public void onAudioEnabled(DecoderCounters counters) {
		audioDecoderCounters = counters;
		logger.debug("Audio decoder counters updated");
	}

	@Override
	public void onAudioSessionId(int sessionId) {
		audioSessionId = sessionId;
		logger.debug("Audio session ID changed to " + sessionId);
	}

	@Override
	public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs,
										  long initializationDurationMs) {
		logger.debug("Audio decoder initialized. " +
			"decoderName=" + decoderName + ", " +
			"initializedTimestampMs=" + initializedTimestampMs + ", " +
			"initializationDurationMs=" + initializationDurationMs);
	}

	@Override
	public void onAudioInputFormatChanged(Format format) {
		logger.info("Audio format changed.");
		audioFormat = format;
	}

	@Override
	public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs,
									long elapsedSinceLastFeedMs) {
		logger.debug("Audio sink underrun occurred. " +
			"bufferSize=" + bufferSize + "," +
			"bufferSizeMs" + bufferSizeMs + "," +
			"elapsedSinceLastFeedMs" + elapsedSinceLastFeedMs);
	}

	@Override
	public void onAudioDisabled(DecoderCounters counters) {
		logger.debug("Audio disabled.");

		audioFormat = null;
		audioDecoderCounters = null;
		audioSessionId = C.AUDIO_SESSION_ID_UNSET;
	}
}
