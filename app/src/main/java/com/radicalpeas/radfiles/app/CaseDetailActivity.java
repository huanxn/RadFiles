package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
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
public class CaseDetailActivity extends NavigationDrawerActivity
{
	private static String TAG = "CaseDetail Activity";

	private Activity activity;

	private CaseDetailFragment fragment = null;
	private long key_id = -1;

	public static final String ARG_HAS_IMAGE = "com.radicalpeas.radcases.ARG_HAS_IMAGE";

	static final int REQUEST_EDIT_CASE = 21;

	private Intent starterIntent; // to recreate Activity if theme/style/fadingactionbar changes due to add/removal of images/header
	boolean hasImage;

	private File tempImageFile;                                 // new image from camera

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		activity = this;

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
//			super.onCreate_for_FAB(savedInstanceState);
			super.onCreate(savedInstanceState, false, R.layout.toolbar_fading, true);

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
			//setTheme(R.style.MaterialTheme_Light);
			//super.onCreate(savedInstanceState, false);


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

		// Set the starred icon and text in action bar
		// also done in onOptionsItemSelected  //todo change?
		if (fragment.isStarred())
		{
			star.setTitle(getResources().getString(R.string.remove_star));
			star.setIcon(R.drawable.ic_star_white_24dp);
		}
		else
		{
			star.setTitle(getResources().getString(R.string.add_star));
			star.setIcon(R.drawable.ic_star_outline_white_24dp);
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

			case R.id.menu_star:
				if (key_id != -1)
				{
//					findViewById(R.id.action_star);
					// toggle favorites star
					fragment.toggleStar();

					if(fragment.isStarred())
					{
						item.setTitle(getResources().getString(R.string.remove_star));
						item.setIcon(R.drawable.ic_star_white_24dp);
					}
					else
					{
						item.setTitle(getResources().getString(R.string.add_star));
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
				File resultFile = new File(resultUri.getPath());

				// Create the Filename where the photo should go
				String tempFilename = null;

				// Create an image file name based on timestamp
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
				tempFilename = "IMAGE_" + timeStamp + "_";	// temp file will add extra random numbers to filename

				// Get the private application storage directory for pictures
				File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

				// Create the file
				try
				{
					tempImageFile = File.createTempFile(tempFilename, ".jpg", storageDir);
					UtilsFile.copyFile(tempImageFile, resultFile);
				}
				catch (IOException ex)
				{
					Log.e("getPictureFromCamera", "Could not open new image file.");
				}

				// clean up
				if(resultFile.exists())
				{
					resultFile.delete();
				}

				// imageGridView.addImage(tempImageFile.getPath());
				addNewImageToDatabase(tempImageFile.getPath());

			} else if (resultCode == com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
				Exception error = result.getError();
			}

		}


	}

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

		Uri row_uri = getContentResolver().insert(CasesProvider.IMAGES_URI, imageValues);
		long new_image_id = Long.parseLong(row_uri.getLastPathSegment());

		// update last modified date field
		UtilClass.updateLastModifiedDate(this, key_id);

		setResult(CaseCardListActivity.RESULT_EDITED);

		if(hasImage)
		{
			// show new image in grid display of key images
			fragment.imageGridView.addImage(imageFilepath, new_image_id);
		}
		else
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

				/*
				showcaseView.setShowcaseX(image_coordinates[0]/2);
				showcaseView.setShowcaseY(image_coordinates[1]);
				*/

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

				/*
				showcaseView.setShowcaseX(image_coordinates[0]/2);
				showcaseView.setShowcaseY(image_coordinates[1]);
				*/

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
		/*
		else if(step == 4)
		{
			final Target viewTarget = new Target() {
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
					.setContentTitle("Back")
					.setContentText("Return to your case list.")
					.setStyle(R.style.CustomShowcaseThemeEnd)
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

		}
		*/
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

		private int thumbnail_pos;
		private ImageGridView imageGridView = null;

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

			String [] image_args = {String.valueOf(mActivity.key_id)};
			Cursor imageCursor = mActivity.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

			if ((hasImage == false && imageCursor.getCount() > 0) || hasImage == true && imageCursor.getCount() == 0)
			{
				mImageView.setVisibility(View.GONE);
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
				mActivity.startActivityForResult(mActivity.starterIntent, CaseCardListActivity.REQUEST_CASE_DETAILS);
				mActivity.setResult(CaseCardListActivity.RESULT_EDITED);
				mActivity.finish();
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
					mActivity.mTitle = new SpannableString(patient_id);
					mActivity.mTitle.setSpan(new TypefaceSpan(getActivity(), "RobotoCondensed-Bold.ttf"), 0, mActivity.mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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
					mActivity.mTitle = new SpannableString("");

				ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
				actionBar.setTitle(mActivity.mTitle);

				// Case Information (DIAGNOSIS and FINDINGS)
				TextView TV_case_info1 = (TextView) rootView.findViewById(R.id.case_info1);
				TextView TV_case_info2 = (TextView) rootView.findViewById(R.id.detail_case_info2);

				if(diagnosis != null && !diagnosis.isEmpty())
				{
					TV_case_info1.setText(diagnosis);
					TV_case_info1.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.CaseInfoLabel).setVisibility(View.VISIBLE);

					TV_case_info1.setOnLongClickListener(new View.OnLongClickListener()
					{
						@Override
						public boolean onLongClick(View v)
						{
							AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

							SpannableString alertTitle = new SpannableString("Search \"" + diagnosis +"\"");

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
												Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri + diagnosis.replace(' ', '+')));
												startActivity(browserIntent);
											}
											catch (ActivityNotFoundException e)
											{
												UtilClass.showMessage(mActivity, "No application can handle this request. Please install a browser");
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
					rootView.findViewById(R.id.CaseInfoLabel).setVisibility(View.VISIBLE);
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
					rootView.findViewById(R.id.CaseInfoLabel).setVisibility(GONE);
				}

				// SECTION
				TextView TV_section = (TextView) rootView.findViewById(R.id.detail_section);
				if(section != null && !section.isEmpty())
				{
					TV_section.setText(section);
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
				if(study_type != null && !study_type.isEmpty())
				{
					TV_study_text1.setText(study_type);
					TV_study_text1.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.StudyLabel).setVisibility(View.VISIBLE);

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
				if (biopsy != null && !biopsy.isEmpty())
				{
					TV_biopsy.setText(biopsy);
					TV_biopsy.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.BiopsyLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_biopsy.setVisibility(GONE);
					rootView.findViewById(R.id.BiopsyLabel).setVisibility(GONE);
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
							                                                                     mActivity.getDrawerLayout(),
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

						/*
						@Override
						public void onDrawerSlide(View drawerView, float slideOffset)
						{
							super.onDrawerSlide(drawerView, slideOffset);

							int baseColorDark = UtilClass.get_attr(getActivity(), R.attr.colorPrimaryDark);
							int baseColor = UtilClass.get_attr(getActivity(), R.attr.colorPrimary);
							int textColor = UtilClass.get_attr(getActivity(), R.attr.actionMenuTextColor);

							if(slideOffset > toolbarAlpha)
							{
								mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(slideOffset, baseColor));
								mToolbar.setTitleTextColor(ScrollUtils.getColorWithAlpha(slideOffset, textColor));

								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
								{
									Window window = getWindow();
									//window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
									//TODO fix if slideoffset > toolbarapha + minalpha
									window.setStatusBarColor(ScrollUtils.getColorWithAlpha(slideOffset, baseColorDark));
								}

							}
						}
						*/

					};

					// Defer code dependent on restoration of previous instance state.
					mActivity.getDrawerLayout().post(new Runnable() {
						@Override
						public void run() {
							mDrawerToggle.syncState();
						}
					});

					mDrawerToggle.setDrawerIndicatorEnabled(false);
					//mDrawerToggle.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

					mActivity.setDrawerListener(mDrawerToggle);


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
					rootView.findViewById(R.id.FollowupLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_followup.setVisibility(GONE);
					rootView.findViewById(R.id.FollowupLabel).setVisibility(GONE);
				}

				// COMMENTS
				TextView TV_comments = (TextView) rootView.findViewById(R.id.detail_comments);
				if (comments != null && !comments.isEmpty())
				{
					TV_comments.setText(comments);
					TV_comments.setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.CommentsLabel).setVisibility(View.VISIBLE);
				}
				else
				{
					TV_comments.setVisibility(GONE);
					rootView.findViewById(R.id.CommentsLabel).setVisibility(GONE);
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
