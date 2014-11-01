package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 10/21/2014.
 */

public class CaseCardAdapter extends RecyclerView.Adapter<CaseCardAdapter.ViewHolder> implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder>, View.OnClickListener, View.OnLongClickListener
{

	private List<Case> caseList;
	private int card_layout_id;
	private NavigationDrawerActivity activity;

	// StickyRecyclerHeadersAdapter
	//private List<Integer> header_id;    // if different than previous (ie in a different group), then it will display it's header
	private List<String> header;

	public CaseCardAdapter(Activity activity, Cursor caseCursor, int card_layout)
	{

		caseList = new ArrayList<Case>();
		if(caseCursor != null)
		{
			loadCases(caseCursor);
		}


		this.card_layout_id = card_layout;
		this.activity = (NavigationDrawerActivity)activity;

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
					new_case.thumbnail_filename = CaseCardListActivity.picturesDir + "/" + image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);
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


		notifyDataSetChanged();
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(card_layout_id, viewGroup, false);

		ViewHolder holder = new ViewHolder(v);

		/*
		holder.thumbnail.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				UtilClass.showMessage(activity, "TEST CLICK THUMBNAIL");

				CaseCardListActivity caseCardListActivity = (CaseCardListActivity) activity;
				ActionMode mActionMode = caseCardListActivity.getActionMode();

				if(mActionMode == null)
				{
					caseCardListActivity.mActionMode = caseCardListActivity.startSupportActionMode(caseCardListActivity.mActionModeCallback);
				}

				if(view.isSelected())
				{
					view.setSelected(false);
					view.setBackgroundColor(0x00000000);
				}
				else
				{
					view.setSelected(true);
					view.setBackgroundColor(0xffb3e5fc);
				}

				//UtilClass.setPic((ImageView)view, )

				return;

			}
		});
				 */

		holder.cardView.setOnClickListener(CaseCardAdapter.this);
		holder.cardView.setOnLongClickListener(CaseCardAdapter.this);

		holder.cardView.setTag(holder);
		//holder.cardView.setTag(i);

		return holder;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int i)
	{
		if(caseList != null)
		{
			Case mCase = caseList.get(i);

			viewHolder.key_id = mCase.key_id;

			viewHolder.card_title.setText(mCase.patient_id);
			viewHolder.card_text1.setText(mCase.diagnosis);
			viewHolder.card_text2.setText(mCase.findings);
			//viewHolder.thumbnail.setImageDrawable(activity.getDrawable(country.getImageResourceId(mContext)));
		//	if(mCase.thumbnail_filename != null && !mCase.thumbnail_filename.isEmpty())
			{
				UtilClass.setPic(viewHolder.thumbnail, mCase.thumbnail_filename, UtilClass.IMAGE_THUMB_SIZE);
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

	@Override
	public void onClick(View view)
	{
		CardView cardView = (CardView)view;
		ViewHolder holder = (ViewHolder) view.getTag();

		//int position = (Integer)view.getTag();
		//Toast.makeText(activity, String.valueOf(caseList.get(position).key_id), Toast.LENGTH_SHORT).show();

//			Toast.makeText(activity, String.valueOf(holder.key_id), Toast.LENGTH_SHORT).show();

		if(activity.mActionMode == null)
		{
			// open detail view for clicked case
			Intent detailIntent = new Intent(view.getContext(), CaseDetailActivity.class);
			detailIntent.putExtra(CaseCardListActivity.ARG_KEY_ID, holder.key_id);

			// activity options
			//ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, Pair.create((View) holder.card_text1, "DetailCaseInfo1" ));

			detailIntent.putExtra(CaseDetailActivity.ARG_HAS_IMAGE, false);

			//activity.startActivityForResult(detailIntent, CaseCardListActivity.REQUEST_CASE_DETAILS, options.toBundle());
			activity.startActivityForResult(detailIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);
		}
		else
		{
			// contextual action bar is open

			if(cardView.isSelected())
			{
				cardView.setSelected(false);
				//view.setBackgroundColor(0x00000000);
				cardView.setCardElevation(4);
				cardView.setMaxCardElevation(4);
				holder.card_title.setTextColor(Color.CYAN);

				((CaseCardListActivity)activity).removeFromMultiselectList(holder.key_id);
			}
			else
			{
				cardView.setSelected(true);
				//view.setBackgroundColor(0xffb3e5fc);
				cardView.setMaxCardElevation(20);
				cardView.setCardElevation(20);
				holder.card_title.setTextColor(Color.RED);

				((CaseCardListActivity)activity).addToMultiselectList(holder.key_id);
			}

			activity.mActionMode.setTitle(((CaseCardListActivity)activity).getMultiselectCount() + " selected");
		}
	}


	@Override
	public boolean onLongClick(View view)
	{
		CardView cardView = (CardView)view;
		ViewHolder holder = (ViewHolder) view.getTag();

		int backgroundColor = 0xffb3e5fc;

		if(activity.mActionMode == null)
		{
			// open contextual menu
			activity.mActionMode = activity.startSupportActionMode(activity.mActionModeCallback);
			cardView.setSelected(true);
			cardView.setMaxCardElevation(20);
			cardView.setCardElevation(20);
			holder.card_title.setTextColor(Color.RED);

			((CaseCardListActivity)activity).addToMultiselectList(holder.key_id);
			activity.mActionMode.setTitle(((CaseCardListActivity)activity).getMultiselectCount() + " selected");


	//		view.setBackgroundColor(0xffb3e5fc);
			//RoundRectDrawable backgroundDrawable = new RoundRectDrawable(backgroundColor, cardView.getRadius());
			//cardView.setBackgroundDrawable(backgroundDrawable);



			return true;
		}
		else
		{
			// close contextual menu
			activity.mActionMode.finish();

			return true;
		}
	}

	/**
	 * Stickyheaders
	 */

	/**
	 * setHeaderLiest
	 * @param text: headers for each item in list
	 * @param IDs
	 */

	public void setHeaderList(List<String> text, List<Integer> IDs)
	{
		if(header != null)
			header.clear();

		header = text;
		//header_id = IDs;

		notifyDataSetChanged();
	}

	public void setHeaderList(List<String> text)
	{
		if(header != null)
			header.clear();

		header = text;

		notifyDataSetChanged();
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
		if(position < header.size())
		{
			TextView textView = (TextView) holder.itemView;
			//textView.setText(String.valueOf(getItem(position)));
			textView.setText(header.get(position));
		}
	}
	@Override
	public long getHeaderId(int position) {
		//return getItem(position).hashCode();
		if(position < header.size())
		{
			//return header_id.get(position);
			return header.get(position).hashCode();
		}
		else
		{
			return -1; // TODO detect invalid number?
		}
	}

	/**
	 *
	 */
	public static class ViewHolder extends RecyclerView.ViewHolder
	{
		private long key_id;
		public CardView cardView;

		public TextView card_title;
		public TextView card_text1;
		public TextView card_text2;
		public ImageView thumbnail;

		public ViewHolder(View itemView) {
			super(itemView);

			cardView = (CardView) itemView.findViewById(R.id.case_card_view);

			card_title = (TextView) itemView.findViewById(R.id.patient_id);
			card_text1 = (TextView) itemView.findViewById(R.id.case_info1);
			card_text2 = (TextView) itemView.findViewById(R.id.case_info2);
			thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
		}
	}
}

