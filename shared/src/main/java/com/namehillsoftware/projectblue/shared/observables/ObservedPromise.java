package com.namehillsoftware.projectblue.shared.observables;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class ObservedPromise<T> extends Observable<T> implements Disposable {

	private volatile boolean isCancelled;

	public static <T> Observable<T> observe(Promise<T> promise) {
		return new ObservedPromise<>(promise);
	}

	private final Promise<T> promise;

	private ObservedPromise(Promise<T> promise) {
		this.promise = promise;
	}

	@Override
	protected void subscribeActual(Observer<? super T> observer) {
		observer.onSubscribe(this);

		promise
			.then(
				new VoidResponse<>(t -> {
					observer.onNext(t);
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
