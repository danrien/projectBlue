package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.connection.helpers.ConnectionTester;
import com.lasthopesoftware.bluewater.client.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.lazyj.AbstractSynchronousLazy;
import com.vedsoft.lazyj.ILazy;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.lasthopesoftware.bluewater.client.connection.SessionConnection.BuildingSessionConnectionStatus.completeConditions;
import static com.lasthopesoftware.bluewater.client.connection.SessionConnection.BuildingSessionConnectionStatus.runningConditions;

public class SessionConnection {

	public static final String buildSessionBroadcast = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "buildSessionBroadcast");
	public static final String buildSessionBroadcastStatus = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "buildSessionBroadcastStatus");
	public static final String refreshSessionBroadcast = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "refreshSessionBroadcast");
	public static final String isRefreshSuccessfulStatus = MagicPropertyBuilder.buildMagicPropertyName(SessionConnection.class, "isRefreshSuccessfulStatus");

	private static volatile boolean isRunning;
	private static volatile int buildingStatus = BuildingSessionConnectionStatus.GettingLibrary;

	private static ConnectionProvider sessionConnectionProvider;

	public static HttpURLConnection getSessionConnection(String... params) throws IOException {
		return sessionConnectionProvider.getConnection(params);
	}

	public static ConnectionProvider getSessionConnectionProvider() {
		return sessionConnectionProvider;
	}

	public static boolean isBuilt() {
		return sessionConnectionProvider != null;
	}
	
	public static synchronized int build(final Context context) {
		if (isRunning) return buildingStatus;
		
		doStateChange(context, BuildingSessionConnectionStatus.GettingLibrary);
		LibrarySession.getActiveLibrary(context).then(VoidFunc.running(library -> {
			if (library == null || library.getAccessCode() == null || library.getAccessCode().isEmpty()) {
				doStateChange(context, BuildingSessionConnectionStatus.GettingLibraryFailed);
				isRunning = false;
				return;
			}

			doStateChange(context, BuildingSessionConnectionStatus.BuildingConnection);

			AccessConfigurationBuilder.buildConfiguration(context, library, (result) -> {
				if (result == null) {
					doStateChange(context, BuildingSessionConnectionStatus.BuildingConnectionFailed);
					return;
				}

				sessionConnectionProvider = new ConnectionProvider(result);

				if (library.getSelectedView() >= 0) {
					doStateChange(context, BuildingSessionConnectionStatus.BuildingSessionComplete);
					return;
				}

				doStateChange(context, BuildingSessionConnectionStatus.GettingView);

				LibraryViewsProvider.provide(sessionConnectionProvider)
						.onComplete((result1) -> {

							if (result1 == null || result1.size() == 0) {
								doStateChange(context, BuildingSessionConnectionStatus.GettingViewFailed);
								return;
							}

							doStateChange(context, BuildingSessionConnectionStatus.GettingView);
							final int selectedView = result1.get(0).getKey();
							library.setSelectedView(selectedView);
							library.setSelectedViewType(Library.ViewType.StandardServerView);

							LibrarySession.saveLibrary(context, library, (savedLibrary) -> doStateChange(context, BuildingSessionConnectionStatus.BuildingSessionComplete));
						})
						.execute();
			});
		}));
		
		return buildingStatus;
	}

	public static void refresh(final Context context) {
		refresh(context, -1);
	}

	private static void refresh(final Context context, final int timeout) {
		if (sessionConnectionProvider == null)
			throw new NullPointerException("The session connection needs to be built first.");

		final OneParameterAction<Boolean> testConnectionCompleteListener = (result) -> {
			if (!result) build(context);

			final Intent refreshBroadcastIntent = new Intent(refreshSessionBroadcast);
			refreshBroadcastIntent.putExtra(isRefreshSuccessfulStatus, result);
			LocalBroadcastManager.getInstance(context).sendBroadcast(refreshBroadcastIntent);
		};

		if (timeout > 0)
			ConnectionTester.doTest(sessionConnectionProvider, timeout, testConnectionCompleteListener);
		else
			ConnectionTester.doTest(sessionConnectionProvider, testConnectionCompleteListener);
	}
	
	private static void doStateChange(final Context context, final int status) {
		buildingStatus = status;
		
		if (runningConditions.contains(status)) isRunning = true;

		final Intent broadcastIntent = new Intent(buildSessionBroadcast);
		broadcastIntent.putExtra(buildSessionBroadcastStatus, status);
		LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
		
		if (status == BuildingSessionConnectionStatus.BuildingSessionComplete)
			LoggerFactory.getLogger(LibrarySession.class).info("Session started.");
		
		if (completeConditions.contains(status)) isRunning = false;
	}

	public static class BuildingSessionConnectionStatus {
		public static final int GettingLibrary = 1;
		public static final int GettingLibraryFailed = 2;
		public static final int BuildingConnection = 3;
		public static final int BuildingConnectionFailed = 4;
		public static final int GettingView = 5;
		public static final int GettingViewFailed = 6;
		public static final int BuildingSessionComplete = 7;

		private static final ILazy<Set<Integer>> runningConditionsLazy =
				new AbstractSynchronousLazy<Set<Integer>>() {
					@Override
					protected Set<Integer> initialize() throws Exception {
						return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BuildingSessionConnectionStatus.GettingLibrary, BuildingSessionConnectionStatus.BuildingConnection, BuildingSessionConnectionStatus.GettingView)));
					}
				};

		private static final ILazy<Set<Integer>> completeConditionsLazy =
				new AbstractSynchronousLazy<Set<Integer>>() {
					@Override
					protected Set<Integer> initialize() throws Exception {
						return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BuildingSessionConnectionStatus.GettingLibraryFailed, BuildingSessionConnectionStatus.BuildingConnectionFailed, BuildingSessionConnectionStatus.GettingViewFailed, BuildingSessionConnectionStatus.BuildingSessionComplete)));
					}
				};

		static final Set<Integer> runningConditions = runningConditionsLazy.getObject();
		public static final Set<Integer> completeConditions = completeConditionsLazy.getObject();
	}
}
