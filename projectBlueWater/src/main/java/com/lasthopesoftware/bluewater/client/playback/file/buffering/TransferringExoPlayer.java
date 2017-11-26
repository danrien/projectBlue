package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class TransferringExoPlayer<S extends DataSource>
implements
	IBufferingPlaybackFile,
	MessengerOperator<IBufferingPlaybackFile>,
	TransferListener<S>
{

	private final CreateAndHold<Promise<IBufferingPlaybackFile>> bufferingPlaybackFilePromise = new AbstractSynchronousLazy<Promise<IBufferingPlaybackFile>>() {
		@Override
		protected Promise<IBufferingPlaybackFile> create() throws Exception {
			return new Promise<>((MessengerOperator<IBufferingPlaybackFile>) TransferringExoPlayer.this);
		}
	};

	private Messenger<IBufferingPlaybackFile> messenger;
	private boolean isTransferComplete;

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return bufferingPlaybackFilePromise.getObject();
	}

	@Override
	public void send(Messenger<IBufferingPlaybackFile> messenger) {
		this.messenger = messenger;
		if (isTransferComplete)
			messenger.sendResolution(this);
	}

	@Override
	public void onTransferStart(S source, DataSpec dataSpec) {
	}

	@Override
	public void onBytesTransferred(S source, int bytesTransferred) {

	}

	@Override
	public void onTransferEnd(S source) {
		isTransferComplete = true;
		if (messenger != null)
			messenger.sendResolution(this);
	}
}
