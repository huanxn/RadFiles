/*
 * ******************************************************************************
 *   Copyright (c) 2013-2014 Gabriele Mariotti.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package com.huantnguyen.radcases.app;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayMultiChoiceAdapter;
import it.gmariotti.cardslib.library.view.CardListView;
import it.gmariotti.cardslib.library.view.CardView;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class StickyCardArrayAdapter extends CardArrayMultiChoiceAdapter implements StickyListHeadersAdapter
{

	/**
	 * {@link CardListView}
	 */
	protected StickyCardListView mCardListView;
	private LayoutInflater inflater;
	private HeaderViewHolder[] holder_list;
	List<Card> cards;

	/**
	 * CardArrayMultiChoiceAdapter
	 */
	private ActionMode mActionMode;

	/**
	 * Constructor
	 *
	 * @param context The current context.
	 * @param cards   The cards to represent in the ListView.
	 */
	public StickyCardArrayAdapter(Context context, List<Card> cards)
	{

		super(context, cards);
		inflater = LayoutInflater.from(context);
		this.cards = cards;
//		holder_list = new HeaderViewHolder[cards.size()];
		holder_list = null;
	}


	@Override
	public View getHeaderView(int position, View convertView, ViewGroup parent)
	{
		HeaderViewHolder holder;

		if(holder_list == null)
			holder_list = new HeaderViewHolder[cards.size()];

		if (convertView == null)
		{
			holder = new HeaderViewHolder();
			convertView = inflater.inflate(R.layout.stickylist_header, parent, false);
			holder.text = (TextView) convertView.findViewById(R.id.stickylist_header);
			convertView.setTag(holder);
		}
		else
		{
			holder = (HeaderViewHolder) convertView.getTag();
		}

		holder_list[position] = holder;

		//set header text
		CaseCard_Kitkat card = (CaseCard_Kitkat)cards.get(position);

		String headerText = card.getGroupHeader();

		//String headerText = "test";// + countries[position].subSequence(0, 1).charAt(0);
		holder.text.setText(headerText);
		return convertView;


		// Build your custom HeaderView
		//In this case I will use a Card, but you can use any view

	    /*
	    LayoutInflater mInflater = LayoutInflater.from(getContext());
        View view = mInflater.inflate(R.layout.card_sticky_header, null);

        CardView cardView= (CardView)view.findViewById(R.id.myList);
        Card card = getItem(position);
        char headerChar = card.getTitle().subSequence(0, 1).charAt(0);

        ColorCard colorCard = new ColorCard(getContext());
        colorCard.setTitle("Header : " + headerChar);
        switch (position / 8) {
            case 0:
                colorCard.setBackgroundResourceId(R.drawable.demoextra_card_selector_color1);
                break;
            case 1:
                colorCard.setBackgroundResourceId(R.drawable.demoextra_card_selector_color2);
                break;
            case 2:
                colorCard.setBackgroundResourceId(R.drawable.demoextra_card_selector_color3);
                break;
            case 3:
                colorCard.setBackgroundResourceId(R.drawable.demoextra_card_selector_color4);
                break;
            case 4:
                colorCard.setBackgroundResourceId(R.drawable.demoextra_card_selector_color5);
                break;
            default:
                colorCard.setBackgroundResourceId(R.drawable.demoextra_card_selector_color1);
                break;
        }

        cardView.setCard(colorCard);
        return view;
        */
	}

	@Override
	public long getHeaderId(int position)
	{

		CaseCard_Kitkat card = (CaseCard_Kitkat)cards.get(position);
		return card.getGroup();
	}


	/**
	 * Sets the {@link CardListView}
	 *
	 * @param cardListView cardListView
	 */
	public void setCardListView(StickyCardListView cardListView)
	{
		this.mCardListView = cardListView;
	}

	//// MultiChoice
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		//It is very important to call the super method
		super.onCreateActionMode(mode, menu);

		mActionMode=mode; // to manage mode in your Fragment/Activity

		//If you would like to inflate your menu
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.case_list_multichoice, menu);

		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (item.getItemId() == R.id.menu_share) {
			Toast.makeText(getContext(), "Share;" + formatCheckedCard(), Toast.LENGTH_SHORT).show();
			return true;
		}

		if (item.getItemId() == R.id.menu_delete) {
			discardSelectedItems(mode);
			return true;
		}

		return false;
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked, CardView cardView, Card card) {
		Toast.makeText(getContext(), "Click;" + position + " - " + checked, Toast.LENGTH_SHORT).show();
	}

	private void discardSelectedItems(ActionMode mode) {
		ArrayList<Card> items = getSelectedCards();
		for (Card item : items) {
			remove(item);
		}
		mode.finish();
	}


	private String formatCheckedCard() {

		SparseBooleanArray checked = mCardListView.getCheckedItemPositions();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < checked.size(); i++) {
			if (checked.valueAt(i) == true) {
				sb.append("\nPosition=" + checked.keyAt(i));
			}
		}
		return sb.toString();
	}

	//// MultiChoice




	class HeaderViewHolder
	{
		TextView text;
	}

	class ViewHolder
	{
		TextView text;
	}
}
