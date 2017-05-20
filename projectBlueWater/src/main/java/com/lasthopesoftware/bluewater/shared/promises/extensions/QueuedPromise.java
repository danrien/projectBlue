package com.lasthopesoftware.bluewater.shared.promises.extensions;

import com.lasthopesoftware.bluewater.shared.promises.WrappedCancellableExecutor;
import com.lasthopesoftware.bluewater.shared.promises.WrappedExecutor;
import com.lasthopesoftware.bluewater.shared.promises.WrappedFunction;
import com.lasthopesoftware.bluewater.shared.promises.WrappedMessenger;
import com.lasthopesoftware.promises.EmptyMessenger;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Created by david on 2/12/17.
 */

public class QueuedPromise<TResult> extends Promise<TResult> {
	public QueuedPromise(ThreeParameterAction<IResolvedPromise<TResult>, IRejectedPromise, OneParameterAction<Runnable>> task, Executor executor) {
		super(new Executors.QueuedCancellableTask<>(task, executor));
	}

	public QueuedPromise(TwoParameterAction<IResolvedPromise<TResult>, IRejectedPromise> task, Executor executor) {
		super(new Executors.QueuedTask<>(task, executor));
	}

	public QueuedPromise(Callable<TResult> task, Executor executor) {
		super(new Executors.QueuedFunction<>(task, executor));
	}

	public QueuedPromise(EmptyMessenger<TResult> messenger, Executor executor) {
		super(new Executors.QueuedMessenger<>(messenger, executor));
	}

	private static class Executors {
		static class QueuedCancellableTask<Result> extends EmptyMessenger<Result> {

			private final ThreeParameterAction<IResolvedPromise<Result>, IRejectedPromise, OneParameterAction<Runnable>> task;
			private final Executor executor;

			QueuedCancellableTask(ThreeParameterAction<IResolvedPromise<Result>, IRejectedPromise, OneParameterAction<Runnable>> task, Executor executor) {
				this.task = task;
				this.executor = executor;
			}

			@Override
			public void requestResolution() {
				this.executor.execute(new WrappedCancellableExecutor<>(task, this));
			}
		}

		static class QueuedTask<Result> extends EmptyMessenger<Result> {

			private final TwoParameterAction<IResolvedPromise<Result>, IRejectedPromise> task;
			private final Executor executor;

			QueuedTask(TwoParameterAction<IResolvedPromise<Result>, IRejectedPromise> task, Executor executor) {
				this.task = task;
				this.executor = executor;
			}

			@Override
			public void requestResolution() {
				this.executor.execute(new WrappedExecutor<>(task, this));
			}
		}

		static class QueuedFunction<Result> extends EmptyMessenger<Result> {

			private final Callable<Result> callable;
			private final Executor executor;

			QueuedFunction(Callable<Result> callable, Executor executor) {
				this.callable = callable;
				this.executor = executor;
			}

			@Override
			public void requestResolution() {
				this.executor.execute(new WrappedFunction<>(callable, this));
			}
		}

		static class QueuedMessenger<Result> extends EmptyMessenger<Result> {

			private final EmptyMessenger<Result> messenger;
			private final Executor executor;

			QueuedMessenger(EmptyMessenger<Result> messenger, Executor executor) {
				this.messenger = messenger;
				this.executor = executor;
			}

			@Override
			public void requestResolution() {
				executor.execute(new WrappedMessenger(messenger));
			}
		}
	}
}
