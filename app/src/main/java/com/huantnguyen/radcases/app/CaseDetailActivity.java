package com.huantnguyen.radcases.app;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.manuelpeinado.fadingactionbar.FadingActionBarHelper;

import static android.view.View.GONE;


/**
 * An activity representing a single Case detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link CaseCardListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link CaseDetailFragment}.
 */
public class CaseDetailActivity extends NavigationDrawerActivity
{

	private CaseDetailFragment fragment;
	private long key_id;

	public static final String ARG_HAS_IMAGE = "com.huantnguyen.radcases.ARG_HAS_IMAGE";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		boolean hasImage;

		key_id = getIntent().getLongExtra(CaseCardListActivity.ARG_KEY_ID, -1);
		hasImage = getIntent().getBooleanExtra(ARG_HAS_IMAGE, false);

		// get db row of clicked case
		Uri uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
		Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);
		cursor.moveToFirst();
		String filename = cursor.getString(CasesProvider.COL_IMAGE_COUNT);
		cursor.close();
		//if(filename == null || filename.contentEquals(""))
		if(!hasImage)
		{
			// set back to normal theme
			setTheme(R.style.AppTheme);
			super.onCreate(savedInstanceState);
		}
		else
		{
			// translucent and overlying action bar theme set in manifest XML

			// for FadingActionBar, add extra top margin to navigation drawer to compensate for transparent overlying action bar
			super.onCreate_for_FAB(savedInstanceState);
		}

		//super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_case_detail);


		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null)
		{
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.

			Bundle arguments = new Bundle();
			arguments.putLong(CaseCardListActivity.ARG_KEY_ID,
					                 getIntent().getLongExtra(CaseCardListActivity.ARG_KEY_ID, -1));
			arguments.putBoolean(ARG_HAS_IMAGE, hasImage);
			fragment = new CaseDetailFragment();
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();

		}
	}

	// ACTION MENU
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.case_detail, menu);
		MenuItem star = menu.findItem(R.id.action_star);

		// Set the starred icon in action bar
		if (fragment.isStarred())
			star.setIcon(R.drawable.ic_action_important);
		else
			star.setIcon(R.drawable.ic_action_not_important);

		return super.onCreateOptionsMenu(menu);
	}

	static final int REQUEST_EDIT_CASE = 1;

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.

		// Handle presses on the action bar items
		switch (item.getItemId())
		{
			case android.R.id.home:
				//NavUtils.navigateUpFromSameTask(this);
				finish();
				return true;

			case R.id.action_star:
				if (key_id != -1)
				{
//					findViewById(R.id.action_star);
					// toggle favorites star
					fragment.toggleStar();

					// reset the action bar to reflex new Star state
					invalidateOptionsMenu();
				}

				return true;

			case R.id.action_edit:
				if (key_id != -1)
				{
					// open CaseEditActivity, giving the CASE key_id argument
					Intent intent = new Intent(this, CaseEditActivity.class);
					intent.putExtra(CaseCardListActivity.ARG_KEY_ID, key_id);

					startActivityForResult(intent, REQUEST_EDIT_CASE);
				}

				return true;

			case R.id.action_camera:
				Toast.makeText(this, "debug: Camera function...", Toast.LENGTH_SHORT).show();
				return true;

			case R.id.action_delete:
				if (key_id != -1)
				{
					// opens alert dialog to confirm delete
					// delete from CASES, IMAGES, and image files
					// setResult() and finish()
					UtilClass.menuItem_deleteCase(this, key_id);
				}

				return true;

			case R.id.action_search:
				//openSearch();

				//String query = intent.getStringExtra(SearchManager.QUERY);
				//Cursor c = db.getWordMatches(query, null);

				Toast.makeText(this, "debug: Search function...", Toast.LENGTH_SHORT).show();
				return true;

			case R.id.action_settings:
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
			case REQUEST_EDIT_CASE:

				// return the result code from CaseEditActivity back, so CaseCardList will know to refresh
				setResult(resultCode);

				if(resultCode == CaseCardListActivity.RESULT_DELETED)
				{
					// case has been deleted.  return to case list.
					finish();
				}
				else if(resultCode == CaseCardListActivity.RESULT_EDITED)
				{
					// refresh data
					fragment.populateFields();
				}

				break;

			default:

				break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * A fragment representing a single Case detail screen.
	 * This fragment is either contained in a {@link CaseCardListActivity}
	 * in two-pane mode (on tablets) or a {@link CaseDetailActivity}
	 * on handsets.
	 */
	public class CaseDetailFragment extends Fragment
	{
		/**
		 * The fragment argument representing the item ID that this fragment
		 * represents.
		 */

		//public static final String ARG_HAS_IMAGE = "boolean_has_image";

		private long selected_key_id;
		//private static Cursor case_cursor;
		//private static Cursor image_cursor;

		private FadingActionBarHelper mFadingHelper;
		private Bundle mArguments;

		private View headerView;
		private boolean hasImage;

		private int favorite; // for action menu toggle in CaseDetail Activity (not in this fragment)

		// Hold a reference to the current animator,
		// so that it can be canceled mid-way.
		private Animator mCurrentAnimator;

		/**
		 * Mandatory empty constructor for the fragment manager to instantiate the
		 * fragment (e.g. upon screen orientation changes).
		 */
		public CaseDetailFragment()
		{
		}


		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);

			mArguments = getArguments();
			hasImage = mArguments.getBoolean(ARG_HAS_IMAGE);

			if(hasImage)
			{
				//FadingActionBar
				//mArguments = getArguments();
				//int actionBarBg = mArguments != null ? mArguments.getInt(ARG_ACTION_BG_RES) : R.drawable.ab_background_light;

				mFadingHelper = new FadingActionBarHelper()
						                .actionBarBackground(R.drawable.ab_background_gray)
						                //.actionBarBackground(R.drawable.ab_background_blue)
						                .headerLayout(R.layout.activity_case_detail_header)
						                .contentLayout(R.layout.fragment_case_detail)
				;

				mFadingHelper.initActionBar(activity);
			}
		}

		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
		                         Bundle savedInstanceState)
		{

			View view;

			if(hasImage)
			{
				//FadingActionBar
				view = headerView = mFadingHelper.createView(inflater);
			}
			else
			{
				// standard ActionBar
				view = inflater.inflate(R.layout.fragment_case_detail, container, false);
			}

			return view;
		}



		@Override
		public void onSaveInstanceState(Bundle outState)
		{
			super.onSaveInstanceState(outState);
		}

		@Override
		public void onResume() {
			super.onResume();


			// if added image, need to redo FadingActionBar
			populateFields();
		}

		public void populateFields()
		{

			View rootView = getView();

			// get the CASE key_id
			if (getArguments().containsKey(CaseCardListActivity.ARG_KEY_ID))
			{
				selected_key_id = getArguments().getLong(CaseCardListActivity.ARG_KEY_ID);

			}
			else    //TODO show error message (no selected key id from listview)
			{
				selected_key_id = -1;
				return;
			}

			// get db row of clicked case
			Uri uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, selected_key_id);
			Cursor case_cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

			if (case_cursor.moveToFirst())
			{
				int key_id = case_cursor.getInt(CasesProvider.COL_ROWID);
				String patient_id = case_cursor.getString(CasesProvider.COL_PATIENT_ID);
				String diagnosis = case_cursor.getString(CasesProvider.COL_DIAGNOSIS);
				String findings = case_cursor.getString(CasesProvider.COL_FINDINGS);
				String section = case_cursor.getString(CasesProvider.COL_SECTION);
				String comments = case_cursor.getString(CasesProvider.COL_COMMENTS);
				String key_words = case_cursor.getString(CasesProvider.COL_KEYWORDS);
				String biopsy = case_cursor.getString(CasesProvider.COL_BIOPSY);
				String followup = case_cursor.getString(CasesProvider.COL_FOLLOWUP_COMMENT);
				boolean followup_bool;
				if(case_cursor.getInt(CasesProvider.COL_FOLLOWUP)==1)
					followup_bool=true;
				else
					followup_bool=false;

				String study_type = case_cursor.getString(CasesProvider.COL_STUDY_TYPE);
				String date_str = case_cursor.getString(CasesProvider.COL_DATE);
				String[] imageFilename = new String[CasesProvider.MAX_NUM_IMAGES];
				int imageCount = case_cursor.getInt(CasesProvider.COL_IMAGE_COUNT);

				// set global variable isStarred for Activity action bar menu toggle
				favorite = case_cursor.getInt(CasesProvider.COL_FAVORITE);

				// ACTION BAR Title
				// mTitle declared in super NavigationDrawerActivity, and used to restore the same title after nav drawer is closed
				if(patient_id != null && !patient_id.isEmpty())
				{
					mTitle = patient_id;

					//if(section != null && !section.isEmpty())
					//	actionBar.setSubtitle(section);
				}
				else if(section != null && !section.isEmpty())
				{
					mTitle = section + " Case";
				}
				else if(diagnosis != null && !diagnosis.isEmpty())
				{
					mTitle = diagnosis;
				}
				else
				{
					mTitle = "Case Details";
				}

				ActionBar actionBar = getActivity().getActionBar();
				actionBar.setTitle(mTitle);

				// Case Information (DIAGNOSIS and FINDINGS)
				TextView TV_case_info1 = (TextView) rootView.findViewById(R.id.detail_case_info1);
				TextView TV_case_info2 = (TextView) rootView.findViewById(R.id.detail_case_info2);

				if(diagnosis != null && !diagnosis.isEmpty())
				{
					TV_case_info1.setText(diagnosis);
					TV_case_info1.setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.CaseInfoLabel)).setVisibility(View.VISIBLE);

					// if both Diagnosis and Findings fields retrieved, set subtext as the Findings
					if(findings != null)    // diagnosis and findings
					{
						TV_case_info2.setText(findings);
						TV_case_info2.setVisibility(View.VISIBLE);
					}
					else    // Diagnosis and no Findings
					{
						TV_case_info2.setVisibility(GONE);
					}
				}
				else if(findings != null && !findings.isEmpty())   // no Diagnosis, there are Findings
				{
					// if no Diagnosis, only Findings
					// set label to say "FINDINGS"
					((TextView) rootView.findViewById(R.id.CaseInfoLabel)).setText("FINDINGS");
					((TextView) rootView.findViewById(R.id.CaseInfoLabel)).setVisibility(View.VISIBLE);
					// set the main CaseInfo text to be the Findings
					TV_case_info1.setText(findings);
					TV_case_info1.setVisibility(View.VISIBLE);

					// don't put anything in the subtext
					TV_case_info2.setVisibility(GONE);
				}
				else    // no Diagnosis, no Findings
				{
					// if both Diagnosis and Findings fields are empty, then hide CaseInfoLabel
					TV_case_info1.setVisibility(GONE);
					TV_case_info2.setVisibility(GONE);
					((TextView) rootView.findViewById(R.id.CaseInfoLabel)).setVisibility(GONE);
				}

				// SECTION
				TextView TV_section = (TextView) rootView.findViewById(R.id.detail_section);
				if(section != null && !section.isEmpty())
				{
					TV_section.setText(section);
					TV_section.setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.SectionLabel)).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_section.setVisibility(GONE);
					((TextView) rootView.findViewById(R.id.SectionLabel)).setVisibility(GONE);
				}

				// STUDY TYPE
				TextView TV_study_type = (TextView) rootView.findViewById(R.id.detail_study_type);
				if(study_type != null && !study_type.isEmpty())
				{
					TV_study_type.setText(study_type);
					TV_study_type.setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.StudyLabel)).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_study_type.setVisibility(GONE);
					((TextView) rootView.findViewById(R.id.StudyLabel)).setVisibility(GONE);
				}

				// STUDY DATE
				TextView TV_date = (TextView) rootView.findViewById(R.id.detail_date);
				if(date_str != null && !date_str.isEmpty())
				{
					TV_date.setText(UtilClass.convertDateString(date_str, "yyyy-MM-dd", "MMMM d, yyyy"));
					TV_date.setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.StudyLabel)).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_date.setVisibility(GONE);
				}

				// BIOPSY
				TextView TV_biopsy = (TextView) rootView.findViewById(R.id.detail_biopsy);
				if (biopsy != null && !biopsy.isEmpty())
				{
					TV_biopsy.setText(biopsy);
					TV_biopsy.setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.BiopsyLabel)).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_biopsy.setVisibility(GONE);
					((TextView) rootView.findViewById(R.id.BiopsyLabel)).setVisibility(GONE);
				}

				// KEY IMAGES
				if (imageCount > 0 && hasImage)  //hasImage to keep crash if FadingActionBar hasn't been done yet
				{

					// get all of the images linked to this case _id
					String [] image_args = {String.valueOf(selected_key_id)};
					Cursor image_cursor = getActivity().getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

					// set image for FadingActionBar.  first image in cursor array
					image_cursor.moveToFirst();
					String headerImageFilename = image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);
					ImageView headerImageView = (ImageView) headerView.findViewById(R.id.image_header);
					UtilClass.setPic(headerImageView, headerImageFilename, UtilClass.IMAGE_SIZE);

					ImageGridView imageGridView = new ImageGridView(getActivity(),(GridView)rootView.findViewById(R.id.imageGridview), selected_key_id, image_cursor);

					image_cursor.close();

					rootView.findViewById(R.id.ImagesLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					rootView.findViewById(R.id.ImagesLabel).setVisibility(View.GONE);
				}



				// KEYWORD_LIST
				TextView TV_key_words = (TextView) rootView.findViewById(R.id.detail_key_words);
				if (key_words != null && !key_words.isEmpty())
				{
					TV_key_words.setText(key_words);
					TV_key_words.setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.KeyWordsLabel)).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_key_words.setVisibility(GONE);
					((TextView) rootView.findViewById(R.id.KeyWordsLabel)).setVisibility(GONE);
				}

				// FOLLOWUP
				TextView TV_followup = (TextView) rootView.findViewById(R.id.detail_followup);
				if(followup_bool)
				{
					if (followup != null && !followup.isEmpty())
					{
						TV_followup.setText(followup);
						TV_followup.setVisibility(View.VISIBLE);
					}
					else
					{
						// follow up comment not specified.  put generic comment.
						TV_followup.setText("Needs follow up");
						TV_followup.setVisibility(View.VISIBLE);
					}
					((TextView) rootView.findViewById(R.id.FollowupLabel)).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_followup.setVisibility(GONE);
					((TextView) rootView.findViewById(R.id.FollowupLabel)).setVisibility(GONE);
				}

				// COMMENTS
				TextView TV_comments = (TextView) rootView.findViewById(R.id.detail_comments);
				if (comments != null && !comments.isEmpty())
				{
					TV_comments.setText(comments);
					TV_comments.setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.CommentsLabel)).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_comments.setVisibility(GONE);
					((TextView) rootView.findViewById(R.id.CommentsLabel)).setVisibility(GONE);
				}

			}

			case_cursor.close();
		}

		public boolean isStarred()
		{
			if(favorite == 1)
				return true;
			else
				return false;
		}
		public void toggleStar()
		{
			if(favorite == 0)
				favorite = 1;
			else
				favorite = 0;

			// update database
			ContentValues values = new ContentValues();

			// put data into "values" for database insert/update
			values.put(CasesProvider.KEY_FAVORITE, favorite);
			Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, selected_key_id);
			getActivity().getContentResolver().update(row_uri, values, null, null);
		}
	}
}
