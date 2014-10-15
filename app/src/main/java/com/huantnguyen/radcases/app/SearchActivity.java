package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.SearchView;

import java.util.ArrayList;

import it.gmariotti.cardslib.library.internal.Card;

public class SearchActivity extends Activity implements SearchView.OnQueryTextListener
{
	String query;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//this.setOnQueryTextListener(this);

		setContentView(R.layout.activity_search);
		handleIntent(getIntent());
	}


	public void onNewIntent(Intent intent)
	{
		setIntent(intent);
		handleIntent(intent);
	}

	// Get the intent, verify the action and get the query
	private void handleIntent(Intent intent)
	{
		if (Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			query = intent.getStringExtra(SearchManager.QUERY);
			doSearch(query);

		}
	}

	private void doSearch(String queryStr)
	{
		String wildQuery = "%" + queryStr + "%";
		String[] selArgs = {wildQuery};

		String selection = "";

		boolean noResults = true;

/*
		// ALL_KEYS[0] is unique key_id
		for (int i = 1; i < CasesProvider.ALL_KEYS.length; i++)
		{
			selection = selection + CasesProvider.ALL_KEYS[i] + " LIKE ? ";

			if (i < CasesProvider.ALL_KEYS.length - 1)
			{
				selection = selection + " OR ";

				selArgs = UtilClass.addArrayElement(selArgs, wildQuery);
			}
		}
		*/

		MergeCursor case_cursor; // merge into one cursor for StickyCard List

		Cursor [] case_cursor_array; // cursor array to be merged
		String [] group_header; //name of header
		int [] case_count_in_group; //number of cases in each filter group

		case_cursor_array = new Cursor[6];
		group_header = new String[6];
		case_count_in_group = new int[6];

		// do searches of each relevant text field
		case_cursor_array[0] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_PATIENT_ID + " LIKE ? ", selArgs, CasesProvider.KEY_DATE + " DESC");
		case_cursor_array[1] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_DIAGNOSIS + " LIKE ? ", selArgs, CasesProvider.KEY_DIAGNOSIS);
		case_cursor_array[2] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FINDINGS + " LIKE ? ", selArgs, CasesProvider.KEY_FINDINGS);
		case_cursor_array[3] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_KEYWORDS + " LIKE ? ", selArgs, CasesProvider.KEY_KEYWORDS);
		case_cursor_array[4] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_STUDY_TYPE + " LIKE ? ", selArgs, CasesProvider.KEY_STUDY_TYPE);
		case_cursor_array[5] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_COMMENTS + " LIKE ? ", selArgs, CasesProvider.KEY_COMMENTS);

		// set group headers
		group_header[0] = "Patient ID";
		group_header[1] = "Diagnosis";
		group_header[2] = "Findings";
		group_header[3] = "Key Words";
		group_header[4] = "Study Type";
		group_header[5] = "Comments";

		// set group counts
		for(int i = 0; i < 6; i++)
			case_count_in_group[i] = case_cursor_array[i].getCount();

		case_cursor = new MergeCursor(case_cursor_array);
		// TODO how to close this cursor?

		populateCards(case_cursor, group_header, case_count_in_group);

	}

	/// ACTION BAR MENU

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.case_list, menu);

		// Get the SearchView and set the searchable configuration
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();

		// Assumes current activity is the searchable activity
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));


		// show the query string in the expanded search box
		searchView.setQuery(query, false);
		//searchView.setPressed(true);
		//searchView.setHovered(true);
		searchView.setIconifiedByDefault(false); //TODO fix this as it doesn't look the same


		return super.onCreateOptionsMenu(menu);
	}

	private void populateCards(MergeCursor case_cursor, String[] group_header, int[] case_count_in_group)
	{
		ArrayList<Card> cards = new ArrayList<Card>();

		// loop through case cursor and put info into cards
		if (case_cursor.moveToFirst())
		{
			int case_counter = 0;
			int group_counter = 0;

			do
			{
				int key_id = case_cursor.getInt(CasesProvider.COL_ROWID);
				String patient_id = case_cursor.getString(CasesProvider.COL_PATIENT_ID);
				String diagnosis = case_cursor.getString(CasesProvider.COL_DIAGNOSIS);
				String findings = case_cursor.getString(CasesProvider.COL_FINDINGS);
				String section = case_cursor.getString(CasesProvider.COL_SECTION);
				String study_type = case_cursor.getString(CasesProvider.COL_STUDY_TYPE);
				String db_date_str = case_cursor.getString(CasesProvider.COL_DATE);
				String key_words = case_cursor.getString(CasesProvider.COL_KEYWORDS);
				int favorite = case_cursor.getInt(CasesProvider.COL_FAVORITE);

				int imageCount = case_cursor.getInt(CasesProvider.COL_IMAGE_COUNT);

				// Create a Card
				CaseCard case_card = new CaseCard(this);

				// Set unique id, will be passed to detail activity
				case_card.setId(Integer.toString(key_id));

				// Set card info: Title, Text1, Text2
				case_card.setTitle(patient_id);
				if(diagnosis != null && !diagnosis.isEmpty())
				{
					case_card.setText1(diagnosis);
				}
				else if(findings != null && !findings.isEmpty())
				{
					case_card.setText1(findings);
				}

				case_card.setText2(key_words);

				// increment to the next non-empty group if the case counter exceeds the number of cases in the current group.  reset case counter of this new group
				while(case_counter >= case_count_in_group[group_counter])
				{
					group_counter += 1;
					case_counter = 0;
				}

				// set the group number
				case_card.setGroup(group_counter);
				case_counter += 1;

				// set the group header
				case_card.setGroupHeader(group_header[group_counter]);

				// set star
				if(favorite == 1)
					case_card.setStar(true);

				// set card thumbnail image
				if (imageCount > 0)
				{
					// get images for this case
					String[] image_args = {String.valueOf(key_id)};
					Cursor image_cursor = getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);
					// first image is selected thumbnail
					if(image_cursor.moveToFirst())
					{
						String imageFilename = image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);

						CaseCard.MyThumbnail thumbnail = new CaseCard.MyThumbnail(this, imageFilename);
						//You need to set true to use an external library
						thumbnail.setExternalUsage(true);

						case_card.addCardThumbnail(thumbnail);
					}
				}

				// set the OnClickListener: opens the case detail activity, passes key_id and has_image arguments
				case_card.setOnClickListener(new Card.OnCardClickListener()
				{
					@Override
					public void onClick(Card card, View view)
					{
						Intent detailIntent = new Intent(view.getContext(), CaseDetailActivity.class);
						detailIntent.putExtra(CaseCardListActivity.ARG_KEY_ID, Long.parseLong(card.getId()));

						if (card.getCardThumbnail() == null)
						{
							detailIntent.putExtra(CaseDetailActivity.ARG_HAS_IMAGE, false);
						}
						else
						{
							detailIntent.putExtra(CaseDetailActivity.ARG_HAS_IMAGE, true);
						}

						startActivityForResult(detailIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);
					}
				});

				cards.add(case_card);

			} while (case_cursor.moveToNext());
		}

		StickyCardArrayAdapter mCardArrayAdapter = new StickyCardArrayAdapter(this, cards);

		StickyCardListView listView = (StickyCardListView) this.findViewById(R.id.mySearchList);
		if (listView != null)
		{
			listView.setAdapter(mCardArrayAdapter);
		}

		case_cursor.close();
	}


	@Override
	public boolean onQueryTextSubmit(String s)
	{
		return false;
	}

	@Override
	public boolean onQueryTextChange(String s)
	{
		doSearch(s);
		return true;
	}
}
