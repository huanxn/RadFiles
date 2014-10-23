package com.huantnguyen.radcases.app;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eowise.recyclerview.stickyheaders.StickyHeadersAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 10/22/2014.
 */
public class ListHeaderAdapter implements StickyHeadersAdapter<ListHeaderAdapter.ViewHolder>
{
	private List<Long> header_id;
	private List<String> header;

	public ListHeaderAdapter()
	{
	//	header_id = new ArrayList<Long>();
	//	header = new ArrayList<String>();
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent)
	{
		View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_header, parent, false);

		return new ViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(ViewHolder headerViewHolder, int position)
	{
		headerViewHolder.header.setText(header.get(position));
	}

	@Override
	public long getHeaderId(int position)
	{
		//return header.get(position).charAt(0);
		return header_id.get(position);
	}

	public void setHeaderList(List<String> text, List<Long> IDs)
	{
		header = text;
		header_id = IDs;
	}

	/*
	public void addHeader(String headerText)
	{
		header.add(headerText);
		group_count.add(0);
	}


	public void setGroupCount(String headerText, int count)
	{
		if(header.contains(headerText))
		{
			int position = header.lastIndexOf(headerText);
			group_count.set(position, count);
		}
		else
		{
			header.add(headerText);
			group_count.add(count);
		}
	}
	*/


	public static class ViewHolder extends RecyclerView.ViewHolder
	{

		TextView header;

		public ViewHolder(View itemView) {
			super(itemView);
			header = (TextView) itemView.findViewById(R.id.header);
		}
	}
}
