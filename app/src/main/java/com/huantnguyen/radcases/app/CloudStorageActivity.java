package com.huantnguyen.radcases.app;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
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
	protected void onResume()
	{
		super.onResume();
		//if(isGooglePlayServicesAvailable(this) == SUCCESS)
		{

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

	public void onClick_Button(View view) throws IOException
	{
		switch(view.getId())
		{
			case R.id.backup_button:
				String filename;  //TODO get from edit text box

				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle("Create Backup File");
				alert.setMessage("Filename");

				// default file
				//File storageDir = getApplication().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

				// Create an image file name based on timestamp
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
				filename = "Backup_cases_" + timeStamp;

				// Set an EditText view to get user input
				final EditText input = new EditText(this);
				input.setText(filename);

				alert.setView(input);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();

						/*
						File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);


						File tempCSV = null;
						try
						{
							tempCSV = File.createTempFile(value, ".csv", storageDir);
							//tempDB = File.createTempFile(value, ".db", storageDir);
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}


						// create CSV file
						if(exportCasesCSV(tempCSV))
						{
							Toast.makeText(getApplicationContext(), "Saved data to " + tempCSV, Toast.LENGTH_LONG).show();
						}
						*/

						// export SQLite file
						backupDB(value);

					}
				});

				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

				alert.show();

				break;

			case R.id.restore_button:
				Intent intent = new Intent();
				intent.setType("DOWNLOADS/*.csv");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				//intent.putExtra("image_filename", filename);
				startActivityForResult(Intent.createChooser(intent,"Select Backup File"), REQUEST_SELECT_BACKUP_FILE);

				break;
		}
	}

	private boolean exportCasesCSV(File outFile)
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
			UtilsFile.zip(image_filenames, zip_filename);

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

	public void importCasesCSV(File inFile)
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



		try
		{
			br = new BufferedReader(new FileReader(inFile));

			// If successfully opened file, clear old database: delete all rows from CASES and IMAGES tables in the database
			getContentResolver().delete(CasesProvider.CASES_URI, null, null);
			getContentResolver().delete(CasesProvider.IMAGES_URI, null, null);

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
			Toast.makeText(this, "Unable to open CSV file", Toast.LENGTH_SHORT).show();
			return;
		}

		// unzip image files
		try
		{
			// zip file name is same as the csv file, except with zip extension
			// unzip image files to android pictures directory
			UtilsFile.unzip(inFile.getPath().replace("csv", "zip"),getApplication().getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to open images zip file:", Toast.LENGTH_SHORT).show();
			return;
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

					//File restoreFile = new File(restoreFilename);
					//importCasesCSV(restoreFile);

					restoreDB(restoreFilename);

				}
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	/////////////////////

	//importing database
	private void restoreDB(String restoreFilename)
	{
		// TODO Auto-generated method stub

		try {
			//File sd = Environment.getExternalStorageDirectory();
			File appDir = getApplication().getExternalFilesDir(null);   // internal app data directory
			File data  = Environment.getDataDirectory();

			String currentDBDirPath = "//data//" + getPackageName() + "//databases//";
			File currentDBDir  = new File(data, currentDBDirPath);

			if (currentDBDir.canWrite())
			{
				/*
				String  currentDBPath= "//data//" + getPackageName() + "//databases//" + CasesProvider.DATABASE_NAME;
				String backupDBPath  = "/Backup/" + restoreFilename;
				File  backupDB= new File(data, currentDBPath);
				File currentDB  = new File(appDir, backupDBPath);

				FileChannel src = new FileInputStream(currentDB).getChannel();
				FileChannel dst = new FileOutputStream(backupDB).getChannel();
				*/

				String currentDBPath = "//data//" + getPackageName() + "//databases//" + CasesProvider.DATABASE_NAME;
				File currentDB  = new File(data, currentDBPath);
				File  backupDB= new File(restoreFilename);

				FileChannel src = new FileInputStream(backupDB).getChannel();
				FileChannel dst = new FileOutputStream(currentDB).getChannel();

				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
				Toast.makeText(getBaseContext(), backupDB.toString(), Toast.LENGTH_LONG).show();

			}
		} catch (Exception e) {

			Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG)
					.show();

		}
	}
	//exporting database
	private boolean backupDB(String backupFilename) {
		// TODO Auto-generated method stub

		try {
			//File sd = Environment.getExternalStorageDirectory();
			File appDir = getApplication().getExternalFilesDir(null);   // internal app data directory
			File data = Environment.getDataDirectory();

			if (appDir.canWrite()) {
				String  currentDBPath= "//data//" + getPackageName() + "//databases//" + CasesProvider.DATABASE_NAME;
				String backupDBPath  = "/Backup/" + backupFilename;
				File currentDB = new File(data, currentDBPath);
				File backupDB = new File(appDir, backupDBPath);


				String backupDirPath  = "/Backup/";
				File backupDir = new File(appDir, backupDirPath);

				if(!backupDir.exists())
				{
					if(backupDir.mkdirs())
					{
						Toast.makeText(this, "Created directory: " + backupDir.getPath(), Toast.LENGTH_LONG).show();
					}
					else
					{
						Toast.makeText(this, "Unable to create directory: " + backupDir.getPath(), Toast.LENGTH_LONG).show();
					}

				}

				FileChannel src = new FileInputStream(currentDB).getChannel();
				FileChannel dst = new FileOutputStream(backupDB).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
				Toast.makeText(getBaseContext(), backupDB.toString(), Toast.LENGTH_LONG).show();

			}
		} catch (Exception e) {

			Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
			return false;

		}

		// success
		return true;
	}


	//////////////////////

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
