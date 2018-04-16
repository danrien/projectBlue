package com.lasthopesoftware.bluewater.client.playback.file.progress;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.joda.time.Duration;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class PollingProgressSource implements Runnable {

	private static final Executor notificationExecutor = Executors.newCachedThreadPool();

	private final ScheduledExecutorService pollingExecutor = Executors.newSingleThreadScheduledExecutor();
	private final ReentrantLock periodSync = new ReentrantLock();
	private final Object startSyncObject = new Object();

	private final Map<ObservableEmitter<FileProgress>, Long> progressEmitters = new ConcurrentHashMap<>();
	private final ReadFileProgress fileProgressReader;
	private final long minimalObservationPeriod;

	private long observationPeriodMilliseconds;
	private boolean isStarted;

	public PollingProgressSource(ReadFileProgress fileProgressReader, Duration minimalObservationPeriod) {
		this.fileProgressReader = fileProgressReader;
		this.minimalObservationPeriod = minimalObservationPeriod.getMillis();
	}

	public ObservableOnSubscribe<FileProgress> observePeriodically(Duration observationPeriod) {
		final long observationMilliseconds = observationPeriod.getMillis();
		periodSync.lock();
		try {
			observationPeriodMilliseconds = Math.max(
				Math.min(observationMilliseconds, observationPeriodMilliseconds),
				minimalObservationPeriod);
		} finally {
			periodSync.unlock();
		}

		return e -> {
			progressEmitters.put(e, observationMilliseconds);

			e.setDisposable(new Disposable() {
				@Override
				public void dispose() {
					progressEmitters.remove(e);

					periodSync.lock();
					try {
						if (observationMilliseconds > observationPeriodMilliseconds) return;

						final Optional<Long> maybeSmallestEmitter =
							Stream.of(progressEmitters.values())
								.sorted()
								.findFirst();

						observationPeriodMilliseconds = Math.max(maybeSmallestEmitter.orElse(minimalObservationPeriod), minimalObservationPeriod);
					} finally {
						periodSync.unlock();
					}
				}

				@Override
				public boolean isDisposed() {
					return !progressEmitters.containsKey(e);
				}
			});

			e.onNext(fileProgressReader.getFileProgress());

			synchronized (startSyncObject) {
				if (isStarted) return;

				pollingExecutor.schedule(this, observationPeriodMilliseconds, TimeUnit.MILLISECONDS);

				isStarted = true;
			}
		};
	}

	@Override
	public void run() {
		try {
			notificationExecutor.execute(
				new ProgressEmitter(
					fileProgressReader.getFileProgress(),
					progressEmitters.keySet()));
		} catch (Throwable t) {
			notificationExecutor.execute(new ErrorEmitter(t, progressEmitters.keySet()));
		}

		if (!progressEmitters.isEmpty()) {
			pollingExecutor.schedule(this, observationPeriodMilliseconds, TimeUnit.MILLISECONDS);
			return;
		}

		synchronized (startSyncObject) {
			isStarted = false;
		}
	}

	public void close() {
		pollingExecutor.shutdown();
	}

	private static class ProgressEmitter implements Runnable {

		private final FileProgress fileProgress;
		private final Set<ObservableEmitter<FileProgress>> emitters;

		ProgressEmitter(FileProgress fileProgress, Set<ObservableEmitter<FileProgress>> emitters) {
			this.fileProgress = fileProgress;
			this.emitters = emitters;
		}

		@Override
		public void run() {
			for (ObservableEmitter<FileProgress> emitter : emitters)
				emitter.onNext(fileProgress);
		}
	}

	private static class ErrorEmitter implements Runnable {

		private final Throwable error;
		private final Set<ObservableEmitter<FileProgress>> emitters;

		ErrorEmitter(Throwable error, Set<ObservableEmitter<FileProgress>> emitters) {
			this.error = error;
			this.emitters = emitters;
		}

		@Override
		public void run() {
			for (ObservableEmitter<FileProgress> emitter : emitters)
				emitter.onError(error);
		}
	}
}
