package com.huantnguyen.radfiles.app;

import android.content.Context;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;

/**
 * Created by Huan on 9/28/2014.
 */
public class SpinnerActionBar extends ArrayAdapter<String>
{

	// CUSTOM SPINNER ADAPTER
	private Context mContext;
	private int mLayoutID;
	private SpannableString mTitle;
	private String[] listItems;

	public SpinnerActionBar(Context context, int textViewResourceId, SpannableString title,
	                        String[] objects) {
		super(context, textViewResourceId, objects);

		mContext = context;
		mLayoutID = textViewResourceId;
		mTitle = title;
		listItems = Arrays.copyOf(objects, objects.length);
	}

/*
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		//return getCustomView(position, convertView, parent);



		LayoutInflater inflater =
				( LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


		//layout for spinner popup

		if (convertView==null)
		{
			convertView = inflater.inflate(R.layout.spinner_popup, null);
		}

		return convertView;
	}
*/

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// TODO Auto-generated method stub
		return getCustomView(position, convertView, parent);
	}

	public View getCustomView(int position, View convertView,ViewGroup parent)
	{
		LayoutInflater inflater =
				( LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ViewHolder holder;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.spinner_toolbar, null);
			holder = new ViewHolder();
			holder.txt01 = (TextView) convertView.findViewById(R.id.title);
			holder.txt02 = (TextView) convertView.findViewById(R.id.subtitle);

			holder.txt01.setText(mTitle);

			convertView.setTag(holder);

		} else {

			holder = (ViewHolder) convertView.getTag();
		}

		holder.txt02.setText(listItems[position]);

		return convertView;
	}

	class ViewHolder {
		TextView txt01;
		TextView txt02;
	}



} // end custom adapter