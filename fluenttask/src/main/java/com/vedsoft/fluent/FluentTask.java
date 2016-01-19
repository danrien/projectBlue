package com.vedsoft.fluent;

import android.os.AsyncTask;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.callables.TwoParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;
import com.vedsoft.lazyj.Lazy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public abstract class FluentTask<TParams, TProgress, TResult>  {

	private final TParams[] params;
	private final Executor defaultExecutor;

	private TwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TProgress[]> twoParameterOnProgressListener;
	private OneParameterRunnable<TProgress[]> oneParameterOnProgressListener;

	private TwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TResult> twoParameterOnCompleteListener;
	private OneParameterRunnable<TResult> oneParameterOnCompleteListener;

	private OneParameterCallable<Exception, Boolean> oneParameterOnErrorListener;
	private TwoParameterCallable<FluentTask<TParams, TProgress, TResult>, Exception, Boolean> twoParameterOnErrorListener;

	private final Lazy<AndroidAsyncTask<Void, TProgress, TResult>> task = new Lazy<AndroidAsyncTask<Void, TProgress, TResult>>() {
		@Override
		protected AndroidAsyncTask<Void, TProgress, TResult> initialize() {
			return new AndroidAsyncTask<Void, TProgress, TResult>(){

				@Override
				protected final TResult doInBackground(Void... params) {
					return executeInBackground(FluentTask.this.params);
				}

				@Override
				protected void onProgressUpdate(TProgress... values) {
					if (twoParameterOnProgressListener != null)
						twoParameterOnProgressListener.run(FluentTask.this, values);

					if (oneParameterOnProgressListener != null)
						oneParameterOnProgressListener.run(values);
				}

				@Override
				protected final void onPostExecute(TResult result, Exception exception) {
					if (handleError(exception)) return;

					if (twoParameterOnCompleteListener != null)
						twoParameterOnCompleteListener.run(FluentTask.this, result);

					if (oneParameterOnCompleteListener != null)
						oneParameterOnCompleteListener.run(result);
				}

				@Override
				protected final void onCancelled(TResult result, Exception exception) {
					if (handleError(exception)) return;

					super.onCancelled(result, exception);
				}
			};
		}
	};

	@SafeVarargs
	public FluentTask(TParams... params) {
		this(AsyncTask.SERIAL_EXECUTOR, params);
	}

	@SafeVarargs
	public FluentTask(Executor defaultExecutor, TParams... params) {
		this.params = params;
		this.defaultExecutor = defaultExecutor;
	}

	public void execute() {
		executeTask();
	}

	public void execute(Executor exec) {
		executeTask(exec);
	}

	public TResult get() throws ExecutionException, InterruptedException {
		final TResult result = executeTask().get();

		throwOnTaskException(task.getObject());

		return result;
	}

	public TResult get(Executor executor) throws ExecutionException, InterruptedException {
		final TResult result = executeTask(executor).get();

		throwOnTaskException(task.getObject());

		return result;
	}

	private static <TParams, TProgress, TResult> void throwOnTaskException(AndroidAsyncTask<TParams, TProgress, TResult> task) throws ExecutionException {
		final Exception exception = task.getException();
		if (exception != null)
			throw new ExecutionException(exception);
	}

	private AsyncTask<Void, TProgress, TResult> executeTask() {
		return executeTask(defaultExecutor);
	}

	private AsyncTask<Void, TProgress, TResult> executeTask(Executor exec) {
		return task.getObject().executeOnExecutor(exec);
	}

	protected void reportProgress(TProgress... progress) {
		task.getObject().updateProgress(progress);
	}

	/**
	 *
	 * @return True if there is an error and it is handled
	 */
	private boolean handleError(Exception exception) {
		return exception != null &&
					(twoParameterOnErrorListener != null && twoParameterOnErrorListener.call(this, exception)) |
					(oneParameterOnErrorListener != null && oneParameterOnErrorListener.call(exception));

	}

	public FluentTask<TParams, TProgress, TResult> cancel() {
		return cancel(true);
	}

	public FluentTask<TParams, TProgress, TResult> cancel(boolean interrupt) {
		task.getObject().cancel(interrupt);
		return this;
	}

	public boolean isCancelled() {
		return task.getObject().isCancelled();
	}

	protected abstract TResult executeInBackground(TParams[] params);

	protected void setException(Exception exception) {
		task.getObject().setException(exception);
	}

	public FluentTask<TParams, TProgress, TResult> onComplete(TwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TResult> listener) {
		twoParameterOnCompleteListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onComplete(OneParameterRunnable<TResult> listener) {
		oneParameterOnCompleteListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onProgress(TwoParameterRunnable<FluentTask<TParams, TProgress, TResult>, TProgress[]> listener) {
		twoParameterOnProgressListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onProgress(OneParameterRunnable<TProgress[]> listener) {
		oneParameterOnProgressListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onError(TwoParameterCallable<FluentTask<TParams, TProgress, TResult>, Exception, Boolean> listener) {
		twoParameterOnErrorListener = listener;
		return this;
	}

	public FluentTask<TParams, TProgress, TResult> onError(OneParameterCallable<Exception, Boolean> listener) {
		oneParameterOnErrorListener = listener;
		return this;
	}

	private static abstract class AndroidAsyncTask<TParams, TProgress, TResult> extends AsyncExceptionTask<TParams, TProgress, TResult> {
		public void updateProgress(TProgress[] progress) {
			publishProgress(progress);
		}
	}
}
