package com.huantnguyen.radcases.app;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
//import android.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;

public class SearchActivity extends ActionBarActivity implements SearchView.OnQueryTextListener
{
	String query;
	private RecyclerView mRecyclerView;
	private CaseCardAdapter mCardAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		//setContentView(R.layout.activity_search);
		setContentView(R.layout.activity_case_cardlist);

		// Find RecyclerView
		mRecyclerView = (RecyclerView)findViewById(R.id.cards_list);

		// Setup RecyclerView
		mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		// Setup CaseCardAdapter
		mCardAdapter = new CaseCardAdapter(this, null, R.layout.card_case);

		mRecyclerView.setAdapter(mCardAdapter);

		// Setup Stickyheaders
		StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mCardAdapter);
		mRecyclerView.addItemDecoration(headersDecor);

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
		boolean noResults = true;

		if(queryStr.isEmpty())
		{
			mCardAdapter.loadCases(null);
			return;
		}

		String wildQuery = "%" + queryStr + "%";
		String[] selArgs = {wildQuery};

		String selection = "";


		MergeCursor case_cursor; // merge into one cursor for StickyCard List

		String [] search_categories = getResources().getStringArray(R.array.search_categories_array);
		Cursor [] case_cursor_array = new Cursor[search_categories.length];

		// set the headers for StickyRecyclerHeaders
		List<String> headerList = new ArrayList<String>();
		List<Integer> headerIdList = new ArrayList<Integer>();

		// do searches of each relevant text field
		case_cursor_array[0] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_PATIENT_ID + " LIKE ? ", selArgs, CasesProvider.KEY_DATE + " DESC");
		case_cursor_array[1] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_DIAGNOSIS + " LIKE ? ", selArgs, CasesProvider.KEY_DIAGNOSIS);
		case_cursor_array[2] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FINDINGS + " LIKE ? ", selArgs, CasesProvider.KEY_FINDINGS);
		case_cursor_array[3] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_KEYWORDS + " LIKE ? ", selArgs, CasesProvider.KEY_KEYWORDS);
		case_cursor_array[4] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_STUDY_TYPE + " LIKE ? ", selArgs, CasesProvider.KEY_STUDY_TYPE);
		case_cursor_array[5] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_COMMENTS + " LIKE ? ", selArgs, CasesProvider.KEY_COMMENTS);

		// merge cursors into one list
		case_cursor = new MergeCursor(case_cursor_array);

		// add search header for each case
		for(int i = 0; i < case_cursor_array.length; i++)
		{
			for(int c = 0; c < case_cursor_array[i].getCount(); c++)
			{
				headerList.add(search_categories[i]);
			}
		}

		//if(case_cursor.getCount() > 0)
		{
			// populate cards and set headers
			mCardAdapter.loadCases(case_cursor);
			mCardAdapter.setHeaderList(headerList);
		}

		case_cursor.close();



//		populateCards(case_cursor, group_header, case_count_in_group);

	}

	/// ACTION BAR MENU

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search, menu);

		// Get the SearchView and set the searchable configuration
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		MenuItem searchItem = menu.findItem(R.id.menu_search);
		android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(searchItem);
		searchView.setOnQueryTextListener(this);

		// Assumes current activity is the searchable activity
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

		// show the query string in the expanded search box
		searchView.setQuery(query, false);
		//searchView.setPressed(true);
		//searchView.setHovered(true);

		searchView.setIconifiedByDefault(false); //TODO fix this as it doesn't look the same
		//		searchView.setIconifiedByDefault(true);


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

				// int imageCount = case_cursor.getInt(CasesProvider.COL_IMAGE_COUNT);

				int thumbnail = 0;
				String thumbnailString = case_cursor.getString(CasesProvider.COL_THUMBNAIL);
				if(thumbnailString != null && !thumbnailString.isEmpty())
					thumbnail = Integer.parseInt(thumbnailString);

				// Create a Card
				CaseCard_Kitkat case_card = new CaseCard_Kitkat(this);

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

				// get images for this case
				String[] image_args = {String.valueOf(key_id)};
				Cursor image_cursor = getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);
				// first image is selected thumbnail
				if(image_cursor.getCount() > 0 && image_cursor.moveToFirst())
				{
					if(thumbnail < image_cursor.getCount())
					{
						image_cursor.move(thumbnail);
					}
					String imageFilename = CaseCardListActivity.picturesDir + "/" + image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);

					CaseCard_Kitkat.MyThumbnail cardThumbnail = new CaseCard_Kitkat.MyThumbnail(this, imageFilename);
					//You need to set true to use an external library
					cardThumbnail.setExternalUsage(true);

					case_card.addCardThumbnail(cardThumbnail);
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

		StickyCardArrayAdapter_Kitkat mCardArrayAdapter = new StickyCardArrayAdapter_Kitkat(this, cards);

		StickyCardListView_Kitkat listView = (StickyCardListView_Kitkat) this.findViewById(R.id.mySearchList);
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
