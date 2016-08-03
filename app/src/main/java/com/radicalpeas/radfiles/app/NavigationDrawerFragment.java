package com.radicalpeas.radfiles.app;


import android.app.Activity;
import android.app.Fragment;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment
{

	/**
	 * Remember the position of the selected item.
	 */
	private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

	/**
	 * Per the design guidelines, you should show the drawer on launch until the user manually
	 * expands it. This shared preference tracks this.
	 */
	private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

	/**
	 * A pointer to the current callbacks instance (the Activity).
	 */
	private NavigationDrawerCallbacks mCallbacks;

	/**
	 * Helper component that ties the action bar to the navigation drawer.
	 */
	private ActionBarDrawerToggle mDrawerToggle;

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerListView;
	public View mDrawerLinearLayout;
	//private ExpandableListView mDrawerListView;
	private View mFragmentContainerView;

	private int mCurrentSelectedPosition = 0;
	private boolean mFromSavedInstanceState;
	private boolean mUserLearnedDrawer;

	private NavigationDrawerActivity mActivity;

	public NavigationDrawerFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Read in the flag indicating whether or not the user has demonstrated awareness of the
		// drawer. See PREF_USER_LEARNED_DRAWER for details.
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

		if (savedInstanceState != null) {
			mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
			mFromSavedInstanceState = true;
		}

		// Select either the default item (0) or the last selected item.
		selectItem(mCurrentSelectedPosition);
	}

	@Override
	public void onActivityCreated (Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		// Indicate that this fragment would like to influence the set of actions in the action bar.
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);

		mDrawerLinearLayout = view.findViewById(R.id.fragment_navigation_drawer_linear_layout);

		/*if(mActivity.hasTransparentStatusbar)
		{
			view.findViewById(R.id.fragment_navigation_drawer_linear_layout).setPadding(0, UtilClass.getStatusBarHeight(getActivity()), 0, 0);
		}*/

		mDrawerListView = (ListView)view.findViewById(R.id.navigation_list);

		/*
		ListView.MarginLayoutParams params = new ListView.MarginLayoutParams(ListView.MarginLayoutParams.MATCH_PARENT, ListView.MarginLayoutParams.MATCH_PARENT);
		params.setMargins(0,0,0,0);
		mDrawerListView.setLayoutParams(params);
		*/

		// set up nav drawer expandable list

		/*

		List<String> navList = new LinkedList<String>(Arrays.asList(getResources().getStringArray(R.array.nav_drawer_array)));
		final int sizeOfStandardNavList = navList.size();

		final List<String> listFilterHeader = Arrays.asList(getResources().getStringArray(R.array.actionbar_sort_list));
		final HashMap<String, List<String>> listDataChild = new HashMap<String, List<String>>();
		List<String> interestingCase_filterList = Arrays.asList(getResources().getStringArray(R.array.actionbar_sort_list));

		// standard nav drawer list with no children
		for(int i = 0; i < navList.size(); i++)
		{
			// get string list from cursor
			listDataChild.put(listFilterHeader.get(i), null);
		}

		navList.add("");
		listDataChild.put("", null);

		for(int i = 0; i < listFilterHeader.size(); i++)
		{
			// append filter list to nav list
			navList.add(listFilterHeader.get(i));
			//navList.add("test");

			// get string list from cursor
			if(i==0)
			{
				listDataChild.put(listFilterHeader.get(i), interestingCase_filterList);
			}
			else
			{
				listDataChild.put(listFilterHeader.get(i), null);
			}
		}


		mDrawerListView = (ExpandableListView)view.findViewById(R.id.case_filter_expandable_list);

		mDrawerListView.setAdapter(new NavigationDrawerExpandableListAdapter(getActivity(), navList, listDataChild));


		mDrawerListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener()
		{
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
			                            int groupPosition, long id)
			{
				if (groupPosition < sizeOfStandardNavList)
				{ //not expandable
					selectItem(groupPosition);
					return false;
				}
				else
				{   // expandable
					return false;
				}
			}
		});


		mDrawerListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
			                            int groupPosition, int childPosition, long id) {
				Toast.makeText(
						              getActivity(),
						              listFilterHeader.get(groupPosition)
								              + " : "
								              + listDataChild.get(
										                                 listFilterHeader.get(groupPosition)).get(
												                                                                       childPosition), Toast.LENGTH_SHORT)
						.show();
				return false;
			}
		});

*/


		mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectItem(position);
			}
		});

		mDrawerListView.setAdapter(new ArrayAdapter<String>(getActivity().getApplicationContext(),
				                                            android.R.layout.simple_list_item_activated_1,
				                                            android.R.id.text1,
				                                            getResources().getStringArray(R.array.nav_drawer_array)
		));
		mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);

		return view;
	}

	public boolean isDrawerOpen() {
		return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
	}

	/**
	 * Users of this fragment must call this method to set up the navigation drawer interactions.
	 *
	 * @param fragmentId   The android:id of this fragment in its activity's layout.
	 * @param drawerLayout The DrawerLayout containing this fragment's UI.
	 */
	public void setUp(int fragmentId, DrawerLayout drawerLayout)
	{
		setUp(fragmentId, drawerLayout, true);
	}

	/**
	 *
	 * @param fragmentId
	 * @param drawerLayout
	 * @param showDrawerIndicator: icon in upper left. default is true.  if false, shows back arrow
	 */
	public void setUp(int fragmentId, DrawerLayout drawerLayout, boolean showDrawerIndicator)
	{
		mFragmentContainerView = getActivity().findViewById(fragmentId);

		//UtilClass.setMargins(mFragmentContainerView, 0, 81, 0, 0);


		mDrawerLayout = drawerLayout;

		// set a custom shadow that overlays the main content when the drawer opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// set up the drawer's list view with items and click listener

		ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		if(actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the navigation drawer and the action bar app icon.
		mDrawerToggle = new ActionBarDrawerToggle(
				                                         getActivity(),                    /* host Activity */
				                                         mDrawerLayout,                    /* DrawerLayout object */
				                                         //R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
				                                         R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
				                                         R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
		) {
			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				if (!isAdded()) {
					return;
				}

				getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
			}

			@Override
			public void onDrawerOpened(View drawerView)
			{
				super.onDrawerOpened(drawerView);
				if (!isAdded()) {
					return;
				}

				if (!mUserLearnedDrawer)
				{
					// The user manually opened the drawer; store this flag to prevent auto-showing
					// the navigation drawer automatically in the future.
					mUserLearnedDrawer = true;
					SharedPreferences sp = PreferenceManager
							                       .getDefaultSharedPreferences(getActivity());
					sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
				}

				getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
			}
		};


		// If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
		// per the navigation drawer design guidelines.
		if (!mUserLearnedDrawer && !mFromSavedInstanceState)
		{
			mDrawerLayout.openDrawer(mFragmentContainerView);
			mUserLearnedDrawer = true;
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(getActivity());
			sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
		}

		// Defer code dependent on restoration of previous instance state.
		mDrawerLayout.post(new Runnable() {
			@Override
			public void run() {
				mDrawerToggle.syncState();
			}
		});

		mDrawerToggle.setDrawerIndicatorEnabled(showDrawerIndicator);
		//mDrawerToggle.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

		mDrawerLayout.setDrawerListener(mDrawerToggle);
	}

	public void setDrawerPosition(int position)
	{
		mCurrentSelectedPosition = position;

		if(position == NavigationDrawerActivity.POS_NONE)
		{
			mDrawerListView.setItemChecked(position, false);
			for(int i = NavigationDrawerActivity.POS_CASE_LIST; i <= NavigationDrawerActivity.POS_INFO; i++)
			{
				mDrawerListView.setItemChecked(i, false);
			}
		}
		else
		{
			mDrawerListView.setItemChecked(position, true);
		}
	}

	private void selectItem(int position)
	{
		// if different item is selected, then change position and perform that action
		if(mCurrentSelectedPosition != position)
		{
			mCurrentSelectedPosition = position;
			if (mDrawerListView != null)
			{
				mDrawerListView.setItemChecked(position, true);
			}
			if (mDrawerLayout != null)
			{
				mDrawerLayout.closeDrawer(mFragmentContainerView);
			}
			if (mCallbacks != null)
			{
				mCallbacks.onNavigationDrawerItemSelected(position);
			}
		}
		// do nothing, drawer stays open
		// otherwise, same item selected, just close the nav drawer, do not reopen activity
		else
		{
			/*
			if (mDrawerLayout != null)
			{
				mDrawerLayout.closeDrawer(mFragmentContainerView);
			}
			*/
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		mActivity = (NavigationDrawerActivity)activity;

		try {
			mCallbacks = (NavigationDrawerCallbacks) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Forward the new configuration the drawer toggle component.
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// If the drawer is open, show the global app actions in the action bar. See also
		// showGlobalContextActionBar, which controls the top-left area of the action bar.
		if (mDrawerLayout != null && isDrawerOpen()) {
			inflater.inflate(R.menu.navigation_drawer, menu);
			showGlobalContextActionBar();
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		// If the nav drawer is open, hide action items related to the content view
		MenuItem hiddenItem = menu.findItem(R.id.menu_search);

		if(hiddenItem != null)
		{
			hiddenItem.setVisible(!isDrawerOpen());
		}

		return;// super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Per the navigation drawer design guidelines, updates the action bar to show the global app
	 * 'context', rather than just what's in the current screen.
	 */
	private void showGlobalContextActionBar()
	{
		android.support.v7.app.ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		SpannableString mTitle = new SpannableString("RAD Files");
		mTitle.setSpan(new TypefaceSpan(getActivity(), "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		mTitle.setSpan(new TypefaceSpan(getActivity(), "RobotoCondensed-Bold.ttf"), "RAD".length(), mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		actionBar.setTitle(mTitle);
	}

	private ActionBar getActionBar() {
		return ((AppCompatActivity)getActivity()).getSupportActionBar();
	}

	/**
	 * Callbacks interface that all activities using this fragment must implement.
	 */
	public static interface NavigationDrawerCallbacks {
		/**
		 * Called when an item in the navigation drawer is selected.
		 */
		void onNavigationDrawerItemSelected(int position);
	}

	public DrawerLayout getDrawerLayout()
	{
		return mDrawerLayout;
	}
	public ListView getDrawerListView()
	{
		return mDrawerListView;
	}

}
