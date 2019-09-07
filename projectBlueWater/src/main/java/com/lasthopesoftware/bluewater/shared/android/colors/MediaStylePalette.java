package com.lasthopesoftware.bluewater.shared.android.colors;

public class MediaStylePalette {
	private final int primaryTextColor;
	private final int secondaryTextColor;
	private final int backgroundColor;
	private final int actionBarColor;

	public MediaStylePalette(int primaryTextColor, int secondaryTextColor, int backgroundColor, int actionBarColor) {
		this.primaryTextColor = primaryTextColor;
		this.secondaryTextColor = secondaryTextColor;
		this.backgroundColor = backgroundColor;
		this.actionBarColor = actionBarColor;
	}

	public int getPrimaryTextColor() {
		return primaryTextColor;
	}

	public int getSecondaryTextColor() {
		return secondaryTextColor;
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public int getActionBarColor() {
		return actionBarColor;
	}
}
