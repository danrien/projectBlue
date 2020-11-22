package com.namehillsoftware.projectblue.shared.exceptions;

import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor;
import com.namehillsoftware.handoff.rejections.UnhandledRejectionsReceiver;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;

public class LoggerUncaughtExceptionHandler implements UncaughtExceptionHandler, UnhandledRejectionsReceiver {

	private static final CreateAndHold<Executor> errorExecutor = new Lazy<>(CachedSingleThreadExecutor::new);

	private static final Logger logger = LoggerFactory.getLogger(LoggerUncaughtExceptionHandler.class);

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		getErrorExecutor().execute(() -> logger.error("Uncaught Exception", ex));
	}

	@Override
	public void newUnhandledRejection(Throwable rejection) {
		getErrorExecutor().execute(() -> logger.warn("An asynchronous exception has not yet been handled", rejection));
	}

	public static Executor getErrorExecutor() {
		return errorExecutor.getObject();
	}
}
