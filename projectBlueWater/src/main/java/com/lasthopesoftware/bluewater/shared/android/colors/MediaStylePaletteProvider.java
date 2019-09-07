/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.lasthopesoftware.bluewater.shared.android.colors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.palette.graphics.Palette;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import java.util.List;

import static androidx.core.graphics.ColorUtils.colorToXYZ;

/**
 * A class the processes media notifications and extracts the right text and background colors.
 */
public class MediaStylePaletteProvider {

	/**
	 * The fraction below which we select the vibrant instead of the light/dark vibrant color
	 */
	private static final float POPULATION_FRACTION_FOR_MORE_VIBRANT = 1.0f;

	/**
	 * Minimum saturation that a muted color must have if there exists if deciding between two
	 * colors
	 */
	private static final float MIN_SATURATION_WHEN_DECIDING = 0.19f;

	/**
	 * Minimum fraction that any color must have to be picked up as a text color
	 */
	private static final double MINIMUM_IMAGE_FRACTION = 0.002;

	/**
	 * The population fraction to select the dominant color as the text color over a the colored
	 * ones.
	 */
	private static final float POPULATION_FRACTION_FOR_DOMINANT = 0.01f;

	/**
	 * The population fraction to select a white or black color as the background over a color.
	 */
	private static final float POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.5f;
	private static final float BLACK_MAX_LIGHTNESS = 0.08f;
	private static final float WHITE_MIN_LIGHTNESS = 0.90f;
	private static final int RESIZE_BITMAP_AREA = 150 * 150;
	/**
	 * The lightness difference that has to be added to the primary text color to obtain the
	 * secondary text color when the background is light.
	 */
	private static final int LIGHTNESS_TEXT_DIFFERENCE_LIGHT = 20;
	/**
	 * The lightness difference that has to be added to the primary text color to obtain the
	 * secondary text color when the background is dark.
	 * A bit less then the above value, since it looks better on dark backgrounds.
	 */
	private static final int LIGHTNESS_TEXT_DIFFERENCE_DARK = -10;
	private final Palette.Filter mBlackWhiteFilter = (rgb, hsl) -> isNotWhiteOrBlack(hsl);
	private final Context context;


	public MediaStylePaletteProvider(Context context) {
		this.context = context;
	}

	public Promise<MediaStylePalette> promisePalette(Drawable drawable) {
		return new QueuedPromise<>(() -> getMediaPalette(drawable), AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public Promise<MediaStylePalette> promisePalette(Bitmap bitmap) {
		final BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
		return promisePalette(drawable);
	}

	/**
	 * Processes a drawable and calculates the appropriate colors that should
	 * be used.
	 */
	private MediaStylePalette getMediaPalette(Drawable drawable) {
		if (drawable != null) {
			// We're transforming the builder, let's make sure all baked in RemoteViews are
			// rebuilt!

			int width = drawable.getIntrinsicWidth();
			int height = drawable.getIntrinsicHeight();
			int area = width * height;
			if (area > RESIZE_BITMAP_AREA) {
				double factor = Math.sqrt((float) RESIZE_BITMAP_AREA / area);
				width = (int) (factor * width);
				height = (int) (factor * height);

				final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				drawable.setBounds(0, 0, width, height);
				drawable.draw(canvas);

				// for the background we only take the left side of the image to ensure
				// a smooth transition
				Palette.Builder paletteBuilder = Palette.from(bitmap)
					.setRegion(0, 0, bitmap.getWidth() / 2, bitmap.getHeight())
					.clearFilters() // we want all colors, red / white / black ones too!
					.resizeBitmapArea(RESIZE_BITMAP_AREA);
				final RgbAndHsl backgroundColor = findBackgroundColorAndFilter(drawable);
				// we want most of the full region again, slightly shifted to the right
				float textColorStartWidthFraction = 0.4f;
				paletteBuilder.setRegion((int) (bitmap.getWidth() * textColorStartWidthFraction), 0,
					bitmap.getWidth(),
					bitmap.getHeight());
				if (backgroundColor.hsl != null) {
					paletteBuilder.addFilter((rgb, hsl) -> {
						// at least 10 degrees hue difference
						float diff = Math.abs(hsl[0] - backgroundColor.hsl[0]);
						return diff > 10 && diff < 350;
					});
				}
				paletteBuilder.addFilter(mBlackWhiteFilter);
				final Palette palette = paletteBuilder.generate();
				final int foregroundColor = selectForegroundColor(backgroundColor.rgb, palette);
				return finalizeColors(backgroundColor.rgb, foregroundColor);
			}
		}

		return null;
	}

	private int selectForegroundColor(int backgroundColor, Palette palette) {
		if (isColorLight(backgroundColor)) {
			return selectForegroundColorForSwatches(palette.getDarkVibrantSwatch(),
				palette.getVibrantSwatch(),
				palette.getDarkMutedSwatch(),
				palette.getMutedSwatch(),
				palette.getDominantSwatch(),
				Color.BLACK);
		} else {
			return selectForegroundColorForSwatches(palette.getLightVibrantSwatch(),
				palette.getVibrantSwatch(),
				palette.getLightMutedSwatch(),
				palette.getMutedSwatch(),
				palette.getDominantSwatch(),
				Color.WHITE);
		}
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

	private int selectForegroundColorForSwatches(Palette.Swatch moreVibrant,
												 Palette.Swatch vibrant, Palette.Swatch moreMutedSwatch, Palette.Swatch mutedSwatch,
												 Palette.Swatch dominantSwatch, int fallbackColor) {
		Palette.Swatch coloredCandidate = selectVibrantCandidate(moreVibrant, vibrant);
		if (coloredCandidate == null) {
			coloredCandidate = selectMutedCandidate(mutedSwatch, moreMutedSwatch);
		}
		if (coloredCandidate != null) {
			if (dominantSwatch == coloredCandidate) {
				return coloredCandidate.getRgb();
			} else if ((float) coloredCandidate.getPopulation() / dominantSwatch.getPopulation()
				< POPULATION_FRACTION_FOR_DOMINANT
				&& dominantSwatch.getHsl()[1] > MIN_SATURATION_WHEN_DECIDING) {
				return dominantSwatch.getRgb();
			} else {
				return coloredCandidate.getRgb();
			}
		} else if (hasEnoughPopulation(dominantSwatch)) {
			return dominantSwatch.getRgb();
		} else {
			return fallbackColor;
		}
	}

	private Palette.Swatch selectMutedCandidate(Palette.Swatch first,
												Palette.Swatch second) {
		boolean firstValid = hasEnoughPopulation(first);
		boolean secondValid = hasEnoughPopulation(second);
		if (firstValid && secondValid) {
			float firstSaturation = first.getHsl()[1];
			float secondSaturation = second.getHsl()[1];
			float populationFraction = first.getPopulation() / (float) second.getPopulation();
			if (firstSaturation * populationFraction > secondSaturation) {
				return first;
			} else {
				return second;
			}
		} else if (firstValid) {
			return first;
		} else if (secondValid) {
			return second;
		}
		return null;
	}

	private Palette.Swatch selectVibrantCandidate(Palette.Swatch first, Palette.Swatch second) {
		boolean firstValid = hasEnoughPopulation(first);
		boolean secondValid = hasEnoughPopulation(second);
		if (firstValid && secondValid) {
			int firstPopulation = first.getPopulation();
			int secondPopulation = second.getPopulation();
			if (firstPopulation / (float) secondPopulation
				< POPULATION_FRACTION_FOR_MORE_VIBRANT) {
				return second;
			} else {
				return first;
			}
		} else if (firstValid) {
			return first;
		} else if (secondValid) {
			return second;
		}
		return null;
	}

	private boolean hasEnoughPopulation(Palette.Swatch swatch) {
		// We want a fraction that is at least 1% of the image
		return swatch != null
			&& (swatch.getPopulation() / (float) RESIZE_BITMAP_AREA > MINIMUM_IMAGE_FRACTION);
	}

	private RgbAndHsl findBackgroundColorAndFilter(Drawable drawable) {
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		int area = width * height;

		double factor = Math.sqrt((float) RESIZE_BITMAP_AREA / area);
		width = (int) (factor * width);
		height = (int) (factor * height);

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, width, height);
		drawable.draw(canvas);

		// for the background we only take the left side of the image to ensure
		// a smooth transition
		Palette.Builder paletteBuilder = Palette.from(bitmap)
			.setRegion(0, 0, bitmap.getWidth() / 2, bitmap.getHeight())
			.clearFilters() // we want all colors, red / white / black ones too!
			.resizeBitmapArea(RESIZE_BITMAP_AREA);
		Palette palette = paletteBuilder.generate();
		// by default we use the dominant palette
		Palette.Swatch dominantSwatch = palette.getDominantSwatch();
		if (dominantSwatch == null) {
			// We're not filtering on white or black
			return new RgbAndHsl(Color.WHITE, null);
		}

		if (isNotWhiteOrBlack(dominantSwatch.getHsl())) {
			return new RgbAndHsl(dominantSwatch.getRgb(), dominantSwatch.getHsl());
		}
		// Oh well, we selected black or white. Lets look at the second color!
		List<Palette.Swatch> swatches = palette.getSwatches();
		float highestNonWhitePopulation = -1;
		Palette.Swatch second = null;
		for (Palette.Swatch swatch : swatches) {
			if (swatch != dominantSwatch
				&& swatch.getPopulation() > highestNonWhitePopulation
				&& isNotWhiteOrBlack(swatch.getHsl())) {
				second = swatch;
				highestNonWhitePopulation = swatch.getPopulation();
			}
		}
		if (second == null) {
			// We're not filtering on white or black
			return new RgbAndHsl(dominantSwatch.getRgb(), null);
		}
		if (dominantSwatch.getPopulation() / highestNonWhitePopulation
			> POPULATION_FRACTION_FOR_WHITE_OR_BLACK) {
			// The dominant swatch is very dominant, lets take it!
			// We're not filtering on white or black
			return new RgbAndHsl(dominantSwatch.getRgb(), null);
		} else {
			return new RgbAndHsl(second.getRgb(), second.getHsl());
		}
	}

	private boolean isNotWhiteOrBlack(float[] hsl) {
		return !isBlack(hsl) && !isWhite(hsl);
	}

	/**
	 * @return true if the color represents a color which is close to black.
	 */
	private boolean isBlack(float[] hslColor) {
		return hslColor[2] <= BLACK_MAX_LIGHTNESS;
	}

	/**
	 * @return true if the color represents a color which is close to white.
	 */
	private boolean isWhite(float[] hslColor) {
		return hslColor[2] >= WHITE_MIN_LIGHTNESS;
	}

	private MediaStylePalette finalizeColors(int backgroundColor, int mForegroundColor) {
		double backLum = NotificationColorUtil.calculateLuminance(backgroundColor);
		double textLum = NotificationColorUtil.calculateLuminance(mForegroundColor);
		double contrast = NotificationColorUtil.calculateContrast(mForegroundColor,
			backgroundColor);
		// We only respect the given colors if worst case Black or White still has
		// contrast
		boolean backgroundLight = backLum > textLum
			&& NotificationColorUtil.satisfiesTextContrast(backgroundColor, Color.BLACK)
			|| backLum <= textLum
			&& !NotificationColorUtil.satisfiesTextContrast(backgroundColor, Color.WHITE);
		int secondaryTextColor, primaryTextColor;
		if (contrast < 4.5f) {
			if (backgroundLight) {
				secondaryTextColor = NotificationColorUtil.findContrastColor(
					mForegroundColor,
					backgroundColor,
					true /* findFG */,
					4.5f);
				primaryTextColor = NotificationColorUtil.changeColorLightness(
					secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_LIGHT);
			} else {
				secondaryTextColor =
					NotificationColorUtil.findContrastColorAgainstDark(
						mForegroundColor,
						backgroundColor,
						true /* findFG */,
						4.5f);
				primaryTextColor = NotificationColorUtil.changeColorLightness(
					secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_DARK);
			}
		} else {
			primaryTextColor = mForegroundColor;
			secondaryTextColor = NotificationColorUtil.changeColorLightness(
				primaryTextColor, backgroundLight ? LIGHTNESS_TEXT_DIFFERENCE_LIGHT
					: LIGHTNESS_TEXT_DIFFERENCE_DARK);
			if (NotificationColorUtil.calculateContrast(secondaryTextColor,
				backgroundColor) < 4.5f) {
				// oh well the secondary is not good enough
				if (backgroundLight) {
					secondaryTextColor = NotificationColorUtil.findContrastColor(
						secondaryTextColor,
						backgroundColor,
						true /* findFG */,
						4.5f);
				} else {
					secondaryTextColor
						= NotificationColorUtil.findContrastColorAgainstDark(
						secondaryTextColor,
						backgroundColor,
						true /* findFG */,
						4.5f);
				}
				primaryTextColor = NotificationColorUtil.changeColorLightness(
					secondaryTextColor, backgroundLight
						? -LIGHTNESS_TEXT_DIFFERENCE_LIGHT
						: -LIGHTNESS_TEXT_DIFFERENCE_DARK);
			}
		}

		return new MediaStylePalette(primaryTextColor, secondaryTextColor, backgroundColor, NotificationColorUtil.resolveActionBarColor(context, backgroundColor));
	}

	private static class RgbAndHsl {
		final int rgb;
		final float[] hsl;

		private RgbAndHsl(int rgb, float[] hsl) {
			this.rgb = rgb;
			this.hsl = hsl;
		}
	}
}
