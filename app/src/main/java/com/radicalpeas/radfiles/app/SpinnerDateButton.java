package com.radicalpeas.radfiles.app;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Huan on 11/29/2014.
 */
public class SpinnerDateButton extends Button implements DatePickerDialog.OnDateSetListener, View.OnClickListener
{
	private Calendar selected_date;
	private String db_date_str;

	private Context context;

	public SpinnerDateButton(Context context)
	{
		super(context);
		init(context);
	}

	public SpinnerDateButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}

	public SpinnerDateButton(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context)
	{
		this.context = context;
		this.setOnClickListener(this);

		selected_date = Calendar.getInstance();
	}

	@Override
	public void onDateSet(DatePicker view, int year, int month, int day)
	{
		//	Calendar selected_date = Calendar.getInstance();
		selected_date.set(year, month, day);

		// format string for display box
		//SimpleDateFormat display_sdf = new SimpleDateFormat("MMMM d, yyyy");
		SimpleDateFormat display_sdf = new SimpleDateFormat("MM/dd/yy");
		String displayDate = display_sdf.format(selected_date.getTime());

		Button date_button = (Button) findViewById(R.id.edit_date);
		date_button.setText(displayDate);

		// set date string for in static string to put into database
		SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd");
		db_date_str = db_sdf.format(selected_date.getTime());
	}



	@Override
	public void onClick(View v)
	{
	//	DialogFragment datePicker = DatePickerFragment.newInstance(selected_date.get(Calendar.YEAR), selected_date.get(Calendar.MONTH), selected_date.get(Calendar.DAY_OF_MONTH));
	//	datePicker.show(context.getApplicationContext()), "datePicker");

		new DatePickerDialog(getContext(), this, selected_date.get(Calendar.YEAR), selected_date.get(Calendar.MONTH), selected_date.get(Calendar.DAY_OF_MONTH)).show();
	}

	public String getDateStr()
	{
		return db_date_str;
	}
}
