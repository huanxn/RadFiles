package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Huan on 6/12/2014.
 */
public class CaseCardListActivity extends NavigationDrawerActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener
{
	// Spinner sort/filter
	private static int defaultFilterMode = 0;  //todo get from shared preferences
	private static int caseFilterMode = defaultFilterMode;
	private static final int FILTER_SECTION = 0;
	private static final int FILTER_RECENT = 1;
	private static final int FILTER_STUDYDATE = 2;
	private static final int NAV_KEYWORDS = 3;
	private static final int FILTER_FOLLOWUP = 4;
	private static final int FILTER_FAVORITE = 5;

	private static final String EMPTY_FIELD_GROUP_HEADER = "Unspecified";

	// intent request codes
	final static int REQUEST_CASE_DETAILS = 1;
	final static int REQUEST_ADD_CASE = 2;

	// intent result codes
	public final static int RESULT_NOCHANGE = 0;
	public final static int RESULT_EDITED = 1;
	public final static int RESULT_DELETED = 2;

	// intent arguments
	public final static String ARG_KEY_ID = "com.huantnguyen.radcases.ARG_KEY_ID";

	// saved state argument for action bar spinner
	private static final String CURRENT_SPINNER_STATE = "spinner_state";

	// search view
	private SearchView searchView;
	private MenuItem searchItem;

	private PlaceholderFragment fragment;

	// standard directories
	public static File downloadsDir;
	public static File picturesDir;
	public static File appDir;             // internal app data directory
	public static File dataDir;            // private data directory (with SQL database)

	public static File CSV_dir;            // contains created zip files with CSV files and images


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_case_cardlist);

		downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		appDir = getExternalFilesDir(null);
		dataDir = Environment.getDataDirectory();
		CSV_dir = new File(appDir, "/CSV/");

		picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

		setDrawerPosition(NavigationDrawerActivity.POS_CASE_LIST);

		if (savedInstanceState == null)
		{
			fragment = new PlaceholderFragment();
			getFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();

			// Set up the Action Bar dropdown spinner list
			// used for sorting the cases per user selected criteria
			//String [] listArray = getResources().getStringArray(R.array.actionbar_sort_list);
			SpinnerActionBar actionbarSpinnerAdapter = new SpinnerActionBar(getSupportActionBar().getThemedContext(), R.layout.spinner_actionbar, "Cases", getResources().getStringArray(R.array.actionbar_sort_list));
			((ArrayAdapter) actionbarSpinnerAdapter).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			ActionBar actionBar = getSupportActionBar();
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setListNavigationCallbacks(actionbarSpinnerAdapter, new android.support.v7.app.ActionBar.OnNavigationListener()
			{
				//String[] strings = getResources().getStringArray(R.array.action_list);

				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId)
				{
					// when item position changes, then repopulate cards using the new criteria
					caseFilterMode = itemPosition;
					fragment.populateCards();

					return false;
				}
			});
		}
		else
		{
			// set the saved filter/spinner state
			caseFilterMode = savedInstanceState.getInt(CURRENT_SPINNER_STATE);
		}

	}


	//
	// ACTION BAR MENU
	//

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.case_list, menu);


		// Get the SearchView and set the searchable configuration
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchItem = menu.findItem(R.id.menu_search);
		searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		searchView.setOnQueryTextListener(this);
		searchView.setOnCloseListener(this);

		// Assumes current activity is the searchable activity
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setIconifiedByDefault(true);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle presses on the action bar items
		switch (item.getItemId())
		{
			case R.id.menu_search:
				// TODO check this onSearchRequested
				//onSearchRequested();
				return true;

			case R.id.menu_addnew:
				Intent intent = new Intent(this, CaseAddActivity.class);
				startActivityForResult(intent, REQUEST_ADD_CASE);

				return true;

			case R.id.menu_settings:
				//openSettings();
				Toast.makeText(this, "debug: Settings function...", Toast.LENGTH_SHORT).show();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch(requestCode)
		{
			case REQUEST_CASE_DETAILS:

				// edited case details
				if(resultCode == RESULT_EDITED || resultCode == RESULT_DELETED)
				{
					// refresh cards
					fragment.populateCards();
				}
				break;

			case REQUEST_ADD_CASE:

				// same as return from editing (for now, change later?)
				// edited case details
				if(resultCode == RESULT_EDITED || resultCode == RESULT_DELETED)
				{
					// refresh cards
					//caseFilterMode = FILTER_RECENT;
					getSupportActionBar().setSelectedNavigationItem(FILTER_RECENT);
					fragment.populateCards();
				}
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Set up / Restore the spinner action bar after navigation drawer is closed
	 * override to use NAVIGATION_MODE_LIST instead of NAVIGATION_MODE_STANDARD
	 */
	@Override
	public void restoreActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
	}


	/**
	 * Save state of the action bar spinner
	 * @param outState
	 */

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putInt(CURRENT_SPINNER_STATE, getSupportActionBar().getSelectedNavigationIndex());
	}

	// SearchView.OnQueryTextListener
	@Override
	public boolean onQueryTextSubmit(String s)
	{
		UtilClass.hideKeyboard(this);

		//true if the query has been handled by the listener, false to let the SearchView perform the default action.
		return false;
	}

	@Override
	public boolean onQueryTextChange(String s)
	{
		fragment.doSearch(s);
		return true;
	}

	@Override
	// SearchView.OnCloseListener
	public boolean onClose()
	{
		fragment.populateCards();
		return false;
	}

	@Override
	/**
	 * override the back button
	 * prevent back button from closing app if searchView is open
	 */
	public void onBackPressed()
	{
		if (searchView != null && !searchView.isIconified())
		{
			searchView.setIconified(true);

			// if still open (only cleared text)
			if (!searchView.isIconified())
			{
				searchView.setIconified(true);
			}
		}
		else
		{
			super.onBackPressed();
		}
	}

	/**
	 * Placeholder fragment
	 */
	public static class PlaceholderFragment extends Fragment
	{
		// Layout
		private RecyclerView mRecyclerView;
		private CaseCardAdapter mCardAdapter;

		View rootView;
		Activity mActivity;

		public PlaceholderFragment()
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			// inflate the activity layout
			rootView = inflater.inflate(R.layout.activity_case_cardlist, container, false);

			// Find RecyclerView
			mRecyclerView = (RecyclerView)rootView.findViewById(R.id.cards_list);

			// Setup RecyclerView
			mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
			mRecyclerView.setItemAnimator(new DefaultItemAnimator());

			// Setup CaseCardAdapter
			mCardAdapter = new CaseCardAdapter(getActivity(), null, R.layout.card_case);

			mRecyclerView.setAdapter(mCardAdapter);

			//sticky headers
			// type2
			StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mCardAdapter);
			//mRecyclerView.addItemDecoration(new DividerDecoration(this));
			mRecyclerView.addItemDecoration(headersDecor);

			populateCards();

			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			mActivity = activity;
		}

		/**
		 * populateCards
		 *
		 */
		private void populateCards()
		{
			// Get cases, filtered/sorted by spinner list in action bar
			MergeCursor case_cursor;

			Cursor [] case_cursor_array;

			// set the headers for StickyRecyclerHeaders
			List<String> headerList = new ArrayList<String>();
			List<Integer> headerIdList = new ArrayList<Integer>();

			switch(caseFilterMode)
			{
				case FILTER_SECTION:

					// get cursor of "Radiology Section List", in order determined by user list preferences
					Cursor section_cursor = getActivity().getBaseContext().getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);

					// instantiate case cursor array with size of section_cursor.getCount() + 1.  Added one for those cases with empty "Radiology Section" field, ie Unknown
					// will use MergeCursor afterwards.  must use cursor array this since a case may be in more than one section
					case_cursor_array = new Cursor[section_cursor.getCount()+1];

					// query each case cursor by "Radiology Section" "LIKE %?%".  Each case may have more than one "Radiology Section" listed. Order by desc "Study Date"
					if(section_cursor.moveToFirst())
					{
						int i = 0;  // section/group counter

						do
						{
							// get the KEY_SECTION name
							String mSection = section_cursor.getString(CasesProvider.COL_VALUE);

							// find all cases with this KEY_SECTION
							case_cursor_array[i] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_SECTION + " LIKE ?", new String[]{"%" + mSection + "%"}, CasesProvider.KEY_DATE + " DESC", null);

							// set KEY_SECTION as headers in each list position in headerList, with same IDs
							for(int c = 0; c < case_cursor_array[i].getCount(); c++)
							{
								headerList.add(mSection);
								headerIdList.add(i);
							}

							i = i + 1;
						} while(section_cursor.moveToNext());

						// last filter group is the cases with empty "Radiology Section" fields
						case_cursor_array[i] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_SECTION + " IS NULL OR " + CasesProvider.KEY_SECTION + " = ?", new String[] {""}, CasesProvider.KEY_DATE + " DESC", null);
						for(int c = 0; c < case_cursor_array[i].getCount(); c++)
						{
							headerList.add(EMPTY_FIELD_GROUP_HEADER);
							headerIdList.add(i);
						}

					}

					section_cursor.close();

					break;

				case FILTER_RECENT:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, null, null, CasesProvider.KEY_ROWID + " DESC", null );

					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Recent");
						headerIdList.add(0);
					}

					break;

				case FILTER_STUDYDATE:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, null, null, CasesProvider.KEY_DATE + " DESC", null);

					if(case_cursor_array[0].moveToFirst())
					{
						String db_date_str;
						String prior_str = "";
						int group_counter = 0;  // section/group counter

						do
						{
							db_date_str = case_cursor_array[0].getString(CasesProvider.COL_DATE);

							if (db_date_str != null && !db_date_str.isEmpty())
							{
								// set header string: group by month
								String new_date_str = UtilClass.convertDateString(db_date_str, "yyyy-MM-dd", "MMMM yyyy");
								// add to headerList
								headerList.add(new_date_str);

								// if different date, then put in next filter group
								if (!new_date_str.contentEquals(prior_str))
								{
									group_counter += 1;
								}
								headerIdList.add(group_counter);

								// update prior_str for next iteration check
								prior_str = new_date_str;
							}
							else
							{
								headerList.add(EMPTY_FIELD_GROUP_HEADER);
							}
						} while (case_cursor_array[0].moveToNext());
					}

					break;
				case NAV_KEYWORDS:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_KEYWORDS + " is not null and " + CasesProvider.KEY_KEYWORDS + " != ?", new String[] {""}, CasesProvider.KEY_KEYWORDS, null);

					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Key Words");
						headerIdList.add(0);
					}

					break;
				case FILTER_FOLLOWUP:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FOLLOWUP + " = ?", new String[] {"1"}, CasesProvider.KEY_DATE + " DESC", null);

					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Followup");
						headerIdList.add(0);
					}

					break;
				case FILTER_FAVORITE:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FAVORITE + " = ?", new String[] {"1"}, CasesProvider.KEY_DATE + " DESC", null);

					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Favorites");
						headerIdList.add(0);
					}

					break;
				default:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, null, null, null, null);

					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("My Cases");
						headerIdList.add(0);
					}

					break;

			}

			case_cursor = new MergeCursor(case_cursor_array);
			// TODO how to close this cursor?


			mCardAdapter.loadCases(case_cursor);
			mCardAdapter.setHeaderList(headerList, headerIdList);

			case_cursor.close();

		}

		/**
		 * doSearch
		 * @param queryStr
		 */
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
			case_cursor_array[0] = getActivity().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_PATIENT_ID + " LIKE ? ", selArgs, CasesProvider.KEY_DATE + " DESC");
			case_cursor_array[1] = getActivity().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_DIAGNOSIS + " LIKE ? ", selArgs, CasesProvider.KEY_DIAGNOSIS);
			case_cursor_array[2] = getActivity().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FINDINGS + " LIKE ? ", selArgs, CasesProvider.KEY_FINDINGS);
			case_cursor_array[3] = getActivity().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_KEYWORDS + " LIKE ? ", selArgs, CasesProvider.KEY_KEYWORDS);
			case_cursor_array[4] = getActivity().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_STUDY_TYPE + " LIKE ? ", selArgs, CasesProvider.KEY_STUDY_TYPE);
			case_cursor_array[5] = getActivity().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_COMMENTS + " LIKE ? ", selArgs, CasesProvider.KEY_COMMENTS);

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

		}
	}
}
