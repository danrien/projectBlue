package com.lasthopesoftware.promises;

import java.util.Collection;
import java.util.concurrent.CancellationException;

/**
 * Created by david on 3/19/17.
 */

public class AggregateCancellationException extends CancellationException {
	private final Collection<?> results;

	<TResult> AggregateCancellationException(Collection<TResult> results) {
		super();
		this.results = results;
	}

	public <TResult> Collection<TResult> getResults() {
		return (Collection<TResult>)results;
	}
}
