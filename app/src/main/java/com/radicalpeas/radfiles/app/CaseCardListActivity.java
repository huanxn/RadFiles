package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableString;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

//import com.gc.materialdesign.widgets.ProgressDialog;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 6/12/2014.
 */
public class CaseCardListActivity extends NavDrawerActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener
{
	private static String TAG = "CaseCardList Activity";
	private static Activity activity;

	// Spinner sort/filter
	private static int defaultFilterMode = 0;  //todo get from shared preferences
	private static int caseFilterMode = defaultFilterMode;
	private static final int FILTER_SECTION = 0;
	private static final int FILTER_SECTION_SELECTED = 300;
	private static final int FILTER_LAST_MODIFIED = 1;
	private static final int FILTER_STUDYDATE = 2;
	private static final int FILTER_MODALITY = 3;
	private static final int FILTER_KEYWORDS = 4;
//	private static final int FILTER_FOLLOWUP = 5;
//	private static final int FILTER_FAVORITE = 6;

	private static final List<Long> filterGroupCollapsedList = new ArrayList<Long>();

	private static final String EMPTY_FIELD_GROUP_HEADER = "Unspecified";

	// intent request codes
	final static int REQUEST_CASE_DETAILS = 1;
	final static int REQUEST_ADD_CASE = 2;

	// intent result codes
	public final static int RESULT_NOCHANGE = 0;
	public final static int RESULT_EDITED = 1;
	public final static int RESULT_DELETED = 2;

	// intent arguments
	public final static String ARG_KEY_ID = "com.radicalpeas.radfiles.ARG_KEY_ID";
	public final static String ARG_CASE_SUBSET = "com.radicalpeas.radfiles.ARG_CASE_SUBSET";


	// saved state argument for action bar spinner
	private static final String CURRENT_SPINNER_STATE = "spinner_state";

	// case subset selected from nav drawer
	protected long case_subset = NavDrawerActivity.POS_CASE_LIST_ALL;

	// search view
	private SearchView searchView;
	private MenuItem searchItem;
	private SearchTask searchTask;

	// ShowCase tutorial
	// TourGuide tutorial
	private boolean showTutorial = true;
	private Target caseFilterTarget;


	//private List<Long> multiselectList;

	public static CaseCardAdapter mCardAdapter;

	public static CaseCardListFragment fragment;

	// standard directories
	public static File downloadsDir;
	public static File picturesDir;
	public static File appDir;             // internal app data directory
	public static File dataDir;            // private data directory (with SQL database)

	private static ProgressDialog progressDialog;

	// firebase
	//DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		setDrawerPosition(NavDrawerActivity.POS_CASE_LIST_ALL);
		super.onCreate(savedInstanceState);


	//	super.onCreate(savedInstanceState, R.layout.toolbar_spinner);

//		setContentView(R.layout.activity_case_cardlist);	// done in fragment

		activity = this;

		// get the intent and the method argument: if called from navigation drawer
		Intent intent = getIntent();
		if(intent.hasExtra(ARG_CASE_SUBSET))
		{
			case_subset = intent.getLongExtra(ARG_CASE_SUBSET, NavDrawerActivity.POS_CASE_LIST_ALL);
			setDrawerPosition((int)case_subset);

			// change title of spinner to reflect subset
			if(case_subset == POS_CASE_LIST_FAV)
			{
				mTitle = new SpannableString(getResources().getString(R.string.navigation_drawer_item_cases_fav));
				mTitle.setSpan(new TypefaceSpan(this, "Roboto-Bold.ttf"), 0, mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			else if(case_subset == POS_CASE_LIST_FOLLOWUP)
			{
				mTitle = new SpannableString(getResources().getString(R.string.navigation_drawer_item_cases_followup));
				mTitle.setSpan(new TypefaceSpan(this, "Roboto-Bold.ttf"), 0, mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			else if(case_subset >= POS_CASE_LIST_SUBSECTION)
			{
				// get cursor of "Radiology Section List", in order determined by user list preferences
				Cursor subsection_cursor = this.getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);
				subsection_cursor.moveToPosition((int) case_subset - POS_CASE_LIST_SUBSECTION);

				mTitle = new SpannableString(subsection_cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE));
				mTitle.setSpan(new TypefaceSpan(this, "Roboto-Bold.ttf"), 0, mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

				subsection_cursor.close();
			}
		}
		else
		{
			setDrawerPosition(NavDrawerActivity.POS_CASE_LIST_ALL);
		}

		downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		appDir = getExternalFilesDir(null);
		dataDir = Environment.getDataDirectory();

		// lollipop transitions
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			//getWindow().setSharedElementExitTransition(TransitionInflater.from(this).inflateTransition(R.transition.shared_element));

			TransitionSet transitionSet = new TransitionSet();

			//transitionSet.setOrdering(transitionSet.ORDERING_SEQUENTIAL);

			//`Transition slideDown = new Slide(Gravity.TOP);
			//slideDown.addTarget(mToolbar);
			//transitionSet.addTransition(slideDown);

			Transition fade = new Fade();
			fade.excludeTarget(mToolbar, true);
			fade.excludeTarget(android.R.id.statusBarBackground, true);
			fade.excludeTarget(android.R.id.navigationBarBackground, true);
			transitionSet.addTransition(fade);

			transitionSet.addTransition(TransitionInflater.from(this).inflateTransition(R.transition.shared_element));

			activity.getWindow().setEnterTransition(transitionSet);
			activity.getWindow().setExitTransition(transitionSet);

		}

		if (savedInstanceState == null)
		{
			fragment = new CaseCardListFragment();
			getFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();
		}
		else
		{
			// set the saved filter/spinner state
			/*
				UtilClass.showSnackbar(this, "DEBUG: state: " + savedInstanceState.toString());

			caseFilterMode = savedInstanceState.getInt(CURRENT_SPINNER_STATE);
			UtilClass.showSnackbar(this, "OnCreate savedInstanceState != null: filterMode " + caseFilterMode);
			//fragment.populateCards();
			*/

			// todo change to get stored data

			fragment = new CaseCardListFragment();
			getFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();
		}


		getSupportActionBar().setDisplayShowTitleEnabled(false);
		Spinner caseFilterSpinner = (Spinner) mToolbar.findViewById(R.id.case_filter_spinner);
		SpinnerActionBar actionbarSpinnerAdapter = new SpinnerActionBar(getSupportActionBar().getThemedContext(), R.layout.spinner_toolbar, mTitle, getResources().getStringArray(R.array.actionbar_sort_list));
		actionbarSpinnerAdapter.setDropDownViewResource(R.layout.spinner_popup);
		caseFilterSpinner.setAdapter(actionbarSpinnerAdapter);
		caseFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				// when item position changes, then repopulate cards using the new criteria
				caseFilterMode = position;

				if(fragment != null)
					fragment.new PopulateCardsTask().execute();

				return;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{

			}
		});

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

		// change text color

		SearchView.SearchAutoComplete searchText = (SearchView.SearchAutoComplete)searchView.findViewById(R.id.search_src_text);
		searchText.setTextColor(getResources().getColor(R.color.text_light));
		searchText.setHint(getResources().getString(R.string.search_prompt));
		searchText.setHintTextColor(getResources().getColor(R.color.text_light_hint));

		/*
		// hide spinner if drawer is open
		if (mNavigationDrawerFragment.isDrawerOpen())
		{
			mToolbar.findViewById(R.id.case_filter_spinner).setVisibility(View.GONE);
			return true;
		}
		*/


		return super.onCreateOptionsMenu(menu);
	}

	@Override
	// called when overflow button opens overflow menu
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		// show menu icons in overflow
		if (menu != null)
		{
			if (menu.getClass().getSimpleName().equals("MenuBuilder"))
			{
				try
				{
					Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
					m.setAccessible(true);
					m.invoke(menu, true);
				}
				catch (NoSuchMethodException e)
				{
				//	Log.e(TAG, "onMenuOpened", e);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		return super.onPrepareOptionsMenu(menu);
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
	//			restoreActionBar();
				return true;

			case R.id.menu_addnew:
				Intent intent = new Intent(this, CaseAddActivity.class);
				startActivityForResult(intent, REQUEST_ADD_CASE);

				return true;

			case R.id.menu_help:

				runTutorial(1);

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
					/*
					fragment.populateCards();
					mCardAdapter.notifyDataSetChanged();
					*/
					fragment.new PopulateCardsTask().execute();
				}
				break;

			case REQUEST_ADD_CASE:

				// same as return from editing (for now, change later?)
				// edited case details
				if(resultCode == RESULT_EDITED || resultCode == RESULT_DELETED)
				{
					// resort by last modified to show newly added case at the top
					caseFilterMode = FILTER_LAST_MODIFIED;

					fragment.new PopulateCardsTask().execute();
				}
				break;

			default:
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Set up / Restore the spinner action bar after navigation drawer is closed
	 * override to use NAVIGATION_MODE_LIST instead of NAVIGATION_MODE_STANDARD
	 */
/*	@Override
	public void restoreActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
//		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		mToolbar.findViewById(R.id.case_filter_spinner).setVisibility(View.VISIBLE);
	}
*/

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
		if(isSearchViewOpen())
		{
			if(fragment != null)
			{
				if(searchTask != null)
				{
					searchTask.cancel(true);

					// todo memory leak?  how to release before making a "new" searchTask
				}

				searchTask = new SearchTask(fragment);
				searchTask.execute(s);
			}

		}

		return true;
	}

	@Override
	// SearchView.OnCloseListener
	public boolean onClose()
	{

		// repopulate full list

		fragment.new PopulateCardsTask().execute();
		//fragment.populateCards();
		//mCardAdapter.notifyDataSetChanged();

		return false;
	}

	@Override
	/**
	 * override the back button
	 * prevent back button from closing app if searchView is open
	 */
	public void onBackPressed()
	{
		if (isSearchViewOpen())
		{
			searchView.setIconified(true);

			// if still open (only cleared text)
			if (isSearchViewOpen())
			{
				searchView.setIconified(true);
			}
		}
		else
		{
			super.onBackPressed();
		}
	}

	private class SearchTask extends AsyncTask<String, Integer, Boolean>
	{
		CaseCardListFragment fragment;

		public SearchTask(CaseCardListFragment fragment)
		{
			this.fragment = fragment;
		}


		protected void onPreExecute()
		{
			fragment.swipeRefreshLayout.post(new Runnable()
			{
				@Override
				public void run()
				{
					fragment.swipeRefreshLayout.setRefreshing(true);
				}
			});
		}

		@Override
		/**
		 * returns Boolean true if has results, false if no search results
		 */
		protected Boolean doInBackground(String... queryStr)
		{
			if(queryStr[0].isEmpty())
			{
				mCardAdapter.loadCases(null);
				return true;
			}
			else if(queryStr[0].length() == 1)
			{
				mCardAdapter.loadCases(null);
				return true;
			}

			String wildQuery = "%" + queryStr[0] + "%";
			String[] selArgs = {wildQuery};

			MergeCursor case_cursor; // merge into one cursor for StickyCard List

			String [] search_categories = getResources().getStringArray(R.array.search_categories_array);
			Cursor [] case_cursor_array = new Cursor[search_categories.length];

			// set the headers for StickyRecyclerHeaders
			List<String> headerList = new ArrayList<String>();

			// do searches of each relevant text field
			case_cursor_array[0] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_CASE_NUMBER + " LIKE ? ", selArgs, CasesProvider.KEY_STUDY_DATE + " DESC");
			case_cursor_array[1] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_DIAGNOSIS + " LIKE ? ", selArgs, CasesProvider.KEY_DIAGNOSIS);
			case_cursor_array[2] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FINDINGS + " LIKE ? ", selArgs, CasesProvider.KEY_FINDINGS);

			if(isCancelled())
			{
				return false;
			}

			case_cursor_array[3] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_KEYWORDS + " LIKE ? ", selArgs, CasesProvider.KEY_KEYWORDS);
			case_cursor_array[4] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_STUDY_TYPE + " LIKE ? ", selArgs, CasesProvider.KEY_STUDY_TYPE);

			if(isCancelled())
			{
				return false;
			}

			case_cursor_array[5] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_BIOPSY + " LIKE ? ", selArgs, CasesProvider.KEY_BIOPSY);
			case_cursor_array[6] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FOLLOWUP_COMMENT + " LIKE ? ", selArgs, CasesProvider.KEY_FOLLOWUP_COMMENT);
			case_cursor_array[7] = getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_COMMENTS + " LIKE ? ", selArgs, CasesProvider.KEY_COMMENTS);

			if(isCancelled())
			{
				return false;
			}

			// merge cursors into one list
			case_cursor = new MergeCursor(case_cursor_array);

			if(case_cursor.getCount() == 0)
			{
				mCardAdapter.loadCases(null);
				case_cursor.close();
				return true;
			}

			// add search header for each case
			for(int i = 0; i < case_cursor_array.length; i++)
			{
				for(int c = 0; c < case_cursor_array[i].getCount(); c++)
				{
					headerList.add(search_categories[i]);
				}

				if(isCancelled())
				{
					return false;
				}

			}

			// change card adapter and set headers with search results
			mCardAdapter.loadCases(case_cursor);
			mCardAdapter.setHeaderList(headerList);

			case_cursor.close();

			return true;
		}

		protected void onPostExecute(Boolean updateResults)
		{
			if(updateResults)
			{
				mCardAdapter.notifyDataSetChanged();
			}

			fragment.swipeRefreshLayout.setRefreshing(false);
		}
	} // end SearchTask

	private boolean isSearchViewOpen()
	{
		if(searchView != null && !searchView.isIconified())
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	private void runTutorial(final int step)
	{
		/*
		TourGuide mTourGuideHandler;



		if(step == 0)
		{
			// Welcome
			View viewTarget = null;
			if(mToolbar != null)
			{
				viewTarget = mToolbar.findViewById(R.id.case_filter_spinner);
			}

			if(viewTarget != null)
			{




				mTourGuideHandler = TourGuide.init(this).with(TourGuide.Technique.Click)
						                    .setPointer(new Pointer())
						                    .setToolTip(new ToolTip())
						                    .setOverlay(new Overlay())
						                    .playOn(viewTarget);

			}
		}
		else */
		 if(step == 1)
		{
			View viewTarget = null;
			if(fragment != null && fragment.getView() != null)
			{
				viewTarget = fragment.getView().findViewById(R.id.cards_list);
			}
			if(viewTarget != null)
			{
				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  .setTarget(new ViewTarget(viewTarget))
						                                  .setContentTitle("Case list")
						                                  .setContentText("Click on a case to see more details. Long press to start selecting multiple cases for sharing.")
						                                  .setStyle(R.style.CustomShowcaseTheme)
						                                  .hideOnTouchOutside()
						                                  .build();
				showcaseView.overrideButtonClick(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						showcaseView.hide();
						runTutorial(step + 1);
					}
				});
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { showcaseView.setPadding(0, 0, 0, UtilClass.getNavigationBarHeight(this)); }
			}
		}
		else if(step == 2)
		{
			View viewTarget = null;
			if(mToolbar != null)
			{
				viewTarget = mToolbar.findViewById(R.id.case_filter_spinner);
			}

			if(viewTarget != null)
			{


				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  .setTarget(new ViewTarget(viewTarget))
						                                  .setContentTitle("Case Sorting")
						                                  .setContentText("Click this to change how your cases are sorted.")
						                                  .setStyle(R.style.CustomShowcaseTheme)
						                                  .hideOnTouchOutside()
						                                  .build();
				showcaseView.overrideButtonClick(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						showcaseView.hide();
						runTutorial(step + 1);
					}
				});
				//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { showcaseView.setPadding(0, 0, ApiUtils.getSoftButtonsBarSizeLand(activity), ApiUtils.getSoftButtonsBarSizePort(activity)); }
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { showcaseView.setPadding(0, 0, 0, UtilClass.getNavigationBarHeight(this)); }


			}

		}
		else if(step == 3)
		{
			final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
					                                  //.setTarget( new ViewTarget( ((ViewGroup)findViewById(R.id.action_bar)).getChildAt(1) ) )
					                                  .setTarget(new ViewTarget(mToolbar.findViewById(R.id.menu_search)))
					                                  .setContentTitle("Search")
					                                  .setContentText("Search through your cases.")
					                                  .setStyle(R.style.CustomShowcaseTheme)
					                                  .hideOnTouchOutside()
					                                  .build();

			//			showcaseView.setShouldCentreText(true);
			showcaseView.overrideButtonClick(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					showcaseView.hide();
					runTutorial(step + 1);
				}
			});
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { showcaseView.setPadding(0, 0, 0, UtilClass.getNavigationBarHeight(this)); }
		}
		else if(step == 4)
		{
			final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
					                                  //.setTarget( new ViewTarget( ((ViewGroup)findViewById(R.id.action_bar)).getChildAt(1) ) )
					                                  .setTarget(new ViewTarget(mToolbar.findViewById(R.id.menu_addnew)))
					                                  .setContentTitle("Add case")
					                                  .setContentText("Add a new case to your list.")
					                                  .setStyle(R.style.CustomShowcaseTheme)
					                                  .hideOnTouchOutside()
					                                  .build();

			showcaseView.overrideButtonClick(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					showcaseView.hide();
					runTutorial(step + 1);
				}
			});
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { showcaseView.setPadding(0, 0, 0, UtilClass.getNavigationBarHeight(this)); }
		}

		else if(step == 5)
		{
			final Target viewTarget = new Target()
			{
				@Override
				public Point getPoint() {
					View navIcon = null;
					for (int i = 0; i < mToolbar.getChildCount(); i++)
					{
						View child = mToolbar.getChildAt(i);
						if (ImageButton.class.isInstance(child))
						{
							navIcon = child;
							break;
						}
					}

					if (navIcon != null)
						return new ViewTarget(navIcon).getPoint();
					else
						return new ViewTarget(mToolbar).getPoint();
				}
			};

			final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
					//.setTarget( new ViewTarget( ((ViewGroup)findViewById(R.id.action_bar)).getChildAt(1) ) )
					.setTarget(viewTarget)
					.setContentTitle("Navigation Drawer")
					.setContentText("Click here or swipe from the left to open the navigation drawer.")
					.setStyle(R.style.CustomShowcaseTheme)
					.hideOnTouchOutside()
					.build();

			showcaseView.overrideButtonClick(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					showcaseView.hide();
					runTutorial(step + 1);
				}
			});
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { showcaseView.setPadding(0, 0, 0, UtilClass.getNavigationBarHeight(this)); }

		}
		else if(step == 6)
		{
			final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
					                                  .setTarget( new ViewTarget(mOverflowTarget) )
							                                   //.setTarget(new ViewTarget(fragment.getView().findViewById(R.id.menu_help)))
					                                  .setContentTitle("Overflow menu")
					                                  .setContentText("More menu options here.\n\nClick the Help button to see this tutorial again.")
					                                  .setStyle(R.style.CustomShowcaseThemeEnd)
					                                  .hideOnTouchOutside()
					                                  .build();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { showcaseView.setPadding(0, 0, 0, UtilClass.getNavigationBarHeight(this)); }

		}
	}


	/**
	 * Placeholder fragment
	 */
	public static class CaseCardListFragment extends Fragment
	{
		// Layout
		private RecyclerView mRecyclerView;
		private CaseCardAdapter mCardAdapter;

		private static View.OnClickListener cardOnClickListener;
		private static View.OnLongClickListener cardOnLongClickListener;

		/**
		 * Contextual action mode
		 */
		public android.view.ActionMode.Callback mActionModeCallback = null;

		View rootView;
		CaseCardListActivity mActivity;

		private SwipeRefreshLayout swipeRefreshLayout;

		public CaseCardListFragment()
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			// inflate the activity layout
			rootView = inflater.inflate(R.layout.activity_case_cardlist, container, false);

			// Setup SwipeRefreshLayout
			swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
			swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
			{
				@Override
				public void onRefresh()
				{
					if(mCardAdapter != null && mCardAdapter.mActionMode != null)    // don't refresh if in contextual action bar mode
					{
						swipeRefreshLayout.setRefreshing(false);
					}
					else if(mActivity.isSearchViewOpen()) //don't refresh if search bar open
					{
						swipeRefreshLayout.setRefreshing(false);
					}
					else
					{
						new PopulateCardsTask().execute();
					}
				}
			});
			swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
					                                android.R.color.holo_green_light,
					                                android.R.color.holo_orange_light,
					                                android.R.color.holo_red_light);

			// Find RecyclerView
			mRecyclerView = (RecyclerView)rootView.findViewById(R.id.cards_list);

			// Setup RecyclerView
			LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
			mRecyclerView.setLayoutManager(mLayoutManager);
			mRecyclerView.setItemAnimator(new DefaultItemAnimator());

			/*// Setup RecyclerView FastScroller
			VerticalRecyclerViewFastScroller fastScroller = (VerticalRecyclerViewFastScroller) rootView.findViewById(R.id.fast_scroller);
			SectionTitleIndicator sectionTitleIndicator = (SectionTitleIndicator) rootView.findViewById(R.id.fast_scroller_section_title_indicator);
			fastScroller.setRecyclerView(mRecyclerView);
			mRecyclerView.addOnScrollListener(fastScroller.getOnScrollListener());
			fastScroller.setSectionIndicator(sectionTitleIndicator);*/

			FastScroller fastScroller=(FastScroller)rootView.findViewById(R.id.fastscroller);
			fastScroller.setRecyclerView(mRecyclerView);

			// Setup CaseCardAdapter
			mCardAdapter = mActivity.mCardAdapter = new CaseCardAdapter(getActivity(), null, R.layout.card_case);

			// contextual menu
			setActionModeCallback();
			mCardAdapter.setActionModeCallback(mActionModeCallback);

			// click listeners
			setCardClickListeners();
			mCardAdapter.setOnClickListeners(cardOnClickListener, cardOnLongClickListener);

			mRecyclerView.setAdapter(mCardAdapter);

			// Sticky headers
			StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mCardAdapter);
			mRecyclerView.addItemDecoration(headersDecor);


			// Sticky headers touch listener
			StickyRecyclerHeadersTouchListener touchListener =
					new StickyRecyclerHeadersTouchListener(mRecyclerView, headersDecor);
			touchListener.setOnHeaderClickListener(
					                                      new StickyRecyclerHeadersTouchListener.OnHeaderClickListener() {
						                                      @Override
						                                      public void onHeaderClick(View header, int position, long headerId)
						                                      {
							                                      mCardAdapter.toggleCollapseHeader(headerId);
						                                      }
					                                      });
			mRecyclerView.addOnItemTouchListener(touchListener);


			mRecyclerView.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					UtilClass.hideKeyboard(getActivity());
					return false;
				}
			});

			//test
			//mLayoutManager.scrollToPosition(20);

			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			mActivity = (CaseCardListActivity)activity;
		}

		private class PopulateCardsTask extends AsyncTask<Void, Integer, Void>
		{
			boolean showProgress;
			long case_subset;

			public PopulateCardsTask()
			{
				this.case_subset = mActivity.case_subset;
				this.showProgress = true;   // default
			}

			public PopulateCardsTask(boolean showProgress)
			{
				this.case_subset = mActivity.case_subset;
				this.showProgress = showProgress;
			}
/*
			public PopulateCardsTask(long subset, boolean showProgress)
			{
				this.case_subset = subset;
				this.showProgress = showProgress;
			}
*/
			protected void onPreExecute()
			{
				if(showProgress)
				{
					// show progress wheel in UI thread
					swipeRefreshLayout.post(new Runnable()
					{
						@Override
						public void run()
						{
							swipeRefreshLayout.setRefreshing(true);
						}
					});
				}
			}

			@Override
			protected Void doInBackground(Void... v)
			{
				// Get cases, filtered/sorted by spinner list in action bar
				MergeCursor case_cursor;

				Cursor [] case_cursor_array;
				int initial_pos = 0; //section select

				// set the headers for StickyRecyclerHeaders
				List<String> headerList = new ArrayList<String>();

				String subset_query_string = null;
				String subset_query_string_and = null;
				if(case_subset == NavDrawerActivity.POS_CASE_LIST_FAV)
				{
					//subset_query_string = new String(CasesProvider.KEY_FAVORITE + " = '1'");
					subset_query_string = CasesProvider.KEY_FAVORITE + " = '1'";
					subset_query_string_and = subset_query_string + " AND ";
				}
				else if(case_subset == NavDrawerActivity.POS_CASE_LIST_FOLLOWUP)
				{
					subset_query_string = CasesProvider.KEY_FOLLOWUP + " = '1'";
					subset_query_string_and = subset_query_string + " AND ";
				}
				else if(case_subset >= NavDrawerActivity.POS_CASE_LIST_SUBSECTION)
				{
					// get cursor of "Radiology Section List", in order determined by user list preferences
					Cursor subsection_cursor = mActivity.getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);

					subsection_cursor.moveToPosition((int)case_subset-POS_CASE_LIST_SUBSECTION);
					subset_query_string = CasesProvider.KEY_SECTION + " = '" + subsection_cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE) + "'";
					subset_query_string_and = subset_query_string + " AND ";

					subsection_cursor.close();

				}
				else
				{
					//subset_query_string = null;
					//subset_query_string_and = new String("");

					subset_query_string = CasesProvider.KEY_USER_ID + " = '" + userID + "'";
					subset_query_string_and = subset_query_string + " AND ";
				}

				if(caseFilterMode == FILTER_SECTION)
				{
					// get cursor of "Radiology Section List", in order determined by user list preferences
					Cursor section_cursor = mActivity.getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);

					// instantiate case cursor array with size of section_cursor.getCount() + 1.  Added one for those cases with empty "Radiology Section" field, ie Unknown
					// will use MergeCursor afterwards.  must use cursor array this since a case may be in more than one section
					case_cursor_array = new Cursor[section_cursor.getCount() + 1];

					// query each case cursor by "Radiology Section" "LIKE %?%".  Each case may have more than one "Radiology Section" listed. Order by desc "Study Date"
					if (section_cursor.moveToFirst())
					{
						int i = 0;  // section/group counter

						do
						{
							// get the KEY_SECTION name
							String mSection = section_cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE);

							// find all cases with this KEY_SECTION
							case_cursor_array[i] = mActivity.getContentResolver().query(CasesProvider.CASES_URI, null, subset_query_string_and + CasesProvider.KEY_SECTION + " LIKE ?", new String[]{"%" + mSection + "%"}, CasesProvider.KEY_STUDY_DATE + " DESC", null);

							// set KEY_SECTION as headers in each list position in headerList, with same IDs
							for (int c = 0; c < case_cursor_array[i].getCount(); c++)
							{
								headerList.add(mSection);
							}

							i = i + 1;
						} while (section_cursor.moveToNext());

						// last filter group is the cases with empty "Radiology Section" fields, find all cases with this KEY_SECTION
						case_cursor_array[i] = mActivity.getContentResolver().query(CasesProvider.CASES_URI, null, subset_query_string_and + CasesProvider.KEY_SECTION + " IS NULL OR " + CasesProvider.KEY_SECTION + " = ?", new String[]{""}, CasesProvider.KEY_STUDY_DATE + " DESC", null);

						for (int c = 0; c < case_cursor_array[i].getCount(); c++)
						{
							headerList.add(EMPTY_FIELD_GROUP_HEADER);
						}

					}

					section_cursor.close();
				}
				else if(caseFilterMode == FILTER_LAST_MODIFIED)
				{
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = mActivity.getContentResolver().query(CasesProvider.CASES_URI, null, subset_query_string, null, CasesProvider.KEY_LAST_MODIFIED_DATE + " DESC, " + CasesProvider.KEY_ROWID + " DESC", null);

					/*
					for(int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Recent");
						//headerIdList.add(0);
					}
					*/

					if (case_cursor_array[0].moveToFirst())
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
							} else
							{
								headerList.add(EMPTY_FIELD_GROUP_HEADER);
							}
						} while (case_cursor_array[0].moveToNext());
					}

				}
				else if(caseFilterMode == FILTER_STUDYDATE)
				{
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = mActivity.getContentResolver().query(CasesProvider.CASES_URI, null, subset_query_string, null, CasesProvider.KEY_STUDY_DATE + " DESC", null);

					if (case_cursor_array[0].moveToFirst())
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
							} else
							{
								headerList.add(EMPTY_FIELD_GROUP_HEADER);
							}
						} while (case_cursor_array[0].moveToNext());
					}
				}
				else if(caseFilterMode == FILTER_MODALITY)
				{
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = mActivity.getContentResolver().query(CasesProvider.CASES_URI, null, subset_query_string_and + CasesProvider.KEY_STUDY_TYPE + " is not null and " + CasesProvider.KEY_STUDY_TYPE + " != ?", new String[]{""}, CasesProvider.KEY_STUDY_TYPE, null);

					if (case_cursor_array[0].moveToFirst())
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
							} else
							{
								headerList.add(EMPTY_FIELD_GROUP_HEADER);
							}
						} while (case_cursor_array[0].moveToNext());
					}
				}
				else if(caseFilterMode == FILTER_KEYWORDS)
				{
					//case_cursor_array = new Cursor[1];
					//case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_KEYWORDS + " is not null and " + CasesProvider.KEY_KEYWORDS + " != ?", new String[] {""}, CasesProvider.KEY_KEYWORDS, null);

					// get cursor of "Radiology Keywords List", in order determined by user list preferences
					Cursor keywords_cursor = mActivity.getContentResolver().query(CasesProvider.KEYWORD_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);

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
							String mKeyword = keywords_cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE);

							// find all cases with this KEY_KEYWORDS
							case_cursor_array[i] = mActivity.getContentResolver().query(CasesProvider.CASES_URI, null, subset_query_string_and + CasesProvider.KEY_KEYWORDS + " LIKE ?", new String[]{"%" + mKeyword + "%"}, CasesProvider.KEY_ROWID + " DESC", null);

							// set KEY_KEYWORDS as headers in each list position in headerList, with same IDs
							for(int c = 0; c < case_cursor_array[i].getCount(); c++)
							{
								headerList.add(mKeyword);
							}

							i = i + 1;
						} while(keywords_cursor.moveToNext());

					}
					keywords_cursor.close();
				}
				/*
				else if(caseFilterMode == FILTER_FOLLOWUP)
				{
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, subset_query_string_and + CasesProvider.KEY_FOLLOWUP + " = ?", new String[]{"1"}, CasesProvider.KEY_STUDY_DATE + " DESC", null);

					for (int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Followup");
						//headerIdList.add(0);
					}

				}
				else if(caseFilterMode == FILTER_FAVORITE)
				{
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, subset_query_string_and + CasesProvider.KEY_FAVORITE + " = ?", new String[]{"1"}, CasesProvider.KEY_STUDY_DATE + " DESC", null);

					for (int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("Favorites");
						//headerIdList.add(0);
					}

				}
				*/
				else
				{
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = mActivity.getContentResolver().query(CasesProvider.CASES_URI, null, subset_query_string, null, null, null);

					for (int c = 0; c < case_cursor_array[0].getCount(); c++)
					{
						headerList.add("My Cases");
						//headerIdList.add(0);
					}

				}

				case_cursor = new MergeCursor(case_cursor_array);

				mCardAdapter.loadCases(case_cursor);
				mCardAdapter.setHeaderList(headerList);

				// close cursors - todo check if doing it right
				for(int i = 0; i < case_cursor_array.length; i++)
				{
					case_cursor_array[i].close();
				}
				case_cursor.close();

				return null;
			}

			protected void onPostExecute(Void v)
			{
				if(showProgress)
				{
					swipeRefreshLayout.setRefreshing(false);

					// show view changes after progress wheel closes
					new Handler().postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							mCardAdapter.notifyDataSetChanged();
						}
					}, 100);
				}
				else
				{
					mCardAdapter.notifyDataSetChanged();
					swipeRefreshLayout.setRefreshing(false);   // just in case
				}
			}
		} // end PopulateCardsTask


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
					activity.setTheme(R.style.ContextualActionMode_Light);
					// Inflate a menu resource providing context menu items
					MenuInflater inflater = mode.getMenuInflater();
					inflater.inflate(R.menu.case_list_contextual, menu);

//					MenuItemCompat.setActionProvider(menu, R.menu.case_list_contextual);
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

							if(!mCardAdapter.getMultiselectList().isEmpty())
							{
								new ShareCasesTask(mCardAdapter.getMultiselectList(), mode).execute();
							}

							// todo don't finish if canceled
							//mode.finish(); // Action picked, so close the CAB

							return true;

						case R.id.menu_delete:

							// delete from database

							final List<Long> deleteIdList = new ArrayList<Long>(mCardAdapter.getMultiselectList());
							final ActionMode actionMode = mode;

							String msg;
							if(deleteIdList.size() == 1)
							{
								msg = "Delete 1 case?";
							}
							else
							{
								msg = "Delete " + deleteIdList.size() + " cases?";
							}

							// Alert dialog to confirm delete
							AlertDialog.Builder builder = new AlertDialog.Builder(activity);

							builder.setMessage(msg)
									.setPositiveButton(activity.getResources().getString(R.string.button_OK), new DialogInterface.OnClickListener()
									{
										public void onClick(DialogInterface dialog, int id)
										{
											dialog.dismiss();

											new DeleteCasesTask(activity, deleteIdList).execute();

											actionMode.finish(); // Action picked, so close the CAB
										}
									})
									.setNegativeButton(activity.getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener()
									{
										public void onClick(DialogInterface dialog, int id)
										{
											// cancel
										}
									});

							builder.show();

							return true;

						case R.id.menu_select_all:

							/*
							// get all cases
							Cursor case_cursor = getActivity().getContentResolver().query(CasesProvider.CASES_URI, null, null, null, null);
							if(case_cursor != null && case_cursor.moveToFirst())
							{
								do
								{
									mCardAdapter.addToMultiselectList(case_cursor.getInt(CasesProvider.COL_ROWID));

								} while (case_cursor.moveToNext());
							}

							mCardAdapter.mActionMode.setTitle(mCardAdapter.getMultiselectList().size() + " selected");
							mCardAdapter.notifyDataSetChanged();

*/
							mCardAdapter.addAllToMultiselectList();

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

					Case mCase = mAdapter.caseList.get(holder.getAdapterPosition());

					if (mAdapter.mActionMode == null)
					{
						// open detail view for clicked case
						Intent detailIntent = new Intent(view.getContext(), CaseDetailActivity.class);
						detailIntent.putExtra(CaseCardListActivity.ARG_KEY_ID, holder.key_id);

						// activity options
						//ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, Pair.create((View) holder.card_text1, "DetailCaseInfo1" ));

						detailIntent.putExtra(CaseDetailActivity.ARG_HAS_IMAGE, false);

						//TODO compat for kitkat
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
						{
							// get the common element for the transition in this activity
							final ImageView thumbnail_header = holder.thumbnail;
							ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, thumbnail_header, "transitionImage");
							activity.startActivityForResult(detailIntent, CaseCardListActivity.REQUEST_CASE_DETAILS, options.toBundle());
						}
						else
						{
							//activity.startActivityForResult(detailIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);

							// get the common element for the transition in this activity
							final ImageView thumbnail_header = holder.thumbnail;
							ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, thumbnail_header, "transitionImage");
							activity.startActivityForResult(detailIntent, CaseCardListActivity.REQUEST_CASE_DETAILS, options.toBundle());
						}
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
					Case mCase = mAdapter.caseList.get(holder.getAdapterPosition());

					if(mAdapter.mActionMode == null)
					{
						// TODO doesn't work as well as before with support action bar
						// open contextual menu
						//mAdapter.mActionMode = ((AppCompatActivity)activity).startSupportActionMode(mActionModeCallback);
						//mAdapter.mActionMode = ((CaseCardListActivity)activity).mToolbar.startActionMode(mActionModeCallback);
						//((CaseCardListActivity)activity).mToolbar.setVisibility(View.GONE);
						mAdapter.mActionMode = activity.startActionMode(mActionModeCallback);
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

		/**
		 *
		 */
		private class DeleteCasesTask extends AsyncTask<Void, Integer, Integer>
		{
			Activity activity;
			List<Long> deleteIdList;

			public DeleteCasesTask(Activity activity, List<Long> list)
			{
				this.activity = activity;
				this.deleteIdList = list;
			}

			protected void onPreExecute()
			{
				// show progress wheel dialog
				progressDialog = new ProgressDialog(activity, R.style.ProgressDialogTheme);
				progressDialog.setMessage("Deleting cases...");
				progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
				progressDialog.setCancelable(false);
				progressDialog.show();
			}

			@Override
			protected Integer doInBackground(Void... v)
			{
				for (int i = 0; i < deleteIdList.size(); i++)
				{
					// get key_id from list array
					long key_id = deleteIdList.get(i);

					// delete case from CASES table
					Uri case_delete_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
					activity.getContentResolver().delete(case_delete_uri, null, null);

					// delete all linked images files
					Cursor image_cursor = activity.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)}, CasesProvider.KEY_ORDER);
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
					activity.getContentResolver().delete(CasesProvider.IMAGES_URI, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)});

				}
				return deleteIdList.size();
			}

			/*
			protected void onProgressUpdate(Integer... progress) {
				setProgressPercent(progress[0]);
			}
			*/
			protected void onPostExecute(Integer numDeleted)
			{
				new PopulateCardsTask(false).execute();

				progressDialog.dismiss();

				if(numDeleted == 1)
				{
					UtilClass.showSnackbar(activity, "Deleted 1 case.");
				}
				else
				{
					UtilClass.showSnackbar(activity, "Deleted " + numDeleted + " cases.");
				}
			}
		} // end DeleteCasesTask

		/**
		 *
		 */
		private class ShareCasesTask extends AsyncTask<Void, Integer, File>
		{
			List<Long> selectList;
			ActionMode actionMode;

			public ShareCasesTask(List<Long> selectList, ActionMode actionMode)
			{
				this.selectList = selectList;
				this.actionMode = actionMode;
			}

			protected void onPreExecute()
			{
				// show progress wheel dialog
				progressDialog = new android.app.ProgressDialog(activity, R.style.ProgressDialogTheme);
				progressDialog.setMessage("Exporting cases...");
				progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
				progressDialog.setCancelable(false);
				progressDialog.show();
			}

			@Override
			protected File doInBackground(Void... v)
			{
				return UtilClass.exportCasesJSON(activity, "RadFiles cases", selectList, ""); //TODO add password stuff
			}

			/*
			protected void onProgressUpdate(Integer... progress) {
				setProgressPercent(progress[0]);
			}
			*/
			protected void onPostExecute(File shareFile)
			{
				if(shareFile == null)
				{
					progressDialog.dismiss();
					UtilClass.showSnackbar(activity, "Unable to create share file.");
					return;
				}

				Uri uriShareFile = Uri.fromFile(shareFile); // file created and stored in shareFile

				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				//shareIntent.setType("message/rfc822");
				shareIntent.setType(ImportExportActivity.RCS_MIMETYPE);
				shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
				shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Radiology cases");
				shareIntent.putExtra(Intent.EXTRA_TEXT, "Please see the attached file.\nOpen it with the RadFiles Android app!"); //todo link to store
				shareIntent.putExtra(Intent.EXTRA_STREAM, uriShareFile);

				progressDialog.dismiss();

				try
				{
					if (selectList.size() == 1)
					{
						startActivity(Intent.createChooser(shareIntent, "Share " + selectList.size() + " case..."));
					}
					else
					{
						startActivity(Intent.createChooser(shareIntent, "Share " + selectList.size() + " cases..."));
					}
				}
				catch (android.content.ActivityNotFoundException ex)
				{
					Toast.makeText(activity, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
				}

				actionMode.finish();
			}
		} // end ShareCasesTask

	} // end fragment

}
