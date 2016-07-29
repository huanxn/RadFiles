package com.radicalpeas.radfiles.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Dialog;
import android.os.Bundle;
import android.app.DialogFragment;
import android.app.DatePickerDialog;

public class DatePickerFragment extends DialogFragment
{

	int year;
	int month;
	int day;

	public static DatePickerFragment newInstance(int year, int month, int day)
	{
		DatePickerFragment datePicker = new DatePickerFragment();

		// Supply date input as an argument.
		Bundle args = new Bundle();

		SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd");

		//db_sdf.get
		args.putInt("year", year);
		args.putInt("month", month);
		args.putInt("day", day);

		datePicker.setArguments(args);

		return datePicker;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{

		year = getArguments().getInt("year");
		month = getArguments().getInt("month");
		day = getArguments().getInt("day");

		if (year == 0)
		{
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			year = c.get(Calendar.YEAR);
			month = c.get(Calendar.MONTH);
			day = c.get(Calendar.DAY_OF_MONTH);
		}

		// Create a new instance of DatePickerDialog and return it

		//TODO extend activity so it'll work in both addnew and edit
		return new DatePickerDialog(getActivity(), (CaseEditActivity) getActivity(), year, month, day);

	}

}
