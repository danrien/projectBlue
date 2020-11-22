package com.namehillsoftware.projectblue.shared.exceptions;

import android.content.Context;
import android.widget.Toast;

public class UnexpectedExceptionToaster {
	public static void announce(Context context, Throwable error) {
		Toast.makeText(context, "An unexpected error occurred! The error was " + error.getClass().getName(), Toast.LENGTH_SHORT).show();
	}
}
