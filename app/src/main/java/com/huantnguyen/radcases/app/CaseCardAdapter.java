package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 10/21/2014.
 */

public class CaseCardAdapter extends RecyclerView.Adapter<CaseCardAdapter.ViewHolder> implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder>
{

	public List<Case> caseList;
	private int card_layout_id;
	private Activity activity;

	private File imageDir = CaseCardListActivity.picturesDir;

	// StickyRecyclerHeadersAdapter
	//private List<Integer> header_id;    // if different than previous (ie in a different group), then it will display it's header
	private List<String> header;

	// contextual action mode set in activity
	private ActionMode.Callback mActionModeCallback = null;
	public ActionMode mActionMode = null;

	// contextual multi-select list
	private List<Long> multiselectList;

	// click listeners set in activity
	private View.OnClickListener mOnClickListener;
	private View.OnLongClickListener mOnLongClickListener;

	public CaseCardAdapter(Activity activity, Cursor caseCursor, int card_layout)
	{
		caseList = new ArrayList<Case>();
		if(caseCursor != null)
		{
			loadCases(caseCursor);
			notifyDataSetChanged();
		}

		this.card_layout_id = card_layout;
		this.activity = activity;

		multiselectList = new ArrayList<Long>();

		setHasStableIds(true);
	}

	public void loadCases(Cursor cursor)
	{
		// loop through case cursor and put info into cards
		if (cursor != null && cursor.moveToFirst())
		{
			// clear old list
			if(!caseList.isEmpty())
				caseList.clear();

			do
			{
				Case new_case = new Case();

				new_case.key_id = cursor.getInt(CasesProvider.COL_ROWID);
				new_case.patient_id = cursor.getString(CasesProvider.COL_PATIENT_ID);
				new_case.diagnosis = cursor.getString(CasesProvider.COL_DIAGNOSIS);
				new_case.findings = cursor.getString(CasesProvider.COL_FINDINGS);
				new_case.section = cursor.getString(CasesProvider.COL_SECTION);
				new_case.study_type = cursor.getString(CasesProvider.COL_STUDY_TYPE);
				new_case.db_date_str = cursor.getString(CasesProvider.COL_DATE);
				new_case.key_words = cursor.getString(CasesProvider.COL_KEYWORDS);
				new_case.favorite = cursor.getInt(CasesProvider.COL_FAVORITE);

				new_case.thumbnail = 0;
				String thumbnailString = cursor.getString(CasesProvider.COL_THUMBNAIL);
				if (thumbnailString != null && !thumbnailString.isEmpty())
					new_case.thumbnail = Integer.parseInt(thumbnailString);

				// get images for this case
				String[] image_args = {String.valueOf(new_case.key_id)};
				Cursor image_cursor = activity.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

				if(image_cursor.getCount() > 0 && image_cursor.moveToFirst())
				{
					if(new_case.thumbnail < image_cursor.getCount())
					{
						image_cursor.move(new_case.thumbnail);
					}

					//new_case.thumbnail_filename = CaseCardListActivity.picturesDir + "/" + image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);
					new_case.thumbnail_filename = imageDir + "/" + image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);
				}
				else
				{
					new_case.thumbnail_filename = null;
				}
				image_cursor.close();

				caseList.add(new_case);

			} while(cursor.moveToNext());

		}
		else
		{
			caseList.clear();
		}

//		notifyDataSetChanged();
	}

	/**
	 * used for import JSON
	 * @param inputCases
	 */
	public void loadCaseList(List<Case> inputCases)
	{
		// loop through case cursor and put info into cards
		if (inputCases != null && !inputCases.isEmpty())
		{
			caseList = inputCases;
		}
		else
		{
			caseList.clear();
		}

		//notifyDataSetChanged();
	}

	public void removeCase(long key_id)
	{
		for(int i = 0; i < caseList.size(); i++)
		{
			if(caseList.get(i).key_id == key_id)
			{
				caseList.remove(i);
				return;
			}
		}
	}

	public void setActionModeCallback(ActionMode.Callback actionModeCallback)
	{
		if(actionModeCallback != null)
		{
			mActionModeCallback = actionModeCallback;
		}
	}

	public void setActionMode(ActionMode actionMode)
	{
		mActionMode = actionMode;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(card_layout_id, viewGroup, false);

		ViewHolder holder = new ViewHolder(v);

		holder.cardView.setOnClickListener(mOnClickListener);
		holder.cardView.setOnLongClickListener(mOnLongClickListener);

		holder.cardView.setTag(holder);

		return holder;
	}

	@Override
	/**
	 * redraws when cards scroll into view
	 */
	public void onBindViewHolder(ViewHolder viewHolder, int i)
	{
		if(caseList != null)
		{
			Case mCase = caseList.get(i);

			viewHolder.key_id = mCase.key_id;

			viewHolder.card_title.setText(mCase.patient_id);
			viewHolder.card_text1.setText(mCase.diagnosis);
			viewHolder.card_text2.setText(mCase.findings);
			viewHolder.card_text3.setText(mCase.key_words);
			//viewHolder.thumbnail.setImageDrawable(activity.getDrawable(country.getImageResourceId(mContext)));

			if(UtilClass.setPic(viewHolder.thumbnail, mCase.thumbnail_filename, UtilClass.IMAGE_THUMB_SIZE))
			{
				viewHolder.thumbnail.setVisibility(View.VISIBLE);
			}
			else
			{
				viewHolder.thumbnail.setVisibility(View.GONE);
			}

			if(mActionMode == null || !mCase.isSelected)
			{
				// clear color highlight of unselected items
				viewHolder.container.setBackgroundColor(activity.getResources().getColor(R.color.default_card_background));
				viewHolder.thumbnail.setColorFilter(0x00000000);

				/*
				cardView.setMaxCardElevation(20);
				cardView.setCardElevation(20);
				*/
			}
			else if(mCase.isSelected)
			{
				//viewHolder.container.setBackgroundColor(activity.getResources().getColor(R.attr.colorAccent));

				// color highlight the selected items
				viewHolder.container.setBackgroundColor(activity.getResources().getColor(R.color.default_colorSelected));
				viewHolder.thumbnail.setColorFilter(activity.getResources().getColor(R.color.default_colorSelected));
				//viewHolder.thumbnail.setColorFilter(UtilClass.get_attr(activity, R.attr.colorControlHighlight)); // too opaque
			}
		}
	}

	@Override
	public int getItemCount()
	{
		return caseList == null ? 0 : caseList.size();
	}

	@Override
	public long getItemId(int position)
	{
		return caseList.get(position).key_id;
	}

	public Case getItem(int position)
	{
		return caseList.get(position);
	}

	public void toggleSelected(Case mCase)
	{
	//	int backgroundColor = 0xffb3e5fc;

		if(mCase.isSelected)
		{
			mCase.isSelected = false;
			removeFromMultiselectList(mCase.key_id);
		}
		else
		{
			mCase.isSelected = true;
			addToMultiselectList(mCase.key_id);

			//API21
			//highlight.setBackgroundColor(activity.getTheme().getResources().getColor(R.attr.colorAccent));
			//cardview-highlight.setBackgroundColor(activity.getResources().getColor(R.color.default_colorAccent));
		}

		if( multiselectList.size() <= 0 )
			mActionMode.finish();
		else
			mActionMode.setTitle(multiselectList.size() + " selected");

		notifyDataSetChanged();
	}

	public void clearSelected()
	{
		for(Case mCase : caseList)
		{
			//if(clearList.contains(mCase.key_id))
			{
				mCase.isSelected = false;
			}
		}

		multiselectList.clear();

		notifyDataSetChanged();
	}

	public void addToMultiselectList(long key_id)
	{
		if(!multiselectList.contains(key_id))
			multiselectList.add(key_id);
	}
	public void removeFromMultiselectList(long key_id)
	{
		if(multiselectList.contains(key_id))
			multiselectList.remove(key_id);
	}

	public void addAllToMultiselectList()
	{
		for(int i = 0; i < caseList.size(); i++)
		{
			Case mCase = caseList.get(i);

			// if not yet selected, then toggle
			if(mCase.isSelected == false)
			{
				mCase.isSelected = true;
				addToMultiselectList(mCase.key_id);
			}
		}

		mActionMode.setTitle(multiselectList.size() + " selected");
		notifyDataSetChanged();
	}
/*
	public void clearMultiselectList()
	{
		multiselectList.clear();
	}
*/
	public List<Long> getMultiselectList()
	{
		return multiselectList;
	}

	/*
	public int getMultiselectCount()
	{
		return multiselectList.size();
	}*/

	/**
	 * Stickyheaders
	 */

	/**
	 * setHeaderLiest
	 * @param text: headers for each item in list
	 * @param IDs
	 */
/*
	public void setHeaderList(List<String> text, List<Integer> IDs)
	{
		if(header != null)
			header.clear();

		header = text;

		notifyDataSetChanged();
	}
	*/

	public void setHeaderList(List<String> text)
	{
		if(header != null)
			header.clear();

		header = text;

//		notifyDataSetChanged();
	}

	@Override
	public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent)
	{
		View view = LayoutInflater.from(parent.getContext())
				            .inflate(R.layout.sticky_header, parent, false);
		return new RecyclerView.ViewHolder(view) { };
	}

	@Override
	public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int position)
	{
		if(header != null && position < header.size())
		{
			TextView textView = (TextView) holder.itemView;
			//textView.setText(String.valueOf(getItem(position)));
			textView.setText(header.get(position));
		}
		else
		{
			if(header == null)
				UtilClass.showToast(activity, "Debug: CardCaseAdapter.header is null");
		}
	}
	@Override
	public long getHeaderId(int position) {
		//return getItem(position).hashCode();
		if(header != null && position < header.size())
		{
			//return header_id.get(position);
			return header.get(position).hashCode();
		}
		else
		{
			return -1; // TODO detect invalid number?
		}
	}

	public void setOnClickListeners(View.OnClickListener cardOnClickListener, View.OnLongClickListener cardOnLongClickListener)
	{
		mOnClickListener = cardOnClickListener;
		mOnLongClickListener = cardOnLongClickListener;
	}

	/**
	 *
	 */
	public static class ViewHolder extends RecyclerView.ViewHolder
	{
		public long key_id;
		public CardView cardView;
		public View container;

		public TextView card_title;
		public TextView card_text1;
		public TextView card_text2;
		public TextView card_text3;
		public ImageView thumbnail;

		public ViewHolder(View itemView)
		{
			super(itemView);

			cardView = (CardView) itemView.findViewById(R.id.case_card_view);

			container = itemView.findViewById(R.id.container);

			card_title = (TextView) itemView.findViewById(R.id.patient_id);
			card_text1 = (TextView) itemView.findViewById(R.id.case_info1);
			card_text2 = (TextView) itemView.findViewById(R.id.case_info2);
			card_text3 = (TextView) itemView.findViewById(R.id.case_info3);
			thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
		}
	}
}

