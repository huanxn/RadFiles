package com.radicalpeas.radfiles.app;

import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import eu.janmuller.android.simplecropimage.CropImage;

public class CaseEditActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener
{
	static Cursor selected_row_cursor;
	static long key_id;

	static String patient_ID;
	static String diagnosis;
	static String findings;

	static String section;

	static Cursor study_types_cursor;
	static String study_type;
	static Calendar selected_date;
	static String db_date_str = null;

	static Cursor key_words_cursor;
	static Cursor section_cursor;

	static String biopsy;
	static String comments;
	static String followup_comment;
	static boolean followup_bool = false;

	private static ImageGridView imageGridView;                 // Grid of images. Also contains image filepaths to be saved into database
	private File tempImageFile;                                 // new image from camera

	private static Animator mCurrentAnimator;

	private static boolean confirmSave = false;
	private static boolean madeChanges = false;


	// standard directories
	//private static File downloadsDir = CaseCardListActivity.downloadsDir;
	private static File picturesDir = CaseCardListActivity.picturesDir;
	//private static File appDir  = CaseCardListActivity.appDir;             // internal app data directory
	//private static File dataDir  = CaseCardListActivity.dataDir;            // private data directory (with SQL database)
	//private static File CSV_dir  = CaseCardListActivity.CSV_dir;            // contains created zip files with CSV files and images

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_case_edit);
		final View activityRootView = findViewById(R.id.container);

		if (savedInstanceState == null)
		{
			final EditCaseFragment fragment = new EditCaseFragment();

			if(fragment != null)
			{
				getFragmentManager().beginTransaction().remove(fragment).commit();
			}

			getFragmentManager().beginTransaction()
					.add(R.id.container, fragment).commit();

		}

		selected_date = Calendar.getInstance();

		// get the intent and the method argument: unique CASE KEY_ID
		Intent intent = getIntent();
		key_id = intent.getLongExtra(CaseCardListActivity.ARG_KEY_ID, -1);


		// Add or Update data, base on whether key_id was passed from parent activity
		if (key_id == -1)
		{
			// AddData to database
			// no key_id passed from the parent activity

			//Toast.makeText(this, "Add data", Toast.LENGTH_SHORT).show();
			// possibly implement AddNewActivity code
			// default date shown in calendar for new case is the current day (today)
		}
		else
		{
			// UpdateData activity
			Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
			selected_row_cursor = getContentResolver().query(row_uri, null, null, null, null);
		}

		// get study types list for the spinner
		study_types_cursor = getContentResolver().query(CasesProvider.STUDYTYPE_LIST_URI, null, CasesProvider.KEY_LIST_ITEM_IS_HIDDEN + " = ?", new String[]{"0"}, CasesProvider.KEY_ORDER);
		key_words_cursor = getContentResolver().query(CasesProvider.KEYWORD_LIST_URI, null, CasesProvider.KEY_LIST_ITEM_IS_HIDDEN + " = ?", new String[]{"0"}, CasesProvider.KEY_ORDER);
		section_cursor = getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, CasesProvider.KEY_LIST_ITEM_IS_HIDDEN + " = ?", new String[]{"0"}, CasesProvider.KEY_ORDER);

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		if (selected_row_cursor != null)
		{
			selected_row_cursor.close();
			selected_row_cursor = null;
		}

		if (section_cursor != null)
		{
			section_cursor.close();
			section_cursor = null;
		}

		if (key_words_cursor != null)
		{
			key_words_cursor.close();
			key_words_cursor = null;
		}

		if (study_types_cursor != null)
		{
			study_types_cursor.close();
			study_types_cursor = null;
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		getMenuInflater().inflate(R.menu.case_edit, menu);

		return true;
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

		switch (item.getItemId())
		{
			// Respond to the action bar's Up/Home button
		    case android.R.id.home:
			    saveToDatabase();
			    finish();
                return true;

			case R.id.menu_camera:
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

				return true;

			case R.id.menu_discard:
				setResult(CaseCardListActivity.RESULT_NOCHANGE);
				finish();
				return true;

			case R.id.menu_delete:
				//Toast.makeText(this, "debug: delete " + key_id, Toast.LENGTH_SHORT).show();

				// opens alert dialog to confirm delete
				// delete from CASES, IMAGES, and image files
				// setResult() and finish()
				UtilClass.menuItem_deleteCase(this, key_id);
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}

	}

	/**
	 * UI clicks
	 */

	public void onClick_Button(View view) throws IOException
	{
		switch(view.getId())
		{
			case R.id.saveButton:
				saveToDatabase();
				finish();
				break;
			/*
			case R.id.cancelButton:
				//TODO delete files that we don't need
				setResult(CaseCardListActivity.RESULT_NOCHANGE);
				finish();
				break;

			case R.id.doneButton:
				saveToDatabase();
				finish();
				break;
			*/

			/*
			case R.id.add_new_study_button:
				UtilClass.showMessage(this, "add new");
				LinearLayout linearLayout = (LinearLayout)findViewById(R.id.study_types_container);



				RelativeLayout editStudyTypeLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.edit_study_type, null);

				SpinnerCustom editStudyType = (SpinnerCustom)editStudyTypeLayout.findViewById(R.id.edit_study_type);
				Button editStudyDate = (Button) editStudyTypeLayout.findViewById(R.id.edit_date);

				editStudyType.setPrompt("Study type");
				editStudyType.setItems(study_types_cursor, CasesProvider.COL_LIST_ITEM_VALUE, "Custom study type");

				linearLayout.addView(editStudyTypeLayout);

				*/
				/*
				SpinnerCustom study_type_spinner = new SpinnerCustom(this);
				study_type_spinner.setPrompt("Study type");
				study_type_spinner.setItems(study_types_cursor, CasesProvider.COL_LIST_ITEM_VALUE, "Custom study type");

				linearLayout.removeView(findViewById(R.id.add_new_study_button));
				linearLayout.addView(study_type_spinner);
				*/

				/*
				android:id="@+id/edit_study_type"
				style="@style/customSpinnerStyle"
				android:prompt="@string/studyType_prompt"/>
				*/

			//	break;
		}
	}

	public void onClick_Checkbox(View view)
	{
		switch(view.getId())
		{
			case R.id.checkbox_followup:

				if (followup_bool)
					followup_bool = false;
				else
					followup_bool = true;

				break;
		}

	}


	/**
	 * saveToDatabase
	 * called when click OK button or navigate up button or back button (and confirmed via dialog box)
	 */
	protected void saveToDatabase()
	{
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean pref_auto_add_to_list = sharedPref.getBoolean(getString(R.string.pref_auto_add_to_list_key), true);

		/**
		 * CASES TABLE
		 */

		// put data into "values" for database insert/update
		ContentValues values = new ContentValues();

		// PATIENT ID
		values.put(CasesProvider.KEY_PATIENT_ID, ((EditText)findViewById(R.id.edit_patient_id)).getText().toString());

		// DIAGNOSIS
		values.put(CasesProvider.KEY_DIAGNOSIS, ((EditText)findViewById(R.id.edit_diagnosis)).getText().toString());

		// FINDINGS
		values.put(CasesProvider.KEY_FINDINGS, ((EditText)findViewById(R.id.edit_findings)).getText().toString());

		// STUDY TYPE
		String new_study_type = ((SpinnerCustom)findViewById(R.id.edit_study_type)).getSelectedString();
		if (new_study_type != null && !new_study_type.isEmpty())
		{
			values.put(CasesProvider.KEY_STUDY_TYPE, new_study_type);
		}
		else
		{
			values.put(CasesProvider.KEY_STUDY_TYPE, (String) null);
		}

		// add new study types to database
		if(pref_auto_add_to_list)
		{
			final Cursor study_types_all_cursor = getContentResolver().query(CasesProvider.STUDYTYPE_LIST_URI, null, null, null, CasesProvider.KEY_ORDER);
			if (new_study_type != null && study_types_all_cursor != null && study_types_all_cursor.moveToFirst())
			{
				int studyType_lastPosition = study_types_all_cursor.getCount();
				boolean found = false;

				do
				{
					if (study_types_all_cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE).contentEquals(new_study_type))
					{
						found = true;
						break;
					}

				} while (study_types_all_cursor.moveToNext());

				if (!found)
				{
					// add to database
					ContentValues studytype_values = new ContentValues();
					studytype_values.put(CasesProvider.KEY_STUDY_TYPE, new_study_type);
					studytype_values.put(CasesProvider.KEY_ORDER, studyType_lastPosition);  // put at end of list
					getContentResolver().insert(CasesProvider.STUDYTYPE_LIST_URI, studytype_values);

					studyType_lastPosition += 1;
				}

				study_types_all_cursor.close();
			}
		}


		// STUDY DATE
		String new_date_str = ((Button)findViewById(R.id.edit_date)).getText().toString();
		if (new_date_str != null && !new_date_str.isEmpty())
		{
			// displayed date format in new_date_str button is different than db_date_str
			values.put(CasesProvider.KEY_DATE, db_date_str);
		}
		else
		{
			values.put(CasesProvider.KEY_DATE, (String) null);
		}

		// SECTIONS
		String new_sections = ((SpinnerMultiSelect)findViewById(R.id.edit_section)).getSelectedString();
		if (new_sections != null && !new_sections.isEmpty())
		{
			values.put(CasesProvider.KEY_SECTION, new_sections);
		}
		else
		{
			values.put(CasesProvider.KEY_SECTION, (String) null);
		}

		// add new sections to database
		if(pref_auto_add_to_list)
		{
			final Cursor section_all_cursor = getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER);
			List<String> sectionsList = ((SpinnerMultiSelect) findViewById(R.id.edit_section)).getSelectedStrings();
			int sections_lastPosition = section_all_cursor.getCount();

			for (String section : sectionsList)
			{
				boolean found = false;

				if (section_all_cursor != null && section_all_cursor.moveToFirst())
				{
					do
					{
						if (section_all_cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE).contentEquals(section))
						{
							found = true;
							break;
						}

					} while (section_all_cursor.moveToNext());

					if (!found)
					{
						// add to database
						ContentValues sections_values = new ContentValues();
						sections_values.put(CasesProvider.KEY_SECTION, section);
						sections_values.put(CasesProvider.KEY_ORDER, sections_lastPosition);  // put at end of list
						getContentResolver().insert(CasesProvider.SECTION_LIST_URI, sections_values);

						sections_lastPosition += 1;
					}
				}
			}
			if (section_all_cursor != null)
			{
				section_all_cursor.close();
			}
		}

		// KEYWORDS
		String new_keyWords = ((SpinnerMultiSelect)findViewById(R.id.edit_key_words)).getSelectedString();

		if (new_keyWords != null && !new_keyWords.isEmpty())
		{
			values.put(CasesProvider.KEY_KEYWORDS, new_keyWords);
		}
		else
		{
			values.put(CasesProvider.KEY_KEYWORDS, (String) null);
		}

		// add new keywords to database
		if(pref_auto_add_to_list)
		{
			final Cursor key_words_all_cursor = getContentResolver().query(CasesProvider.KEYWORD_LIST_URI, null, null, null, CasesProvider.KEY_ORDER);
			List<String> keyWordsList = ((SpinnerMultiSelect) findViewById(R.id.edit_key_words)).getSelectedStrings();
			int keyWords_lastPosition = key_words_all_cursor.getCount();

			for (String keyWord : keyWordsList)
			{
				boolean found = false;

				if (key_words_all_cursor != null && key_words_all_cursor.moveToFirst())
				{
					do
					{
						if (key_words_all_cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE).contentEquals(keyWord))
						{
							found = true;
							break;
						}

					} while (key_words_all_cursor.moveToNext());

					if (!found)
					{
						// add to database
						ContentValues keyWords_values = new ContentValues();
						keyWords_values.put(CasesProvider.KEY_KEYWORDS, keyWord);
						keyWords_values.put(CasesProvider.KEY_ORDER, keyWords_lastPosition);  // put at end of list
						getContentResolver().insert(CasesProvider.KEYWORD_LIST_URI, keyWords_values);

						keyWords_lastPosition += 1;
					}
				}
			}
			if (key_words_all_cursor != null)
			{
				key_words_all_cursor.close();
			}
		}

		// BIOPSY
		values.put(CasesProvider.KEY_BIOPSY, ((EditText)findViewById(R.id.edit_biopsy)).getText().toString());

		// COMMENTS
		values.put(CasesProvider.KEY_COMMENTS, ((EditText)findViewById(R.id.edit_comments)).getText().toString());

		// FOLLOWUP
		values.put(CasesProvider.KEY_FOLLOWUP_COMMENT, ((EditText)findViewById(R.id.edit_followup)).getText().toString());

		if(((CheckBox)findViewById(R.id.checkbox_followup)).isChecked())
			values.put(CasesProvider.KEY_FOLLOWUP, 1);
		else
			values.put(CasesProvider.KEY_FOLLOWUP, 0);

		//values.put(CasesProvider.KEY_FAVORITE, ((EditText)findViewById(R.id.edit_favorite)).getText().toString());

		// THUMBNAIL
		values.put(CasesProvider.KEY_THUMBNAIL, imageGridView.getThumbnail());

		// IMAGE COUNT
		//values.put(CasesProvider.KEY_IMAGE_COUNT, numImages + newImageFiles.size());
		values.put(CasesProvider.KEY_IMAGE_COUNT, imageGridView.getCount());

		// LAST MODIFIED DATE
		// format string for database
		SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd-HHmm-ss");
		String today_date_str = db_sdf.format(Calendar.getInstance().getTime());
		values.put(CasesProvider.KEY_LAST_MODIFIED_DATE, today_date_str);

		// UPDATE DATABASE
		if(key_id == -1)
		{
			// Add a new case into the database
			Uri new_case_uri = getContentResolver().insert(CasesProvider.CASES_URI, values);

			// get the key_id of the new case
			key_id = ContentUris.parseId(new_case_uri);
		}
		else
		{
			// Update the existing case in the database
			Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
			getContentResolver().update(row_uri, values, null, null);
		}


		/**
		 * IMAGES TABLE
		 */
		if(imageGridView != null)
		{
			// Save image table data
			ContentValues imageValues = new ContentValues();

			for (int i = 0; i < imageGridView.getCount(); i++)
			{
				// new images in imageGridView have invalid row ID (ie -1)
				if (imageGridView.getImageID(i) == -1)
				{
					//store in image table
					imageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, key_id);
					imageValues.put(CasesProvider.KEY_IMAGE_FILENAME, imageGridView.getImageFilename(i));
					imageValues.put(CasesProvider.KEY_IMAGE_CAPTION, imageGridView.getImageCaption(i));
					imageValues.put(CasesProvider.KEY_ORDER, i);      // set order to display images.  new files last.  //todo user reodering

					getContentResolver().insert(CasesProvider.IMAGES_URI, imageValues);
				}
				else
				{
					imageValues.put(CasesProvider.KEY_IMAGE_CAPTION, imageGridView.getImageCaption(i));
					imageValues.put(CasesProvider.KEY_ORDER, i);      // set order to display images.  new files last.  //todo user reodering

					Uri uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, imageGridView.getImageID(i));
					getContentResolver().update(uri, imageValues, null, null);
				}
			}

			// delete old images
			ArrayList<String> deletedImageList = imageGridView.getDeletedImageList();   // contains full path of files to be deleted
			File deleteFile = null;
			for (int i = 0; i < deletedImageList.size(); i++)
			{
				deleteFile = new File(deletedImageList.get(i));

				// delete from IMAGES table, select by case key_id and fileNAME
				String[] selArgs = {String.valueOf(key_id), deleteFile.getName()};
				getContentResolver().delete(CasesProvider.IMAGES_URI, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ? AND " + CasesProvider.KEY_IMAGE_FILENAME + " = ?", selArgs);

				// delete actual jpg File
				if (deleteFile != null && deleteFile.exists())
				{
					deleteFile.delete();
				}
			}

		}

		setResult(CaseCardListActivity.RESULT_EDITED);
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
								tempImageFile = UtilClass.getPictureFromCamera(activity, REQUEST_IMAGE_CAPTURE);
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
								startActivityForResult(Intent.createChooser(intent,"Select Picture"), REQUEST_SELECT_IMAGE_FROM_FILE);

								break;

						}

					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}


	////////////////////////
	static final int REQUEST_IMAGE_CAPTURE = 31;
	static final int REQUEST_CROP_IMAGE = 32;
	static final int REQUEST_SELECT_IMAGE_FROM_FILE = 33;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
		{
			// successful capture of new photo from camera (called from UtilClass method)
			// replaces tempImageFile with the new cropped image
			// if successful, return to onActivityResult(REQUEST_CROP_IMAGE)

			UtilClass.CropPicture(this, tempImageFile, REQUEST_CROP_IMAGE);

		}
		else if(requestCode == REQUEST_CROP_IMAGE && resultCode == RESULT_OK)
		{
			// successful crop of new photo from the camera (called from onActivityResult(REQUEST_IMAGE_CAPTURE)->UtilClass.CropPicture())
			String path = data.getStringExtra(CropImage.IMAGE_PATH);
			if (path != null)
			{
				// show new image in grid display of key images
				imageGridView.addImage(tempImageFile.getPath());
			}
			else
			{
				// delete tempImageFile since crop was canceled?
				if(tempImageFile.exists())
				{
					tempImageFile.delete();
					tempImageFile = null;
				}
			}

		}
		else if(requestCode == REQUEST_SELECT_IMAGE_FROM_FILE && resultCode == RESULT_OK)
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
				newImageFile = File.createTempFile(key_id + "_", ".jpg", picturesDir);
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
				inputStream = (FileInputStream)getContentResolver().openInputStream(selectedImageUri);

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

			imageGridView.addImage(newImageFile.getPath());
		}
		else if(resultCode != RESULT_OK)
		{
			// delete tempImageFile if image capture was canceled
			if(tempImageFile != null && tempImageFile.exists())
			{
				tempImageFile.delete();
				tempImageFile = null;
			}

			UtilClass.showMessage(this, "Canceled");
		}
	}


	// Button to show datePicker
	public void onClick_showDatePicker(View v)
	{
		//DialogFragment datePicker = DatePickerFragment.newInstance(selected_date.get(Calendar.YEAR), selected_date.get(Calendar.MONTH), selected_date.get(Calendar.DAY_OF_MONTH));
		//datePicker.show(this.getFragmentManager(), "datePicker");


		new DatePickerDialog(this, this, selected_date.get(Calendar.YEAR), selected_date.get(Calendar.MONTH), selected_date.get(Calendar.DAY_OF_MONTH)).show();
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.DatePickerDialog.OnDateSetListener#onDateSet(android.widget.DatePicker, int, int, int)
	 */
	@Override
	public void onDateSet(DatePicker view, int year, int month, int day)
	{

		//	Calendar selected_date = Calendar.getInstance();
		selected_date.set(year, month, day);

		// format string for display box
		SimpleDateFormat display_sdf = new SimpleDateFormat("MMMM d, yyyy");
		String displayDate = display_sdf.format(selected_date.getTime());

		Button date_button = (Button) findViewById(R.id.edit_date);
		//Button date_button = (Button) view;
		date_button.setText(displayDate);

		// set date string for in static string to put into database
		SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd");
		db_date_str = db_sdf.format(selected_date.getTime());

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		// if pressing back, ask if user wants to save the data
		if (keyCode == KeyEvent.KEYCODE_BACK )
		{
			// Alert dialog to confirm save
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setMessage("Save changes?")
					.setPositiveButton(getResources().getString(R.string.button_save), new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{
							// save confirmed
							saveToDatabase();
							finish();

						}
					})
					.setNegativeButton(getResources().getString(R.string.button_discard), new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{
							// discard
							setResult(CaseCardListActivity.RESULT_NOCHANGE);
							finish();
						}
					});

			AlertDialog alert = builder.create();
			alert.show();
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class EditCaseFragment extends Fragment
	{
		private static Cursor case_cursor;

		private SpinnerCustom study_type_spinner;
		private SpinnerMultiSelect key_words_spinner;
		private SpinnerMultiSelect section_spinner;

		private Activity activity;
		private View rootView;

		public EditCaseFragment()
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			final View view = inflater.inflate(R.layout.fragment_case_edit, container, false);

			Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
			((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

			ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setDisplayShowTitleEnabled(false);


			((EditText) view.findViewById(R.id.edit_patient_id)).setRawInputType(Configuration.KEYBOARD_QWERTY);

			// hide soft keyboard if click off keyboard
			view.findViewById(R.id.edit_scrollview).setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					UtilClass.hideKeyboard(getActivity());
					return false;
				}
			});

			// lollipop transitions
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				//getWindow().setSharedElementEnterTransition(TransitionInflater.from(mActivity).inflateTransition(R.transition.shared_element));

				TransitionSet transitionSet = new TransitionSet();

				Transition slideDown = new Slide(Gravity.TOP);
				slideDown.addTarget(toolbar);
				transitionSet.addTransition(slideDown);

				Transition slideUp = new Slide(Gravity.BOTTOM);
				slideUp.addTarget(view.findViewById(R.id.edit_scrollview));
				transitionSet.addTransition(slideUp);

				Transition fade = new Fade();
				fade.excludeTarget(toolbar, true);
				fade.excludeTarget(android.R.id.statusBarBackground, true);
				fade.excludeTarget(android.R.id.navigationBarBackground, true);
				transitionSet.addTransition(fade);

				activity.getWindow().setEnterTransition(transitionSet);
				activity.getWindow().setExitTransition(transitionSet);
			}

			rootView = view;
			return view;
		}


		@Override
		public void onAttach(Activity activity)
		{
			super.onAttach(activity);
			this.activity = activity;
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState)
		{
			// STUDY TYPES SPINNER
			study_type_spinner = (SpinnerCustom) view.findViewById(R.id.edit_study_type);
			study_type_spinner.setItems(study_types_cursor, CasesProvider.COL_LIST_ITEM_VALUE, "Custom study type");

			// SECTION MULTI SPINNER
			section_spinner = (SpinnerMultiSelect) view.findViewById(R.id.edit_section);
			section_spinner.setItems(section_cursor, CasesProvider.COL_LIST_ITEM_VALUE);

			// KEYWORDS MULTI SPINNER
			key_words_spinner = (SpinnerMultiSelect) view.findViewById(R.id.edit_key_words);
			key_words_spinner.setItems(key_words_cursor, CasesProvider.COL_LIST_ITEM_VALUE);

			// Fetch and display data
			// Fetch and display data
			if (selected_row_cursor != null)
			{
				selected_row_cursor.moveToFirst();
				populateFields(getView(), Long.parseLong(selected_row_cursor.getString(CasesProvider.COL_ROWID)));
			}
			// else selected_row_cursor==null, then adding new data row.  no data to fetch
			else
			{
				// set up imageGridView to be able to add images later to new case
				imageGridView = new ImageGridView(getActivity(),(GridView)view.findViewById(R.id.key_image));
				imageGridView.setMode(ImageGridView.EDIT_ACTIVITY);
			}

			super.onViewCreated(view, savedInstanceState);
		}

		public void populateFields(View view, final long selected_key_id)
		{
			// get db row of clicked case
			Uri uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, selected_key_id);
			case_cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

			if (case_cursor.moveToFirst())
			{
				//int key_id = case_cursor.getInt(CasesProvider.COL_ROWID);
				patient_ID = case_cursor.getString(CasesProvider.COL_PATIENT_ID);
				diagnosis = case_cursor.getString(CasesProvider.COL_DIAGNOSIS);
				findings = case_cursor.getString(CasesProvider.COL_FINDINGS);
				String original_sections = case_cursor.getString(CasesProvider.COL_SECTION);
				comments = case_cursor.getString(CasesProvider.COL_COMMENTS);
				String original_keyWords = case_cursor.getString(CasesProvider.COL_KEYWORDS);
				biopsy = case_cursor.getString(CasesProvider.COL_BIOPSY);
				followup_comment = case_cursor.getString(CasesProvider.COL_FOLLOWUP_COMMENT);

				if(case_cursor.getInt(CasesProvider.COL_FOLLOWUP)== 1)
					followup_bool = true;
				else
					followup_bool=false;

				study_type = case_cursor.getString(CasesProvider.COL_STUDY_TYPE);
				db_date_str = case_cursor.getString(CasesProvider.COL_DATE);


				if (patient_ID != null)
				{
					((EditText) view.findViewById(R.id.edit_patient_id)).setText(patient_ID);
					// getActivity().setTitle(patient_ID);
				}

				// Case Information (Diagnosis and Findings)
				if(diagnosis != null)
				{
					EditText TV_diagnosis = (EditText) view.findViewById(R.id.edit_diagnosis);
					TV_diagnosis.setText(diagnosis);
				}
				if(findings != null)
				{
					EditText TV_findings = (EditText) view.findViewById(R.id.edit_findings);
					TV_findings.setText(findings);
				}

				// SECTION LIST
				if (original_sections != null && !original_sections.isEmpty())
				{
					section_spinner.setSelection(original_sections);
				}


				// STUDY TYPE
				if (study_type != null)
				{
					study_type_spinner.setSelection(study_type);
				}


				// STUDY DATE
				// date picker in a button
				if (db_date_str != null && !db_date_str.isEmpty())
				{
					SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd");
					SimpleDateFormat display_sdf = new SimpleDateFormat("MMMM d, yyyy");

					try
					{
						selected_date.setTime(db_sdf.parse(db_date_str));
					}
					catch (ParseException e)
					{
						// Auto-generated catch block
						e.printStackTrace();
					}

					String displayDate = UtilClass.convertDateString(db_date_str, db_sdf, display_sdf);

					Button date_button = (Button) view.findViewById(R.id.edit_date);
					date_button.setText(displayDate);
				}


				// BIOPSY
				if (biopsy != null)
				{
					EditText TV_biopsy = (EditText) view.findViewById(R.id.edit_biopsy);
					TV_biopsy.setText(biopsy);
				}

				// KEY IMAGES
				String [] image_args = {String.valueOf(selected_key_id)};
				Cursor image_cursor = getActivity().getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);
				//numImages = image_cursor.getCount();

				imageGridView = new ImageGridView(getActivity(),(GridView)view.findViewById(R.id.key_image), selected_key_id, image_cursor);
				imageGridView.setThumbnailPosition(Integer.parseInt(case_cursor.getString(CasesProvider.COL_THUMBNAIL)));
				imageGridView.notifyDataSetChanged();

				image_cursor.close();


				// KEYWORD_LIST
				if (original_keyWords != null && !original_keyWords.isEmpty())
				{
					key_words_spinner.setSelection(original_keyWords);
				}

				// FOLLOWUP
				((CheckBox)view.findViewById(R.id.checkbox_followup)).setChecked(followup_bool);

				if(followup_bool)
				{
					if(followup_comment != null && !followup_comment.isEmpty())
					{
						EditText TV_followup = (EditText) view.findViewById(R.id.edit_followup);
						TV_followup.setText(followup_comment);
					}
				}

				// COMMENTS
				if (comments != null)
				{
					EditText TV_comments = (EditText) view.findViewById(R.id.edit_comments);
					TV_comments.setText(comments);
				}


				/*
				ActionBar actionBar = getActivity().getActionBar();
				if(patient_ID != null && !patient_ID.isEmpty())
				{
					actionBar.setTitle(patient_ID);

					if(section != null)
						actionBar.setSubtitle(section);
				}
				else if(section != null)
				{
					actionBar.setTitle(section + " Case");
				}
				else if(diagnosis != null)
				{
					actionBar.setTitle(diagnosis);
				}
				else
				{
					actionBar.setTitle(getResources().getString(R.string.title_activity_edit_case));
				}
				*/

				case_cursor.close();
			}

		} //end populateFields

	} // end EditCaseFragment


}
