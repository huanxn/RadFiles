package com.huantnguyen.radcases.app;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
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

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import com.google.android.gms.drive.DriveApi.DriveIdResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CloudStorageActivity extends GoogleDriveBaseActivity
{
	String TAG = "CloudStorageActivity";

	private CloudStorageFragment fragment;
	String restoreFilename;
	String exportFilename;

	final static int REQUEST_SELECT_BACKUP_FILE = 0;
	final static int REQUEST_SELECT_CSV_FILE = 1;
	final static int REQUEST_CREATE_GOOGLE_DRIVE_FILE = 2;

	private File local_backup_zip_file;


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


		switch(view.getId())
		{
			case R.id.backup_button:


				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();

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

			case R.id.exportCSV_button:


				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						exportFilename = value;

						// create CSV file
						if(exportCasesCSV(value))
						{
							Toast.makeText(getApplicationContext(), "Saved data to " + value + ".zip", Toast.LENGTH_LONG).show();
						}
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
				Intent restoreIntent = new Intent();
				//restoreIntent.setType("DOWNLOADS/*.db");
				restoreIntent.setDataAndType(Uri.parse(getApplication().getExternalFilesDir(null).getPath() + "/Backup/"), "text/db");
				restoreIntent.setAction(Intent.ACTION_GET_CONTENT);
				//intent.putExtra("image_filename", filename);
				startActivityForResult(Intent.createChooser(restoreIntent,"Select Backup File"), REQUEST_SELECT_BACKUP_FILE);

				break;

			case R.id.importCSV_button:
				Intent importIntent = new Intent();
				//importIntent.setType("DOWNLOADS/*.csv");
				importIntent.setDataAndType(Uri.parse(getApplication().getExternalFilesDir(null).getPath() + "/CSV/"), "application/zip");
				importIntent.setAction(Intent.ACTION_GET_CONTENT);
				//intent.putExtra("image_filename", filename);
				startActivityForResult(Intent.createChooser(importIntent,"Select Backup File"), REQUEST_SELECT_CSV_FILE);

				break;

			case R.id.fix_DB_button:
				//todo put image data into images table

				break;
		}
	}

	//  todo export select cases, selected by integer list of case_id
	private boolean exportCasesCSV(String filename /*File outFile, List<Integer> caseList*/)
	{
		Boolean returnCode = false;

		// attempt to create CSV file
		File casesCSV = null;
		File imagesCSV = null;

		// CSV subdirectory within internal app data directory
		File CSV_dir = new File(getApplication().getExternalFilesDir(null), "/CSV/");

		// create CSV dir if doesn't already exist
		if(!CSV_dir.exists())
		{
			if(CSV_dir.mkdirs())
			{
				Toast.makeText(this, "Created directory: " + CSV_dir.getPath(), Toast.LENGTH_LONG).show();
			}
			else
			{
				Toast.makeText(this, "Unable to create directory: " + CSV_dir.getPath(), Toast.LENGTH_LONG).show();
			}
		}


		try
		{
			//casesCSV = File.createTempFile(filename + "_cases", ".csv", storageDir);
			casesCSV = new File(CSV_dir.getPath(), filename + "_cases.csv");
			//imagesCSV = File.createTempFile(filename + "_images", ".csv", storageDir);
			imagesCSV = new File(CSV_dir.getPath(), filename + "_images.csv");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			showMessage("unable to create local CSV backup files.");
			return false;
		}

		String csvHeader = "";
		String case_csvValues = "";
		String image_csvValues = "";

		// to zip all images into a file for backup
		String zip_filenames[] = new String[0];

		String value = "";

		for (int i = 0; i < CasesProvider.ALL_KEYS.length; i++) {
			if (csvHeader.length() > 0) {
				csvHeader += ",";
			}
			csvHeader += "\"" + CasesProvider.ALL_KEYS[i] + "\"";
		}

		csvHeader += ",\"Image Files\"\n";

		Log.d(TAG, "header=" + csvHeader);

		/*
		// convert list of integers into array
		// todo test this!!!!!!!!!
		int j=0;
		String[] selectionArgs = new String[caseList.size()];
		for(Integer i : caseList)
		{
			selectionArgs[j++] = String.valueOf(i);
		}
*/

		// create local CSV files
		try
		{
			FileWriter casesWriter = new FileWriter(casesCSV);
			BufferedWriter casesOut = new BufferedWriter(casesWriter);

			FileWriter imagesWriter = new FileWriter(imagesCSV);
			BufferedWriter imagesOut = new BufferedWriter(imagesWriter);

			//			FileOutputStream outputStream = openFileOutput(outFileName, Context.MODE_PRIVATE);


			// Cases table
			Cursor caseCursor = getContentResolver().query(CasesProvider.CASES_URI, null, null, null, null, null);

			if (caseCursor.moveToFirst())
			{
				casesOut.write(csvHeader);
				//	outputStream.write(csvHeader.getBytes());

				// loop through all cases
				do
				{
					case_csvValues = "";
					// output all case columns for this case
					for (int i = 0; i < CasesProvider.ALL_KEYS.length; i++)
					{
						if (i > 0)
						{
							case_csvValues += ",";
						}

						if (i == 6 || i == 11)
							case_csvValues += "\"" + String.valueOf(caseCursor.getInt(i)) + "\"";
						else
						{

							value = caseCursor.getString(i);

							if(value == null)
								value = "";

							case_csvValues += "\"" + value + "\"";
						}
					}

					// output image files in last columns
					String [] image_args = {String.valueOf(caseCursor.getInt(CasesProvider.COL_ROWID))};
					Cursor imageCursor = getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

					// loop through all images of this case
					if(imageCursor.moveToFirst())
					{
						do
						{
							image_csvValues = "";
							// output all image columns for this image

							image_csvValues = imageCursor.getString(0) + "," + imageCursor.getString(1) + "," + imageCursor.getString(2) + "," + imageCursor.getString(3) + "\n";
							imagesOut.write(image_csvValues);

						} while (imageCursor.moveToNext());
					}

					// store image filenames in string array for zip
					if(imageCursor.moveToFirst())
					{
						do
						{
							case_csvValues += "," + "\"" + imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME) + "\"";

							zip_filenames = UtilClass.addArrayElement(zip_filenames, imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME));

						} while (imageCursor.moveToNext());
					}

					imageCursor.close();

					casesOut.write(case_csvValues + "\n");
					//			outputStream.write(case_csvValues.getBytes());
				} while (caseCursor.moveToNext());
				caseCursor.close();

			}
			casesOut.close();
			imagesOut.close();
			//outputStream.close();

			//local_backup_zip_file = casesCSV;

			//todo
			// Images table


			// zip image and csv files
			String zip_filename = casesCSV.getPath().replace("_cases", "");
			zip_filename = zip_filename.replace(".csv", ".zip");
			zip_filenames = UtilClass.addArrayElement(zip_filenames, casesCSV.getPath());
			zip_filenames = UtilClass.addArrayElement(zip_filenames, imagesCSV.getPath());
			local_backup_zip_file = UtilsFile.zip(zip_filenames, zip_filename);

			casesCSV.delete();
			imagesCSV.delete();

			returnCode = true;
		}
		catch (IOException e)
		{
			returnCode = false;
			e.printStackTrace();
			Log.d(TAG, "IOException: " + e.getMessage());
		}


		// Show the Google Drive interface to choose filename and location to create cloud backup file
		Drive.DriveApi.newDriveContents(getGoogleApiClient())
				.setResultCallback(driveContentsCallback);


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


		// create local copy (if from cloud storage file)
		File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		File tempCSV = null;
		try
		{
			tempCSV = File.createTempFile("temp", ".csv", storageDir);

			UtilsFile.copyFile(tempCSV, inFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to copy CSV file", Toast.LENGTH_SHORT).show();
			return;
		}



		try
		{
			br = new BufferedReader(new FileReader(tempCSV));

			// If successfully opened file, clear old database: delete all rows from CASES and IMAGES tables in the database
			// todo change this to just add to existing database
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
		switch (requestCode)
		{
			// Restore DB
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

			// Import CSV
			case REQUEST_SELECT_CSV_FILE:
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
					importCasesCSV(restoreFile);
				}
				break;

			case REQUEST_CREATE_GOOGLE_DRIVE_FILE:
				if (resultCode == RESULT_OK) {
					driveId = (DriveId) data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

					showMessage("File created with ID: " + driveId);
				}
				//finish();
				break;

			default:
				super.onActivityResult(requestCode, resultCode, data);
				break;
		}

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


	// Create new Google Drive file
	final ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
			new ResultCallback<DriveApi.DriveContentsResult>() {
				@Override
				public void onResult(DriveApi.DriveContentsResult result)
				{
					if (!result.getStatus().isSuccess()) {
						showMessage("Error while trying to create new file contents");
						return;
					}

					final DriveContents driveContents = result.getDriveContents();
					final File inFile = local_backup_zip_file;
					final String filename = exportFilename;

					// Perform I/O off the UI thread.
					new Thread()
					{
						@Override
						public void run()
						{
							// write content to DriveContents
							OutputStream outputStream = driveContents.getOutputStream();
							FileInputStream inputStream = null;
							try
							{
								// copy file contents to outstream
								inputStream = new FileInputStream(inFile);
								UtilsFile.copyFile((FileOutputStream)outputStream, inputStream);
							}
							catch (FileNotFoundException e)
							{
								e.printStackTrace();
								showMessage("local CSV file not found");
							}
							catch (IOException e)
							{
								e.printStackTrace();
								showMessage("Copy CSV to Google Drive: IO exception");
							}


							// setup google drive file
							MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
									                              .setTitle(filename)
									                              .setMimeType("application/zip")
									                              .setStarred(true).build();

							// create a file on root folder
							/*
							Drive.DriveApi.getRootFolder(getGoogleApiClient())
									.createFile(getGoogleApiClient(), changeSet, driveContents)
									.setResultCallback(fileCallback);
									*/

							// open up google drive file chooser and create file
							IntentSender intentSender = Drive.DriveApi
									                            .newCreateFileActivityBuilder()
									                            .setInitialMetadata(changeSet)
									                            .setInitialDriveContents(driveContents)
									                            .build(getGoogleApiClient());
							try
							{
								startIntentSenderForResult(intentSender, REQUEST_CREATE_GOOGLE_DRIVE_FILE, null, 0, 0, 0);
							}
							catch (IntentSender.SendIntentException e)
							{
								Log.w(TAG, "Unable to send intent", e);
							}

						}
					}.start();


				}
			};

	final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new ResultCallback<DriveFolder.DriveFileResult>()
	 {
	     @Override
	     public void onResult(DriveFolder.DriveFileResult result) {
	         if (!result.getStatus().isSuccess()) {
	             showMessage("Error while trying to create the file");
	             return;
	         }
	         showMessage("Created a file with content: " + result.getDriveFile().getDriveId());
	     }
	 };



	//////////////////
	// Write/Edit Google Drive file
	final ResultCallback<DriveIdResult> idCallback = new ResultCallback<DriveIdResult>() {
		@Override
		public void onResult(DriveIdResult result) {
			if (!result.getStatus().isSuccess()) {
				showMessage("Cannot find DriveId. Are you authorized to view this file?");
				return;
			}
			DriveFile file = Drive.DriveApi.getFile(getGoogleApiClient(), result.getDriveId());
			new EditContentsAsyncTask(CloudStorageActivity.this).execute(file);
		}
	};

	public class EditContentsAsyncTask extends ApiClientAsyncTask<DriveFile, Void, Boolean>
	{

		public EditContentsAsyncTask(Context context)
		{
			super(context);
		}

		@Override
		protected Boolean doInBackgroundConnected(DriveFile... args) {
			DriveFile file = args[0];
			try {
				DriveApi.DriveContentsResult driveContentsResult = file.open(getGoogleApiClient(), DriveFile.MODE_WRITE_ONLY, null).await();
				if (!driveContentsResult.getStatus().isSuccess()) {
					return false;
				}
				DriveContents driveContents = driveContentsResult.getDriveContents();
				OutputStream outputStream = driveContents.getOutputStream();
				outputStream.write("Hello world".getBytes());
				com.google.android.gms.common.api.Status status =
						driveContents.commit(getGoogleApiClient(), null).await();
				return status.getStatus().isSuccess();
			} catch (IOException e) {
				Log.e(TAG, "IOException while appending to the output stream", e);
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (!result) {
				showMessage("Error while editing contents");
				return;
			}
			showMessage("Successfully edited contents");
		}
	}
///////////////



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
