package com.lasthopesoftware.bluewater.client.library.items.access;

import android.util.LruCache;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.Promise;
import com.lasthopesoftware.providers.AbstractProvider;
import com.lasthopesoftware.providers.Cancellation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class ItemProvider {

    private static final Logger logger = LoggerFactory.getLogger(ItemProvider.class);

    private static class ItemHolder {
        ItemHolder(Integer revision, List<Item> items) {
            this.revision = revision;
            this.items = items;
        }

        final Integer revision;
        public final List<Item> items;
    }

    private static final int maxSize = 50;
    private static final LruCache<UrlKeyHolder<Integer>, ItemHolder> itemsCache = new LruCache<>(maxSize);

    private final int itemKey;

	private final ConnectionProvider connectionProvider;

	public static Promise<List<Item>> provide(ConnectionProvider connectionProvider, int itemKey) {
		return new ItemProvider(connectionProvider, itemKey).promiseItems();
	}
	
	public ItemProvider(ConnectionProvider connectionProvider, int itemKey) {
		this.connectionProvider = connectionProvider;
        this.itemKey = itemKey;
	}

    public Promise<List<Item>> promiseItems() {
		return
			RevisionChecker.promiseRevision(connectionProvider)
				.then(serverRevision -> new QueuedPromise<>((messenger) -> {
					final Cancellation cancellation = new Cancellation();
					messenger.cancellationRequested(cancellation::cancel);

					final UrlKeyHolder<Integer> boxedItemKey = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), itemKey);

					final ItemHolder itemHolder;
					synchronized (itemsCache) {
						itemHolder = itemsCache.get(boxedItemKey);
					}

					if (itemHolder != null && itemHolder.revision.equals(serverRevision)) {
						messenger.sendResolution(itemHolder.items);
						return;
					}

					if (cancellation.isCancelled()) {
						messenger.sendResolution(new ArrayList<>());
						return;
					}

					final HttpURLConnection connection;
					try {
						connection = connectionProvider.getConnection(LibraryViewsProvider.browseLibraryParameter, "ID=" + String.valueOf(itemKey), "Version=2");
					} catch (IOException e) {
						messenger.sendRejection(e);
						return;
					}

					try {
						try (InputStream is = connection.getInputStream()) {
							final List<Item> items = ItemResponse.GetItems(connectionProvider, is);

							final ItemHolder newItemHolder = new ItemHolder(serverRevision, items);

							synchronized (itemsCache) {
								itemsCache.put(boxedItemKey, newItemHolder);
							}

							messenger.sendResolution(items);
						}
					} catch (IOException e) {
						logger.error("There was an error getting the inputstream", e);
						messenger.sendRejection(e);
					} finally {
						if (connection != null)
							connection.disconnect();
					}
				}, AbstractProvider.providerExecutor));
	}
}
