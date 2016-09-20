package com.radicalpeas.radfiles.app;

/**
 * Created by huanx on 9/18/2016.
 */
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

import android.content.Context;
import android.database.Cursor;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

/**
 * A Button view that opens a dialog with multiple check boxes
 *
 */
public class SpinnerMultiSelect extends Button
{
    private String TAG = "SpinnerMultiSelect";

    private String title;
    private String custom_title;

    String[] _items = null;
    Integer[] _selectedIndices = null;

    private String hint = "";

    private MaterialDialog dialog = null;


    Context context;

    public SpinnerMultiSelect(Context context)
    {
        super(context);
        this.context = context;
    }

    public SpinnerMultiSelect(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
    }

    public void setAddCustomTitle(String text)
    {
        custom_title = text;
    }

    public void buildDialog()
    {
        final SpinnerMultiSelect button = this;
        dialog = new MaterialDialog.Builder(context)
                .title(title)
                .items(_items)
                .itemsCallbackMultiChoice(_selectedIndices, new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text)
                    {
                        /**
                         * If you use alwaysCallMultiChoiceCallback(), which is discussed below,
                         * returning false here won't allow the newly selected check box to actually be selected.
                         * See the limited multi choice dialog example in the sample project for details.
                         **/
                        _selectedIndices = which;
                        Log.d(TAG, "text: " + text);

                        button.showText();
                        return true;
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {
                        // alert dialog for adding a new item into list
                        new MaterialDialog.Builder(context)
                                .title(button.custom_title)
                                .negativeText("Cancel")
                                .inputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                                .input(null, null, new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(MaterialDialog dialog, CharSequence input) {
                                        // Add new keyword to list
                                        _items = addElement(_items, input.toString());
                                        _selectedIndices = addElement(_selectedIndices, _items.length-1);
                                        button.showText();
                                    }
                                }).show();

                    }
                })
                .positiveText("OK")
                .neutralText("New...")
                .build();
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void showDialog()
    {
        buildDialog();
        dialog.show();
    }

    public void showText()
    {
        String sb = buildSelectedItemString();
        setText(sb);
    }

    /**
     * {@inheritDoc}
     */
    /*
    @Override
    public void onClick(DialogInterface dialogInterface, int which, boolean isChecked)
    {
        final SpinnerMultiSelect spinnerMultiSelect = this;
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

                        //TODO uncheck custom box on cancel and close
                        _selection[_selection.length-1] = false;



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
    */

    /**
     * {@inheritDoc}
     */
    /*
    @Override
    public boolean performClick()
    {
        dialog.show();
        return true;
    }
*/

    /**
     * Sets the options for this spinner.
     * @param items
     */
    public void setItems(String[] items)
    {
        _items = items;
    }

    /**
     * Sets the options for this spinner.
     * @param items
     */
    public void setItems(List<String> items)
    {
        _items = items.toArray(new String[items.size()]);
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

        _items = stringList.toArray(new String[stringList.size()]);

    }

    /**
     * Sets the selected options based on an array of string.
     * @param selection
     */
    /*
    public void setSelection(String[] selection) {
        for (String sel : selection) {
            for (int j = 0; j < _items.length; ++j) {
                if (_items[j].equals(sel)) {
                    _selection[j] = true;
                }
            }
        }
    }
    */

    /**
     * Sets the selected options based on a list of string.
     * @param selection
     */
    public void setSelection(List<String> selection)
    {
        Boolean isInItemList;
        _selectedIndices = null;

        for (String sel : selection)
        {
            int index = 0;
            isInItemList = false;
            for (int j = 0; j < _items.length; ++j) {
                if (_items[j].equals(sel)) {
                    _selectedIndices = addElement(_selectedIndices, j);
                    isInItemList = true;
                }
            }

            if(!isInItemList)
            {
		        _items = addElement(_items, sel);
                _selectedIndices = addElement(_selectedIndices, _items.length-1);
                //_selectedIndices[index++] = _items.length-1;
            }
        }
        showText();
    }

    /**
     * Sets the selected options based on an array of positions.
     * @param selectedIndicies
     */
    /*
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
    */

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

        for(int i = 0; i < _selectedIndices.length; i++)
        {
            selection.add(_items[_selectedIndices[i]]);
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
    /*
    public List<Integer> getSelectedIndicies() {
        List<Integer> selection = new LinkedList<Integer>();
        for (int i = 0; i < _items.length; ++i) {
            if (_selection[i]) {
                selection.add(i);
            }
        }
        return selection;
    }
    */

    /**
     * Builds the string for display in the spinner.
     * @return comma-separated list of selected items
     */
    private String buildSelectedItemString()
    {
        StringBuilder sb = new StringBuilder();

        if(_selectedIndices == null)
        {
            return null;
        }

        for(int i = 0; i < _selectedIndices.length; i++)
        {
            if(i > 0)
            {
                sb.append(", ");
            }

            sb.append(_items[_selectedIndices[i]]);
        }

        return sb.toString();
    }

    private boolean[] addElement(boolean[] org, boolean added) {
        boolean[] result = Arrays.copyOf(org, org.length +1);
        result[org.length] = added;
        return result;
    }
    private Integer[] addElement(Integer[] org, Integer added)
    {
        Integer result[];

        if(org == null)
        {
            result = new Integer[1];
            result[0] = added;
        }
        else
        {
            result = Arrays.copyOf(org, org.length + 1);
            result[org.length] = added;
        }
        return result;
    }
    private String[] addElement(String[] org, String added) {
        String[] result = Arrays.copyOf(org, org.length +1);
        result[org.length] = added;
        return result;
    }


}
