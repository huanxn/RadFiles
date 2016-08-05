package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 11/5/2014.
 */
public class ManageListsAdapter
		extends RecyclerView.Adapter<ManageListsAdapter.ViewHolder>
		implements View.OnClickListener, View.OnLongClickListener, DraggableItemAdapter<ManageListsAdapter.ViewHolder>
{
	private Activity activity;
	private List<String> itemList;
	private List<Integer> keyList;
	private List<Boolean> isHiddenList;
	private final String ADD_CUSTOM_TEXT = "Add new...";

	private ViewHolder firstViewHolder = null;
	private ViewHolder lastViewHolder = null;

	// Provide a suitable constructor (depends on the kind of dataset)
	public ManageListsAdapter(Activity activity, Cursor cursor)
	{
		this.activity = activity;
		itemList = new ArrayList<String>();
		keyList = new ArrayList<Integer>();
		isHiddenList = new ArrayList<Boolean>();

		if(cursor != null && cursor.moveToFirst())
		{
			do
			{
				itemList.add(cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE));
				keyList.add(cursor.getInt(CasesProvider.COL_ROWID));
				if(cursor.getInt(CasesProvider.COL_LIST_ITEM_IS_HIDDEN) == 1)
				{
					isHiddenList.add(true);
				}
				else    // == 0
				{
					isHiddenList.add(false);    // visible
				}

			} while(cursor.moveToNext());
		}
		itemList.add(ADD_CUSTOM_TEXT);
		keyList.add(-1);
		isHiddenList.add(false);

		setHasStableIds(true);
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

		holder.mContainer.setTag(holder);   // used to get holder with clickListener

		return holder;
	}

	// Replace the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		// - get element from your dataset at this position
		// - replace the contents of the view with that element
		holder.mTextView.setText(itemList.get(position));

		// grey-out if hidden
		if(isHiddenList.get(position))
		{
			holder.mTextView.setTextColor(activity.getResources().getColor(R.color.text_dark_hint));
		}
		else
		{
			holder.mTextView.setTextColor(activity.getResources().getColor(R.color.text_dark));
		}

		holder.mContainer.setOnClickListener(this);
		holder.mContainer.setOnLongClickListener(this);

		//if(holder.mTextView.equals(ADD_CUSTOM_TEXT))
		if(position >= itemList.size()-1)
		{
			//holder.mHandle.setImageDrawable(activity.getDrawable(R.drawable.ic_plus_circle_grey600_18dp));
			holder.mHandle.setImageResource(activity.getResources().getIdentifier("com.radicalpeas.radcases.app:drawable/ic_plus_circle_grey600_18dp", null, null));

			lastViewHolder = holder;
		}
		else
		{
			holder.mHandle.setImageResource(activity.getResources().getIdentifier("com.radicalpeas.radcases.app:drawable/ic_menu_grey600_18dp", null, null));
		}

		// set background resource (target view ID: container)
		final int dragState = holder.getDragStateFlags();

		if (((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_UPDATED) != 0)) {
			int bgResId;

			if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_ACTIVE) != 0) {
				bgResId = R.drawable.bg_item_dragging_active_state;
			} else if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_DRAGGING) != 0) {
				bgResId = R.drawable.bg_item_dragging_state;
			} else {
				bgResId = R.drawable.bg_item_normal_state;
			}

			holder.mContainer.setBackgroundResource(bgResId);
		}

		if(firstViewHolder == null)
			firstViewHolder = holder;


	}

	// for ShowcaseView tutorials
	public ViewHolder getFirstViewHolder()
	{
		return firstViewHolder;
	}
	public ViewHolder getLastViewHolder()
	{
		return lastViewHolder;
	}
	public int getLastViewPosition()
	{
		return itemList.size()-1;
	}

	public void removeItem(int position)
	{
		itemList.remove(position);
		keyList.remove(position);
		isHiddenList.remove(position);
	}

	// Return the size of your dataset (invoked by the layout manager)
	@Override
	public int getItemCount() {
		return itemList.size();
	}

	@Override
	public long getItemId(int position) {
		//return position;

		//return itemList.get(position).hashCode();
		if(keyList == null)
			return -1;
		else
			return keyList.get(position);
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

	public int getIsHidden(int position)
	{
		if(isHiddenList.get(position))
			return 1;
		else            // false or null
			return 0;

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

		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);

		// Set an EditText view to get user input
		final EditText input = new EditText(activity);
		input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		//input.setHighlightColor(activity.getResources().getColor(R.color.default_colorControlHighlight));
		input.setHighlightColor(UtilClass.get_attr(activity, R.attr.colorControlHighlight));

		final int position = holder.getPosition();

		if(holder.getPosition() < itemList.size()-1)
		{
			alertBuilder.setTitle("Edit Item");

			// show current text in the edit box
			input.setText(holder.mTextView.getText());
		}
		else
		{
			alertBuilder.setTitle("Add Item");
		}

		alertBuilder.setView(input);

		alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				String value = input.getText().toString();

				itemList.set(position, value);

				// if added a new item, replace the "add new" item in last position
				if (position == itemList.size() - 1)
				{
					itemList.add(ADD_CUSTOM_TEXT);
					keyList.add(-1);
					isHiddenList.add(false);

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

		alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				// Canceled.
			}
		});

		AlertDialog dialog = alertBuilder.create();
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

		// if not the add-custom button
		if(holder.getPosition() < itemList.size()-1)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);

			String hideString = "Hide";

			if(isHiddenList.get(holder.getPosition()))
			{
				hideString = "Unhide";
			}

			CharSequence[] choices = {hideString, "Delete"};
			builder.setTitle(holder.mTextView.getText())
					.setItems(choices, new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int index)
						{
							switch (index)
							{
								// hide list item
								case 0:

									ContentValues values = new ContentValues();
									// toggle
									if(isHiddenList.get(holder.getPosition()))
									{
										isHiddenList.set(holder.getPosition(), false);
									}
									else
									{
										isHiddenList.set(holder.getPosition(), true);
									}

									// change database in ManageListsActivity
									notifyItemChanged(position);

									break;

								// delete list item
								case 1:
									// delete item from database and from Lists in ManageListsActivity
									notifyItemRemoved(holder.getPosition());

									break;



								// Do Nothing.
								default:
									break;

							}
						}
					});

			AlertDialog alert = builder.create();
			alert.show();
		}



		return true;
	}


	/**
	 * Drag drop resorting
	 * @param holder
	 * @param x
	 * @param y
	 * @return
	 */
	@Override
	public boolean onCheckCanStartDrag(ManageListsAdapter.ViewHolder holder, int x, int y) {

		// x, y --- relative from the itemView's top-left
		final View containerView = holder.mContainer;
		final View dragHandleView = holder.mHandle;

		final int offsetX = containerView.getLeft() + (int) (ViewCompat.getTranslationX(containerView) + 0.5f);
		final int offsetY = containerView.getTop() + (int) (ViewCompat.getTranslationY(containerView) + 0.5f);

		return UtilClass.hitTest(dragHandleView, x - offsetX, y - offsetY);



		//return true;

	}

	/**
	 * drag drop resorting
	 * @param fromPosition
	 * @param toPosition
	 */
	@Override
	public void onMoveItem(int fromPosition, int toPosition)
	{
		if (fromPosition == toPosition)
		{
			return;
		}

		// don't change position of last item (add new custom item)
		if(fromPosition >= itemList.size()-1 || toPosition >= itemList.size()-1)
			return;

		// remember selected item info ("from")
		String temp = itemList.get(fromPosition);
		int temp_key = keyList.get(fromPosition);
		boolean temp_hidden = isHiddenList.get(fromPosition);

		if(fromPosition < toPosition)
		{
			// move all items inbetween down one position
			for(int i = fromPosition; i < toPosition; i++)
			{
				itemList.set(i, itemList.get(i+1));
				keyList.set(i, keyList.get(i+1));
				isHiddenList.set(i, isHiddenList.get(i+1));
			}
		}
		else if(fromPosition > toPosition)
		{
			// move all items inbetween up one position
			for(int i = fromPosition; i > toPosition; i--)
			{
				itemList.set(i, itemList.get(i-1));
				keyList.set(i, keyList.get(i-1));
				isHiddenList.set(i, isHiddenList.get(i-1));
			}
		}

		// move the selected item from old position to new position
		itemList.set(toPosition, temp);
		keyList.set(toPosition, temp_key);
		isHiddenList.set(toPosition, temp_hidden);

		// update database in ManageListsActivity
		notifyItemMoved(fromPosition, toPosition);
	}


	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder
	public static class ViewHolder extends AbstractDraggableSwipeableItemViewHolder
	{
		// each data item is just a string in this case
		public ViewGroup mContainer;
		public TextView mTextView;
		public ImageView mHandle;

		public ViewHolder(View v)
		{
			super(v);
			mContainer = (ViewGroup) v.findViewById(R.id.container);
			mHandle = (ImageView) v.findViewById(R.id.handle);
			mTextView = (TextView) v.findViewById(R.id.item_text);
		}

		@Override
		public View getSwipeableContainerView()
		{
			return mContainer;
		}

	}

}

