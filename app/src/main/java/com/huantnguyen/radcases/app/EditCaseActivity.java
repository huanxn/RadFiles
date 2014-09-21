package com.huantnguyen.radcases.app;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import eu.janmuller.android.simplecropimage.CropImage;

public class EditCaseActivity extends Activity implements DatePickerDialog.OnDateSetListener
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
	static String db_date_str;

	static Cursor key_words_cursor;
	static Cursor section_cursor;

	static String biopsy;
	static String comments;
	static String followup_comment;
	static boolean followup_bool = false;

	private static ImageAdapter mGridAdapter;
	private static GridView gridview;

	private static final int MAX_IMAGES = CasesProvider.MAX_NUM_IMAGES;
	private static String [] tempImageFilename;	            // images to add if user presses "Save"
	private static int numImages;
	private File tempImageFile;
	private static int imageCounter;
	private static int new_image_index;
	private static int numNewImages;
	private static int [] deleteImages = new int[MAX_IMAGES];  // indeces of images marked for deletion if user presses "Save"

	private static Animator mCurrentAnimator;

	private static boolean confirmSave = false;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_case);

		if (savedInstanceState == null)
		{
			getFragmentManager().beginTransaction()
					.add(R.id.container, new EditCaseFragment()).commit();
		}

		tempImageFilename = new String[MAX_IMAGES];
		imageCounter = 0;
		numNewImages = 0;
		numImages = 0;

		selected_date = Calendar.getInstance();

		// get the intent and the method argument: unique CASE KEY_ID
		Intent intent = getIntent();
		key_id = intent.getLongExtra(CaseCardListActivity.ARG_KEY_ID, -1);


		// Add or Update data, base on whether key_id was passed from parent activity
		if (key_id == -1)
		{
			// AddData to database
			// no key_id passed from the parent activity

			Toast.makeText(this, "Add data", Toast.LENGTH_SHORT).show();

/*
			//TODO possibly implement AddNewActivity code
			//AddNew
			study_type = "";
			// default date shown in calendar for new case is the current day (today)

			//Action Bar
*/
		}
		else
		{
			// UpdateData activity


			Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
			selected_row_cursor = getContentResolver().query(row_uri, null, null, null, null);

			//ActionBar
		}


		// get study types list for the spinner
		study_types_cursor = getContentResolver().query(CasesProvider.STUDYTYPE_LIST_URI, null, null, null, CasesProvider.KEY_ORDER);
		key_words_cursor = getContentResolver().query(CasesProvider.KEYWORD_LIST_URI, null, null, null, CasesProvider.KEY_ORDER);
		section_cursor = getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER);

		getActionBar().setDisplayHomeAsUpEnabled(false);
		ActionBar actionBar = getActionBar();
		actionBar.setTitle("Save");
		actionBar.setIcon(R.drawable.ic_action_accept);


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
		if(super.onCreateOptionsMenu(menu))
		{
			// Inflate the menu; this adds items to the action bar if it is present.
			if (key_id == -1)
			{
				getMenuInflater().inflate(R.menu.add_case, menu);
			}
			else
			{
				getMenuInflater().inflate(R.menu.edit_case, menu);
			}
		}

		return true;
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

			    /*
			    try
			    {

			    }
			    catch(IOException ex)
			    {
				    Log.e(getClass().getName(), "Could not save to image file.");
			    }
			    */
                return true;

			case R.id.action_camera:
				onClick_getPicture(item.getActionView());
				return true;

			case R.id.action_delete:
				Toast.makeText(this, "debug: delete " + key_id, Toast.LENGTH_SHORT).show();

				// opens alert dialog to confirm delete
				// delete from CASES, IMAGES, and image files
				// setResult() and finish()
				UtilClass.menuItem_deleteCase(this, key_id);
				return true;


			case R.id.action_settings:
				return true;

		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * UI clicks
	 */
	public void onCheckboxClicked_followup(View view)
	{
		if(followup_bool)
			followup_bool=false;
		else
			followup_bool=true;

	}

	/**
	 * Called when the user clicks the OK button
	 */
	public void onClick_doneButton(View view) throws IOException
	{
		saveToDatabase();
		finish();

	}

	/**
	 * saveToDatabase
	 * called when click OK button or navigate up button or back button (and confirmed via dialog box)
	 */
	private void saveToDatabase()
	{
		ContentValues values = new ContentValues();

		// put data into "values" for database insert/update
		values.put(CasesProvider.KEY_PATIENT_ID, ((EditText)findViewById(R.id.edit_patient_id)).getText().toString());
		values.put(CasesProvider.KEY_DIAGNOSIS, ((EditText)findViewById(R.id.edit_diagnosis)).getText().toString());
		values.put(CasesProvider.KEY_FINDINGS, ((EditText)findViewById(R.id.edit_findings)).getText().toString());


		if (study_type != null)
		{
			values.put(CasesProvider.KEY_STUDY_TYPE, study_type);
		}
		else
		{
			values.put(CasesProvider.KEY_STUDY_TYPE, (String) null);
		}

		if (db_date_str != null)
		{
			values.put(CasesProvider.KEY_DATE, db_date_str);
		}
		else
		{
			values.put(CasesProvider.KEY_DATE, (String) null);
		}

		// SECTIONS
		String new_sections = ((MultiSelectSpinner)findViewById(R.id.edit_section)).getSelectedString();
		if (new_sections != null && !new_sections.isEmpty())
		{
			values.put(CasesProvider.KEY_SECTION, new_sections);
		}
		else
		{
			values.put(CasesProvider.KEY_SECTION, (String) null);
		}

		// KEYWORDS
		String new_keyWords = ((MultiSelectSpinner)findViewById(R.id.edit_key_words)).getSelectedString();
		if (new_keyWords != null && !new_keyWords.isEmpty())
		{
			values.put(CasesProvider.KEY_KEYWORDS, new_keyWords);
		}
		else
		{
			values.put(CasesProvider.KEY_KEYWORDS, (String) null);
		}

		values.put(CasesProvider.KEY_BIOPSY, ((EditText)findViewById(R.id.edit_biopsy)).getText().toString());
		values.put(CasesProvider.KEY_COMMENTS, ((EditText)findViewById(R.id.edit_comments)).getText().toString());
		values.put(CasesProvider.KEY_FOLLOWUP_COMMENT, ((EditText)findViewById(R.id.edit_followup)).getText().toString());
		if(followup_bool)
			values.put(CasesProvider.KEY_FOLLOWUP, 1);
		else
			values.put(CasesProvider.KEY_FOLLOWUP, 0);

		//values.put(CasesProvider.KEY_FAVORITE, ((EditText)findViewById(R.id.edit_favorite)).getText().toString());

		values.put(CasesProvider.KEY_IMAGE_COUNT, numImages+numNewImages);

		if(key_id == -1)
		{
			// Add a new case into the database
			Uri new_case_uri = getContentResolver().insert(CasesProvider.CASES_URI, values);

			// get the key_id of the new case
			key_id = ContentUris.parseId(new_case_uri);
		}
		else
		{
			// Update the case in the database
			Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
			getContentResolver().update(row_uri, values, null, null);
		}


		ContentValues imageValues = new ContentValues();

		for(int i = 0; i < numNewImages; i++)
		{
			imageValues.clear();

			//TODO create copy of file into app directory (probably should right after image capture)
			//get file path
			//store in image table
			imageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, key_id);
			imageValues.put(CasesProvider.KEY_IMAGE_FILENAME, tempImageFilename[i]);
			imageValues.put(CasesProvider.KEY_ORDER, numImages+i);

			getContentResolver().insert(CasesProvider.IMAGES_URI, imageValues);
		}

		// TODO delete old images
		/*
		//for(int i = 0; i < imageCounter; i++)
		{

			imageValues.clear();
			imageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, (int)key_id);
			imageValues.put(CasesProvider.KEY_IMAGE_FILENAME, tempImageFilename[0]);
			getContentResolver().insert(CasesProvider.IMAGES_URI, imageValues);
		}
*/

		setResult(CaseCardListActivity.RESULT_EDITED);
	}

	/**
	 * Called when the user clicks the cancel button
	 */
	public void onClick_cancelButton(View view)
	{
		//TODO delete files that we don't need
		setResult(CaseCardListActivity.RESULT_NOCHANGE);
		finish();
	}

	/**
	 * Called when clicking to add new image
	 * opens alert dialog to choose from file/gallery or take a photo with camera intent
	 * @param view
	 */
	public void onClick_getPicture(View view)
	{

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
								getPictureFromCamera();
								//TODO change for multiple
								break;

							// select file from chooser
							// either local database or TODO cloud storage
							case 1:
								// in onCreate or any event where your want the user to
								// select a file
								Intent intent = new Intent();
								intent.setType("image/*");
								intent.setAction(Intent.ACTION_GET_CONTENT);
								intent.putExtra("image_filename", tempImageFilename[imageCounter]);
								startActivityForResult(Intent.createChooser(intent,"Select Picture"), REQUEST_SELECT_IMAGE_FROM_FILE);

								break;

						}

					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}


	/**
	 * Runs from alert dialog selection after user chooses to add a new photo with the camera
	 * take new photo with UtilClass method, which uses the camera intent and crop intent
	 */
	public void getPictureFromCamera()
	{
		/*
		imageCounter = 0;
		tempImageFilename[imageCounter] = UtilClass.getCroppedPictureFromCamera(this);

		UtilClass.setPic((ImageView) findViewById(R.id.edit_ImageView), tempImageFilename[imageCounter], IMAGE_SIZE);
		UtilClass.setPic((ImageView) findViewById(R.id.edit_ImageView_expanded), tempImageFilename[imageCounter], IMAGE_SIZE_EXPANDED);

		// Increment counter for next image captured
		imageCounter += 1;
		*/

		// runs camera intent.  returns result code to onActivityResult, which will run crop intent if successful
		tempImageFile = UtilClass.getPictureFromCamera(this, REQUEST_IMAGE_CAPTURE);

	}

	////////////////////////
	static final int REQUEST_IMAGE_CAPTURE = 1;
	static final int REQUEST_CROP_IMAGE = 2;
	static final int REQUEST_SELECT_IMAGE_FROM_FILE = 3;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
		{
			// successful capture of new photo from camera (called from UtilClass method)
			// replaces tempImageFile with the new cropped image
			// if successful, return to onActivityResult request crop image
			UtilClass.CropPicture(this, tempImageFile, REQUEST_CROP_IMAGE);
		}
		else if(requestCode == REQUEST_CROP_IMAGE && resultCode == RESULT_OK)
		{
			/*
			//Wysie_Soh: Delete the temporary file
			File f = new File(mImageCaptureUri.getPath());
			if (f.exists())
			{
				f.delete();
			}
			*/

			String path = data.getStringExtra(CropImage.IMAGE_PATH);
			if (path != null)
			{
				// set the new cropped image File name into temporary variable; for update to database after user clicks "OK" to save
				tempImageFilename[imageCounter] = tempImageFile.getAbsolutePath();

				// show new image in grid display of key images
				if(mGridAdapter != null)
				{
					mGridAdapter.addImage(tempImageFilename[imageCounter]);
					UtilClass.expandGridView(gridview, UtilClass.IMAGE_GRID_SIZE);
				}

				// Increment counter for next image captured
				imageCounter += 1;
				numNewImages += 1;

			}
			else
			{
				// delete tempImageFile since crop was canceled?
			}



		}
		else if(requestCode == REQUEST_SELECT_IMAGE_FROM_FILE && resultCode == RESULT_OK)
		{
			//todo make copy of file into app pictures folder
			//UtilClass.copyFile(src, dst);

			Uri selectedImageUri = data.getData();
			tempImageFilename[imageCounter] = UtilClass.getFilePathFromResult(this, selectedImageUri);

			//UtilClass.setPic(imageViews[imageCounter], tempImageFilename[imageCounter], UtilClass.IMAGE_THUMB_SIZE);

			if (mGridAdapter != null)
			{
				mGridAdapter.addImage(tempImageFilename[imageCounter]);
				UtilClass.expandGridView(gridview, UtilClass.IMAGE_GRID_SIZE);
			}



	//not using this anymore i think
	// 		UtilClass.setPic((ImageView) findViewById(R.id.edit_ImageView_expanded), tempImageFilename[imageCounter], UtilClass.IMAGE_SIZE);

			// Increment counter for next image captured
			imageCounter += 1;
			numNewImages += 1;

			//UtilClass.expandGridView(gridview, UtilClass.IMAGE_GRID_SIZE);
		}
	}


	// Button to show datePicker
	public void onClick_showDatePicker(View v)
	{
		//TODO open datepicker with default to date selected in button text
		DialogFragment datePicker = DatePickerFragment.newInstance(selected_date.get(Calendar.YEAR), selected_date.get(Calendar.MONTH), selected_date.get(Calendar.DAY_OF_MONTH));

		datePicker.show(this.getFragmentManager(), "datePicker");
	}

	// Button to show KeyWords checkboxlist
	public void onClick_showKeyWords(View v)
	{
		//TODO open datepicker with default to date selected in button text
		Toast.makeText(this,"Clicked key words",Toast.LENGTH_SHORT).show();
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

		private CustomSpinner study_type_spinner;
		private MultiSelectSpinner key_words_spinner;
		private MultiSelectSpinner section_spinner;

		public EditCaseFragment()
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_edit_case, container, false);
			return rootView;
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState)
		{


			// SPINNER
			study_type_spinner = (CustomSpinner) view.findViewById(R.id.edit_study_type);
			String[] columns = new String[]{CasesProvider.KEY_STUDY_TYPE};
			int[] to = new int[]{android.R.id.text1};

			SimpleCursorAdapter spinner_list_adapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_spinner_dropdown_item, study_types_cursor, columns, to, 0);
			spinner_list_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

			study_type_spinner.setAdapter(spinner_list_adapter);

			study_type_spinner.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
				{
					//if(position == ??) TODO custom input
					Cursor c = (Cursor) parentView.getItemAtPosition(position);
					study_type = c.getString(CasesProvider.COL_VALUE);

		//			c.close();

					return;
				}

				@Override
				public void onNothingSelected(AdapterView<?> parentView)
				{
				}


			});
			//end spinner


			section_spinner = (MultiSelectSpinner) view.findViewById(R.id.edit_section);
			section_spinner.setItems(section_cursor);


			key_words_spinner = (MultiSelectSpinner) view.findViewById(R.id.edit_key_words);
			key_words_spinner.setItems(key_words_cursor);

			// Fetch and display data
			if (selected_row_cursor != null)
			{
				selected_row_cursor.moveToFirst();
				populateFields(getView(), Long.parseLong(selected_row_cursor.getString(CasesProvider.COL_ROWID)));
			}
			// else selected_row_cursor==null, then adding new data row.  no data to fetch
			else
			{
				gridview = (GridView) view.findViewById(R.id.imageGridview);
				mGridAdapter = new ImageAdapter(getActivity());
				gridview.setAdapter(mGridAdapter);
			}

			super.onViewCreated(view, savedInstanceState);
		}

		public void populateFields(View view, final long selected_key_id)
		{
			/*
			//TODO FIX THIS to refresh?
			if (getArguments().containsKey(ARG_CASE_ID))
			{
				selected_key_id = getArguments().getLong(ARG_CASE_ID);

				// get db row of clicked case
				Uri uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, selected_key_id);
				case_cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

			}
			else    //TODO show error message (no selected key id from listview)
			{
				selected_key_id = -1;
				return;
			}
			*/

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
				numImages = case_cursor.getInt(CasesProvider.COL_IMAGE_COUNT);


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
					//find study type position
					if (study_types_cursor.moveToFirst())
					{
						do
						{
							if (study_type.equalsIgnoreCase(study_types_cursor.getString(CasesProvider.COL_VALUE)))
							{
								int position = study_types_cursor.getPosition();
								study_type_spinner.setSelection(position);
								break;
							}

						} while (study_types_cursor.moveToNext());

					}

				}


				// STUDY DATE
				// date picker in a button
				if (db_date_str != null)
				{
					SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd");
					SimpleDateFormat display_sdf = new SimpleDateFormat("MMMM d, yyyy");

					try
					{
						selected_date.setTime(db_sdf.parse(db_date_str));
					}
					catch (ParseException e)
					{
						// TODO Auto-generated catch block
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

				//if(numImages > 0)
				{

					// get all of the images linked to this case _id
					String [] image_args = {String.valueOf(selected_key_id)};
					Cursor image_cursor = getActivity().getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

					//////////////
					// IMAGE GRID VIEW
					gridview = (GridView) view.findViewById(R.id.imageGridview);
					mGridAdapter = new ImageAdapter(getActivity());
					mGridAdapter.setImages(image_cursor);
					gridview.setAdapter(mGridAdapter);

					gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
						public void onItemClick(AdapterView<?> parent, View v, int position, long id)
						{
							Intent imageGalleryIntent = new Intent(getActivity(), KeyImageGalleryActivity.class);
							imageGalleryIntent.putExtra(CaseCardListActivity.ARG_KEY_ID, selected_key_id);
							imageGalleryIntent.putExtra(KeyImageGalleryActivity.ARG_POSITION, position);
							startActivity(imageGalleryIntent);
						}
					});

					gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
						public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
						{
							// Delete image
							final long image_key_id = id;
							final int image_position = position;

							AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
							CharSequence[] imageSources = {"Delete Image", "Set Thumbnail", "Cancel"};
							builder.setItems(imageSources, new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog, int index)
								{
									switch (index)
									{
										// Delete the photo.
										case 0:

											// if image_key_id = -1, then it is a temporary image, not in database
											if(image_key_id != -1)
											{
												// delete from IMAGES table by unique row id
												Uri image_row_uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, image_key_id);
												getActivity().getContentResolver().delete(image_row_uri, null, null);

												// Update the CASES table, with image count decremented by 1
												ContentValues case_values = new ContentValues();
												case_values.put(CasesProvider.KEY_IMAGE_COUNT, numImages--);

												Uri case_row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
												getActivity().getContentResolver().update(case_row_uri, case_values, null, null);
											}

											// delete from gridview adapter
											mGridAdapter.deleteImage(image_position);
											UtilClass.expandGridView(gridview, UtilClass.IMAGE_GRID_SIZE);

											// TODO Delete the actual file


											break;

										// Set thumbnail
										case 1:
											//TODO set thumbnail
											break;

										// Cancel.  Do Nothing.
										case 2:
											break;

									}
								}
							});

							AlertDialog alert = builder.create();
							alert.show();

							Toast.makeText(getActivity(), "Long press image: " + position + " / " + id, Toast.LENGTH_SHORT).show();
							return false;
						}
					});

					mGridAdapter.notifyDataSetChanged();
					UtilClass.expandGridView(gridview, UtilClass.IMAGE_GRID_SIZE);
					////////////////
				}

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

			}
		}



	}


}
