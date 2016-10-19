package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.FourParameterRunnable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.ThreeParameterRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DependentPromise<TInput, TResult> implements IPromise<TResult> {

	private final FourParameterRunnable<TInput, Exception, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor;

	private DependentPromise<TResult, ?> resolution;
	private final Object resolutionSync = new Object();

	private TResult fulfilledResult;
	private Exception fulfilledError;
	private final Object fulfilledResultSync = new Object();
	private final Object fulfilledErrorSync = new Object();

	DependentPromise(@NotNull FourParameterRunnable<TInput, Exception, OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		this.executor = executor;
	}

	final void provide(@Nullable TInput input, @Nullable Exception exception) {
		this.executor.run(input, exception, result -> {
			synchronized (fulfilledResultSync) {
				fulfilledResult = result;
			}

			synchronized (resolutionSync) {
				if (resolution != null)
					resolution.provide(result, null);
			}
		}, error -> {
			synchronized (fulfilledErrorSync) {
				fulfilledError = error;
			}

			synchronized (resolutionSync) {
				if (resolution != null)
					resolution.provide(null, error);
			}
		});
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> then(@NotNull FourParameterRunnable<TResult, Exception, OneParameterRunnable<TNewResult>, OneParameterRunnable<Exception>> onFulfilled) {
		final DependentPromise<TResult, TNewResult> newResolution = new DependentPromise<>(onFulfilled);

		synchronized (resolutionSync) {
			resolution = newResolution;
		}

		synchronized (fulfilledResultSync) {
			if (fulfilledResult != null)
				newResolution.provide(fulfilledResult, null);
		}

		synchronized (fulfilledErrorSync) {
			if (fulfilledError != null)
				newResolution.provide(null, fulfilledError);
		}

		return newResolution;
	}

	@NotNull
	@Override
	public <TNewResult> IPromise<TNewResult> then(@NotNull ThreeParameterRunnable<TResult, OneParameterRunnable<TNewResult>, OneParameterRunnable<Exception>> onFulfilled) {
		return then(new ErrorPropagatingResolveExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public final <TNewResult> IPromise<TNewResult> then(@NotNull final OneParameterCallable<TResult, TNewResult> onFulfilled) {
		return then(new ExpectedResultExecutor<>(onFulfilled));
	}

	@NotNull
	@Override
	public final IPromise<Void> then(@NotNull OneParameterRunnable<TResult> onFulfilled) {
		return then(new NullReturnRunnable<>(onFulfilled));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull ThreeParameterRunnable<Exception, OneParameterRunnable<TNewRejectedResult>, OneParameterRunnable<Exception>> onRejected) {
		return then(new RejectionDependentExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public <TNewRejectedResult> IPromise<TNewRejectedResult> error(@NotNull OneParameterCallable<Exception, TNewRejectedResult> onRejected) {
		return error(new ExpectedResultExecutor<>(onRejected));
	}

	@NotNull
	@Override
	public final IPromise<Void> error(@NotNull OneParameterRunnable<Exception> onRejected) {
		return error(new NullReturnRunnable<>(onRejected));
	}

}
