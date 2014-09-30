package com.huantnguyen.radcases.app;

import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CloudStorageActivity extends GoogleBaseActivity
{
	String TAG = "CloudStorageActivity";

	private CloudStorageFragment fragment;
	String restoreFilename;

	final static int REQUEST_SELECT_BACKUP_FILE = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    setDrawerPosition(NavigationDrawerActivity.POS_CLOUD_STORAGE);
        //setContentView(R.layout.activity_cloud_storage);

	    if (savedInstanceState == null)
	    {
		    // Create the detail fragment and add it to the activity
		    // using a fragment transaction.

		    fragment = new CloudStorageFragment();

		    getFragmentManager().beginTransaction()
				    .add(R.id.container, fragment)
				    .commit();

	    }
    }


    @Override
    // Inflate the menu; this adds items to the action bar if it is present.
    public boolean onCreateOptionsMenu(Menu menu)
    {
		getMenuInflater().inflate(R.menu.cloud_storage, menu);

	    return super.onCreateOptionsMenu(menu);     // nav drawer
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	public void onClick_backupButton(View view)
	{
		String filename;  //TODO get from edit text box



		//File storageDir = getApplication().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		String state = Environment.getExternalStorageState();

		// Create an image file name based on timestamp
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
		filename = "Backup_cases_" + timeStamp + "_";


		File tempFile = null;
		try
		{
			tempFile= File.createTempFile(filename, ".csv", storageDir);

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		/*
//		if(tempFile != null)
		File tempFile = new File(filename);
		try
		{
			tempFile.createNewFile();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
*/
		if(backupCases(tempFile))
		{
			Toast.makeText(this, "Saved data to " + tempFile, Toast.LENGTH_LONG).show();
		}


		/*
		// IMAGES
		filename = "Backup_images_" + timeStamp + "_";

		try
		{
			tempFile= File.createTempFile(filename, ".csv", storageDir);

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
*/
	}

	public void onClick_restoreButton(View view)
	{
		Intent intent = new Intent();
		intent.setType("DOWNLOADS/*.csv");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		//intent.putExtra("image_filename", filename);
		startActivityForResult(Intent.createChooser(intent,"Select Backup"), REQUEST_SELECT_BACKUP_FILE);
	}

	private boolean backupCases(File outFile)
	{
		Boolean returnCode = false;


		String csvHeader = "";
		String csvValues = "";

		// to zip all images into a file for backup
		String image_filenames[] = new String[0];
		int image_counter = 0;

		String value = "";

		for (int i = 0; i < CasesProvider.ALL_KEYS.length; i++) {
			if (csvHeader.length() > 0) {
				csvHeader += ",";
			}
			csvHeader += "\"" + CasesProvider.ALL_KEYS[i] + "\"";
		}

		csvHeader += ",\"Image Files\"\n";

		Log.d(TAG, "header=" + csvHeader);


		try {
			//File outFile = new File(outFileName);
			FileWriter fileWriter = new FileWriter(outFile);
			BufferedWriter out = new BufferedWriter(fileWriter);

			//			FileOutputStream outputStream = openFileOutput(outFileName, Context.MODE_PRIVATE);


			Cursor cursor = getContentResolver().query(CasesProvider.CASES_URI, null, null, null, null, null);

			if (cursor.moveToFirst())
			{
				out.write(csvHeader);
				//	outputStream.write(csvHeader.getBytes());

				// loop through all cases
				do
				{
					csvValues = "";
					// output all case columns for this case
					for (int i = 0; i < CasesProvider.ALL_KEYS.length; i++)
					{
						if (i > 0)
						{
							csvValues += ",";
						}

						if (i == 6 || i == 11)
							csvValues += "\"" + String.valueOf(cursor.getInt(i)) + "\"";
						else
						{

							value = cursor.getString(i);

							if(value == null)
								value = "";

							csvValues += "\"" + value + "\"";
						}
					}

					// output image files in last columns
					String [] image_args = {String.valueOf(cursor.getInt(CasesProvider.COL_ROWID))};
					Cursor imageCursor = getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

					if(imageCursor.moveToFirst())
					{
						do
						{
							csvValues += "," + "\"" + imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME) + "\"";

							image_filenames = UtilClass.addArrayElement(image_filenames, imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME));

						} while (imageCursor.moveToNext());
					}

					out.write(csvValues+"\n");
					//			outputStream.write(csvValues.getBytes());
				} while (cursor.moveToNext());
				cursor.close();

			}
			out.close();
			//outputStream.close();

			// backup actual images into a zip
			String zip_filename = outFile.getPath().replace("csv", "zip");
			UtilClass.zip(image_filenames, zip_filename);

			returnCode = true;
		} catch (IOException e) {
			returnCode = false;
			e.printStackTrace();
			Log.d(TAG, "IOException: " + e.getMessage());
		}

		/*
		ResultCallback<ContentsResult> contentsCallback = new
				                                                           ResultCallback<ContentsResult>()
				                                                           {
					                                                           @Override
					                                                           public void onResult(ContentsResult result)
					                                                           {
						                                                           if (!result.getStatus().isSuccess())
						                                                           {
							                                                           // Handle error
							                                                           return;
						                                                           }

						                                                           MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
								                                                                                                 .setMimeType("text/html").build();
						                                                           IntentSender intentSender = Drive.DriveApi
								                                                                                       .newCreateFileActivityBuilder()
								                                                                                       .setInitialMetadata(metadataChangeSet)
								                                                                                       .setInitialContents(result.getContents())
								                                                                                       .build(getGoogleApiClient());
						                                                           try
						                                                           {
							                                                           startIntentSenderForResult(intentSender, 1, null, 0, 0, 0);
						                                                           }
						                                                           catch (IntentSender.SendIntentException e)
						                                                           {
							                                                           // Handle the exception
						                                                           }
					                                                           }
				                                                           };

*/

		return returnCode;
	}

	public void restoreCases(File inFile)
	{
		BufferedReader br = null;
		String line;
		Uri rowUri = null;
		int parent_id;
		int imageCount = 0;

		ContentValues insertCaseValues = new ContentValues();
		ContentValues insertImageValues = new ContentValues();


		// Clear old data
		// TODO: Delete old image files using the images table to find the file locations

		// Delete all rows from CASES and IMAGES tables in the database
		getContentResolver().delete(CasesProvider.CASES_URI, null, null);
		getContentResolver().delete(CasesProvider.IMAGES_URI, null, null);

		try
		{
			br = new BufferedReader(new FileReader(inFile));

			br.readLine(); // header

			while ( (line=br.readLine()) != null)
			{
				line = line.substring(1, line.length()-1);  // trim the double-quotes off
				String[] values = line.split("\",\"");
				insertCaseValues.clear();
				// input all columns for this case, except row_id
				for (int i = 1; i < CasesProvider.ALL_KEYS.length; i++)
				{
					if(i>=values.length) // the rest of fields are blank
						break;

					insertCaseValues.put(CasesProvider.ALL_KEYS[i], values[i]);

					if(CasesProvider.ALL_KEYS[i].contentEquals(CasesProvider.KEY_IMAGE_COUNT))
						imageCount = Integer.valueOf(values[i]);
				}

				// insert the set of case info into the DB cases table
				rowUri = getContentResolver().insert(CasesProvider.CASES_URI, insertCaseValues);
				// get parent key information
				parent_id = Integer.valueOf(rowUri.getLastPathSegment());

				// insert image file info to Images table, linking to parent case info
				for(int j = 0; j < imageCount; j++)
				{
					insertImageValues.clear();
					insertImageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, parent_id);
					insertImageValues.put(CasesProvider.KEY_IMAGE_FILENAME, values[CasesProvider.ALL_KEYS.length + j]);
					getContentResolver().insert(CasesProvider.IMAGES_URI, insertImageValues);
					// insertImageValues.put(KEY_ORDER, 0); backup in correct order.  so that it will restore in specified order
				}


			}
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		// unzip image files
		try
		{
			// zip file name is same as the csv file, except with zip extension
			// unzip image files to android pictures directory
			UtilClass.unzip(inFile.getPath().replace("csv", "zip"),getApplication().getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		Toast.makeText(this, "Restored database", Toast.LENGTH_SHORT).show();

	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_SELECT_BACKUP_FILE:
				if (resultCode == RESULT_OK) {
					// Get the Uri of the selected file
					Uri uri = data.getData();
					//String filename = data.getStringExtra()
					Log.d(TAG, "File Uri: " + uri.toString());
					// Get the path
				//	String path = FileUtils.getPath(this, uri);
					restoreFilename = uri.getPath();
					Log.d(TAG, "File Uri: " + restoreFilename);

				//	Log.d(TAG, "File Path: " + path);
					// Get the file instance
					// File file = new File(path);
					// Initiate the upload

					File restoreFile = new File(restoreFilename);
					restoreCases(restoreFile);

				}
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public static class CloudStorageFragment extends Fragment
	{
		public CloudStorageFragment()
		{

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_cloud_storage, container, false);
			return rootView;
		}
	}

}
