package com.lasthopesoftware.bluewater.client.connection;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import kotlin.Unit;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;


public class FakeConnectionProvider implements IConnectionProvider {
	private final HashMap<Set<String>, CarelessOneParameterFunction<String[], FakeConnectionResponseTuple>> mappedResponses = new HashMap<>();

	public final void mapResponse(CarelessOneParameterFunction<String[], FakeConnectionResponseTuple> response, String... params) {
		final HashSet<String> paramsSet = new HashSet<>(Arrays.asList(params));
		mappedResponses.put(paramsSet, response);
	}

	@Override
	public Promise<Response> promiseResponse(String... params) {
		try {
			return new Promise<>(getResponse(params));
		} catch (IOException e) {
			return new Promise<>(e);
		} catch (RuntimeException e) {
			return new Promise<>(e.getCause());
		}
	}

	private Response getResponse(String... params) throws IOException {
		final Request.Builder builder = new Request.Builder();
		builder.url(getUrlProvider().getUrl(params));

		final Buffer buffer = new Buffer();

		final Response.Builder responseBuilder = new Response.Builder();
		responseBuilder
			.request(builder.build())
			.protocol(Protocol.HTTP_1_1)
			.message("Not Found")
			.body(new RealResponseBody(null, 0, buffer.write("Not found".getBytes())))
			.code(404);

		CarelessOneParameterFunction<String[], FakeConnectionResponseTuple> mappedResponse = mappedResponses.get(new HashSet<>(Arrays.asList(params)));

		if (mappedResponse == null) {
			final Optional<Set<String>> optionalResponse = Stream.of(mappedResponses.keySet())
				.filter(set -> Stream.of(set).allMatch(sp -> Stream.of(params).anyMatch(p -> p.matches(sp))))
				.findFirst();

			if (optionalResponse.isPresent())
				mappedResponse = mappedResponses.get(optionalResponse.get());
		}

		if (mappedResponse == null) return responseBuilder.build();

		try {
			buffer.clear();

			final FakeConnectionResponseTuple result = mappedResponse.resultFrom(params);
			buffer.write(result.response);
			responseBuilder.code(result.code);
			responseBuilder.body(new RealResponseBody(null, result.response.length, buffer));
		} catch (IOException io) {
			throw io;
		} catch (Throwable error) {
			throw new RuntimeException(error);
		}

		return responseBuilder.build();
	}

	@Override
	public IUrlProvider getUrlProvider() {
		return new MediaServerUrlProvider(null, "test", 80);
	}

	@NotNull
	@Override
	public Promise<Unit> promiseSentPacket(@NotNull byte[] packet) {
		return new Promise<>(Unit.INSTANCE);
	}
}
