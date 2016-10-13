package com.radicalpeas.radfiles.app;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

/**
 * A modified Spinner that doesn't automatically select the first entry in the list.
 * <p/>
 * Shows the prompt if nothing is selected.
 * <p/>
 * Limitations: does not display prompt if the entry list is empty.
 */
public class SpinnerCustom extends Spinner // implements DialogInterface.OnMultiChoiceClickListener
{
	private int selected_position = -1;                      // current selected position in the spinner list

	private int custom_position;                        // position in list, which is at the end
	private String custom_alert_title;                  // alert dialog title
	static final private String CUSTOM_TEXT = "Custom..."; // test in spinner list
	private int previous_position;                      // in case canceled custom input, revert back to previous

	private Context context;
	private ArrayAdapter<String> spinnerArrayAdapter;

	public SpinnerCustom(Context context)
	{
		super(context);
		this.context = context;
	}

	public SpinnerCustom(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
	}

	public SpinnerCustom(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		this.context = context;
	}

	@Override
	public void setAdapter(SpinnerAdapter orig)
	{
		final SpinnerAdapter adapter = newProxy(orig);

		super.setAdapter(adapter);

		try
		{
			final Method m = AdapterView.class.getDeclaredMethod("setNextSelectedPositionInt", int.class);
			m.setAccessible(true);
			m.invoke(this, -1);

			final Method n = AdapterView.class.getDeclaredMethod("setSelectedPositionInt", int.class);
			n.setAccessible(true);
			n.invoke(this, -1);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}



	// default custom message
	public void setItems(Cursor cursor, int column)
	{
		setItems(cursor, column, "Add new item");
	}

	// initialize with list items by cursor, and new custom alert dialog title
	public void setItems(Cursor cursor, int column, String title)
	{
		custom_alert_title = title;

		List<String> stringList = new ArrayList<String>();
		//items = new ArrayList<String>();

		if(cursor.moveToFirst())
		{
			do
			{
				stringList.add(cursor.getString(column));

			} while(cursor.moveToNext());
		}

		// set spinner position of custom item at the of list
		custom_position = cursor.getCount();
		// add Custom selection at the end of the list
		stringList.add(CUSTOM_TEXT);

		//items = stringList.toArray(new String[stringList.size()]);

		spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, stringList);
		//spinnerArrayAdapter.addAll(items);
		spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		setAdapter(spinnerArrayAdapter);

		setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				final AdapterView<?> adapter = parent;

				// normal selection of item, ie not Custom item
				selected_position = position;

				if(position == custom_position)
				{
					// Get user input for new list item
					AlertDialog.Builder alert = new AlertDialog.Builder(context);

					alert.setTitle(custom_alert_title);
					//alert.setMessage("message");

					// Set an EditText view to get user input
					final EditText input = new EditText(context);
					input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
					//input.setHighlightColor(context.getResources().getColor(R.color.default_colorControlHighlight));
					input.setHighlightColor(UtilClass.get_attr(context, R.attr.colorControlHighlight));

					alert.setView(input);

					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int whichButton)
						{
							String value = input.getText().toString();

							// add value to end of list, but before the Custom item
							spinnerArrayAdapter.remove(CUSTOM_TEXT);
							spinnerArrayAdapter.add(value);
							spinnerArrayAdapter.add(CUSTOM_TEXT);
							// adjust position of Custom item to reflect addition of new item
							custom_position += 1;

							// save position for future
							previous_position = selected_position;
						}
					});

					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// revert back to previous position in list
							selected_position = previous_position;
							adapter.setSelection(previous_position);

						}
					});

					AlertDialog dialog = alert.create();
					// Show keyboard
					dialog.setOnShowListener(new DialogInterface.OnShowListener() {
						@Override
						public void onShow(DialogInterface dialog) {
							InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
						}
					});
					dialog.show();
				}
				else
				{
					// selection of normal list item (not Custom)
					// save position for future
					previous_position = position;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{

			}
		});

	}

	protected SpinnerAdapter newProxy(SpinnerAdapter obj)
	{
		return (SpinnerAdapter) java.lang.reflect.Proxy.newProxyInstance(
				                                                                obj.getClass().getClassLoader(),
				                                                                new Class[]{SpinnerAdapter.class},
				                                                                new SpinnerAdapterProxy(obj));
	}

	/**
	 * Intercepts getView() to display the prompt if position < 0
	 */
	protected class SpinnerAdapterProxy implements InvocationHandler
	{

		protected SpinnerAdapter obj;
		protected Method getView;


		protected SpinnerAdapterProxy(SpinnerAdapter obj)
		{
			this.obj = obj;
			try
			{
				this.getView = SpinnerAdapter.class.getMethod("getView", int.class, View.class, ViewGroup.class);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		public Object invoke(Object proxy, Method m, Object[] args) throws Throwable
		{
			try
			{
				return m.equals(getView) &&
						       (Integer) (args[0]) < 0 ?
						       getView((Integer) args[0], (View) args[1], (ViewGroup) args[2]) :
						       m.invoke(obj, args);
			}
			catch (InvocationTargetException e)
			{
				throw e.getTargetException();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		protected View getView(int position, View convertView, ViewGroup parent)
				throws IllegalAccessException
		{

			if (position < 0)
			{
				final TextView v = (TextView) ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_spinner_item, parent, false);

				// set default text prompt and color
				//TODO set text font/style to match
				v.setText(getPrompt());

				// get hint color
				//final ColorStateList colors = new EditText(context).getHintTextColors();
				v.setTextColor(ContextCompat.getColor(context, R.color.text_dark_hint));
				//v.setTextColor(colors);
				return v;
			}
			return obj.getView(position, convertView, parent);
		}
	}

	// returns the String value of currently selected list item
	public String getSelectedString()
	{
		if(selected_position != -1)
		{
			return spinnerArrayAdapter.getItem(selected_position);
		}
		else
		{
			return null;
		}
	}

	// sets the current selection by inputed String
	public void setSelection(String selection)
	{
		selected_position = spinnerArrayAdapter.getPosition(selection);

		// if selected String not found in array list, add to bottom
		if(selected_position == -1)
		{
			// add selection to end of list, but before the Custom item
			spinnerArrayAdapter.remove(CUSTOM_TEXT);
			spinnerArrayAdapter.add(selection);
			spinnerArrayAdapter.add(CUSTOM_TEXT);
			// adjust position of Custom item to reflect addition of new item
			custom_position += 1;

			// item created, set position
			selected_position = spinnerArrayAdapter.getPosition(selection);
			previous_position = selected_position;
		}

		setSelection(selected_position);


		return;
	}

}
