package com.lasthopesoftware.bluewater.client.browsing.items.access;

import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.LibraryViewsByConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import okhttp3.ResponseBody;

public class ItemProvider implements ProvideItems {

    private static final Logger logger = LoggerFactory.getLogger(ItemProvider.class);

	private final IConnectionProvider connectionProvider;

	public static Promise<List<Item>> provide(IConnectionProvider connectionProvider, int itemKey) {
		return new ItemProvider(connectionProvider).promiseItems(itemKey);
	}
	
	public ItemProvider(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
    public Promise<List<Item>> promiseItems(int itemKey) {
		return connectionProvider.promiseResponse(
			LibraryViewsByConnectionProvider.browseLibraryParameter,
			"ID=" + itemKey,
			"Version=2").then(response -> {
			final ResponseBody body = response.body();
			if (body == null) return Collections.emptyList();

			try (final InputStream is = body.byteStream()) {
				return ItemResponse.GetItems(is);
			} catch (IOException e) {
				logger.error("There was an error getting the inputstream", e);
				throw e;
			} finally {
				body.close();
			}
		});
	}
}
