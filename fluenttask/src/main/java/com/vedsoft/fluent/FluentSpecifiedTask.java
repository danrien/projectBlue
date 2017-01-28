package com.vedsoft.fluent;

import android.os.AsyncTask;

import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;
import com.vedsoft.lazyj.AbstractSynchronousLazy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class FluentSpecifiedTask<TParams, TProgress, TResult> implements IFluentTask<TParams, TProgress, TResult> {

	private final TParams[] params;
	private final Executor defaultExecutor;

	private OneParameterAction<IFluentTask<TParams, TProgress, TResult>> oneParameterBeforeStartListener;
	private Runnable beforeStartListener;

	private TwoParameterAction<IFluentTask<TParams, TProgress, TResult>, TProgress[]> twoParameterOnProgressListener;
	private OneParameterAction<TProgress[]> oneParameterOnProgressListener;

	private ThreeParameterAction<IFluentTask<TParams, TProgress, TResult>, TResult, Exception> threeParameterOnCompleteListener;
	private TwoParameterAction<TResult, Exception> twoParameterOnCompleteListener;
	private OneParameterAction<TResult> oneParameterOnCompleteListener;

	private OneParameterFunction<Exception, Boolean> oneParameterOnErrorListener;
	private TwoParameterFunction<IFluentTask<TParams, TProgress, TResult>, Exception, Boolean> twoParameterOnErrorListener;

	private volatile boolean isExecuting = false;

	private final AbstractSynchronousLazy<AndroidAsyncTask<Void, TProgress, TResult>> task = new AbstractSynchronousLazy<AndroidAsyncTask<Void, TProgress, TResult>>() {
		@Override
		protected final AndroidAsyncTask<Void, TProgress, TResult> initialize() {
			return new AndroidAsyncTask<Void, TProgress, TResult>(){

				@Override
				protected final void onPreExecute() {
					if (oneParameterBeforeStartListener != null)
						oneParameterBeforeStartListener.runWith(FluentSpecifiedTask.this);

					if (beforeStartListener != null)
						beforeStartListener.run();
				}

				@Override
				protected final TResult doInBackground(Void... params) {
					return executeInBackground(FluentSpecifiedTask.this.params);
				}

				@Override
				protected final void onProgressUpdate(TProgress... values) {
					if (twoParameterOnProgressListener != null)
						twoParameterOnProgressListener.runWith(FluentSpecifiedTask.this, values);

					if (oneParameterOnProgressListener != null)
						oneParameterOnProgressListener.runWith(values);
				}

				@Override
				protected final void onPostExecute(TResult result, Exception exception) {
					handleError(exception);

					if (threeParameterOnCompleteListener != null)
						threeParameterOnCompleteListener.runWith(FluentSpecifiedTask.this, result, exception);

					if (twoParameterOnCompleteListener != null)
						twoParameterOnCompleteListener.runWith(result, exception);

					if (oneParameterOnCompleteListener != null)
						oneParameterOnCompleteListener.runWith(result);
				}

				@Override
				protected final void onCancelled(TResult result, Exception exception) {
					handleError(exception);
				}
			};
		}
	};

	@SafeVarargs
	public FluentSpecifiedTask(TParams... params) {
		this(AsyncTask.SERIAL_EXECUTOR, params);
	}

	@SafeVarargs
	public FluentSpecifiedTask(Executor defaultExecutor, TParams... params) {
		this.params = params;
		this.defaultExecutor = defaultExecutor;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> execute() {
		return execute(null);
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> execute(Executor exec) {
		executeTask(exec);
		return this;
	}

	@Override
	public TResult get() throws ExecutionException, InterruptedException {
		return get(null);
	}

	@Override
	public TResult get(long timeout, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
		return get(null, timeout, timeUnit);
	}

	@Override
	public TResult get(Executor executor) throws ExecutionException, InterruptedException {
		if (!isExecuting)
			executeTask(executor);

		final TResult result = task.getObject().get();

		throwOnTaskException(task.getObject());

		return result;
	}

	@Override
	public TResult get(Executor executor, long timeout, TimeUnit timeUnit) throws TimeoutException, ExecutionException, InterruptedException {
		if (!isExecuting)
			executeTask(executor);

		final TResult result = task.getObject().get(timeout, timeUnit);

		throwOnTaskException(task.getObject());

		return result;
	}

	private static <TParams, TProgress, TResult> void throwOnTaskException(AndroidAsyncTask<TParams, TProgress, TResult> task) throws ExecutionException {
		final Exception exception = task.getException();
		if (exception != null)
			throw new ExecutionException(exception);
	}

	private synchronized AsyncTask<Void, TProgress, TResult> executeTask(Executor exec) {
		isExecuting = true;
		return task.getObject().executeOnExecutor(exec != null ? exec : defaultExecutor);
	}

	protected void reportProgress(TProgress... progress) {
		task.getObject().updateProgress(progress);
	}

	private void handleError(Exception exception) {
		if (exception == null) return;

		if (twoParameterOnErrorListener != null)
			twoParameterOnErrorListener.resultFrom(this, exception);

		if (oneParameterOnErrorListener != null)
			oneParameterOnErrorListener.resultFrom(exception);
	}

	@Override
	public IFluentTask<TParams,TProgress,TResult> cancel() {
		return cancel(true);
	}

	@Override
	public IFluentTask<TParams,TProgress,TResult> cancel(boolean interrupt) {
		task.getObject().cancel(interrupt);
		return this;
	}

	@Override
	public boolean isCancelled() {
		return task.getObject().isCancelled();
	}

	protected abstract TResult executeInBackground(TParams[] params);

	protected void setException(Exception exception) {
		task.getObject().setException(exception);
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> beforeStart(OneParameterAction<IFluentTask<TParams, TProgress, TResult>> listener) {
		oneParameterBeforeStartListener = listener;
		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> beforeStart(Runnable listener) {
		beforeStartListener = listener;
		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onComplete(ThreeParameterAction<IFluentTask<TParams, TProgress, TResult>, TResult, Exception> listener) {
		threeParameterOnCompleteListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED) {
			try {
				listener.runWith(this, task.getObject().get(), task.getObject().getException());
			} catch (InterruptedException | ExecutionException ignored) {
			}
		}

		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onComplete(TwoParameterAction<TResult, Exception> listener) {
		twoParameterOnCompleteListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED) {
			try {
				listener.runWith(task.getObject().get(), task.getObject().getException());
			} catch (InterruptedException | ExecutionException ignored) {
			}
		}

		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onComplete(OneParameterAction<TResult> listener) {
		oneParameterOnCompleteListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED) {
			try {
				listener.runWith(task.getObject().get());
			} catch (InterruptedException | ExecutionException ignored) {
			}
		}

		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onProgress(TwoParameterAction<IFluentTask<TParams, TProgress, TResult>, TProgress[]> listener) {
		twoParameterOnProgressListener = listener;
		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onProgress(OneParameterAction<TProgress[]> listener) {
		oneParameterOnProgressListener = listener;
		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onError(TwoParameterFunction<IFluentTask<TParams, TProgress, TResult>, Exception, Boolean> listener) {
		twoParameterOnErrorListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED)
			handleError(task.getObject().getException());

		return this;
	}

	@Override
	public IFluentTask<TParams, TProgress, TResult> onError(OneParameterFunction<Exception, Boolean> listener) {
		oneParameterOnErrorListener = listener;

		if (task.isInitialized() && task.getObject().getStatus() == AsyncTask.Status.FINISHED)
			handleError(task.getObject().getException());

		return this;
	}

	private static abstract class AndroidAsyncTask<TParams, TProgress, TResult> extends AsyncExceptionTask<TParams, TProgress, TResult> {
		void updateProgress(TProgress[] progress) {
			publishProgress(progress);
		}
	}
}
