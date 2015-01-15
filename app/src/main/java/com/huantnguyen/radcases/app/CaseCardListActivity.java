package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 6/12/2014.
 */
public class CaseCardListActivity extends NavigationDrawerActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener
{
	private static Activity activity;

	// Spinner sort/filter
	private static int defaultFilterMode = 0;  //todo get from shared preferences
	private static int caseFilterMode = defaultFilterMode;
	private static final int FILTER_SECTION = 0;
	private static final int FILTER_LAST_MODIFIED = 1;
	private static final int FILTER_STUDYDATE = 2;
	private static final int FILTER_MODALITY = 3;
	private static final int FILTER_KEYWORDS = 4;
	private static final int FILTER_FOLLOWUP = 5;
	private static final int FILTER_FAVORITE = 6;

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

	// ShowCase tutorial
	private boolean showTutorial = false;

	//private List<Long> multiselectList;

	public static CaseCardAdapter mCardAdapter;

	public static PlaceholderFragment fragment;

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

		activity = this;

		downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		appDir = getExternalFilesDir(null);
		dataDir = Environment.getDataDirectory();
		CSV_dir = new File(appDir, "/CSV/");

		setDrawerPosition(NavigationDrawerActivity.POS_CASE_LIST);

		if (savedInstanceState == null)
		{
			fragment = new PlaceholderFragment();
			getFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();

			SpannableString mTitle = new SpannableString("RAD Cases");
			mTitle.setSpan(new TypefaceSpan(this, "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			mTitle.setSpan(new TypefaceSpan(this, "RobotoCondensed-Bold.ttf"), "RAD".length(), mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			// Set up the Action Bar dropdown spinner list
			// used for sorting the cases per user selected criteria
			//String [] listArray = getResources().getStringArray(R.array.actionbar_sort_list);
			SpinnerActionBar actionbarSpinnerAdapter = new SpinnerActionBar(getSupportActionBar().getThemedContext(), R.layout.spinner_toolbar, mTitle, getResources().getStringArray(R.array.actionbar_sort_list));
			//((ArrayAdapter) actionbarSpinnerAdapter).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			((ArrayAdapter) actionbarSpinnerAdapter).setDropDownViewResource(R.layout.spinner_popup);
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
			/*
			SpannableString mTitle = new SpannableString("RAD debug" +
					                                             "");
			mTitle.setSpan(new TypefaceSpan(this, "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			mTitle.setSpan(new TypefaceSpan(this, "RobotoCondensed-Bold.ttf"), "RAD".length(), mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


			// set the saved filter/spinner state
			caseFilterMode = savedInstanceState.getInt(CURRENT_SPINNER_STATE);
			UtilClass.showMessage(this, "OnCreate savedInstanceState != null: filterMode " + caseFilterMode);
			//fragment.populateCards();
			*/

			// todo change to get stored data

			fragment = new PlaceholderFragment();
			getFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();

			SpannableString mTitle = new SpannableString("RAD Cases");
			mTitle.setSpan(new TypefaceSpan(this, "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			mTitle.setSpan(new TypefaceSpan(this, "RobotoCondensed-Bold.ttf"), "RAD".length(), mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			// Set up the Action Bar dropdown spinner list
			// used for sorting the cases per user selected criteria
			//String [] listArray = getResources().getStringArray(R.array.actionbar_sort_list);
			SpinnerActionBar actionbarSpinnerAdapter = new SpinnerActionBar(getSupportActionBar().getThemedContext(), R.layout.spinner_toolbar, mTitle, getResources().getStringArray(R.array.actionbar_sort_list));
			//((ArrayAdapter) actionbarSpinnerAdapter).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			((ArrayAdapter) actionbarSpinnerAdapter).setDropDownViewResource(R.layout.spinner_popup);
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

		if(showTutorial)
		{
			new ShowcaseView.Builder(this)
					//.setTarget( new ViewTarget( ((ViewGroup)findViewById(R.id.action_bar)).getChildAt(1) ) )
					.setTarget(new ViewTarget(findViewById(R.id.menu_addnew)))
					.setContentTitle("Case Filter")
					.setContentText("This is highlighting the sorting list")
					.hideOnTouchOutside()
					.build();
		}

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
				restoreActionBar();
				return true;

			case R.id.menu_addnew:
				Intent intent = new Intent(this, CaseAddActivity.class);
				startActivityForResult(intent, REQUEST_ADD_CASE);

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
					//caseFilterMode = FILTER_LAST_MODIFIED;
					getSupportActionBar().setSelectedNavigationItem(FILTER_LAST_MODIFIED);
					fragment.populateCards();
				}
				break;

			default:
				UtilClass.showMessage(this, "debug: CaseCardListActivity onActivityResult");
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
	/**
	 * SearchView
	 * searches on query string with each change in character, only when search bar is open
	 *  - also triggers on NavigationDrawer open and close.
	 */
	public boolean onQueryTextChange(String s)
	{
		if(!searchView.isIconified())
			fragment.doSearch(s);

		return true;
	}

	@Override
	// SearchView.OnCloseListener
	public boolean onClose()
	{
		// repopulate full list
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

		private static View.OnClickListener cardOnClickListener;
		private static View.OnLongClickListener cardOnLongClickListener;

		/**
		 * Contextual action mode
		 */
		public ActionMode.Callback mActionModeCallback = null;

		View rootView;
		CaseCardListActivity mActivity;

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
			mCardAdapter = ((CaseCardListActivity)mActivity).mCardAdapter = new CaseCardAdapter(getActivity(), null, R.layout.card_case);

			// contextual menu
			setActionModeCallback();
			mCardAdapter.setActionModeCallback(mActionModeCallback);

			// click listeners
			setCardClickListeners();
			mCardAdapter.setOnClickListeners(cardOnClickListener, cardOnLongClickListener);
/*
			mCardAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
			                                         {
				                                         @Override
				                                         public void onChanged()
				                                         {
					                                         populateCards();
				                                         }
			                                         });
*/

			mRecyclerView.setAdapter(mCardAdapter);

			//sticky headers
			// type2
			StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mCardAdapter);
			//mRecyclerView.addItemDecoration(new DividerDecoration(this));
			mRecyclerView.addItemDecoration(headersDecor);

			mRecyclerView.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					UtilClass.hideKeyboard(getActivity());
					return false;
				}
			});

			populateCards();

			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			mActivity = (CaseCardListActivity)activity;
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

			//todo: delete-- don't use this anymore
			//List<Integer> headerIdList = new ArrayList<Integer>();

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
								//headerIdList.add(i);
							}

							i = i + 1;
						} while(section_cursor.moveToNext());

						// last filter group is the cases with empty "Radiology Section" fields
						case_cursor_array[i] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_SECTION + " IS NULL OR " + CasesProvider.KEY_SECTION + " = ?", new String[] {""}, CasesProvider.KEY_DATE + " DESC", null);
						for(int c = 0; c < case_cursor_array[i].getCount(); c++)
						{
							headerList.add(EMPTY_FIELD_GROUP_HEADER);
							//headerIdList.add(i);
						}

					}

					section_cursor.close();

					break;

				case FILTER_LAST_MODIFIED:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, null, null, CasesProvider.KEY_LAST_MODIFIED_DATE + " DESC, " + CasesProvider.KEY_ROWID + " DESC", null );

					/*
					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Recent");
						//headerIdList.add(0);
					}
					*/

					if(case_cursor_array[0].moveToFirst())
					{
						String db_modified_date_str;
						String prior_str = "";
						int group_counter = 0;  // section/group counter

						do
						{
							db_modified_date_str = case_cursor_array[0].getString(CasesProvider.COL_LAST_MODIFIED_DATE);

							if (db_modified_date_str != null && !db_modified_date_str.isEmpty())
							{
								// set header string: group by month
								String new_date_str = UtilClass.convertDateString(db_modified_date_str, "yyyy-MM-dd", "MMMM yyyy");
								// add to headerList
								headerList.add(new_date_str);

								// if different date, then put in next filter group
								if (!new_date_str.contentEquals(prior_str))
								{
									group_counter += 1;
								}
								//headerIdList.add(group_counter);

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
								//headerIdList.add(group_counter);

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

				case FILTER_MODALITY:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_STUDY_TYPE + " is not null and " + CasesProvider.KEY_STUDY_TYPE + " != ?", new String[] {""}, CasesProvider.KEY_STUDY_TYPE, null);

					if(case_cursor_array[0].moveToFirst())
					{
						String stickyHeader;
						String prior_stickyHeader = "";
						int group_counter = 0;  // section/group counter

						do
						{
							stickyHeader = case_cursor_array[0].getString(CasesProvider.COL_STUDY_TYPE);

							if (stickyHeader != null && !stickyHeader.isEmpty())
							{
								// set header string
								// add to headerList
								headerList.add(stickyHeader);

								// if different date, then put in next filter group
								if (!stickyHeader.contentEquals(prior_stickyHeader))
								{
									group_counter += 1;
								}
								//headerIdList.add(group_counter);

								// update prior_stickyHeader for next iteration check
								prior_stickyHeader = stickyHeader;
							}
							else
							{
								headerList.add(EMPTY_FIELD_GROUP_HEADER);
							}
						} while (case_cursor_array[0].moveToNext());
					}

					break;

				case FILTER_KEYWORDS:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_KEYWORDS + " is not null and " + CasesProvider.KEY_KEYWORDS + " != ?", new String[] {""}, CasesProvider.KEY_KEYWORDS, null);

					// get cursor of "Radiology Keywords List", in order determined by user list preferences
					Cursor keywords_cursor = getActivity().getBaseContext().getContentResolver().query(CasesProvider.KEYWORD_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);

					// instantiate case cursor array with size of section_cursor.getCount().
					// will use MergeCursor afterwards.  must use cursor array this since a case may be in more than one section
					case_cursor_array = new Cursor[keywords_cursor.getCount()];

					// query each case cursor by "keyword" "LIKE %?%".  Each case may have more than one "keyword" listed. Order by recent
					if(keywords_cursor.moveToFirst())
					{
						int i = 0;  // section/group counter

						do
						{
							// get the KEYWORD name
							String mKeyword = keywords_cursor.getString(CasesProvider.COL_VALUE);

							// find all cases with this KEY_KEYWORDS
							case_cursor_array[i] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_KEYWORDS + " LIKE ?", new String[]{"%" + mKeyword + "%"}, CasesProvider.KEY_ROWID + " DESC", null);

							// set KEY_KEYWORDS as headers in each list position in headerList, with same IDs
							for(int c = 0; c < case_cursor_array[i].getCount(); c++)
							{
								headerList.add(mKeyword);
								//headerIdList.add(i);
							}

							i = i + 1;
						} while(keywords_cursor.moveToNext());

					}
					keywords_cursor.close();

					break;
				case FILTER_FOLLOWUP:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FOLLOWUP + " = ?", new String[] {"1"}, CasesProvider.KEY_DATE + " DESC", null);

					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Followup");
						//headerIdList.add(0);
					}

					break;
				case FILTER_FAVORITE:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FAVORITE + " = ?", new String[] {"1"}, CasesProvider.KEY_DATE + " DESC", null);

					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Favorites");
						//headerIdList.add(0);
					}

					break;
				default:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, null, null, null, null);

					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("My Cases");
						//headerIdList.add(0);
					}

					break;

			}

			case_cursor = new MergeCursor(case_cursor_array);
			// TODO how to close this cursor?


			mCardAdapter.loadCases(case_cursor);
			mCardAdapter.setHeaderList(headerList);

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

		/**
		 * Contextual Menu
		 */
		private void setActionModeCallback()
		{
			mActionModeCallback = new ActionMode.Callback()
			{
				// Called when the action mode is created; startActionMode() was called
				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu)
				{
					// Inflate a menu resource providing context menu items
					MenuInflater inflater = mode.getMenuInflater();
					inflater.inflate(R.menu.case_list_contextual, menu);
					return true;
				}

				// Called each time the action mode is shown. Always called after onCreateActionMode, but
				// may be called multiple times if the mode is invalidated.
				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu)
				{
					return false; // Return false if nothing is done
				}

				// Called when the user selects a contextual menu item
				@Override
				public boolean onActionItemClicked(ActionMode mode, MenuItem item)
				{
					switch (item.getItemId())
					{
						case R.id.menu_share:
							File shareFile = null;

							if(!mCardAdapter.getMultiselectList().isEmpty())
							{
								shareFile = UtilClass.exportCasesJSON(activity, "TEST_share_file", mCardAdapter.getMultiselectList());
								Uri uriShareFile = Uri.fromFile(shareFile);

								Intent shareIntent = new Intent(Intent.ACTION_SEND);
								shareIntent.setType("message/rfc822");
								shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
								shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Radiology cases");
								shareIntent.putExtra(Intent.EXTRA_TEXT, "Please see the attached file.\nOpen it with the RadCases Android app!");
								shareIntent.putExtra(Intent.EXTRA_STREAM, uriShareFile);


								//shareIntent.setData(Uri.parse("mailto:")); // or just "mailto:" for blank
								//shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
								try
								{
									if(mCardAdapter.getMultiselectList().size() == 1)
										startActivity(Intent.createChooser(shareIntent, "Share " + mCardAdapter.getMultiselectList().size() + " case..."));
									else
										startActivity(Intent.createChooser(shareIntent, "Share " + mCardAdapter.getMultiselectList().size() + " cases..."));
								}
								catch (android.content.ActivityNotFoundException ex)
								{
									Toast.makeText(activity, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
								}
							}

							// todo don't finish if canceled
							mode.finish(); // Action picked, so close the CAB
							return true;

						case R.id.menu_delete:

							// delete from database
							//UtilClass.menuItem_deleteCases(getActivity(), mCardAdapter.getMultiselectList());

							final Context context = getActivity();
							final List<Long> deleteIdList = new ArrayList<Long>(mCardAdapter.getMultiselectList());
							final ActionMode actionMode = mode;
							final PlaceholderFragment frag = fragment;

							// Alert dialog to confirm delete
							AlertDialog.Builder builder = new AlertDialog.Builder(context);

							builder.setMessage("Delete " + deleteIdList.size() + " cases?")
									.setPositiveButton(context.getResources().getString(R.string.button_OK), new DialogInterface.OnClickListener()
									{
										public void onClick(DialogInterface dialog, int id)
										{
											// delete confirmed

											for (int i = 0; i < deleteIdList.size(); i++)
											{
												// get key_id from list array
												long key_id = deleteIdList.get(i);

												// delete case from CASES table
												Uri case_delete_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
												context.getContentResolver().delete(case_delete_uri, null, null);

												// delete all linked images files
												Cursor image_cursor = context.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)}, CasesProvider.KEY_ORDER);
												File imageFile = null;
												if (image_cursor.moveToFirst())
												{
													do
													{
														imageFile = new File(image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME));
														imageFile.delete();
													} while (image_cursor.moveToNext());
												}
												image_cursor.close();

												// delete all child rows from IMAGES table, by parent case key_id
												context.getContentResolver().delete(CasesProvider.IMAGES_URI, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)});

												// remove from recyclerView
												//mCardAdapter.removeCase(key_id);
												frag.populateCards();

												mCardAdapter.notifyDataSetChanged();
												actionMode.finish(); // Action picked, so close the CAB
											}

											// update CaseCardListActivity
											//context.setResult(CaseCardListActivity.RESULT_DELETED);
										}
									})
									.setNegativeButton(context.getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener()
									{
										public void onClick(DialogInterface dialog, int id)
										{
											// cancel
										}
									});

							AlertDialog alert = builder.create();
							alert.show();

							return true;

						default:
							return false;
					}
				}

				// Called when the user exits the action mode
				@Override
				public void onDestroyActionMode(ActionMode mode)
				{
					mCardAdapter.setActionMode(null);

					// clear list of key_id
					//mCardAdapter.getMultiselectList().clear();

					// clear list of selected cards to highlight
					if(mCardAdapter != null)
					{
						mCardAdapter.clearSelected();
					}
				}
			};
		}

		/**
		 * Click listeners
		 */
		private void setCardClickListeners()
		{
			final CaseCardAdapter mAdapter = mCardAdapter;
			cardOnClickListener = new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					CaseCardAdapter.ViewHolder holder = (CaseCardAdapter.ViewHolder) view.getTag();

					Case mCase = mAdapter.caseList.get(holder.getPosition());

					if (mAdapter.mActionMode == null)
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
						mAdapter.toggleSelected(mCase);

					}
				}
			};

			cardOnLongClickListener = new View.OnLongClickListener()
			{
				@Override
				public boolean onLongClick(View view)
				{
					CaseCardAdapter.ViewHolder holder = (CaseCardAdapter.ViewHolder) view.getTag();
					Case mCase = mAdapter.caseList.get(holder.getPosition());

					if(mAdapter.mActionMode == null)
					{
						// open contextual menu
						mAdapter.mActionMode = ((ActionBarActivity)activity).startSupportActionMode(mActionModeCallback);
					}
					else
					{
						// close contextual menu
						//activity.mActionMode.finish();
					}

					mAdapter.toggleSelected(mCase);

					return true;
				}
			};
		}

	} // end fragment

}
