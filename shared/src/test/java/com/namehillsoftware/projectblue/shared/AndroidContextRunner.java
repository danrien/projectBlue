package com.namehillsoftware.projectblue.shared;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.internal.SandboxTestRunner;

import java.lang.reflect.Method;
import java.util.HashMap;

public class AndroidContextRunner extends RobolectricTestRunner {
	private final HashMap<Class<?>, HelperTestRunner> testRunners = new HashMap<>();

	public AndroidContextRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	@Override
	protected synchronized SandboxTestRunner.HelperTestRunner getHelperTestRunner(Class bootstrappedTestClass) {
		try {
			if (!testRunners.containsKey(bootstrappedTestClass))
				testRunners.put(bootstrappedTestClass, new HelperTestRunner(bootstrappedTestClass));

			return testRunners.get(bootstrappedTestClass);
		} catch (InitializationError initializationError) {
			throw new RuntimeException(initializationError);
		}
	}

	protected static class HelperTestRunner extends RobolectricTestRunner.HelperTestRunner {

		private boolean isCheckedForBeforeMethod;

		public HelperTestRunner(Class bootstrappedTestClass) throws InitializationError {
			super(bootstrappedTestClass);
		}

		@Override
		public Object createTest() throws Exception {
			final Object test = super.createTest();
			// Note that JUnit4 will call this createTest() multiple times for each
			// test method, so we need to ensure to call "beforeClassSetup" only once.
			if (!isCheckedForBeforeMethod) {
				final Method beforeMethod = test.getClass().getMethod("before");
				beforeMethod.invoke(test);
				isCheckedForBeforeMethod = true;
			}
			return test;
		}
	}
}
