package com.lasthopesoftware.bluewater.shared.android.colors;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;

import static androidx.core.graphics.ColorUtils.colorToXYZ;

public class MediaStylePalette {
	private final int primaryTextColor;
	private final int secondaryTextColor;
	private final int backgroundColor;

	public MediaStylePalette(int primaryTextColor, int secondaryTextColor, int backgroundColor) {
		this.primaryTextColor = primaryTextColor;
		this.secondaryTextColor = secondaryTextColor;
		this.backgroundColor = backgroundColor;
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

	private static boolean isColorLight(int backgroundColor) {
		return calculateLuminance(backgroundColor) > 0.5f;
	}

	/**
	 * Returns the luminance of a color as a float between {@code 0.0} and {@code 1.0}.
	 * <p>Defined as the Y component in the XYZ representation of {@code color}.</p>
	 */
	@FloatRange(from = 0.0, to = 1.0)
	private static double calculateLuminance(@ColorInt int color) {
		final double[] result = new double[3];
		colorToXYZ(color, result);
		// Luminance is the Y component
		return result[1] / 100;
	}
}
