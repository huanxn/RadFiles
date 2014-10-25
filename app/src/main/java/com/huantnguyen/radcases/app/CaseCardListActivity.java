package com.huantnguyen.radcases.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;

//import com.timehop.stickyheadersrecyclerview.*;


/**
 * Created by Huan on 6/12/2014.
 */
public class CaseCardListActivity extends NavigationDrawerActivity
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

	// intent results
	public final static int RESULT_NOCHANGE = 0;
	public final static int RESULT_EDITED = 1;
	public final static int RESULT_DELETED = 2;

	// intent arguments
	public final static String ARG_KEY_ID = "com.huantnguyen.radcases.ARG_KEY_ID";

	// saved state argument for action bar spinner
	private static final String CURRENT_SPINNER_STATE = "spinner_state";

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
			SpinnerActionBar actionbarSpinnerAdapter = new SpinnerActionBar(getActionBar().getThemedContext(), R.layout.spinner_actionbar, "Cases", getResources().getStringArray(R.array.actionbar_sort_list));
			((ArrayAdapter) actionbarSpinnerAdapter).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setListNavigationCallbacks(actionbarSpinnerAdapter, new ActionBar.OnNavigationListener()
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

		// TODO add searchable code here
		// Get the SearchView and set the searchable configuration
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
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
				onSearchRequested();
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

	final static int REQUEST_CASE_DETAILS = 1;
	final static int REQUEST_ADD_CASE = 2;


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
					ActionBar actionBar = getActionBar();
					actionBar.setSelectedNavigationItem(FILTER_RECENT);
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
		ActionBar actionBar = getActionBar();
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
		outState.putInt(CURRENT_SPINNER_STATE, getActionBar().getSelectedNavigationIndex());
	}


	/**
	 * Placeholder fragment
	 */
	public static class PlaceholderFragment extends Fragment
	{
		// Layout
		private RecyclerView mRecyclerView;
		private CaseCardAdapter mAdapter;

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
			mAdapter = new CaseCardAdapter(getActivity(), null, R.layout.card_case);

			mRecyclerView.setAdapter(mAdapter);

			//sticky headers
			// type2
			StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mAdapter);
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
		 */
		private void populateCards()
		{
			// Get cases, filtered/sorted by spinner list in action bar
			MergeCursor case_cursor;

			Cursor [] case_cursor_array;
			String [] group_header; //name of header
			int [] case_count_in_group; //number of cases in each filter group

			switch(caseFilterMode)
			{
				case FILTER_SECTION:

					// get cursor of "Radiology Section List", in order determined by user list preferences
					Cursor section_cursor = getActivity().getBaseContext().getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);

					// instantiate case cursor array with size of section_cursor.getCount() + 1.  Added one for those cases with empty "Radiology Section" field, ie Unknown
					case_cursor_array = new Cursor[section_cursor.getCount()+1];
					group_header = new String[section_cursor.getCount()+1];
					case_count_in_group = new int[section_cursor.getCount()+1];

					// query each case cursor by "Radiology Section" "LIKE %?%".  Each case may have more than one "Radiology Section" listed. Order by desc "Study Date"
					if(section_cursor.moveToFirst())
					{
						int i = 0;  // section/group counter

						do
						{
							// get the section name
							String mSection = section_cursor.getString(CasesProvider.COL_VALUE);

							// store the section name in the group_header array
							if(mSection != null && !mSection.isEmpty())
							{
								group_header[i] = mSection;

							}
							else
							{
								// shouldn't ever go here
								group_header[i] = "Unknown";
							}

							// find all cases with this KEY_SECTION
							case_cursor_array[i] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_SECTION + " LIKE ?", new String[] {"%"+ mSection +"%"}, CasesProvider.KEY_DATE + " DESC", null);
							// store the number of cases with this KEY_SECTION; used to determine when to switch sticky header in the list
							case_count_in_group[i] = case_cursor_array[i].getCount();

							i = i + 1;
						} while(section_cursor.moveToNext());

						// last filter group is the cases with empty "Radiology Section" fields
						case_cursor_array[i] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_SECTION + " IS NULL OR " + CasesProvider.KEY_SECTION + " = ?", new String[] {""}, CasesProvider.KEY_DATE + " DESC", null);
						case_count_in_group[i] = case_cursor_array[i].getCount();
						group_header[i] = EMPTY_FIELD_GROUP_HEADER;
					}

					section_cursor.close();

					break;

				case FILTER_RECENT:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, null, null, CasesProvider.KEY_ROWID + " DESC", null );

					group_header = new String[1];
					group_header[0] = "Recent";

					case_count_in_group = new int[1];
					case_count_in_group[0] = case_cursor_array[0].getCount();

					break;

				case FILTER_STUDYDATE:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, null, null, CasesProvider.KEY_DATE + " DESC", null);

					group_header = new String[1];
					group_header[0] = "Date";

					case_count_in_group = new int[1];
					case_count_in_group[0] = case_cursor_array[0].getCount();

					break;
				case NAV_KEYWORDS:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_KEYWORDS + " is not null and " + CasesProvider.KEY_KEYWORDS + " != ?", new String[] {""}, CasesProvider.KEY_KEYWORDS, null);

					group_header = new String[1];
					group_header[0] = "keywords";

					case_count_in_group = new int[1];
					case_count_in_group[0] = case_cursor_array[0].getCount();

					break;
				case FILTER_FOLLOWUP:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FOLLOWUP + " = ?", new String[] {"1"}, CasesProvider.KEY_DATE + " DESC", null);

					group_header = new String[1];
					group_header[0] = "followup";

					case_count_in_group = new int[1];
					case_count_in_group[0] = case_cursor_array[0].getCount();

					break;
				case FILTER_FAVORITE:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_FAVORITE + " = ?", new String[] {"1"}, CasesProvider.KEY_DATE + " DESC", null);

					group_header = new String[1];
					group_header[0] = "fav";

					case_count_in_group = new int[1];
					case_count_in_group[0] = case_cursor_array[0].getCount();

					break;
				default:
					case_cursor_array = new Cursor[1];
					case_cursor_array[0] = getActivity().getBaseContext().getContentResolver().query(CasesProvider.CASES_URI, null, null, null, null, null);

					group_header = new String[1];
					group_header[0] = "default";

					case_count_in_group = new int[1];
					case_count_in_group[0] = case_cursor_array[0].getCount();

					break;

			}

			case_cursor = new MergeCursor(case_cursor_array);
			// TODO how to close this cursor?


			// set the headers for StickyRecyclerHeaders
			List<String> headerList = new ArrayList<String>();
			List<Long> headerID = new ArrayList<Long>();

			for (int g = 0; g < group_header.length; g++)
			{
				for(int i = 0; i < case_count_in_group[g]; i ++)
				{
					headerList.add(group_header[g]);
					//headerID.add((long)g);
					headerID.add((long)group_header[g].hashCode());

				}
			}

			mAdapter.setHeaderList(headerList, headerID);
			mAdapter.loadCases(case_cursor);
/*
			ArrayList<Card> cards = new ArrayList<Card>();
			final StickyCardArrayAdapter_Kitkat mCardArrayAdapter = new StickyCardArrayAdapter_Kitkat(getActivity(), cards);

			// loop through case cursor and put info into cards
			if (case_cursor.moveToFirst())
			{
				int case_counter = 0;
				int group_counter = 0;

				String prior_str = "";

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

					int thumbnail = 0;
					String thumbnailString = case_cursor.getString(CasesProvider.COL_THUMBNAIL);
					if(thumbnailString != null && !thumbnailString.isEmpty())
						thumbnail = Integer.parseInt(thumbnailString);

					//int imageCount = case_cursor.getInt(CasesProvider.COL_IMAGE_COUNT);

					// Create a Card
					CaseCard_Kitkat case_card = new CaseCard_Kitkat(getActivity());

					// Set unique id, will be passed to detail activity
					case_card.setId(Integer.toString(key_id));

					// Create a CardHeader
					//CardHeader header = new CardHeader(this);
					// Add Header to card
					//header.setTitle(patient_id);
					//card.addCardHeader(header);


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


					// set the StickyCard List group
					switch(caseFilterMode)
					{
						case FILTER_SECTION:

							// find next non-empty group
							while(case_counter >= case_count_in_group[group_counter])
							{
								group_counter += 1;
								case_counter = 0;
							}

							case_card.setGroup(group_counter);
							case_counter += 1;

							case_card.setGroupHeader(group_header[group_counter]);
							break;

						case FILTER_STUDYDATE:

							if(db_date_str != null && !db_date_str.isEmpty())
							{
								String new_str = UtilClass.convertDateString(db_date_str, "yyyy-MM-dd", "MMMM yyyy");
								case_card.setGroupHeader(new_str);

								// if different date, then put in next filter group
								if (!new_str.contentEquals(prior_str))
									group_counter += 1;
								case_card.setGroup(group_counter);

								// for next iteration check
								prior_str = new_str;
							}
							else
							{
								case_card.setGroupHeader(EMPTY_FIELD_GROUP_HEADER);
							}

							break;

						// TODO  other filter types

						default:
							case_card.setGroupHeader(group_header[group_counter]);
							break;

					}

					// set star
					if(favorite == 1)
						case_card.setStar(true);


					// get images for this case
					String[] image_args = {String.valueOf(key_id)};
					Cursor image_cursor = getActivity().getBaseContext().getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

					if(image_cursor.getCount() > 0 && image_cursor.moveToFirst())
					{
						if(thumbnail < image_cursor.getCount())
						{
							image_cursor.move(thumbnail);
						}
						String imageFilename = picturesDir + "/" + image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);

						CaseCard_Kitkat.MyThumbnail cardThumbnail = new CaseCard_Kitkat.MyThumbnail(getActivity(), imageFilename);
						//You need to set true to use an external library
						cardThumbnail.setExternalUsage(true);

						case_card.addCardThumbnail(cardThumbnail);

					}
					image_cursor.close();

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
								// fading action bar
								detailIntent.putExtra(CaseDetailActivity.ARG_HAS_IMAGE, true);
							}

							mActivity.startActivityForResult(detailIntent, REQUEST_CASE_DETAILS);
						}
					});

					// set the OnLongClickListener: opens alert dialog
					case_card.setOnLongClickListener(new Card.OnLongCardClickListener()
					{
						@Override
						public boolean onLongClick(Card card, View view)
						{

							//return mCardArrayAdapter.startActionMode(getActivity());


							// declared "final" for access from within alert dialog
							final long key_id = Long.parseLong(card.getId());
							final StickyCardArrayAdapter_Kitkat mAdapter = mCardArrayAdapter;

							AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
							CharSequence[] imageSources = {"Edit", "MultiChoice", "Share", "Cancel"};
							builder.setItems(imageSources, new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog, int index)
								{
									switch (index)
									{
										// Edit Case
										case 0:
											// open CaseEditActivity, giving the CASE key_id argument
											Intent intent = new Intent(getActivity(), CaseEditActivity.class);
											intent.putExtra(CaseCardListActivity.ARG_KEY_ID, key_id);

											mActivity.startActivityForResult(intent, CaseDetailActivity.REQUEST_EDIT_CASE);

											break;

										// multichoice
										case 1:
											// multichoice
											// returns boolean
							//				mAdapter.startActionMode(getActivity());

											break;

										// Share Case
										case 2:
											String exportFilename = "test";
											List<Long> caseList = new ArrayList<Long>();
											caseList.add(key_id);

											File exportFile = UtilClass.exportCasesCSV(getActivity(), exportFilename, caseList);

											UtilClass.showMessage(getActivity(), "Exported case to " + exportFile.getPath());
											break;

										// Cancel.  Do Nothing.
										case 3:
											break;

									}
								}
							});

							AlertDialog alert = builder.create();
							alert.show();

							return true; // not sure what this does TODO

						}
					});

					cards.add(case_card);

				} while (case_cursor.moveToNext());
			}
*/
/*
			//StickyCardArrayAdapter_Kitkat mCardArrayAdapter = new StickyCardArrayAdapter_Kitkat(getActivity(), cards);
			StickyCardListView_Kitkat listView = (StickyCardListView_Kitkat) rootView.findViewById(R.id.myList);

			if (listView != null)
			{

			//SwingBottomInAnimationAdapter animCardArrayAdapter = new SwingBottomInAnimationAdapter(mCardArrayAdapter);
			//animCardArrayAdapter.setAbsListView((listView);
			//listView.setExternalAdapter(animCardArrayAdapter, mCardArrayAdapter);

				listView.setAdapter(mCardArrayAdapter);
				listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			}
*/
			case_cursor.close();

		}
	}
}
