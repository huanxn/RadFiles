package com.huantnguyen.radcases.app;

import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.database.Cursor;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;


public class ManageListsActivity extends NavigationDrawerActivity {

	String TAG = "ManageListsActivity";
	private TabbedFragment fragment;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_case_detail);

		if (savedInstanceState == null)
		{
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.

			fragment = new TabbedFragment();

			getFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();

		}
	}

	@Override
	// Inflate the menu; this adds items to the action bar if it is present.
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.manage_lists, menu);

		return super.onCreateOptionsMenu(menu);     // nav drawer
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.menu_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public class TabbedFragment extends Fragment
	{

		//public static final String TAG = TabbedFragment.class.getSimpleName();

		/**
		 * The {@link android.support.v4.view.PagerAdapter} that will provide
		 * fragments for each of the sections. We use a
		 * {@link FragmentPagerAdapter} derivative, which will keep every
		 * loaded fragment in memory. If this becomes too memory intensive, it
		 * may be best to switch to a
		 * {@link android.support.v13.app.FragmentStatePagerAdapter}.
		 */
		SectionsPagerAdapter mSectionsPagerAdapter;

		/**
		 * The {@link ViewPager} that will host the section contents.
		 */
		ViewPager mViewPager;

		Activity activity;


		public TabbedFragment newInstance()
		{
			return new TabbedFragment();
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View view = inflater.inflate(R.layout.fragment_manage_lists_tabs, container, false);
			mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

			// Initialize the ViewPager and set the adapter
			mViewPager = (ViewPager) view.findViewById(R.id.pager);
			mViewPager.setAdapter(mSectionsPagerAdapter);

			// Bind the PagerSlidingTabStrip to the ViewPager
			PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) view.findViewById(R.id.tabs);
			tabs.setViewPager(mViewPager);
//			tabs.setBackgroundColor(activity.getResources().getColor(R.attr.colorPrimary));
//			tabs.setTextColor(activity.getResources().getColor(R.attr.actionMenuTextColor));

			// TODO get from attr
			tabs.setIndicatorColor(activity.getResources().getColor(R.color.default_actionMenuTextColor));
			tabs.setIndicatorColor(activity.getResources().getColor(R.color.default_actionMenuTextColor));
			tabs.setShouldExpand(true);


			//TypedValue textColor = new TypedValue();
			//tabs.setDividerColor(textColor.);

			return view;
		}

		@Override
		public void onAttach(Activity activity)
		{
			super.onAttach(activity);
			this.activity = activity;
		}

		public class SectionsPagerAdapter extends FragmentPagerAdapter
		{

			String [] tab_titles;

			public SectionsPagerAdapter(FragmentManager fm)
			{
				super(fm);

				tab_titles = getResources().getStringArray(R.array.list_tab_titles_array);
			}

			@Override
			public Fragment getItem(int position)
			{
				if(position < getCount())
				{
					Fragment fragment = new TabbedContentFragment();
					Bundle args = new Bundle();
					args.putInt(TabbedContentFragment.ARG_SECTION_NUMBER, position);
					fragment.setArguments(args);

					return fragment;
				}
				else
				{
					return null;
				}

			}

			@Override
			public int getCount()
			{
				return tab_titles.length;
			}

			@Override
			public CharSequence getPageTitle(int position)
			{
				Locale l = Locale.getDefault();

				if(position < getCount())
					return tab_titles[position].toUpperCase(l);
				else
					return null;
			}
		} // end SectionsPagerAdapter

		public class TabbedContentFragment extends Fragment {

			public static final String ARG_SECTION_NUMBER = "section_number";


			private View rootView;
			private RecyclerView mRecyclerView;

			public TabbedContentFragment() {
			}

			@Override
			public View onCreateView(LayoutInflater inflater, ViewGroup container,
			                         Bundle savedInstanceState)
			{
				rootView = inflater.inflate(R.layout.fragment_manage_lists, container, false);
			//	TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
			//	dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));

				// Find RecyclerView
				mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);

				// Setup RecyclerView
				mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
				mRecyclerView.setItemAnimator(new DefaultItemAnimator());

				// Setup CaseCardAdapter
				Cursor listCursor;
				switch(getArguments().getInt(ARG_SECTION_NUMBER))
				{
					case 0:
						listCursor = getContentResolver().query(CasesProvider.KEYWORD_LIST_URI, null, null, null, CasesProvider.KEY_ORDER);
						break;

					case 1:
						listCursor = getContentResolver().query(CasesProvider.STUDYTYPE_LIST_URI, null, null, null, CasesProvider.KEY_ORDER);
						break;

					case 2:
						listCursor = getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER);
						break;

					default:
						listCursor = null;
						break;
				}

				ManageListsAdapter mListAdapter = new ManageListsAdapter(listCursor);
				mRecyclerView.setAdapter(mListAdapter);

				return rootView;
			}

		} // end TabbedContentFragment

	}

}
