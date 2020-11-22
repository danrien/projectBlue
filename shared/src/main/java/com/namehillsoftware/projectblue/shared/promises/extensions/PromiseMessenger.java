package com.namehillsoftware.projectblue.shared.promises.extensions;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;

public class PromiseMessenger<Resolution> extends Promise<Resolution> implements Messenger<Resolution> {
	@Override
	public void sendResolution(Resolution resolution) {
		resolve(resolution);
	}

	@Override
	public void sendRejection(Throwable error) {
		reject(error);
	}

	@Override
	public void cancellationRequested(Runnable response) {
		respondToCancellation(response);
	}
}
