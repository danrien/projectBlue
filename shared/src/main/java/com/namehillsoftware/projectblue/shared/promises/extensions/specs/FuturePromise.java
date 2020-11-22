package com.namehillsoftware.projectblue.shared.promises.extensions.specs;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FuturePromise<Resolution> implements Future<Resolution> {

	private final CancellationProxy cancellationProxy = new CancellationProxy();
	private final Promise<Void> promise;

	private Resolution resolution;
	private Throwable rejection;
	private boolean isCompleted;

	public FuturePromise(Promise<Resolution> promise) {
		cancellationProxy.doCancel(promise);
		this.promise =
			promise
				.then(r -> {
					resolution = r;
					isCompleted = true;
					return null;
				}, e -> {
					rejection = e;
					isCompleted = true;
					return null;
				});
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (isCompleted) return false;

		cancellationProxy.run();
		return true;
	}

	@Override
	public boolean isCancelled() {
		return cancellationProxy.isCancelled();
	}

	@Override
	public boolean isDone() {
		return isCompleted;
	}

	@Override
	public Resolution get() throws InterruptedException, ExecutionException {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		promise
			.then(r -> {
				countDownLatch.countDown();
				return null;
			});
		countDownLatch.await();

		return getResolution();
	}

	@Override
	public Resolution get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		promise
			.then(r -> {
				countDownLatch.countDown();
				return null;
			});

		if (countDownLatch.await(timeout, unit))
			return getResolution();

		throw new TimeoutException();
	}

	private Resolution getResolution() throws ExecutionException {
		if (rejection == null)
			return resolution;

		throw new ExecutionException(rejection);
	}
}
