package com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.specs.GivenPowerOnlyIsNotSet;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Constraints;

import com.lasthopesoftware.bluewater.client.stored.scheduling.constraints.SyncWorkerConstraints;
import com.lasthopesoftware.bluewater.settings.ApplicationConstants;
import com.lasthopesoftware.specs.AndroidContext;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenGettingConstraints extends AndroidContext {

	private static Constraints constraints;

	@Override
	public void before() {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());
		sharedPreferences.edit()
			.putBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false)
			.apply();
		final SyncWorkerConstraints syncWorkerConstraints = new SyncWorkerConstraints(sharedPreferences);
		constraints = syncWorkerConstraints.getCurrentConstraints();
	}

	@Test
	public void thenTheConstraintsAreCorrect() {
		assertThat(constraints.requiresCharging()).isFalse();
	}
}
