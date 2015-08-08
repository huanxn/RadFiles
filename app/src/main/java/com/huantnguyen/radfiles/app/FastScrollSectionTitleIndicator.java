package com.huantnguyen.radfiles.app;

/**
 * Created by Huan on 8/6/2015.
 */

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;

public class FastScrollSectionTitleIndicator extends SectionTitleIndicator<FastScrollSectionTitleIndicator.ColorGroup> {

	public enum ColorGroup {
		RED_UPPER (345, 360, "Red"),
		PINK (300, RED_UPPER.mMinimumHue, "Pink"),
		PURPLE (260, PINK.mMinimumHue, "Purple"),
		BLUE (175, PURPLE.mMinimumHue, "Blue"),
		GREEN (70, BLUE.mMinimumHue, "Green"),
		YELLOW (50, GREEN.mMinimumHue, "Yellow"),
		ORANGE (25, YELLOW.mMinimumHue, "Orange"),
		RED_LOWER (0, ORANGE.mMinimumHue, "Red"),
		;

		private final int mMinimumHue, mMaximumHue;
		private final int mIntegerRepresentation;
		private final String mName;

		ColorGroup(int minimumHue, int maximumHue, String groupName) {
			mMinimumHue = minimumHue;
			mMaximumHue = maximumHue;
			mName = groupName;
			mIntegerRepresentation = Color.HSVToColor(new float[]{(maximumHue + minimumHue) / 2, 1, 1});
		}

		public String getName() {
			return mName;
		}

		public boolean containsHue(int hue) {
			return (hue >= mMinimumHue && hue < mMaximumHue);
		}

		public int getAsColor() {
			return mIntegerRepresentation;
		}
	}

	public FastScrollSectionTitleIndicator(Context context) {
		super(context);
	}

	public FastScrollSectionTitleIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FastScrollSectionTitleIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void setSection(ColorGroup colorGroup) {
		// Example of using a single character
		setTitleText(colorGroup.getName().charAt(0) + "");

		// Example of using a longer string
		// setTitleText(colorGroup.getName());

		setIndicatorTextColor(colorGroup.getAsColor());
	}
}
