package com.lasthopesoftware.bluewater.shared.promises.extensions;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.MessageWriter;
import com.lasthopesoftware.messenger.promises.queued.PreparedMessengerOperator;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellableMessageWriter;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellablePreparedMessengerOperator;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;
import com.lasthopesoftware.messenger.promises.response.PromisedResponse;

public class LoopedInPromise<Result> extends Promise<Result> {

	public LoopedInPromise(MessageWriter<Result> task, Context context) {
		this(task, new Handler(context.getMainLooper()));
	}

	public LoopedInPromise(MessageWriter<Result> task, Handler handler) {
		super(new Executors.LoopedInResponse<>(new PreparedMessengerOperator<>(task), handler));
	}

	public LoopedInPromise(CancellableMessageWriter<Result> task, Handler handler) {
		super(new Executors.LoopedInResponse<>(new CancellablePreparedMessengerOperator<>(task), handler));
	}

	public LoopedInPromise(MessengerOperator<Result> task, Handler handler) {
		super(new Executors.LoopedInResponse<>(task, handler));
	}

	public static <TResult, TNewResult> PromisedResponse<TResult, TNewResult> response(ImmediateResponse<TResult, TNewResult> task, Context context) {
		return response(task, new Handler(context.getMainLooper()));
	}

	public static <TResult, TNewResult> PromisedResponse<TResult, TNewResult> response(ImmediateResponse<TResult, TNewResult> task, Handler handler) {
		return new OneParameterExecutors.ReducingFunction<>(task, handler);
	}

	private static class Executors {
		static final class LoopedInResponse<Result> implements MessengerOperator<Result>, Runnable {

			private final MessengerOperator<Result> task;
			private final Handler handler;
			private Messenger<Result> resultMessenger;

			LoopedInResponse(MessengerOperator<Result> task, Handler handler) {
				this.task = task;
				this.handler = handler;
			}

			@Override
			public void send(Messenger<Result> resultMessenger) {
				this.resultMessenger = resultMessenger;

				if (handler.getLooper().getThread() == Thread.currentThread())
					run();
				else
					handler.post(this);
			}

			@Override
			public void run() {
				task.send(resultMessenger);
			}
		}
	}

	private static class OneParameterExecutors {

		static class ReducingFunction<TResult, TNewResult> implements PromisedResponse<TResult, TNewResult>, MessageWriter<TNewResult> {

			private final ImmediateResponse<TResult, TNewResult> callable;
			private final Handler handler;

			private TResult result;

			ReducingFunction(ImmediateResponse<TResult, TNewResult> callable, Handler handler) {
				this.callable = callable;
				this.handler = handler;
			}

			@Override
			public TNewResult prepareMessage() throws Throwable {
				return callable.respond(result);
			}

			@Override
			public Promise<TNewResult> promiseResponse(TResult result) throws Throwable {
				this.result = result;
				return new LoopedInPromise<>(this, handler);
			}
		}
	}
}
