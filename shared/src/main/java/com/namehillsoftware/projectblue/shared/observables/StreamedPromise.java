package com.namehillsoftware.projectblue.shared.observables;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class StreamedPromise<T, S extends Iterable<T>> extends Observable<T> implements Disposable {

	public static <T, S extends Iterable<T>> Observable<T> stream(Promise<S> promise) {
		return new StreamedPromise<>(promise);
	}

	private final Promise<S> promise;

	private volatile boolean isCancelled;

	private StreamedPromise(Promise<S> promise) {
		this.promise = promise;
	}

	@Override
	protected void subscribeActual(Observer<? super T> observer) {
		observer.onSubscribe(this);

		promise
			.then(
				new VoidResponse<>(ts -> {
					for (final T t : ts) observer.onNext(t);
					observer.onComplete();
				}),
				new VoidResponse<>(observer::onError));
	}

	@Override
	public void dispose() {
		isCancelled = true;
		promise.cancel();
	}

	@Override
	public boolean isDisposed() {
		return isCancelled;
	}
}
