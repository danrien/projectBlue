package com.namehillsoftware.projectblue.shared.promises.extensions;

import android.content.Context;
import android.os.Handler;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.MessageWriter;
import com.namehillsoftware.handoff.promises.queued.PreparedMessengerOperator;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellablePreparedMessengerOperator;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

import org.joda.time.Duration;

public class LoopedInPromise<Result> extends Promise<Result> {

	public LoopedInPromise(MessageWriter<Result> task, Context context) {
		this(task, new Handler(context.getMainLooper()));
	}

	public LoopedInPromise(MessageWriter<Result> task, Handler handler) {
		super(new Executors.LoopedInResponse<>(new PreparedMessengerOperator<>(task), handler));
	}

	public LoopedInPromise(MessageWriter<Result> task, Context context, Duration delay) {
		this(task, new Handler(context.getMainLooper()), delay);
	}

	public LoopedInPromise(MessageWriter<Result> task, Handler handler, Duration delay) {
		super(new Executors.DelayedLoopedInPromise<>(new PreparedMessengerOperator<>(task), handler, delay));
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

		static final class DelayedLoopedInPromise<Result> implements MessengerOperator<Result>, Runnable {
			private final MessengerOperator<Result> task;
			private final Handler handler;
			private final Duration delay;
			private Messenger<Result> resultMessenger;

			DelayedLoopedInPromise(MessengerOperator<Result> task, Handler handler, Duration delay) {
				this.task = task;
				this.handler = handler;
				this.delay = delay;
			}

			@Override
			public void send(Messenger<Result> resultMessenger) {
				this.resultMessenger = resultMessenger;
				handler.postDelayed(this, (int)delay.getMillis());
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
			public Promise<TNewResult> promiseResponse(TResult result) {
				this.result = result;
				return new LoopedInPromise<>(this, handler);
			}
		}
	}
}
