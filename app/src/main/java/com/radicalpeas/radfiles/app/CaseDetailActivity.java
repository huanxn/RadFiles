package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;


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
public class CaseDetailActivity extends AppCompatActivity
{
	private static String TAG = "CaseDetail Activity";

	private Activity activity;

	private CaseDetailFragment fragment = null;
	private long key_id = -1;

	public static final String ARG_HAS_IMAGE = "com.radicalpeas.radcases.ARG_HAS_IMAGE";
	public static final String ARG_CASE_INFO = "com.radicalpeas.radcases.ARG_CASE_INFO";

	static final int REQUEST_EDIT_CASE = 21;

	private Intent starterIntent; // to recreate Activity if theme/style/fadingactionbar changes due to add/removal of images/header
	boolean hasImage;

	private File tempImageFile;                                 // new image from camera

	private SpannableString mTitle;
	private Toolbar mToolbar;
	private View mOverflowTarget = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		activity = this;

		starterIntent = getIntent();    // save original intent info
		key_id = getIntent().getLongExtra(CaseCardListActivity.ARG_KEY_ID, -1);
		//hasImage = getIntent().getBooleanExtra(ARG_HAS_IMAGE, false);

		if(key_id == -1)
		{
			UtilClass.showSnackbar(this, "debug: CaseDetail key_id = -1");
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
//			super.onCreate_for_FAB(savedInstanceState);
//			super.onCreate(savedInstanceState, false, R.layout.toolbar_fading, true);

		}
		else
		{
			hasImage = false;

			// Set back to normal theme
			//setTheme(R.style.MaterialTheme_Light);
			//super.onCreate(savedInstanceState, false);

			//setDrawerPosition(NavDrawerActivity.POS_CASE_LIST_DETAIL_NOIMAGE);
	//		super.onCreate(savedInstanceState, false);

		}

		imageCursor.close();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_case_detail);

		// set appropriate toolbar view
		FrameLayout toolbarContainer = (FrameLayout) findViewById(R.id.toolbar_container);
		if(toolbarContainer != null)
		{
			toolbarContainer.addView(getLayoutInflater().inflate(R.layout.toolbar_fading, null, false));
		}
		// set the toolbar layout element
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		if (mToolbar != null)
		{
			setSupportActionBar(mToolbar);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		}

		if(hasImage)
		{
			// show picture under transparent toolbar, ie no margin
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT
			);
			params.setMargins(0, 0, 0, 0);
			findViewById(R.id.container).setLayoutParams(params);
		}
		else
		{
			// show fragment below transparent toolbar, ie with a margin
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT
			);
			//params.setMargins(0, UtilClass.getStatusBarHeight(this) + UtilClass.getToolbarHeight(this), 0, 0);
			params.setMargins(0, UtilClass.getToolbarHeight(this), 0, 0);

			findViewById(R.id.container).setLayoutParams(params);
		}

		// toolbar title
		mToolbar.setTitleTextColor(UtilClass.get_attr(this, R.attr.actionMenuTextColor));
		mTitle = new SpannableString(getTitle());
		mTitle.setSpan(new TypefaceSpan(this, "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		mTitle.setSpan(new TypefaceSpan(this, "RobotoCondensed-Bold.ttf"), "RAD".length(), mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		// for ShowcaseView tutorial
		mOverflowTarget = findViewById(R.id.overflow_menu_target);


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

	// called from Case.java after sync with cloud data
	public void populateFields()
	{
		fragment.populateFields();
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
		MenuItem star = menu.findItem(R.id.menu_like);

		// Set the starred icon and text in action bar
		// also done in onOptionsItemSelected  //todo change?
		if (fragment.isStarred())
		{
			star.setTitle(getResources().getString(R.string.unlike));
			star.setIcon(new IconicsDrawable(activity)
					.icon(GoogleMaterial.Icon.gmd_star)
					.color(Color.WHITE)
					.sizeDp(24));
		}
		else
		{
			star.setTitle(getResources().getString(R.string.like));
			star.setIcon(new IconicsDrawable(activity)
					.icon(GoogleMaterial.Icon.gmd_star_border)
					.color(Color.WHITE)
					.sizeDp(24));
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	// show menu icons in overflow
	public boolean onMenuOpened(int featureId, Menu menu)
	{
		if (featureId == Window.FEATURE_ACTION_BAR && menu != null)
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
					Log.e(TAG, "onMenuOpened", e);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		return super.onMenuOpened(featureId, menu);
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

			case R.id.menu_like:
				if (key_id != -1)
				{
//					findViewById(R.id.action_star);
					// toggle favorites star
					fragment.toggleStar();

					if(fragment.isStarred())
					{
						item.setTitle(getResources().getString(R.string.unlike));
						item.setIcon(R.drawable.ic_star_white_24dp);
					}
					else
					{
						item.setTitle(getResources().getString(R.string.like));
						item.setIcon(R.drawable.ic_star_outline_white_24dp);
					}

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

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					{
						ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity);
						startActivityForResult(intent, REQUEST_EDIT_CASE, options.toBundle());
					}
					else
					{
						startActivityForResult(intent, REQUEST_EDIT_CASE);
					}

				}

				return true;

			case R.id.menu_camera:

				// start CropImage activity, which returns with onActivityResult
				com.theartofdev.edmodo.cropper.CropImage.startPickImageActivity(this);

				/*

				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				int pref_image_source = Integer.parseInt(sharedPref.getString(getString(R.string.pref_image_source_key), "0"));
				switch(pref_image_source)
				{
					// from camera
					case 1:
						tempImageFile = UtilClass.getPictureFromCamera(this, CaseEditActivity.REQUEST_IMAGE_CAPTURE);
						break;

					// from file
					case 2:
						Intent intent = new Intent();
						intent.setType("image/*");
						intent.setAction(Intent.ACTION_GET_CONTENT);
						//intent.putExtra("image_filename", tempImageFilename);
						startActivityForResult(Intent.createChooser(intent,"Select Picture"), CaseEditActivity.REQUEST_SELECT_IMAGE_FROM_FILE);
						break;

					// always ask
					default:
						choosePictureAlertDialog(item.getActionView());
						break;
				}

				*/

				return true;

			case R.id.menu_share:

				//new ShareCasesTask(mCardAdapter.getMultiselectList(), mode).execute();

				// todo add share code similar to CaseCardListActivity

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

			case R.id.menu_help:

				// ShowcaseView tutorial
				runTutorial(0);

				return true;


			default:
				return super.onOptionsItemSelected(item);
		}

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
		// almost same as CaseEditActivity
		// called when user picks image source (camera vs cloud vs local gallery)
		else if (requestCode == com.theartofdev.edmodo.cropper.CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK)
		{
			Uri imageUri = com.theartofdev.edmodo.cropper.CropImage.getPickImageResultUri(this, data);
			com.theartofdev.edmodo.cropper.CropImage.activity(imageUri)
					.setGuidelines(CropImageView.Guidelines.ON)
					.setFixAspectRatio(true)
					.setAspectRatio(1,1)
					.setInitialCropWindowPaddingRatio(0)
					.setAllowRotation(true)
					.setAutoZoomEnabled(true)
					.start(this);
		}
		// called after user crops image
		else if (requestCode == com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
		{
			com.theartofdev.edmodo.cropper.CropImage.ActivityResult result = com.theartofdev.edmodo.cropper.CropImage.getActivityResult(data);
			if (resultCode == RESULT_OK)
			{

				Uri resultUri = result.getUri();

				// Create the Filename where the photo should go
				String tempFilename = null;

				// Create an image file name based on timestamp
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
				tempFilename = "IMAGE_" + timeStamp + ".png";

				// Create the scaled image file
				try
				{
					tempImageFile = UtilClass.getScaledImageFile(this, resultUri, tempFilename);
				}
				catch (IOException ex)
				{
					Log.e("getPictureFromCamera", "Could not open new image file.");
				}

				addNewImageToDatabase(tempImageFile.getPath());

			} else if (resultCode == com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
				Exception error = result.getError();
			}

		}


	}

	// called after cropping camera picture
	private void addNewImageToDatabase(String imageFilepath)
	{
		int new_image_index = 0;

		if(hasImage)
		{
			new_image_index = fragment.imageGridView.getCount();
		}
		else
		{
			new_image_index = 0;
		}

		//store in image table
		ContentValues imageValues = new ContentValues();
		imageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, key_id);
		imageValues.put(CasesProvider.KEY_IMAGE_FILENAME, new File(imageFilepath).getName());
		imageValues.put(CasesProvider.KEY_ORDER, new_image_index);      // set order to display images.  new files last.  //todo user reodering

		//Uri row_uri = getContentResolver().insert(CasesProvider.IMAGES_URI, imageValues);
		Uri row_uri = UtilsDatabase.insertImage(this, imageValues);
		long new_image_id = Long.parseLong(row_uri.getLastPathSegment());


		setResult(CaseCardListActivity.RESULT_EDITED);

		if(hasImage)
		{
			// show new image in grid display of key images
			fragment.imageGridView.addImage(imageFilepath, new_image_id);
		}
		else
		{
			//update image count
			ContentValues caseValues = new ContentValues();
			caseValues.put(CasesProvider.KEY_IMAGE_COUNT, 1);
			UtilsDatabase.updateCase(this, key_id, caseValues);

			//reload activity to show new header
			startActivityForResult(starterIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);

			finish();
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
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_POSITION, fragment.mCase.thumbnail);
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_IMAGE_FILES, fragment.imageGridView.getImageFilepaths());
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_IMAGE_CAPTIONS, fragment.imageGridView.getImageCaptions());
/*
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				{
					// get the common element for the transition in this activity
					final ImageView image_header = (ImageView)view;
					ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, image_header, "transitionImage");
					startActivity(imageGalleryIntent, options.toBundle());
				}
				else
*/				{

					startActivity(imageGalleryIntent);
				}
				break;
		}
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
			viewTarget = mToolbar.findViewById(R.id.menu_like);
			if (viewTarget != null)
			{
				TapTargetView tapTargetView = new TapTargetView.Builder(this)
						.title("Favorites")
						.description("Star this case as one of your favorites.")
						.outerCircleColor(R.color.default_colorHeaderLight)
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
			viewTarget = mToolbar.findViewById(R.id.menu_edit);
			if (viewTarget != null)
			{
				TapTargetView tapTargetView = new TapTargetView.Builder(this)
						.title("Edit")
						.description("Edit and add details to this case.")
						.outerCircleColor(R.color.default_colorHeaderLight)
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
		else if(step == 2)
		{
			View diagnosisTarget = null;
			View diagnosisLabelTarget = null;
			String contentText = null;
			if(fragment != null && fragment.getView() != null)
			{

				diagnosisLabelTarget = fragment.getView().findViewById(R.id.CaseInfoLabel);
				/*
				if(diagnosisLabelTarget != null)
				{
					// Scroll to Diagnosis Label
					int[] view_coordinates = new int[2];
					diagnosisLabelTarget.getLocationInWindow(view_coordinates);
					if(fragment.mScrollView != null)
					{
						fragment.mScrollView.scrollVerticallyTo(view_coordinates[1]);
					}
				}
				*/

				// Diagnosis vs Findings
				if( diagnosisLabelTarget != null && ((TextView)diagnosisLabelTarget).getText().toString().contentEquals("Diagnosis"))  //todo change to @string
				{
					contentText = "Long press to search for this diagnosis online.";
				}
				else
				{
					contentText = "In a case with a diagnosis, long press to search for the diagnosis online.";
				}

				diagnosisTarget = fragment.getView().findViewById(R.id.detail_case_info1);
				if(diagnosisTarget != null && diagnosisLabelTarget != null)
				{
					new TapTargetView.Builder(this)
							.title("Diagnosis")
							.description(contentText)
							.outerCircleColor(R.color.default_colorHeaderLight)
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
							.showFor(diagnosisTarget);

				}
				else
				{
					// no target
				}
			}


		}
		else if(step == 3)
		{
			View imagesTarget = null;
			if(fragment != null && fragment.imageGridView != null)
			{
				//imagesTarget = fragment.getView().findViewById(R.id.ImagesLabel);
				imagesTarget = fragment.imageGridView.getView();
				//imagesTarget = fragment.getView().findViewById(R.id.ImagesTarget);
			}

			if(imagesTarget != null)
			{
				int[] image_coordinates = new int[2];
				imagesTarget.getLocationInWindow(image_coordinates);

				fragment.mScrollView.scrollVerticallyTo(image_coordinates[1]);

				new TapTargetView.Builder(this)
						.title("Case Images")
						.description("Click to open the image gallery.\nLong press for more options.")
						.outerCircleColor(R.color.default_colorHeaderLight)
						.cancelable(true)
						.listener(new TapTargetView.Listener()
						{
							@Override
							public void onTargetClick(TapTargetView view)
							{
								view.dismiss(true);
								//runTutorial(step + 1);
							}

							@Override
							public void onTargetLongClick(TapTargetView view)
							{

							}
						})
						.showFor(imagesTarget);

			}
			else
			{
				// no target
			}
		}
		/*
		if(step == 0)
		{
			final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
					                            //.setTarget( new ViewTarget( ((ViewGroup)findViewById(R.id.action_bar)).getChildAt(1) ) )
					                            .setTarget(new ViewTarget(mToolbar.findViewById(R.id.menu_edit)))
					                            .setContentTitle("Edit")
					                            .setContentText("Edit and add details to this case.")
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
		else if(step == 1)
		{
			final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
					                            //.setTarget( new ViewTarget( ((ViewGroup)findViewById(R.id.action_bar)).getChildAt(1) ) )
					                            .setTarget(new ViewTarget(mToolbar.findViewById(R.id.menu_camera)))
					                            .setContentTitle("Add Image")
					                            .setContentText("Add a new image from your camera or file storage.")
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

		else if(step == 2)
		{
			View diagnosisTarget = null;
			View diagnosisLabelTarget = null;
			String contentText = null;
			if(fragment != null && fragment.getView() != null)
			{
				diagnosisTarget = fragment.getView().findViewById(R.id.case_info1);
				diagnosisLabelTarget = fragment.getView().findViewById(R.id.CaseInfoLabel);

				// Diagnosis vs Findings
				if( diagnosisLabelTarget != null && ((TextView)diagnosisLabelTarget).getText().toString().contentEquals("Diagnosis"))  //todo change to @string
				{
					contentText = "Long press to search for this diagnosis online.";
				}
				else
				{
					contentText = "In a case with a diagnosis, long press to search for the diagnosis online.";
				}
			}

			if(diagnosisTarget != null && diagnosisLabelTarget != null)
			{
				// Scroll to Diagnosis Label
				int[] view_coordinates = new int[2];
				diagnosisLabelTarget.getLocationInWindow(view_coordinates);
				if(fragment.mScrollView != null)
				{
					fragment.mScrollView.smoothScrollTo(view_coordinates[0], view_coordinates[1] - UtilClass.getToolbarHeight(this) - UtilClass.getStatusBarHeight(this));
				}

				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  //.setTarget(new ViewTarget(fragment.imageGridView.getView()))
						                                  .setTarget(new ViewTarget(diagnosisTarget))
						                                  .setContentTitle("Diagnosis")
						                                  .setContentText(contentText)
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
			else
			{
				// no target
				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  .setContentTitle("Diagnosis")
						                                  .setContentText(contentText)
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
		else if(step == 3)
		{
			View imagesTarget = null;
			if(fragment != null && fragment.imageGridView != null)
			{
				//imagesTarget = fragment.getView().findViewById(R.id.ImagesLabel);
				imagesTarget = fragment.imageGridView.getView();
				//imagesTarget = fragment.getView().findViewById(R.id.ImagesTarget);
			}

			if(imagesTarget != null)
			{
				int[] image_coordinates = new int[2];
				imagesTarget.getLocationInWindow(image_coordinates);

				fragment.mScrollView.smoothScrollTo(image_coordinates[0], image_coordinates[1]);


				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  .setTarget(new ViewTarget(imagesTarget))
						                                  .setContentTitle("Case Images")
						                                  .setContentText("Click on an image to open the image gallery.\n\nLong press on an image for more options.")
						                                  .setStyle(R.style.CustomShowcaseTheme)
						                                  .hideOnTouchOutside()
						                                  .build();


			//	showcaseView.setShowcaseX(image_coordinates[0]/2);
			//	showcaseView.setShowcaseY(image_coordinates[1]);


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
			else
			{
				// no target
				final ShowcaseView showcaseView = new ShowcaseView.Builder(this)
						                                  .setContentTitle("Case Images")
						                                  .setContentText("In a case with images,\nclick on an one to open \nthe image gallery.\n\nLong press on an image \nfor more options.")
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
		else if(step == 4)
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
*/
		return;
	}

	/**
	 * A fragment representing a single Case detail screen.
	 * This fragment is either contained in a {@link CaseCardListActivity}
	 * in two-pane mode (on tablets) or a {@link CaseDetailActivity}
	 * on handsets.
	 */
	public static class CaseDetailFragment extends Fragment implements ObservableScrollViewCallbacks
	{
		/**
		 * The fragment argument representing the item ID that this fragment
		 * represents.
		 */
		private long selected_key_id;
		private CaseDetailActivity mActivity;

		private Bundle mArguments;
		private boolean hasImage;

		private int favorite; // for action menu toggle in CaseDetail Activity (not in this fragment)

		// parallax fading toolbar
		private ImageView mImageView;
		private Toolbar mToolbar;
		private ObservableScrollView mScrollView;
		private float toolbarAlpha;
		final private float statusbarMinAlpha = (float)0.2;
		//private int mParallaxImageHeight;

		//private int thumbnail_pos;
		private ImageGridView imageGridView = null;

		private SwipeRefreshLayout swipeRefreshLayout;

		private Case mCase;
		private Case mFirebaseCase;

		// Hold a reference to the current animator,
		// so that it can be canceled mid-way.
		//private Animator mCurrentAnimator;

		/**
		 * Mandatory empty constructor for the fragment manager to instantiate the
		 * fragment (e.g. upon screen orientation changes).
		 */
		public CaseDetailFragment()
		{
			mCase = new Case();
			mFirebaseCase = new Case();
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);

			this.mActivity = (CaseDetailActivity) activity;

			mArguments = getArguments();
			hasImage = mArguments.getBoolean(ARG_HAS_IMAGE);
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
				// Initialize colors and transparency for Fading Toolbar
				view = inflater.inflate(R.layout.fragment_case_detail, container, false);

				mImageView = (ImageView) view.findViewById(R.id.header_image);

				mToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);  // in activity content view
				toolbarAlpha = 0;
				mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, 0));   // transparent
				mToolbar.setTitleTextColor(ScrollUtils.getColorWithAlpha(0, 0));    // transparent
				getActivity().findViewById(R.id.toolbar_dropshadow).setVisibility(View.GONE);

				// adjustments for transparent statusbar
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				{
					Window window = mActivity.getWindow();
					window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
					//window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

					// pad toolbar to place below the transparent status bar
					mToolbar.setPadding(0, UtilClass.getStatusBarHeight(getActivity()), 0, 0);
					getActivity().findViewById(R.id.toolbar_background).setPadding(0, UtilClass.getStatusBarHeight(getActivity()), 0, 0);

					/*
					// include statusbar height in margin for drawer content
					//UtilClass.setMargins(getActivity().findViewById(R.id.navigation_drawer), 0, UtilClass.getStatusBarHeight(getActivity()) + UtilClass.getToolbarHeight(getActivity()), 0, 0);  // activity_navigation_drawer_fab  TODO rename to drawer_container or something

					ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
							                                                                   ViewGroup.MarginLayoutParams.WRAP_CONTENT,
							                                                                   ViewGroup.MarginLayoutParams.WRAP_CONTENT);
					params.setMargins(0, UtilClass.getStatusBarHeight(getActivity()) + UtilClass.getToolbarHeight(getActivity()), 0, 0);
					getActivity().findViewById(R.id.navigation_drawer).setLayoutParams(params);
*/


					int baseColorDark = UtilClass.get_attr(mActivity, R.attr.colorPrimaryDark);
					window.setStatusBarColor(ScrollUtils.getColorWithAlpha(statusbarMinAlpha, baseColorDark));
				}

				mScrollView = (ObservableScrollView) view.findViewById(R.id.scroll);
				mScrollView.setScrollViewCallbacks(this);
				mScrollView.setVerticalScrollBarEnabled(false);

				// get size of screen display
				Point size = new Point();
				mActivity.getWindowManager().getDefaultDisplay().getSize(size);

				// set minimum height so that user can scroll through the entire parallax header image
				view.findViewById(R.id.detail_container).setMinimumHeight(size.y - UtilClass.getToolbarHeight(mActivity) - UtilClass.getStatusBarHeight(mActivity));
			}
			else
			{
				// standard ActionBar
				view = inflater.inflate(R.layout.fragment_case_detail, container, false);
				mImageView = (ImageView) view.findViewById(R.id.header_image);
			}

			// Setup SwipeRefreshLayout
			swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
			swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
			{
				@Override
				public void onRefresh()
				{
					populateFields();
				}
			});
			swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
					android.R.color.holo_green_light,
					android.R.color.holo_orange_light,
					android.R.color.holo_red_light);


			// lollipop transitions
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				mActivity.getWindow().setSharedElementEnterTransition(TransitionInflater.from(mActivity).inflateTransition(R.transition.shared_element));

				TransitionSet set = new TransitionSet();

				Transition slideUp = new Slide(Gravity.BOTTOM);
				slideUp.addTarget(view.findViewById(R.id.detail_container));
				set.addTransition(slideUp);

				Transition slideDown = new Slide(Gravity.TOP);
				slideDown.addTarget(mToolbar);
				set.addTransition(slideDown);

				Transition fade = new Fade();
				fade.excludeTarget(android.R.id.statusBarBackground, true);
				fade.excludeTarget(android.R.id.navigationBarBackground, true);
				set.addTransition(fade);

				mActivity.getWindow().setExitTransition(set);
				mActivity.getWindow().setEnterTransition(set);
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

			// get images of this case (by case key_id)
			String [] image_args = {String.valueOf(mActivity.key_id)};
			Cursor imageCursor = mActivity.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

			if ((hasImage == false && imageCursor.getCount() > 0) || hasImage == true && imageCursor.getCount() == 0)
			{
				mImageView.setVisibility(View.GONE);
				if(rootView != null)
					rootView.findViewById(R.id.anchor).setVisibility(View.GONE);

				// reload activity theme
				mActivity.setResult(CaseCardListActivity.RESULT_EDITED);
				mActivity.startActivityForResult(mActivity.starterIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);

				mActivity.finish();
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

				//UtilClass.setPic(mImageView, headerImageFilename, UtilClass.IMAGE_SIZE);
				Glide.with(this).load(headerImageFilename).into(mImageView);

				imageCursor.close();
			}
		}

		public void populateFields()
		{
			View rootView = getView();

			if(rootView == null)
			{
				Log.d(TAG, "Can't get rootView in populateFields()");
				return;
			}

			// get the CASE key_id
			if (getArguments().containsKey(CaseCardListActivity.ARG_KEY_ID))
			{
				mCase.key_id = selected_key_id = getArguments().getLong(CaseCardListActivity.ARG_KEY_ID);
				mFirebaseCase.key_id = selected_key_id;
			}
			else    //TODO show error message (no selected key id from listview)
			{
				selected_key_id = -1;
				mCase.key_id = -1;
				mFirebaseCase.key_id = -1;
				return;
			}

			String [] image_args = {String.valueOf(mCase.key_id)};
			Cursor imageCursor = getActivity().getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

			if ((hasImage == false && imageCursor.getCount() > 0) || hasImage == true && imageCursor.getCount() == 0)
			{
				mActivity.startActivityForResult(mActivity.starterIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);
				mActivity.setResult(CaseCardListActivity.RESULT_EDITED);
				mActivity.finish();
				return;
			}


			// get db row of clicked case
	//		Uri uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, mCase.key_id);
	//		Cursor case_cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

		//	if (case_cursor != null && case_cursor.moveToFirst())
			if(mCase.setCase(getActivity(), mCase.key_id))	// if set case info is successful
			{
				mCase.syncCaseWithCloud(mActivity);

		//		mCase.setCaseFromCursor(getActivity(), case_cursor);
				//mFirebaseCase.setCaseFromCloud(selected_key_id);

				boolean followup_bool;
				if(mCase.followup==1)
					followup_bool=true;
				else
					followup_bool=false;

				// set global variable isStarred for Activity action bar menu toggle
				if(mCase.favorite != null && !mCase.favorite.isEmpty())
				{
					favorite = Integer.parseInt(mCase.favorite);
				}
				else
				{
					favorite = 0;
				}

				ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
				if(actionBar != null)
					actionBar.setTitle(mActivity.mTitle);

				// Case Information (DIAGNOSIS and FINDINGS)
				TextView TV_case_info1 = (TextView) rootView.findViewById(R.id.detail_case_info1);
				TextView TV_case_info2 = (TextView) rootView.findViewById(R.id.detail_case_info2);

				if(mCase.diagnosis != null && !mCase.diagnosis.isEmpty())
				{
					TV_case_info1.setText(mCase.diagnosis);
					TV_case_info1.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.CaseInfoLabel).setVisibility(View.VISIBLE);

					TV_case_info1.setOnLongClickListener(new View.OnLongClickListener()
					{
						@Override
						public boolean onLongClick(View v)
						{
							AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

							SpannableString alertTitle = new SpannableString("Search \"" + mCase.diagnosis +"\"");

							//mTitle.setSpan(new TypefaceSpan(this, "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							alertTitle.setSpan(new TypefaceSpan(mActivity, "RobotoCondensed-Bold.ttf"), "Search \"".length(), alertTitle.length()-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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
												Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri + mCase.diagnosis.replace(' ', '+')));
												startActivity(browserIntent);
											}
											catch (ActivityNotFoundException e)
											{
												UtilClass.showSnackbar(mActivity, "No application can handle this request. Please install a browser");
												e.printStackTrace();
											}
										}
									});
							builder.create().show();
							return true;
						}
					});

					// if both Diagnosis and Findings fields retrieved, set subtext as the Findings
					if(mCase.findings != null && !mCase.findings.isEmpty())    // diagnosis and findings
					{
						TV_case_info2.setText(mCase.findings);
						TV_case_info2.setVisibility(View.VISIBLE);
					}
					else    // Diagnosis and no Findings
					{
						TV_case_info2.setVisibility(GONE);
					}
				}
				else if(mCase.findings != null && !mCase.findings.isEmpty())   // no Diagnosis, there are Findings
				{
					// if no Diagnosis, only Findings
					// set label to say "FINDINGS"
					((TextView) rootView.findViewById(R.id.CaseInfoLabel)).setText("FINDINGS");
					rootView.findViewById(R.id.CaseInfoLabel).setVisibility(View.VISIBLE);
					// set the main CaseInfo text to be the Findings
					TV_case_info1.setText(mCase.findings);
					TV_case_info1.setVisibility(View.VISIBLE);

					// don't put anything in the subtext
					TV_case_info2.setVisibility(GONE);
				}
				else    // no Diagnosis, no Findings
				{
					// if both Diagnosis and Findings fields are empty, then hide CaseInfoLabel
					TV_case_info1.setVisibility(GONE);
					TV_case_info2.setVisibility(GONE);
					rootView.findViewById(R.id.CaseInfoLabel).setVisibility(GONE);
				}

				// SECTION
				TextView TV_section = (TextView) rootView.findViewById(R.id.detail_section);
				if(mCase.section != null && !mCase.section.isEmpty())
				{
					TV_section.setText(mCase.section);
					TV_section.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.SectionLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_section.setVisibility(GONE);
					rootView.findViewById(R.id.SectionLabel).setVisibility(GONE);
				}

				// STUDY TYPE and STUDY DATE
				TextView TV_study_text1 = (TextView) rootView.findViewById(R.id.detail_study_type);
				TextView TV_study_text2 = (TextView) rootView.findViewById(R.id.detail_date);
				if(mCase.study_type != null && !mCase.study_type.isEmpty())
				{
					TV_study_text1.setText(mCase.study_type);
					TV_study_text1.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.StudyLabel).setVisibility(View.VISIBLE);

					// STUDY DATE
					if(mCase.db_date_str != null && !mCase.db_date_str.isEmpty())
					{
						TV_study_text2.setText(UtilClass.convertDateString(mCase.db_date_str, "yyyy-MM-dd", "MMMM d, yyyy"));
						TV_study_text2.setVisibility(View.VISIBLE);
					}
					else
					{
						TV_study_text2.setVisibility(GONE);
					}
				}
				else if(mCase.db_date_str != null && !mCase.db_date_str.isEmpty())
				{
					// only study date known
					TV_study_text1.setText(UtilClass.convertDateString(mCase.db_date_str, "yyyy-MM-dd", "MMMM d, yyyy"));
					TV_study_text1.setVisibility(View.VISIBLE);

					rootView.findViewById(R.id.StudyLabel).setVisibility(View.VISIBLE);
					((TextView) rootView.findViewById(R.id.StudyLabel)).setText("Study Date");

					TV_study_text2.setVisibility(GONE);
				}
				else
				{
					// neither study type or date known
					rootView.findViewById(R.id.StudyLabel).setVisibility(GONE);
					TV_study_text2.setVisibility(GONE);
					TV_study_text1.setVisibility(GONE);
				}

				// BIOPSY
				TextView TV_biopsy = (TextView) rootView.findViewById(R.id.detail_biopsy);
				if (mCase.biopsy != null && !mCase.biopsy.isEmpty())
				{
					TV_biopsy.setText(mCase.biopsy);
					TV_biopsy.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.BiopsyLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_biopsy.setVisibility(GONE);
					rootView.findViewById(R.id.BiopsyLabel).setVisibility(GONE);
				}

				// KEY IMAGES
				if(mCase.image_count > 0)
				{
					rootView.findViewById(R.id.ImagesLabel).setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.key_image).setVisibility(View.VISIBLE);
					mImageView.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.anchor).setVisibility(View.VISIBLE);

					String headerImageFilename = CaseCardListActivity.picturesDir + "/" + mCase.thumbnail_filename;

					Glide.with(this).load(headerImageFilename).into(mImageView);

					// image grid
					imageGridView = new ImageGridView(getActivity(), (GridView) rootView.findViewById(R.id.key_image), selected_key_id, mCase.caseImageList);
					imageGridView.setThumbnailPosition(mCase.thumbnail);
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
				if (mCase.key_words != null && !mCase.key_words.isEmpty())
				{
					TV_key_words.setText(mCase.key_words);
					TV_key_words.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.KeyWordsLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_key_words.setVisibility(GONE);
					rootView.findViewById(R.id.KeyWordsLabel).setVisibility(GONE);
				}

				// FOLLOWUP
				TextView TV_followup = (TextView) rootView.findViewById(R.id.detail_followup);
				if(followup_bool)
				{
					if (mCase.followup_comment != null && !mCase.followup_comment.isEmpty())
					{
						TV_followup.setText(mCase.followup_comment);
						TV_followup.setVisibility(View.VISIBLE);
					}
					else
					{
						// follow up comment not specified.  put generic comment.
						TV_followup.setText("Needs follow up");
						TV_followup.setVisibility(View.VISIBLE);
					}
					rootView.findViewById(R.id.FollowupLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_followup.setVisibility(GONE);
					rootView.findViewById(R.id.FollowupLabel).setVisibility(GONE);
				}

				// COMMENTS
				TextView TV_comments = (TextView) rootView.findViewById(R.id.detail_comments);
				if (mCase.comments != null && !mCase.comments.isEmpty())
				{
					TV_comments.setText(mCase.comments);
					TV_comments.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.CommentsLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_comments.setVisibility(GONE);
					rootView.findViewById(R.id.CommentsLabel).setVisibility(GONE);
				}

				// CASE NUMBER
				TextView TV_caseNumber = (TextView) rootView.findViewById(R.id.detail_caseNumber);
				if (mCase.case_id != null && !mCase.case_id.isEmpty())
				{
					TV_caseNumber.setText(mCase.case_id);
					TV_caseNumber.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.CaseNumberLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_caseNumber.setVisibility(GONE);
					rootView.findViewById(R.id.CaseNumberLabel).setVisibility(GONE);
				}
			}

//			case_cursor.close();

			swipeRefreshLayout.setRefreshing(false);

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
			//Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, selected_key_id);
			//getActivity().getContentResolver().update(row_uri, values, null, null);
			UtilsDatabase.updateCase(getActivity(), selected_key_id, values);
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
			int baseColorDark = UtilClass.get_attr(mActivity, R.attr.colorPrimaryDark);
			int baseColor = UtilClass.get_attr(mActivity, R.attr.colorPrimary);
			int textColor = UtilClass.get_attr(mActivity, R.attr.actionMenuTextColor);

			int parallaxDistance = (int)mActivity.getResources().getDimension(R.dimen.large_image_height) - (int)mActivity.getResources().getDimension(R.dimen.toolbar_size); // todo change to attr
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				// adjust for transparent status bar
				parallaxDistance = parallaxDistance - UtilClass.getStatusBarHeight(getActivity());
			}


			toolbarAlpha = 1 - (float) Math.max(0, parallaxDistance - scrollY) / parallaxDistance;

			mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(toolbarAlpha, baseColor));
			mToolbar.setTitleTextColor(ScrollUtils.getColorWithAlpha(toolbarAlpha, textColor));

			// fading status bar
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				Window window = mActivity.getWindow();
				//window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				window.setStatusBarColor(ScrollUtils.getColorWithAlpha(toolbarAlpha + statusbarMinAlpha, baseColorDark));
			}

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
