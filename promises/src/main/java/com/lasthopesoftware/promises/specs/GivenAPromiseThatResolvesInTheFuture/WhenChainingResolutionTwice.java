package com.lasthopesoftware.promises.specs.GivenAPromiseThatResolvesInTheFuture;

import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.OneParameterFunction;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 10/29/16.
 */

public class WhenChainingResolutionTwice {

	private OneParameterFunction<String, ?> firstResultHandler;
	private OneParameterFunction<String, ?> secondResultHandler;

	@Before
	public void before() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final IPromise<String> rootPromise =
			new Promise<>((resolve, reject) -> new Thread(() -> {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				resolve.withResult("test");
				latch.countDown();
			}).start());

		firstResultHandler = mock(OneParameterFunction.class);

		rootPromise
			.then(firstResultHandler);

		secondResultHandler = mock(OneParameterFunction.class);

		rootPromise
			.then(secondResultHandler);

		latch.await(1000, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheFirstResolutionIsCalled() {
		verify(firstResultHandler, times(1)).expectedUsing(any());
	}

	@Test
	public void thenTheSecondResolutionIsCalled() {
		verify(secondResultHandler, times(1)).expectedUsing(any());
	}
}
