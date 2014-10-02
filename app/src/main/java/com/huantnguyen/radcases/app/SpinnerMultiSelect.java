/*
 * Copyright (C) 2012 Kris Wong
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
 * limitations under the License.
 */

package com.huantnguyen.radcases.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.database.Cursor;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

/**
 * A Spinner view that does not dismiss the dialog displayed when the control is "dropped down"
 * and the user presses it. This allows for the selection of more than one option.
 */
public class SpinnerMultiSelect extends Spinner implements OnMultiChoiceClickListener {
	List<String> stringList = new ArrayList<String>();
	String[] _items = null;
    boolean[] _selection = null;

	private int custom_position;                        // position in list, which is at the end
	private String custom_string;                       // inputed string
	private String custom_alert_title;                  // alert dialog title
	static final private String CUSTOM_TEXT = "CUSTOM"; // test in spinner list
    
    private ArrayAdapter<String> spinnerArrayAdapter;

	Context context;
    
    /**
     * Constructor for use when instantiating directly.
     * @param context
     */
    public SpinnerMultiSelect(Context context)
    {
        super(context);
	    this.context = context;
        
        spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item);
        super.setAdapter(spinnerArrayAdapter);
    }

    /**
     * Constructor used by the layout inflater.
     * @param context
     * @param attrs
     */
    public SpinnerMultiSelect(Context context, AttributeSet attrs)
    {
        super(context, attrs);
	    this.context = context;
        
        spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item);
        super.setAdapter(spinnerArrayAdapter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked)
    {
        if (_selection != null)
        {
	        if(which < _selection.length-1)
	        {
		        _selection[which] = isChecked;

		        spinnerArrayAdapter.clear();
		        spinnerArrayAdapter.add(buildSelectedItemString());
		        setSelection(0);
	        }
	        else if (which == _selection.length-1) // add custom
	        {
		        // alert dialog for custom item text

		        // Get user input for new list item
		        AlertDialog.Builder alert = new AlertDialog.Builder(context);

		        alert.setTitle(custom_alert_title);
		        //alert.setMessage("message");

		        // Set an EditText view to get user input
		        final EditText input = new EditText(context);
		        //input.requestFocus();
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

				        // save position for future
				        //previous_position = selected_position;
			        }
		        });

		        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int whichButton) {
				        // revert back to previous position in list
				        //selected_position = previous_position;
				        //adapter.setSelection(previous_position);

			        }
		        });

		        alert.show();
	        }
        }
        else
        {
            throw new IllegalArgumentException("Argument 'which' is out of bounds.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMultiChoiceItems(_items, _selection, this);
        builder.show();
        return true;
    }
    
    /**
     * SpinnerMultiSelect does not support setting an adapter. This will throw an exception.
     * @param adapter
     */
    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        throw new RuntimeException("setAdapter is not supported by SpinnerMultiSelect.");
    }
    
    /**
     * Sets the options for this spinner.
     * @param items
     */
    public void setItems(String[] items)
    {
        _items = items;
        _selection = new boolean[_items.length];
        
        Arrays.fill(_selection, false);
    }
    
    /**
     * Sets the options for this spinner.
     * @param items
     */
    public void setItems(List<String> items)
    {
	    stringList = items;//does this work?

        _items = items.toArray(new String[items.size()]);
        _selection = new boolean[_items.length];
        
        Arrays.fill(_selection, false);
    }

	public void setItems(Cursor cursor, int column)
	{
		if(cursor.moveToFirst())
		{
			do
			{
				stringList.add(cursor.getString(column));

			} while(cursor.moveToNext());
		}

		stringList.add(CUSTOM_TEXT);

		_items = stringList.toArray(new String[stringList.size()]);
		_selection = new boolean[_items.length];

		Arrays.fill(_selection, false);
	}
    
    /**
     * Sets the selected options based on an array of string.
     * @param selection
     */
    public void setSelection(String[] selection) {
        for (String sel : selection) {
            for (int j = 0; j < _items.length; ++j) {
                if (_items[j].equals(sel)) {
                    _selection[j] = true;
                }
            }
        }
    }
    
    /**
     * Sets the selected options based on a list of string.
     * @param selection
     */
    public void setSelection(List<String> selection) {
	    Boolean isInItemList;

        for (String sel : selection) {
	        isInItemList = false;
            for (int j = 0; j < _items.length; ++j) {
                if (_items[j].equals(sel)) {
                    _selection[j] = true;
	                isInItemList = true;
                }
            }

	        if(!isInItemList)
	        {
		        _items = addElement(_items, sel);
		        _selection = addElement(_selection, true);
	        }
        }
	    spinnerArrayAdapter.add(buildSelectedItemString());
    }
    
    /**
     * Sets the selected options based on an array of positions.
     * @param selectedIndicies
     */
    public void setSelection(int[] selectedIndicies) {
        for (int index : selectedIndicies) {
            if (index >= 0 && index < _selection.length) {
                _selection[index] = true;
            }
            else {
                throw new IllegalArgumentException("Index " + index + " is out of bounds.");
            }
        }
    }

	// comma-separated single string
	public void setSelection(String itemsString) {
		List<String> itemsList = Arrays.asList(itemsString.split("\\s*,\\s*"));
		setSelection(itemsList);
	}
    
    /**
     * Returns a list of strings, one for each selected item.
     * @return
     */
    public List<String> getSelectedStrings() {
        List<String> selection = new LinkedList<String>();
        for (int i = 0; i < _items.length; ++i) {
            if (_selection[i]) {
                selection.add(_items[i]);
            }
        }
        return selection;
    }

	public String getSelectedString()
	{
		return buildSelectedItemString();
	}
    
    /**
     * Returns a list of positions, one for each selected item.
     * @return
     */
    public List<Integer> getSelectedIndicies() {
        List<Integer> selection = new LinkedList<Integer>();
        for (int i = 0; i < _items.length; ++i) {
            if (_selection[i]) {
                selection.add(i);
            }
        }
        return selection;
    }
    
    /**
     * Builds the string for display in the spinner.
     * @return comma-separated list of selected items
     */
    private String buildSelectedItemString() {
        StringBuilder sb = new StringBuilder();
        boolean foundOne = false;
        
        for (int i = 0; i < _items.length; ++i) {
            if (_selection[i]) {
                if (foundOne) {
                    sb.append(", ");
                }
                foundOne = true;
                
                sb.append(_items[i]);
            }
        }
        
        return sb.toString();
    }

	private boolean[] addElement(boolean[] org, boolean added) {
		boolean[] result = Arrays.copyOf(org, org.length +1);
		result[org.length] = added;
		return result;
	}
	private String[] addElement(String[] org, String added) {
		String[] result = Arrays.copyOf(org, org.length +1);
		result[org.length] = added;
		return result;
	}
}
