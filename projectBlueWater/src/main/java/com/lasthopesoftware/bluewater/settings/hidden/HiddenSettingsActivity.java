package com.lasthopesoftware.bluewater.settings.hidden;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.RadioGroup;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.PlaybackEngineTypeSelectionPersistence;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.SelectedPlaybackEngineTypeAccess;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.broadcast.PlaybackEngineTypeChangedBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.view.PlaybackEngineTypeSelectionView;


public class HiddenSettingsActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_hidden_settings);

		final PlaybackEngineTypeSelectionPersistence selection = new PlaybackEngineTypeSelectionPersistence(
			this,
			new PlaybackEngineTypeChangedBroadcaster(this));

		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(this);

		final PlaybackEngineTypeSelectionView playbackEngineTypeSelectionView =
			new PlaybackEngineTypeSelectionView(this);

		final RadioGroup playbackEngineOptions = findViewById(R.id.playbackEngineOptions);
		playbackEngineTypeSelectionView.buildPlaybackEngineTypeSelections()
			.forEach(playbackEngineOptions::addView);
		playbackEngineOptions.check(selectedPlaybackEngineTypeAccess.getSelectedPlaybackEngineType().ordinal());
		playbackEngineOptions
			.setOnCheckedChangeListener((group, checkedId) -> selection.selectPlaybackEngine(PlaybackEngineType.values()[checkedId]));
	}
}
