package com.lasthopesoftware.promises.specs.GivenAPromiseThatResolvesInTheFuture;

import com.lasthopesoftware.promises.Promise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by david on 10/20/16.
 */

public class WhenThePromiseIsFulfilled {

	private Object result;
	private Object expectedResult;

	@Before
	public void before() throws InterruptedException {
		expectedResult = new Object();
		final CountDownLatch latch = new CountDownLatch(1);
		new Promise<>((resolve, reject) -> new Thread(() -> {
			resolve.run(result);
			latch.countDown();
		}).run())
		.then(result -> { this.result = expectedResult; });

		latch.await(500, TimeUnit.MILLISECONDS);
	}

	@Test
	public void thenTheResultIsNull() {
		Assert.assertEquals(expectedResult, result);
	}
}
