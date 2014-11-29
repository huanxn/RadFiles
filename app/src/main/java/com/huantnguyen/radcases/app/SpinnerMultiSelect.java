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
import android.text.InputType;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

/**
 * A Spinner view that does not dismiss the dialog displayed when the control is "dropped down"
 * and the user presses it. This allows for the selection of more than one option.
 */
public class SpinnerMultiSelect extends Spinner implements OnMultiChoiceClickListener {
	//List<String> stringList = new ArrayList<String>();
    String[] _items = null;
    boolean[] _selection = null;

	private int custom_position;                        // position in list, which is at the end
	private String custom_alert_title;                  // alert dialog title
	static final private String CUSTOM_TEXT = "Custom..."; // test in spinner list
	private int previous_position;                      // in case canceled custom input, revert back to previous

	private String hint = "";

    ArrayAdapter<String> _proxyAdapter;
    
   	Context context;

    /**
     * Constructor for use when instantiating directly.
     * @param context
     */
    public SpinnerMultiSelect(Context context)
    {
        super(context);
	    this.context = context;
        
        //_proxyAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item);
	    _proxyAdapter = new ArrayAdapter<String>(context, R.layout.spinner_multiselect);
        super.setAdapter(_proxyAdapter);
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

        //_proxyAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item);
	    _proxyAdapter = new ArrayAdapter<String>(context, R.layout.spinner_multiselect);
        super.setAdapter(_proxyAdapter);

    }

	public void showText()
	{
		String sb = buildSelectedItemString();

		if(sb == null || sb.isEmpty())
		{
			_proxyAdapter = new ArrayAdapter<String>(context, R.layout.spinner_multiselect);
			super.setAdapter(_proxyAdapter);

			_proxyAdapter.clear();
			_proxyAdapter.add(String.valueOf(getPrompt()));

			/*
			// get hint color
			final ColorStateList colors = new EditText(context).getHintTextColors();
			//v.setTextColor(context.getResources().getColor(R.color.light_grey_text));
			this.setTextColor(colors);
			*/
		}
		else
		{
			_proxyAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item);
			super.setAdapter(_proxyAdapter);

			_proxyAdapter.clear();
			_proxyAdapter.add(sb);
			setSelection(0);
		}
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(DialogInterface dialogInterface, int which, boolean isChecked)
    {
        if (_selection != null)
        {
	        if(which < _selection.length-1)
	        {
		        _selection[which] = isChecked;

                showText();
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
		        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

		        // doesn't work....  todo fix
		        input.setHighlightColor(UtilClass.get_attr(context, R.attr.colorControlHighlight));

		        //input.requestFocus();
		        alert.setView(input);

		        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		        {
			        public void onClick(DialogInterface dialog, int whichButton)
			        {
				        String value = input.getText().toString();

				        // add value to end of list, but before the Custom item
				        _items[_items.length-1] = value;
				        _selection[_selection.length-1] = true; //set newly created custom item (which is now last in list) to be true

				        _items = addElement(_items, CUSTOM_TEXT);
				        _selection = addElement(_selection, false);

				        // save position for future
				        //previous_position = selected_position;

				        showText();
			        }
		        });

		        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int whichButton) {
				        // revert back to previous position in list
				        //selected_position = previous_position;
				        //adapter.setSelection(previous_position);

				        //TODO uncheck custom box on cancel

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
    public boolean performClick()
    {
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
	    //stringList = items;//does this work?

        _items = items.toArray(new String[items.size()]);
        _selection = new boolean[_items.length];
        
        Arrays.fill(_selection, false);
    }

	public void setItems(Cursor cursor, int column)
	{
		List<String> stringList = new ArrayList<String>();

		if(cursor != null && cursor.moveToFirst())
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

		showText();
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
    public void setSelection(List<String> selection)
    {
	    Boolean isInItemList;

        for (String sel : selection)
        {
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

	    //_proxyAdapter.add(buildSelectedItemString());
	    showText();
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
            if (_selection[i])
            {
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
    private String buildSelectedItemString()
    {
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

	/*
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


	protected SpinnerAdapter newProxy(SpinnerAdapter obj)
	{
		return (SpinnerAdapter) java.lang.reflect.Proxy.newProxyInstance(
				                                                                obj.getClass().getClassLoader(),
				                                                                new Class[]{SpinnerAdapter.class},
				                                                                new SpinnerAdapterProxy(obj));
	}
	*/
	
	/**
	 * Intercepts getView() to display the prompt if no items selected
	 */
	/*
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

			if (buildSelectedItemString().isEmpty())
			{
				final TextView v = (TextView) ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_spinner_item, parent, false);

				// set default text prompt and color
				//TODO set text font/style to match
				v.setText(getPrompt());

				// get hint color
				final ColorStateList colors = new EditText(context).getHintTextColors();
				//v.setTextColor(context.getResources().getColor(R.color.light_grey_text));
				v.setTextColor(colors);
				return v;
			}
			return obj.getView(position, convertView, parent);
		}
	}
	*/
}
