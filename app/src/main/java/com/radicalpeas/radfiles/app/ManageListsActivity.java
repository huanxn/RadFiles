package com.radicalpeas.radfiles.app;

import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.BasicSwapTargetTranslationInterpolator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;


public class ManageListsActivity extends NavDrawerActivity
{

	String TAG = "ManageListsActivity";
	private TabbedFragment fragment;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setDrawerPosition(NavDrawerActivity.POS_MANAGE_LISTS);
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
		if (id == R.id.menu_addnew)
		{
			UtilClass.showSnackbar(this, "placeholder: add new item");
			return true;
		}
		else if(id == R.id.menu_help)
		{
			runTutorial(1);
		}



		return super.onOptionsItemSelected(item);
	}

	private void runTutorial(final int step)
	{
		if(mToolbar == null)
		{
			return;
		}

		View viewTarget = null;

		if(step == 0)
		{
			viewTarget = fragment.rootView.findViewById(R.id.tabs);
			if (viewTarget != null)
			{
				TapTargetView tapTargetView = new TapTargetView.Builder(this)
						.title("Select a list")
						.description("Swipe or click the tabs to select a list.")
						.outerCircleColor(R.color.default_colorHeaderText)
						.cancelable(false)
						.listener(new TapTargetView.Listener()
						{
							@Override
							public void onTargetClick(TapTargetView view)
							{
								view.dismiss(true);
								runTutorial(step + 1);
							}

							@Override
							public void onTargetLongClick(TapTargetView view)
							{

							}
						})
						.showFor(viewTarget);
			}
		}
		else if(step == 1)
		{
			viewTarget = fragment.mSectionsPagerAdapter.getList(fragment.mViewPager.getCurrentItem()).getListItemTextView();
			if (viewTarget != null)
			{
				TapTargetView tapTargetView = new TapTargetView.Builder(this)
						.title("Edit list item")
						.description("Click to edit the list item.\n\nLong press for more options.")
						.outerCircleColor(R.color.default_colorHeaderText)
						.cancelable(false)
						.listener(new TapTargetView.Listener()
						{
							@Override
							public void onTargetClick(TapTargetView view)
							{
								view.dismiss(true);
								runTutorial(step + 1);
							}

							@Override
							public void onTargetLongClick(TapTargetView view)
							{

							}
						})
						.showFor(viewTarget);
			}
		}
		if(step == 2)
		{
			viewTarget = fragment.mSectionsPagerAdapter.getList(fragment.mViewPager.getCurrentItem()).getListItemHandleView();

			if (viewTarget != null)
			{
				TapTargetView tapTargetView = new TapTargetView.Builder(this)
						.title("Sort list items")
						.description("Drag the handle up or down to change the order of list items.")
						.outerCircleColor(R.color.default_colorHeaderText)
						.cancelable(false)
						.listener(new TapTargetView.Listener()
						{
							@Override
							public void onTargetClick(TapTargetView view)
							{
								view.dismiss(true);
								runTutorial(step + 1);
							}

							@Override
							public void onTargetLongClick(TapTargetView view)
							{

							}
						})
						.showFor(viewTarget);
			}
		}
		else if(step == 3)
		{
			viewTarget = fragment.mSectionsPagerAdapter.getList(fragment.mViewPager.getCurrentItem()).getListItemAddView();
			if (viewTarget != null)
			{
				TapTargetView tapTargetView = new TapTargetView.Builder(this)
						.title("Add new item")
						.description("Click to add a new item to the list.")
						.outerCircleColor(R.color.default_colorHeaderText)
						.cancelable(false)
						.listener(new TapTargetView.Listener()
						{
							@Override
							public void onTargetClick(TapTargetView view)
							{
								view.dismiss(false);
							}

							@Override
							public void onTargetLongClick(TapTargetView view)
							{

							}
						})
						.showFor(viewTarget);
			}
		}
		/*
		if (step == 1)
		{
			View viewTarget = fragment.rootView.findViewById(R.id.tabs);

			if (viewTarget != null)
			{
				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  .setTarget(new ViewTarget(viewTarget))
						                                  .setContentTitle("Select a list")
						                                  .setContentText("Swipe or click the tabs to select a list.")
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
		else if (step == 2)
		{
			//LinearLayoutManager layoutManager = ((LinearLayoutManager) )
			//((TabbedFragment.TabbedContentFragment) ((TabbedFragment.SectionsPagerAdapter) fragment.mSectionsPagerAdapter).mCurrentPrimaryItem).mRecyclerView.findViewHolderForPosition(0);
			//View viewTarget = ((ManageListsAdapter.ViewHolder)((TabbedFragment.TabbedContentFragment)fragment.mSectionsPagerAdapter.getItem(fragment.mViewPager.getCurrentItem())).mRecyclerView.findViewHolderForPosition(0)).mTextView;
			View viewTarget = fragment.mSectionsPagerAdapter.getList(fragment.mViewPager.getCurrentItem()).getListItemTextView();

			if (viewTarget != null)
			{
				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  //.setTarget( new ViewTarget( ((ViewGroup)findViewById(R.id.action_bar)).getChildAt(1) ) )
						                                  .setTarget(new ViewTarget(viewTarget))
						                                  .setContentTitle("Edit list item")
						                                  .setContentText("Click to edit the list item.\n\nLong press for more options.")
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
		else if (step == 3)
		{
			View viewTarget = fragment.mSectionsPagerAdapter.getList(fragment.mViewPager.getCurrentItem()).getListItemHandleView();

			if (viewTarget != null)
			{
				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  //.setTarget( new ViewTarget( ((ViewGroup)findViewById(R.id.action_bar)).getChildAt(1) ) )
						                                  .setTarget(new ViewTarget(viewTarget))
						                                  .setContentTitle("Sort list items")
						                                  .setContentText("Drag the handle up or down to change the order of list items.")
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

						// scroll to bottom
						fragment.mSectionsPagerAdapter.getList(fragment.mViewPager.getCurrentItem()).scrollToAddView();
						new Handler().postDelayed(new Runnable()
						{
							@Override
							public void run()
							{
								runTutorial(step + 1);
							}
						}, 100);
					}
				});
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { showcaseView.setPadding(0, 0, 0, UtilClass.getNavigationBarHeight(this)); }
			}
		}

		else if (step == 4)
		{

			View viewTarget = fragment.mSectionsPagerAdapter.getList(fragment.mViewPager.getCurrentItem()).getListItemAddView();

			if (viewTarget != null)
			{
				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  .setTarget(new ViewTarget(viewTarget))
						                                  .setContentTitle("Add new item")
						                                  .setContentText("Click to add a new item to the list.")
						                                  .setStyle(R.style.CustomShowcaseThemeEnd)
						                                  .hideOnTouchOutside()
						                                  .build();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { showcaseView.setPadding(0, 0, 0, UtilClass.getNavigationBarHeight(this)); }
			}
		}
		*/
	}

	public static class TabbedFragment extends Fragment
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

		static Activity activity;
		private View rootView;


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

			rootView = view;
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
			TabbedContentFragment [] tabbedContentFragments;// = new TabbedContentFragment[tab_titles.length];

			public SectionsPagerAdapter(FragmentManager fm)
			{
				super(fm);

				tab_titles = getResources().getStringArray(R.array.list_tab_titles_array);
				tabbedContentFragments = new TabbedContentFragment[tab_titles.length];
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

					tabbedContentFragments[position] = (TabbedContentFragment) fragment;

					return fragment;
				}
				else
				{
					return null;
				}

			}

			public TabbedContentFragment getList(int position)
			{
				return tabbedContentFragments[position];
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

		public static class TabbedContentFragment extends Fragment
		{

			public static final String ARG_SECTION_NUMBER = "section_number";


			private View rootView;
			private RecyclerView mRecyclerView;
			//private RecyclerView.Adapter mWrappedAdapter;
			private RecyclerViewDragDropManager mRecyclerViewDragDropManager;

			private ManageListsAdapter mListAdapter;

			private LinearLayoutManager mLinearLayoutManager;

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
				mLinearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
				mRecyclerView.setLayoutManager(mLinearLayoutManager);
				//mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
				//mRecyclerView.setItemAnimator(new DefaultItemAnimator());
				mRecyclerView.setItemAnimator(null);

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
					listCursor = activity.getContentResolver().query(tableURI, null, null, null, CasesProvider.KEY_ORDER);
				}
				else
				{
					listCursor = null;
				}

				// drag & drop manager
				mRecyclerViewDragDropManager = new RecyclerViewDragDropManager();
				mRecyclerViewDragDropManager.setSwapTargetTranslationInterpolator(new BasicSwapTargetTranslationInterpolator());
				mRecyclerViewDragDropManager.setDraggingItemShadowDrawable(
						                                                          (NinePatchDrawable) getResources().getDrawable(R.drawable.drawer_shadow));  //change


				//final ManageListsAdapter mListAdapter = new ManageListsAdapter(getActivity(), listCursor);
				mListAdapter = new ManageListsAdapter(getActivity(), listCursor);

				mListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
				{
					@Override
					public void onItemRangeChanged(int positionStart, int itemCount)
					{
						super.onItemRangeChanged(positionStart, itemCount);
						//UtilClass.showSnackbar(getActivity(), "item Changed: position: " + positionStart + ", item: " + mListAdapter.getItem(positionStart));

						// changed item text
						String newItemString = mListAdapter.getItem(positionStart);

						// put data into "values" for database insert/update
						ContentValues values = new ContentValues();

						values.put(tableKEY, newItemString);
						values.put(CasesProvider.KEY_LIST_ITEM_IS_HIDDEN, mListAdapter.getIsHidden(positionStart));

						Uri row_uri = ContentUris.withAppendedId(tableURI, mListAdapter.getKey(positionStart));
						activity.getContentResolver().update(row_uri, values, null, null);

					}

					@Override
					public void onItemRangeInserted(int positionStart, int itemCount)
					{
						super.onItemRangeInserted(positionStart, itemCount);
						//UtilClass.showSnackbar(getActivity(), "onItemRangeInserted: positionStart: " + positionStart + ", item: " + mListAdapter.getItem(positionStart));

						// new item text
						String newItemString = mListAdapter.getItem(positionStart);

						// put data into "values" for database insert/update
						ContentValues values = new ContentValues();
						values.put(tableKEY, newItemString);
						values.put(CasesProvider.KEY_ORDER, positionStart);

						// Add a new list item into the database
						Uri new_item_uri = activity.getContentResolver().insert(tableURI, values);

						// get the key_id of the new case
						long key_id = ContentUris.parseId(new_item_uri);

						// set key
						mListAdapter.setKey(positionStart, (int)key_id);

					}

					@Override
					public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount)
					{
						// don't move last position (add new custom item)
					//	if(fromPosition >= mListAdapter.getList().size()-1 || toPosition >= mListAdapter.getList().size()-1)
					//		return;

						super.onItemRangeMoved(fromPosition, toPosition, itemCount);
						//UtilClass.showSnackbar(getActivity(), "onItemRangeMoved: fromPosition: " + fromPosition + ", toPosition: " + toPosition + ", itemCount: " + itemCount);

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
							activity.getContentResolver().update(row_uri, values, null, null);

							//UtilClass.showSnackbar(getActivity(), "item moved: Position: " + i + ", item: " + mListAdapter.getItem(i));
						}

						// tell adapter to keep animated list changes
						mListAdapter.notifyDataSetChanged();

					}

					@Override
					public void onItemRangeRemoved(int positionStart, int itemCount)
					{
						super.onItemRangeRemoved(positionStart, itemCount);
					//	UtilClass.showSnackbar(getActivity(), "onItemRangeRemoved: positionStart: " + positionStart + ", itemCount: " + itemCount);

						// delete from database
						Uri row_uri = ContentUris.withAppendedId(tableURI, mListAdapter.getKey(positionStart));
						activity.getContentResolver().delete(row_uri, null, null);

						// delete list item
						mListAdapter.removeItem(positionStart);

					}

				});

				RecyclerView.Adapter mWrappedAdapter;
				mWrappedAdapter = mRecyclerViewDragDropManager.createWrappedAdapter(mListAdapter);      // wrap for dragging

				final GeneralItemAnimator animator = new RefactoredDefaultItemAnimator();


				mRecyclerView.setAdapter(mWrappedAdapter);
				mRecyclerView.setItemAnimator(animator);

				// additional decorations
				//noinspection StatementWithEmptyBody
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					// Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
				} else {
		//			mRecyclerView.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z1)));
				}
				mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(getResources().getDrawable(R.drawable.list_divider), true));

				mRecyclerViewDragDropManager.attachRecyclerView(mRecyclerView);

				return rootView;
			}

			// For ShowcaseView tutorial
			public View getListItemTextView()
			{
				//return ((ManageListsAdapter)mRecyclerView.getAdapter()).getFirstViewHolder().mTextView;
				if(mListAdapter.getFirstViewHolder() != null)
				{
					return mListAdapter.getFirstViewHolder().mTextView;
				}
				else
				{
					return null;
				}
			}
			public void scrollToAddView()
			{
				mLinearLayoutManager.scrollToPosition(mListAdapter.getLastViewPosition());
				mListAdapter.notifyDataSetChanged();
			}
			public View getListItemAddView()
			{
				if(mListAdapter.getLastViewHolder() != null)
				{
					return mListAdapter.getLastViewHolder().mTextView;
				}
				else
				{
					return null;
				}
			}
			public View getListItemHandleView()
			{
				if(mListAdapter.getFirstViewHolder() != null)
				{
					return mListAdapter.getFirstViewHolder().mHandle;
				}
				else
				{
					return null;
				}
			}

		} // end TabbedContentFragment

	}

}
