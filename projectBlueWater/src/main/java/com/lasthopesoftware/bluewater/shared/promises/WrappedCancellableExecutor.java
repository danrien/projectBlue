package com.lasthopesoftware.bluewater.shared.promises;

import com.lasthopesoftware.promises.EmptyMessenger;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Messenger;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

public class WrappedCancellableExecutor<Result> implements Runnable {
	private final Messenger<Result> messenger;
	private final OneParameterAction<Messenger<Result>> task;

	public WrappedCancellableExecutor(Messenger<Result> messenger, OneParameterAction<Messenger<Result>> task) {
		this.messenger = messenger;
		this.task = task;
	}

	@Override
	public void run() {
		task.runWith(messenger);
	}
}
