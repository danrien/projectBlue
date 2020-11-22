package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues;


import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.util.Arrays;

public class QueueProviders {
	private static final CreateAndHold<Iterable<IPositionedFileQueueProvider>> lazyProviders = new AbstractSynchronousLazy<Iterable<IPositionedFileQueueProvider>>() {
		@Override
		protected Iterable<IPositionedFileQueueProvider> create() throws Exception {
			return Arrays.asList(new CompletingFileQueueProvider(), new CyclicalFileQueueProvider());
		}
	};

	public static Iterable<IPositionedFileQueueProvider> providers() {
		return lazyProviders.getObject();
	}
}
