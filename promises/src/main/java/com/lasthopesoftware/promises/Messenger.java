package com.lasthopesoftware.promises;

import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class Messenger<Input, Resolution> implements
	IResolvedPromise<Resolution>,
	IRejectedPromise,
	OneParameterAction<Runnable> {

	private final ReadWriteLock resolveSync = new ReentrantReadWriteLock();
	private final Queue<Messenger<Resolution, ?>> recipients = new ConcurrentLinkedQueue<>();
	private final Cancellation cancellation = new Cancellation();

	private boolean isResolved;
	private Resolution resolution;
	private Throwable rejection;

	protected abstract void requestResolution(Input input, Throwable throwable);

	private boolean isResolvedSynchronously() {
		final Lock readLock = resolveSync.readLock();
		readLock.lock();
		try {
			return isResolved;
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public final void sendRejection(Throwable error) {
		resolve(null, error);
	}

	@Override
	public final void sendResolution(Resolution resolution) {
		resolve(resolution, null);
	}

	@Override
	public final void runWith(Runnable response) {
		cancellation.runWith(response);
	}

	final void cancel() {
		if (!isResolvedSynchronously())
			cancellation.cancel();
	}

	final void awaitResolution(Messenger<Resolution, ?> recipient) {
		recipients.offer(recipient);

		dispatchMessage(resolution, rejection);
	}

	private void resolve(Resolution resolution, Throwable rejection) {
		resolveSync.writeLock().lock();
		try {
			if (isResolved) return;

			this.resolution = resolution;
			this.rejection = rejection;

			isResolved = true;
		} finally {
			resolveSync.writeLock().unlock();
		}

		dispatchMessage(resolution, rejection);
	}

	private synchronized void dispatchMessage(Resolution resolution, Throwable rejection) {
		if (!isResolvedSynchronously()) return;

		for (Messenger<Resolution, ?> r = recipients.poll(); r != null; r = recipients.poll())
			r.requestResolution(resolution, rejection);
	}
}
