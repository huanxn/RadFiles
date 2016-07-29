package com.radicalpeas.radfiles.app;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Huan on 2/24/2015.
 */
public class NavigationDrawerExpandableListAdapter extends BaseExpandableListAdapter
{
	private Context mContext;
	private List<String> mListDataHeader; // header titles

	// child data in format of header title, child title
	private HashMap<String, List<String>> mListDataChild;

	// Constructor
	public NavigationDrawerExpandableListAdapter(Context context, List<String> listDataHeader,
	                             HashMap<String, List<String>> listChildData) {
		this.mContext = context;
		this.mListDataHeader = listDataHeader;
		this.mListDataChild = listChildData;


	}

	@Override
	public Object getChild(int groupPosition, int childPosition)
	{
		return this.mListDataChild.get(this.mListDataHeader.get(groupPosition)).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition)
	{
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
	{
		final String childText = (String) getChild(groupPosition, childPosition);

		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) this.mContext
					                                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.navigation_drawer_list_item, null);
		}

		TextView txtListChild = (TextView) convertView.findViewById(R.id.lblListItem);

		txtListChild.setText(childText);
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition)
	{
		if(mListDataChild == null || mListDataChild.get(this.mListDataHeader.get(groupPosition)) == null)
		{
			return 0;
		}
		else
		{
			return this.mListDataChild.get(this.mListDataHeader.get(groupPosition)).size();
		}
	}

	@Override
	public Object getGroup(int groupPosition)
	{
		return this.mListDataHeader.get(groupPosition);
	}

	@Override
	public int getGroupCount()
	{
		return this.mListDataHeader.size();
	}

	@Override
	public long getGroupId(int groupPosition)
	{
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
	                         View convertView, ViewGroup parent)
	{
		String headerTitle = (String) getGroup(groupPosition);
		if (convertView == null)
		{
			LayoutInflater infalInflater = (LayoutInflater) this.mContext
					                                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.navigation_drawer_list_group, null);
		}

		TextView lblListHeader = (TextView) convertView
				                                    .findViewById(R.id.lblListHeader);
		lblListHeader.setTypeface(null, Typeface.BOLD);
		lblListHeader.setText(headerTitle);

		return convertView;
	}

	@Override
	public boolean hasStableIds()
	{
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition)
	{
		return true;
	}
}
