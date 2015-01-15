package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
//import com.manuelpeinado.fadingactionbar.extras.actionbarcompat.FadingActionBarHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import eu.janmuller.android.simplecropimage.CropImage;

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
	private CaseDetailFragment fragment = null;
	private long key_id = -1;

	public static final String ARG_HAS_IMAGE = "com.huantnguyen.radcases.ARG_HAS_IMAGE";

	static final int REQUEST_EDIT_CASE = 21;

	private Intent starterIntent; // to recreate Activity if theme/style/fadingactionbar changes due to add/removal of images/header
	boolean hasImage;

	private File tempImageFile;                                 // new image from camera

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		starterIntent = getIntent();    // save original intent info
		key_id = getIntent().getLongExtra(CaseCardListActivity.ARG_KEY_ID, -1);
		//hasImage = getIntent().getBooleanExtra(ARG_HAS_IMAGE, false);

		if(key_id == -1)
		{
			UtilClass.showMessage(this, "debug: CaseDetail key_id = -1");
			finish();
			return;
		}

		String [] image_args = {String.valueOf(key_id)};

		// see how many images are linked to this case to determine if fading toolbar image header should be used
		Cursor imageCursor = getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, null);

		if (imageCursor.getCount() > 0)
		{
			hasImage = true;
			// FadingActionBar
			// translucent and overlying action bar theme set as default for this CaseDetailActivity in manifest XML
			super.onCreate_for_FAB(savedInstanceState);

			/*
			// set back icon
			getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
			*/
		}
		else
		{
			hasImage = false;

			// Set back to normal theme
			setTheme(R.style.MaterialTheme_Light);
			super.onCreate(savedInstanceState, false);

		}

		setDrawerPosition(NavigationDrawerActivity.POS_NONE);

		//super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_case_detail);


		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		//if (savedInstanceState == null)
		{

			if(fragment != null)
			{
				getFragmentManager().beginTransaction().remove(fragment).commit();
			}

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

	//todo change to just do headerview update
	public void reloadHeaderView(int thumbnail)
	{
		fragment.populateHeader(thumbnail);
	}

	// ACTION MENU
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.case_detail, menu);
		MenuItem star = menu.findItem(R.id.menu_star);

		// Set the starred icon in action bar
		if (fragment.isStarred())
			star.setIcon(R.drawable.ic_action_important);
		else
			star.setIcon(R.drawable.ic_action_not_important);

		return super.onCreateOptionsMenu(menu);
	}

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

			case R.id.menu_star:
				if (key_id != -1)
				{
//					findViewById(R.id.action_star);
					// toggle favorites star
					fragment.toggleStar();

					// reset the action bar to reflex new Star state
					invalidateOptionsMenu();
				}

				return true;

			case R.id.menu_edit:
				if (key_id != -1)
				{
					// open CaseEditActivity, giving the CASE key_id argument
					Intent intent = new Intent(this, CaseEditActivity.class);
					intent.putExtra(CaseCardListActivity.ARG_KEY_ID, key_id);

					startActivityForResult(intent, REQUEST_EDIT_CASE);
				}

				return true;

			case R.id.menu_camera:
				choosePictureAlertDialog(item.getActionView());
				return true;

			case R.id.menu_delete:
				if (key_id != -1)
				{
					// opens alert dialog to confirm delete
					// delete from CASES, IMAGES, and image files
					// setResult() and finish()
					UtilClass.menuItem_deleteCase(this, key_id);
				}

				return true;

			case R.id.menu_search:
				//openSearch();

				//String query = intent.getStringExtra(SearchManager.QUERY);
				//Cursor c = db.getWordMatches(query, null);

				Toast.makeText(this, "debug: Search function...", Toast.LENGTH_SHORT).show();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}

	}

	/**
	 * Called when clicking to add new image
	 * opens alert dialog to choose from file/gallery or take a photo with camera intent
	 * @param view
	 */
	public void choosePictureAlertDialog(View view)
	{
		final Activity activity = this;

		// Alert dialog to choose either to take a new photo with camera, or select existing picture from file storage
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		CharSequence[] imageSources = {"Take photo", "Choose file"};
		builder.setTitle("Add image")
				.setItems(imageSources, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int index)
					{

						switch(index)
						{
							// take new photo with the camera intent
							// then crop photo
							// store photo and put filename in database
							case 0:
								// runs camera intent.  returns result code to onActivityResult, which will run crop intent if successful
								tempImageFile = UtilClass.getPictureFromCamera(activity, CaseEditActivity.REQUEST_IMAGE_CAPTURE);
								break;

							// select file from chooser
							// either local database or TODO cloud storage
							case 1:
								// in onCreate or any event where your want the user to
								// select a file
								Intent intent = new Intent();
								intent.setType("image/*");
								intent.setAction(Intent.ACTION_GET_CONTENT);
								//intent.putExtra("image_filename", tempImageFilename);
								startActivityForResult(Intent.createChooser(intent,"Select Picture"), CaseEditActivity.REQUEST_SELECT_IMAGE_FROM_FILE);

								break;

						}

					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_EDIT_CASE)
		{

			// return the result code from CaseEditActivity back, so CaseCardList will know to refresh
			setResult(resultCode);

			if (resultCode == CaseCardListActivity.RESULT_DELETED)
			{
				// case has been deleted.  return to case list.
				finish();
			}
			else if (resultCode == CaseCardListActivity.RESULT_EDITED)
			{
				// refresh data
				fragment.populateFields();
			}

		}

		// IMAGE CHOOSER (same as CaseEditActivity)
		else if (requestCode == CaseEditActivity.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
		{
			// successful capture of new photo from camera (called from UtilClass method)
			// replaces tempImageFile with the new cropped image
			// if successful, return to onActivityResult(REQUEST_CROP_IMAGE)

			UtilClass.CropPicture(this, tempImageFile, CaseEditActivity.REQUEST_CROP_IMAGE);

		}
		else if (requestCode == CaseEditActivity.REQUEST_CROP_IMAGE && resultCode == RESULT_OK)
		{
			// successful crop of new photo from the camera (called from onActivityResult(REQUEST_IMAGE_CAPTURE)->UtilClass.CropPicture())
			String path = data.getStringExtra(CropImage.IMAGE_PATH);
			if (path != null)
			{
				addNewImageToDatabase(tempImageFile.getPath());
			}
			else
			{
				// delete tempImageFile since crop was canceled?
				if (tempImageFile.exists())
				{
					tempImageFile.delete();
					tempImageFile = null;
				}
			}

		}
		else if (requestCode == CaseEditActivity.REQUEST_SELECT_IMAGE_FROM_FILE && resultCode == RESULT_OK)
		{
			// successful selection of photo from file explorer
			Uri selectedImageUri = data.getData();
			//String originalImageFilename = UtilClass.getFilePathFromResult(this, selectedImageUri);

			// make copy of file into app pictures folder
			//File originalImageFile = new File(originalImageFilename);
			//String newImageFilename = originalImageFile.getName();
			File newImageFile = null;
			try
			{
				newImageFile = File.createTempFile(key_id + "_", ".jpg", CaseCardListActivity.picturesDir);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				UtilClass.showMessage(this, "Unable to create file.");
			}

			FileOutputStream outputStream = null;
			FileInputStream inputStream = null;
			try
			{
				// the selected local or cloud image file
				inputStream = (FileInputStream) getContentResolver().openInputStream(selectedImageUri);

				// new local file
				outputStream = new FileOutputStream(newImageFile);

				// copy backup file contents to local file
				UtilsFile.copyFile(outputStream, inputStream);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				UtilClass.showMessage(this, "File not found.");
			}
			catch (IOException e)
			{
				e.printStackTrace();
				UtilClass.showMessage(this, "File IO exception.");
			}

			addNewImageToDatabase(newImageFile.getPath());
			//imageGridView.addImage(newImageFile.getPath());
		}
		else if (resultCode != RESULT_OK)
		{
			// delete tempImageFile if image capture was canceled
			if (tempImageFile != null && tempImageFile.exists())
			{
				tempImageFile.delete();
				tempImageFile = null;
			}

			UtilClass.showMessage(this, "Canceled");
		}
	}

	private void addNewImageToDatabase(String imageFilepath)
	{
		int new_image_index = fragment.imageGridView.getCount();

		//store in image table
		ContentValues imageValues = new ContentValues();
		imageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, key_id);
		imageValues.put(CasesProvider.KEY_IMAGE_FILENAME, new File(imageFilepath).getName());
		imageValues.put(CasesProvider.KEY_ORDER, new_image_index);      // set order to display images.  new files last.  //todo user reodering

		Uri row_uri = getContentResolver().insert(CasesProvider.IMAGES_URI, imageValues);
		long new_image_id = Long.parseLong(row_uri.getLastPathSegment());

		// show new image in grid display of key images
		fragment.imageGridView.addImage(imageFilepath, new_image_id);

		// update last modified date field
		UtilClass.updateLastModifiedDate(this, key_id);

		setResult(CaseCardListActivity.RESULT_EDITED);

		if(fragment.imageGridView.getCount() > 0 && hasImage == false)
		{
			//reload activity to show new header
			startActivityForResult(starterIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);

			finish();
			return;
		}
	}

	/**
	 * UI clicks
	 */

	public void onClick_Button(View view)
	{
		switch (view.getId())
		{
			case R.id.header_image:
				Intent imageGalleryIntent = new Intent(this, ImageGalleryActivity.class);
				//imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_IMAGE_FILES, fragment.imageGridView.getImageFilepaths());
				imageGalleryIntent.putExtra(CaseCardListActivity.ARG_KEY_ID, key_id);
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_POSITION, fragment.thumbnail_pos);
				startActivity(imageGalleryIntent);
				break;
		}
	}

	/**
	 * A fragment representing a single Case detail screen.
	 * This fragment is either contained in a {@link CaseCardListActivity}
	 * in two-pane mode (on tablets) or a {@link CaseDetailActivity}
	 * on handsets.
	 */
	public class CaseDetailFragment extends Fragment implements ObservableScrollViewCallbacks
	{
		/**
		 * The fragment argument representing the item ID that this fragment
		 * represents.
		 */
		private long selected_key_id;
		private Activity activity;

	//	private FadingActionBarHelper mFadingHelper;
		private View headerView; //fadingactionbar
		//private ImageView headerImageView;

		private Bundle mArguments;

		private boolean hasImage;

		private int favorite; // for action menu toggle in CaseDetail Activity (not in this fragment)

		// parallax fading toolbar
		private ImageView mImageView;
		private Toolbar mToolbar;
		private ObservableScrollView mScrollView;
		private float toolbarAlpha;
		//private int mParallaxImageHeight;

		private int thumbnail_pos;
		private ImageGridView imageGridView;

		// Hold a reference to the current animator,
		// so that it can be canceled mid-way.
		//private Animator mCurrentAnimator;

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

			this.activity = activity;

			mArguments = getArguments();
			hasImage = mArguments.getBoolean(ARG_HAS_IMAGE);
/*
			if(hasImage)
			{
				//FadingActionBar
				//mArguments = getArguments();
				//int actionBarBg = mArguments != null ? mArguments.getInt(ARG_ACTION_BG_RES) : R.drawable.ab_background_light;

				mFadingHelper = new FadingActionBarHelper()
						                .actionBarBackground(R.drawable.ab_background_dark)
						                .headerLayout(R.layout.activity_case_detail_header)
						                .contentLayout(R.layout.fragment_case_detail)
				;

				mFadingHelper.initActionBar(activity);
			}
			*/
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
	//			view = headerView = mFadingHelper.createView(inflater);
				//view.findViewById(R.id.detail_container).setMinimumHeight((int)UtilClass.getDisplayHeight(getActivity()));


				// Fading Toolbar
				view = inflater.inflate(R.layout.fragment_case_detail, container, false);

				mImageView = (ImageView) view.findViewById(R.id.header_image);
				mToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);  // in activity content view
				toolbarAlpha = 0;
				mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, 0));   // transparent
				mToolbar.setTitleTextColor(ScrollUtils.getColorWithAlpha(0, 0));    // transparent
				getActivity().findViewById(R.id.toolbar_dropshadow).setVisibility(View.GONE);

				mScrollView = (ObservableScrollView) view.findViewById(R.id.scroll);
				mScrollView.setScrollViewCallbacks(this);

				Point size = new Point();
				getWindowManager().getDefaultDisplay().getSize(size);

				int test2 = UtilClass.get_attr(activity, R.attr.actionBarSize);
				int test = UtilClass.convertDpToPixels(activity, UtilClass.get_attr(activity, R.attr.actionBarSize));



				int test3 = mToolbar.getHeight();

				view.findViewById(R.id.detail_container).setMinimumHeight(size.y - UtilClass.getToolbarHeight(activity)  - UtilClass.getStatusBarHeight(activity));
			}
			else
			{
				// standard ActionBar
				view = inflater.inflate(R.layout.fragment_case_detail, container, false);
				mImageView = (ImageView) view.findViewById(R.id.header_image);
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

			// if added image, need to redo FadingActionBar TODO
			//populateFields();
		}


		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);

			populateFields();
		}


		/**
		 * //todo use the at the beginning of populate fields?
		 * @param thumbnail
		 */
		public void populateHeader(int thumbnail)
		{
			View rootView = getView();

			String [] image_args = {String.valueOf(key_id)};
			Cursor imageCursor = getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

			if ((hasImage == false && imageCursor.getCount() > 0) || hasImage == true && imageCursor.getCount() == 0)
			{
				mImageView.setVisibility(View.GONE);
				rootView.findViewById(R.id.anchor).setVisibility(View.GONE);

				// reload activity theme
				activity.setResult(CaseCardListActivity.RESULT_EDITED);
				activity.startActivityForResult(starterIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);

				finish();
				return;
			}

			if (imageCursor.getCount() > 0 && imageCursor.moveToFirst())
			{
				mImageView.setVisibility(View.VISIBLE);
				rootView.findViewById(R.id.anchor).setVisibility(View.VISIBLE);

				// set image for FadingActionBar.  first image in cursor array
				if(thumbnail < imageCursor.getCount())
				{
					imageCursor.move(thumbnail);
				}

				String headerImageFilename = CaseCardListActivity.picturesDir + "/" + imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME);
				//ImageView headerImageView = (ImageView) headerView.findViewById(R.id.thumbnail);
				//UtilClass.setPic(headerImageView, headerImageFilename, UtilClass.IMAGE_SIZE);
				UtilClass.setPic(mImageView, headerImageFilename, UtilClass.IMAGE_SIZE);

				imageCursor.close();
			}
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

			String [] image_args = {String.valueOf(selected_key_id)};
			Cursor imageCursor = getActivity().getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

			if ((hasImage == false && imageCursor.getCount() > 0) || hasImage == true && imageCursor.getCount() == 0)
			{
				activity.startActivityForResult(starterIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);
				activity.setResult(CaseCardListActivity.RESULT_EDITED);
				finish();
				return;
			}

			// get db row of clicked case
			Uri uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, selected_key_id);
			Cursor case_cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

			if (case_cursor.moveToFirst())
			{
				int key_id = case_cursor.getInt(CasesProvider.COL_ROWID);
				String patient_id = case_cursor.getString(CasesProvider.COL_PATIENT_ID);
				final String diagnosis = case_cursor.getString(CasesProvider.COL_DIAGNOSIS);
				String findings = case_cursor.getString(CasesProvider.COL_FINDINGS);
				String section = case_cursor.getString(CasesProvider.COL_SECTION);
				String comments = case_cursor.getString(CasesProvider.COL_COMMENTS);
				String key_words = case_cursor.getString(CasesProvider.COL_KEYWORDS);
				String biopsy = case_cursor.getString(CasesProvider.COL_BIOPSY);
				String followup = case_cursor.getString(CasesProvider.COL_FOLLOWUP_COMMENT);

				thumbnail_pos = 0;
				String thumbnailString = case_cursor.getString(CasesProvider.COL_THUMBNAIL);
				if(thumbnailString != null && !thumbnailString.isEmpty())
					thumbnail_pos = Integer.parseInt(thumbnailString);

				boolean followup_bool;
				if(case_cursor.getInt(CasesProvider.COL_FOLLOWUP)==1)
					followup_bool=true;
				else
					followup_bool=false;

				String study_type = case_cursor.getString(CasesProvider.COL_STUDY_TYPE);
				String date_str = case_cursor.getString(CasesProvider.COL_DATE);
				//String[] imageFilename = new String[CasesProvider.MAX_NUM_IMAGES];
				//int imageCount = case_cursor.getInt(CasesProvider.COL_IMAGE_COUNT);

				// set global variable isStarred for Activity action bar menu toggle
				favorite = case_cursor.getInt(CasesProvider.COL_FAVORITE);


				// ACTION BAR Title
				// mTitle declared in super NavigationDrawerActivity, and used to restore the same title after nav drawer is closed
				if(patient_id != null && !patient_id.isEmpty())
				{
					mTitle = new SpannableString(patient_id);
					mTitle.setSpan(new TypefaceSpan(getActivity(), "RobotoCondensed-Bold.ttf"), 0, mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					//if(section != null && !section.isEmpty())
					//	actionBar.setSubtitle(section);
				}
				/*
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
				*/

				else
					mTitle = new SpannableString("");

				ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
				actionBar.setTitle(mTitle);

				// Case Information (DIAGNOSIS and FINDINGS)
				TextView TV_case_info1 = (TextView) rootView.findViewById(R.id.case_info1);
				TextView TV_case_info2 = (TextView) rootView.findViewById(R.id.detail_case_info2);

				if(diagnosis != null && !diagnosis.isEmpty())
				{
					TV_case_info1.setText(diagnosis);
					TV_case_info1.setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.CaseInfoLabel)).setVisibility(View.VISIBLE);

					TV_case_info1.setOnLongClickListener(new View.OnLongClickListener()
					{
						@Override
						public boolean onLongClick(View v)
						{
							AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

							SpannableString alertTitle = new SpannableString("Search \"" + diagnosis +"\"");

							//mTitle.setSpan(new TypefaceSpan(this, "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							alertTitle.setSpan(new TypefaceSpan(activity, "RobotoCondensed-Bold.ttf"), "Search \"".length(), alertTitle.length()-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

							builder.setTitle(alertTitle)
									.setItems(R.array.browser_search_array, new DialogInterface.OnClickListener()
									{
										public void onClick(DialogInterface dialog, int which)
										{
											String uri;
											switch (which)
											{
												case 0:
													uri = "http://www.google.com/#q=";
													break;
												case 1:
													uri = "http://www.radiopaedia.org/search?q=";
													break;
												case 2:
													uri = "https://en.wikipedia.org/wiki/Special:Search?search=";
													break;
												default:
													uri = "http://www.google.com/#q=";
													break;
											}

											try
											{
												Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri + diagnosis.replace(' ', '+')));
												startActivity(browserIntent);
											}
											catch (ActivityNotFoundException e)
											{
												UtilClass.showMessage(activity, "No application can handle this request. Please install a browser");
												e.printStackTrace();
											}

										}
									});
							builder.create().show();
							return true;
						}
					});

					// if both Diagnosis and Findings fields retrieved, set subtext as the Findings
					if(findings != null && !findings.isEmpty())    // diagnosis and findings
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

				// STUDY TYPE and STUDY DATE
				TextView TV_study_text1 = (TextView) rootView.findViewById(R.id.detail_study_type);
				TextView TV_study_text2 = (TextView) rootView.findViewById(R.id.detail_date);
				if(study_type != null && !study_type.isEmpty())
				{
					TV_study_text1.setText(study_type);
					TV_study_text1.setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.StudyLabel)).setVisibility(View.VISIBLE);

					// STUDY DATE
					if(date_str != null && !date_str.isEmpty())
					{
						TV_study_text2.setText(UtilClass.convertDateString(date_str, "yyyy-MM-dd", "MMMM d, yyyy"));
						TV_study_text2.setVisibility(View.VISIBLE);
					}
					else
					{
						TV_study_text2.setVisibility(GONE);
					}

				}
				else if(date_str != null && !date_str.isEmpty())
				{
					// only study date known
					TV_study_text1.setText(UtilClass.convertDateString(date_str, "yyyy-MM-dd", "MMMM d, yyyy"));
					TV_study_text1.setVisibility(View.VISIBLE);

					((TextView) rootView.findViewById(R.id.StudyLabel)).setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.StudyLabel)).setText("Study Date");

					TV_study_text2.setVisibility(GONE);
				}
				else
				{
					// neither study type or date known
					((TextView) rootView.findViewById(R.id.StudyLabel)).setVisibility(GONE);
					TV_study_text2.setVisibility(GONE);
					TV_study_text1.setVisibility(GONE);

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
				// get all of the images linked to this case _id
				//String [] image_args = {String.valueOf(selected_key_id)};
				//Cursor imageCursor = getActivity().getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

				if (imageCursor != null && imageCursor.getCount() > 0 && imageCursor.moveToFirst())  //hasImage to keep crash if FadingActionBar hasn't been done yet
				{
					rootView.findViewById(R.id.ImagesLabel).setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.key_image).setVisibility(View.VISIBLE);
					mImageView.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.anchor).setVisibility(View.VISIBLE);

					// set image for FadingActionBar.  first image in cursor array
					if(thumbnail_pos < imageCursor.getCount())
					{
						imageCursor.move(thumbnail_pos);
					}

					// fading action bar
					String headerImageFilename = CaseCardListActivity.picturesDir + "/" + imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME);
					//ImageView headerImageView = (ImageView) headerView.findViewById(R.id.thumbnail);
					//UtilClass.setPic(headerImageView, headerImageFilename, UtilClass.IMAGE_SIZE);
					UtilClass.setPic(mImageView, headerImageFilename, UtilClass.IMAGE_SIZE);


					// set back icon
					//getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

					// fading toolbar with drawer open
					// ActionBarDrawerToggle ties together the the proper interactions between the navigation drawer and the action bar app icon.
					final ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(
							                                         getActivity(),
							                                         getDrawerLayout(),
							                                         //R.drawable.ic_drawer,
							                                         R.string.navigation_drawer_open,
							                                         R.string.navigation_drawer_close
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
						public void onDrawerOpened(View drawerView) {
							super.onDrawerOpened(drawerView);
							if (!isAdded()) {
								return;
							}
							getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
						}

						@Override
						public void onDrawerSlide(View drawerView, float slideOffset)
						{
							super.onDrawerSlide(drawerView, slideOffset);

							int baseColor = UtilClass.get_attr(getActivity(), R.attr.colorPrimary);
							int textColor = UtilClass.get_attr(getActivity(), R.attr.actionMenuTextColor);

							if(slideOffset > toolbarAlpha)
							{
								mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(slideOffset, baseColor));
								mToolbar.setTitleTextColor(ScrollUtils.getColorWithAlpha(slideOffset, textColor));
							}
						}

					};

					// Defer code dependent on restoration of previous instance state.
					getDrawerLayout().post(new Runnable() {
						@Override
						public void run() {
							mDrawerToggle.syncState();
						}
					});

					mDrawerToggle.setDrawerIndicatorEnabled(false);
					//mDrawerToggle.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

					setDrawerListener(mDrawerToggle);


					// image grid
					imageGridView = new ImageGridView(getActivity(), (GridView) rootView.findViewById(R.id.key_image), selected_key_id, imageCursor);
					imageGridView.setThumbnailPosition(thumbnail_pos);
					imageGridView.setMode(ImageGridView.DETAIL_ACTIVITY);

					imageCursor.close();
				}
				else
				{
					// no images
					rootView.findViewById(R.id.ImagesLabel).setVisibility(View.GONE);
					rootView.findViewById(R.id.key_image).setVisibility(View.GONE);
					mImageView.setVisibility(View.GONE);
					rootView.findViewById(R.id.anchor).setVisibility(View.GONE);
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

		}// end populateFields

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


		/**
		 *
		 * @param scrollY
		 * @param firstScroll
		 * @param dragging
		 */
		@Override
		public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging)
		{
			int baseColor = UtilClass.get_attr(activity, R.attr.colorPrimary);
			int textColor = UtilClass.get_attr(activity, R.attr.actionMenuTextColor);

			int parallaxDistance = (int)activity.getResources().getDimension(R.dimen.large_image_height) - (int)activity.getResources().getDimension(R.dimen.toolbar_size); // todo change to attr
			toolbarAlpha = 1 - (float) Math.max(0, parallaxDistance - scrollY) / parallaxDistance;

			mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(toolbarAlpha, baseColor));
			mToolbar.setTitleTextColor(ScrollUtils.getColorWithAlpha(toolbarAlpha, textColor));

			mImageView.setTranslationY(scrollY / 2);

			if(scrollY >= parallaxDistance)
			{
				getActivity().findViewById(R.id.toolbar_dropshadow).setVisibility(View.VISIBLE);
			}
			else
			{
				getActivity().findViewById(R.id.toolbar_dropshadow).setVisibility(View.GONE);
			}
		}

		@Override
		public void onDownMotionEvent()
		{

		}

		@Override
		public void onUpOrCancelMotionEvent(ScrollState scrollState)
		{

		}
	}// end fragment
}
