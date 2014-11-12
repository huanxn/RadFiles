package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 11/5/2014.
 */
public class ManageListsAdapter extends RecyclerView.Adapter<ManageListsAdapter.ViewHolder> implements View.OnClickListener {
	private Activity activity;
	private List<String> itemList;
	private final String ADD_CUSTOM_TEXT = "Add new...";

	// Provide a suitable constructor (depends on the kind of dataset)
	public ManageListsAdapter(Activity activity, Cursor cursor)
	{
		this.activity = activity;
		itemList = new ArrayList<String>();

		if(cursor != null && cursor.moveToFirst())
		{
			do
			{
				itemList.add(cursor.getString(CasesProvider.COL_VALUE));

			} while(cursor.moveToNext());
		}
		itemList.add(ADD_CUSTOM_TEXT);

		//mDataset = stringList.toArray(new String[stringList.size()]);
	}

	// Create new views (invoked by the layout manager)
	@Override
	public ManageListsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
	                                               int viewType) {
		// create a new view
		TextView v = (TextView)LayoutInflater.from(parent.getContext()).inflate(R.layout.list_sortable, parent, false);
		// set the view's size, margins, paddings and layout parameters
		ViewHolder holder = new ViewHolder(v);

		holder.mTextView.setTag(holder);



		return holder;
	}

	// Replace the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		// - get element from your dataset at this position
		// - replace the contents of the view with that element
		holder.mTextView.setText(itemList.get(position));
		holder.mTextView.setClickable(true);

		holder.mTextView.setOnClickListener(this);
	}

	// Return the size of your dataset (invoked by the layout manager)
	@Override
	public int getItemCount() {
		return itemList.size();
	}

	@Override
	public void onClick(View view)
	{
		final ViewHolder holder = (ViewHolder) view.getTag();

		AlertDialog.Builder alert = new AlertDialog.Builder(activity);

		// Set an EditText view to get user input
		final EditText input = new EditText(activity);
		input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

		final int position = holder.getPosition();

		if(holder.getPosition() < itemList.size()-1)
		{
			alert.setTitle("Edit Item");

			// show current text in the edit box
			input.setText(holder.mTextView.getText());
		}
		else
		{
			alert.setTitle("Add Item");
		}

		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				String value = input.getText().toString();

				itemList.set(position, value);

				// if added a new item, replace the "add new" item in last position
				if(position == itemList.size()-1)
				{
					itemList.add(ADD_CUSTOM_TEXT);
				}

				notifyDataSetChanged();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		AlertDialog dialog = alert.create();
		// Show keyboard
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
			}
		});
		dialog.show();

	}



	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder
	public static class ViewHolder extends RecyclerView.ViewHolder
	{
		// each data item is just a string in this case
		public TextView mTextView;
		public ViewHolder(TextView v)
		{
			super(v);
			mTextView = v;
		}
	}

}

