package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 11/5/2014.
 */
public class ManageListsAdapter extends RecyclerView.Adapter<ManageListsAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener{
	private Activity activity;
	private List<String> itemList;
	private List<Integer> keyList;
	private final String ADD_CUSTOM_TEXT = "Add new...";

	private int visible_discard_position = -1;

	private ImageButton discard_button = null;

	// Provide a suitable constructor (depends on the kind of dataset)
	public ManageListsAdapter(Activity activity, Cursor cursor)
	{
		this.activity = activity;
		itemList = new ArrayList<String>();
		keyList = new ArrayList<Integer>();

		if(cursor != null && cursor.moveToFirst())
		{
			do
			{
				itemList.add(cursor.getString(CasesProvider.COL_VALUE));
				keyList.add(cursor.getInt(CasesProvider.COL_ROWID));

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
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_sortable, parent, false);
		//TextView v = (TextView)LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_sortable, parent, false);
		// set the view's size, margins, paddings and layout parameters
		ViewHolder holder = new ViewHolder(view);

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
		holder.mTextView.setOnLongClickListener(this);

		holder.mDiscardButton.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View view)
			{
				// delete item from database and from Lists in ManageListsActivity
				notifyItemRemoved(visible_discard_position);

				// reset
				visible_discard_position = -1;
			}

		});

		//if(holder.mTextView.equals(ADD_CUSTOM_TEXT))
		if(position >= itemList.size()-1)
		{
			//holder.mHandle.setImageDrawable(activity.getDrawable(R.drawable.ic_plus_circle_grey600_18dp));
			holder.mHandle.setImageResource(activity.getResources().getIdentifier("com.huantnguyen.radcases.app:drawable/ic_plus_circle_grey600_18dp", null, null));
		}
		else
		{
			holder.mHandle.setImageResource(activity.getResources().getIdentifier("com.huantnguyen.radcases.app:drawable/ic_menu_grey600_18dp", null, null));
		}

		if(position == visible_discard_position)
		{
			showDiscardButton(holder);
		}
		else
		{
			holder.mDiscardButton.setVisibility(View.INVISIBLE);
		}
	}

	private void showDiscardButton(ViewHolder holder)
	{
		visible_discard_position = holder.getPosition();

		// hide old button
		if(discard_button != null)
		{
			discard_button.setVisibility(View.INVISIBLE);
		}

		// show new button
		discard_button = holder.mDiscardButton;
		discard_button.setVisibility(View.VISIBLE);
	}

	public void removeItem(int position)
	{
		itemList.remove(position);
		keyList.remove(position);
	}

	// Return the size of your dataset (invoked by the layout manager)
	@Override
	public int getItemCount() {
		return itemList.size();
	}

	@Override
	public long getItemId(int position) {
		//return itemList.get(position).hashCode();
		return position;
	}

	public List<String> getList()
	{
		return itemList;
	}

	public String getItem(int position)
	{
		return itemList.get(position);
	}

	public int getKey(int position)
	{
		return keyList.get(position);
	}

	public void setKey(int position, int key_id)
	{
		if(position < keyList.size())
			keyList.set(position, key_id);
	}


	@Override
	public void onClick(View view)
	{
		final ViewHolder holder = (ViewHolder) view.getTag();

		AlertDialog.Builder alert = new AlertDialog.Builder(activity);

		// Set an EditText view to get user input
		final EditText input = new EditText(activity);
		input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		//input.setHighlightColor(activity.getResources().getColor(R.color.default_colorControlHighlight));
		input.setHighlightColor(UtilClass.get_attr(activity, R.attr.colorControlHighlight));

		final int position = holder.getPosition();

		if(holder.getPosition() < itemList.size()-1)
		{
			alert.setTitle("Edit Item");

			// show current text in the edit box
			input.setText(holder.mTextView.getText());

			// show delete item button
			showDiscardButton(holder);
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
					keyList.add(-1);

					// add item to database and set key_id in ManageListsActivity
					notifyItemInserted(position);
				}
				else
				{
					notifyItemChanged(position);
				}

				//notifyDataSetChanged();

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

	@Override
	public boolean onLongClick(View view)
	{
		final ViewHolder holder = (ViewHolder) view.getTag();

		final int position = holder.getPosition();

		if(holder.getPosition() < itemList.size()-1)
		{
			// show delete item button
			showDiscardButton(holder);
		}

		/*
		if(position == visible_discard_position)
		{
			// hide old button
			if(discard_button != null)
			{
				discard_button.setVisibility(View.INVISIBLE);
			}

			// show new button
			discard_button = holder.mDiscardButton;
			discard_button.setVisibility(View.VISIBLE);
			//holder.mDiscardButton.setVisibility(View.VISIBLE);
		}
		else
		{
			discard_button.setVisibility(View.INVISIBLE);
		}
*/

		return true;
	}

	/*
	@Override
	public void moveElements(int fromIndex, int toIndex)
	{
		// don't change position of last item (add new custom item)
		if(fromIndex >= itemList.size()-1 || toIndex >= itemList.size()-1)
			return;

		String temp = itemList.get(fromIndex);
		itemList.set(fromIndex, itemList.get(toIndex));
		itemList.set(toIndex, temp);

		int temp_key = keyList.get(fromIndex);
		keyList.set(fromIndex, keyList.get(toIndex));
		keyList.set(toIndex, temp_key);

		//notifyDataSetChanged();
		notifyItemMoved(fromIndex, toIndex);
	}
	*/

	/**
	 * moveElements
	 * move list items in adapter (only)
	 * @param fromIndex
	 * @param toIndex
	 */
	public void moveElements(int fromIndex, int toIndex)
	{
		// don't change position of last item (add new custom item)
		if(fromIndex >= itemList.size()-1 || toIndex >= itemList.size()-1)
			return;
		else if(fromIndex == toIndex)
		{
			visible_discard_position = toIndex;
			notifyDataSetChanged();
			return;
		}

		// remember selected item info ("from")
		String temp = itemList.get(fromIndex);
		int temp_key = keyList.get(fromIndex);

		if(fromIndex < toIndex)
		{
			// move all items inbetween down one position
			for(int i = fromIndex; i < toIndex; i++)
			{
				itemList.set(i, itemList.get(i+1));
				keyList.set(i, keyList.get(i+1));
			}
		}
		else if(fromIndex > toIndex)
		{
			// move all items inbetween up one position
			for(int i = fromIndex; i > toIndex; i--)
			{
				itemList.set(i, itemList.get(i-1));
				keyList.set(i, keyList.get(i-1));
			}
		}

		// move the selected item from old position to new position
		itemList.set(toIndex, temp);
		keyList.set(toIndex, temp_key);

		visible_discard_position = toIndex;

		// update database in ManageListsActivity
		notifyItemMoved(fromIndex, toIndex);
	}


	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder
	public static class ViewHolder extends RecyclerView.ViewHolder
	{
		// each data item is just a string in this case
		public ImageView mHandle;
		public TextView mTextView;
		public ImageButton mDiscardButton;

		public ViewHolder(View v)
		{
			super(v);
			mHandle = (ImageView) v.findViewById(R.id.handle);
			mTextView = (TextView) v.findViewById(R.id.item_text);
			mDiscardButton = (ImageButton) v.findViewById(R.id.discard_button);
		}
	}

}

