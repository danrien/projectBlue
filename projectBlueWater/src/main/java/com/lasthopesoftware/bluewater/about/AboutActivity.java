package com.lasthopesoftware.bluewater.about;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.BuildConfig;
import com.lasthopesoftware.bluewater.R;

public class AboutActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final TextView textView = findViewById(R.id.aboutDescription);
		textView.setText(
			String.format(
				getString(R.string.aboutAppText),
				getString(R.string.app_name),
				BuildConfig.VERSION_NAME,
				String.valueOf(BuildConfig.VERSION_CODE),
				getString(R.string.company_name),
				getString(R.string.copyright_year)));
	}
}
