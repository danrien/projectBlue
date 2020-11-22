package com.namehillsoftware.projectblue.shared.promises;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.Duration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class PromiseDelay<Response> extends Promise<Response> implements Runnable {
	private static final CreateAndHold<ScheduledExecutorService> delayScheduler =
		new Lazy<>(() -> Executors.newScheduledThreadPool(0));

	public static <Response> Promise<Response> delay(Duration delay) {
		return new PromiseDelay<>(delay);
	}

	private PromiseDelay(Duration delay) {
		final ScheduledFuture<?> future = delayScheduler.getObject()
			.schedule(this, delay.getMillis(), TimeUnit.MILLISECONDS);

		respondToCancellation(new FutureCancellation(future));
	}

	@Override
	public void run() {
		resolve(null);
	}

	private static class FutureCancellation implements Runnable {
		private final ScheduledFuture<?> future;

		FutureCancellation(ScheduledFuture<?> future) {
			this.future = future;
		}

		@Override
		public void run() {
			future.cancel(false);
		}
	}
}
