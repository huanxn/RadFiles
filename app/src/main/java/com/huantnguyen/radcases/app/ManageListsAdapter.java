package com.huantnguyen.radcases.app;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 11/5/2014.
 */
public class ManageListsAdapter extends RecyclerView.Adapter<ManageListsAdapter.ViewHolder> {
	private String[] mDataset;
	private final String ADD_CUSTOM_TEXT = "Add new...";

	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder
	public static class ViewHolder extends RecyclerView.ViewHolder {
		// each data item is just a string in this case
		public TextView mTextView;
		public ViewHolder(TextView v) {
			super(v);
			mTextView = v;
		}
	}

	// Provide a suitable constructor (depends on the kind of dataset)
	public ManageListsAdapter(Cursor cursor)
	{
		List<String> stringList = new ArrayList<String>();

		if(cursor != null && cursor.moveToFirst())
		{
			do
			{
				stringList.add(cursor.getString(CasesProvider.COL_VALUE));

			} while(cursor.moveToNext());
		}
		stringList.add(ADD_CUSTOM_TEXT);

		mDataset = stringList.toArray(new String[stringList.size()]);
	}

	// Create new views (invoked by the layout manager)
	@Override
	public ManageListsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
	                                               int viewType) {
		// create a new view
		TextView v = (TextView)LayoutInflater.from(parent.getContext()).inflate(R.layout.list_sortable, parent, false);
		// set the view's size, margins, paddings and layout parameters
		ViewHolder vh = new ViewHolder(v);
		return vh;
	}

	// Replace the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		// - get element from your dataset at this position
		// - replace the contents of the view with that element
		holder.mTextView.setText(mDataset[position]);

	}

	// Return the size of your dataset (invoked by the layout manager)
	@Override
	public int getItemCount() {
		return mDataset.length;
	}
}

