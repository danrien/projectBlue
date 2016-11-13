package com.lasthopesoftware.promises.specs.GivenAPromiseThatResolves;

import com.lasthopesoftware.promises.ExpectedPromise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */
public class WhenAnotherReturningPromiseIsExpected {

	private Integer nextReturningPromiseResult;

	@Before
	public void before() {
		new ExpectedPromise<>(() -> "test")
				.then(result -> 330 + result.hashCode())
				.then(nextResult -> nextReturningPromiseResult = nextResult);
	}

	@Test
	public void thenTheNextActionReturnsAPromiseOfTheCorrectType() {
		Assert.assertEquals(330 + "test".hashCode(), nextReturningPromiseResult.intValue());
	}
}
