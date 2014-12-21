package com.huantnguyen.radcases.app;

import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
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
import android.widget.EditText;

import com.astuetz.PagerSlidingTabStrip;


public class ManageListsActivity extends NavigationDrawerActivity {

	String TAG = "ManageListsActivity";
	private TabbedFragment fragment;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setDrawerPosition(NavigationDrawerActivity.POS_MANAGE_LISTS);
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
			tabs.setIndicatorColor(activity.getResources().getColor(R.color.white_text));
			tabs.setIndicatorColor(activity.getResources().getColor(R.color.white_text));
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

				// Setup list to be placed in adapter
				Cursor listCursor;
				final Uri tableURI;
				final String tableKEY;

				switch(getArguments().getInt(ARG_SECTION_NUMBER))
				{
					case 0:
						tableURI = CasesProvider.KEYWORD_LIST_URI;
						tableKEY = CasesProvider.KEY_KEYWORDS;
						break;

					case 1:
						tableURI = CasesProvider.STUDYTYPE_LIST_URI;
						tableKEY = CasesProvider.KEY_STUDY_TYPE;
						break;

					case 2:
						tableURI = CasesProvider.SECTION_LIST_URI;
						tableKEY = CasesProvider.KEY_SECTION;
						break;

					default:
						tableURI = null;
						tableKEY = null;
						break;
				}
				if(tableURI != null)
				{
					listCursor = getContentResolver().query(tableURI, null, null, null, CasesProvider.KEY_ORDER);
				}
				else
				{
					listCursor = null;
				}

				final ManageListsAdapter mListAdapter = new ManageListsAdapter(getActivity(), listCursor);
				mListAdapter.setHasStableIds(true);
				mListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
				{
					@Override
					public void onChanged()
					{
						super.onChanged();
						UtilClass.showMessage(getActivity(), "test data changed");
					}

					@Override
					public void onItemRangeChanged(int positionStart, int itemCount)
					{
						super.onItemRangeChanged(positionStart, itemCount);
						UtilClass.showMessage(getActivity(), "item Changed: position: " + positionStart + ", item: " + mListAdapter.getItem(positionStart));

						// changed item text
						String newItemString = mListAdapter.getItem(positionStart);

						// put data into "values" for database insert/update
						ContentValues values = new ContentValues();

						values.put(tableKEY, newItemString);
						Uri row_uri = ContentUris.withAppendedId(tableURI, mListAdapter.getKey(positionStart));
						getContentResolver().update(row_uri, values, null, null);

					}

					@Override
					public void onItemRangeInserted(int positionStart, int itemCount)
					{
						super.onItemRangeInserted(positionStart, itemCount);
						UtilClass.showMessage(getActivity(), "onItemRangeInserted: positionStart: " + positionStart + ", itemCount: " + itemCount);

						// insert into database
						// get key

					}

					@Override
					public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount)
					{
						// don't move last position (add new custom item)
					//	if(fromPosition >= mListAdapter.getList().size()-1 || toPosition >= mListAdapter.getList().size()-1)
					//		return;

						super.onItemRangeMoved(fromPosition, toPosition, itemCount);
						//UtilClass.showMessage(getActivity(), "onItemRangeMoved: fromPosition: " + fromPosition + ", toPosition: " + toPosition + ", itemCount: " + itemCount);

						ContentValues values = new ContentValues();

						// update KEY_ORDER for all rows between swapped list items
						int start;
						int end;
						if(fromPosition < toPosition)
						{
							start = fromPosition;
							end = toPosition;
						}
						else
						{
							start = toPosition;
							end = fromPosition;
						}
						for(int i = start; i <= end; i++)
						{
							values.clear();
							values.put(CasesProvider.KEY_ORDER, i);
							Uri row_uri = ContentUris.withAppendedId(tableURI, mListAdapter.getKey(i));
							getContentResolver().update(row_uri, values, null, null);

							//UtilClass.showMessage(getActivity(), "item moved: Position: " + i + ", item: " + mListAdapter.getItem(i));
						}

					}

					@Override
					public void onItemRangeRemoved(int positionStart, int itemCount)
					{
						super.onItemRangeRemoved(positionStart, itemCount);
						UtilClass.showMessage(getActivity(), "onItemRangeRemoved: positionStart: " + positionStart + ", itemCount: " + itemCount);

						// delete from database

					}

				});
				mRecyclerView.setAdapter(mListAdapter);

				return rootView;
			}

		} // end TabbedContentFragment

	}

}
