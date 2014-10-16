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
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import com.google.android.gms.drive.DriveApi.DriveIdResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CloudStorageActivity extends GoogleDriveBaseActivity
{
	String TAG = "CloudStorageActivity";

	private CloudStorageFragment fragment;


	final static int REQUEST_SELECT_BACKUP_FILE = 0;
	final static int REQUEST_SELECT_CSV_FILE = 1;
	final static int REQUEST_CREATE_GOOGLE_DRIVE_FILE = 2;

	// for uploading to cloud
	private File local_file_to_cloud;
	private String exportFilename;

	final static String CASES_CSV_FILENAME = "cases_table.csv";
	final static String IMAGES_CSV_FILENAME = "images_table.csv";
	final static String STUDIES_CSV_FILENAME = "studies_table.csv";

	final static String DB_FILENAME = "RadCases.db";

	// standard directories
	private static File downloadsDir;
	private static File picturesDir;
	private static File appDir;             // internal app data directory
	private static File dataDir;            // private data directory (with SQL database)

	public static File CSV_dir;            // contains created zip files with CSV files and images




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    setDrawerPosition(NavigationDrawerActivity.POS_CLOUD_STORAGE);
        //setContentView(R.layout.activity_cloud_storage);

	    downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
	    picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		appDir = getExternalFilesDir(null);
	    dataDir = Environment.getDataDirectory();
	    CSV_dir = new File(appDir, "/CSV/");




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
        if (id == R.id.menu_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	public void onClick_Button(View view) throws IOException
	{
		String filename;

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Create Backup File");
		//alert.setMessage("Filename");

		// Create an image file name based on timestamp
		//String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HHmm").format(new Date());

		// Set an EditText view to get user input
		final EditText input = new EditText(this);

		switch(view.getId())
		{
			case R.id.backup_button:

				filename = "RadBackup (" + timeStamp + ")";
				input.setText(filename);
				alert.setView(input);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						exportFilename = value + ".zip";

						// backup SQLite file
						local_file_to_cloud = backupDB(value);

						if(local_file_to_cloud != null)
						{
							// Show the Google Drive interface to choose filename and location to create cloud backup file
							Drive.DriveApi.newDriveContents(getGoogleApiClient())
									.setResultCallback(driveCreateCopyCallback);

							Toast.makeText(getApplicationContext(), "Saved data to " + local_file_to_cloud.getPath(), Toast.LENGTH_LONG).show();
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

			case R.id.exportCSV_button:

				filename = "RadExport (" + timeStamp + ")";
				input.setText(filename);
				alert.setView(input);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						exportFilename = value + ".zip";

						// create CSV file
						local_file_to_cloud = exportCasesCSV(value);
						if(local_file_to_cloud != null)
						{
							// Show the Google Drive interface to choose filename and location to create cloud backup file
							Drive.DriveApi.newDriveContents(getGoogleApiClient())
									.setResultCallback(driveCreateCopyCallback);

							Toast.makeText(getApplicationContext(), "Saved data to " + local_file_to_cloud.getPath(), Toast.LENGTH_LONG).show();
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
				importIntent.setDataAndType(Uri.parse(CSV_dir.getPath()), "application/zip");
				importIntent.setAction(Intent.ACTION_GET_CONTENT);
				//intent.putExtra("image_filename", filename);
				startActivityForResult(Intent.createChooser(importIntent,"Select Cases File"), REQUEST_SELECT_CSV_FILE);

				break;

			case R.id.fix_DB_button:
				//todo put image data into images table
				showMessage("Does nothing now...");

				break;
		}
	}


	private File exportCasesCSV(String filename)
	{
		return UtilClass.exportCasesCSV(this, filename, null);
	}


	public void importCasesCSV(File inFile)
	{
		BufferedReader br = null;
		String line;
		Uri rowUri = null;
		int parent_id;
		int imageCount = 0;

		// unzip image files and csv files
		try
		{
			// unzip image files to android pictures directory
			UtilsFile.unzip(inFile.getPath(),picturesDir.getPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to open zip file:", Toast.LENGTH_SHORT).show();
			return;
		}

		File tempCasesCSV = null;
		File tempImagesCSV = null;
		try
		{
			// open existing files that should have been unzipped
			tempCasesCSV = new File(picturesDir, CASES_CSV_FILENAME);
			tempImagesCSV = new File(picturesDir, IMAGES_CSV_FILENAME);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to copy CSV file", Toast.LENGTH_SHORT).show();
			return;
		}

		//////////////////parent ids will change in new database!!!!
		// IMAGES TABLE
		try
		{
			br = new BufferedReader(new FileReader(tempImagesCSV));

			ContentValues insertImageValues = new ContentValues();

			// If successfully opened file, clear old database: delete all rows from IMAGES tables in the database
			// todo change this to just add to existing database
			//getContentResolver().delete(CasesProvider.IMAGES_URI, null, null);

			//br.readLine(); // no header

			while ( (line=br.readLine()) != null)
			{
				String[] values = line.split(",");

				// input all columns for this case, except row_id
				insertImageValues.clear();
				insertImageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, values[1]);
				insertImageValues.put(CasesProvider.KEY_IMAGE_FILENAME, values[2]);
				insertImageValues.put(CasesProvider.KEY_ORDER, values[3]);

				// insert the set of case info into the DB cases table
				rowUri = getContentResolver().insert(CasesProvider.IMAGES_URI, insertImageValues);
			}
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to open Images CSV file", Toast.LENGTH_SHORT).show();
			return;
		}

		// CASES TABLE
		try
		{
			br = new BufferedReader(new FileReader(tempCasesCSV));

			ContentValues insertCaseValues = new ContentValues();

			// If successfully opened file, clear old database: delete all rows from CASES tables in the database
			// todo change this to just add to existing database
			//getContentResolver().delete(CasesProvider.CASES_URI, null, null);

			br.readLine(); // header

			while ( (line=br.readLine()) != null)
			{
				line = line.substring(1, line.length()-1);  // trim the double-quotes off
				String[] values = line.split("\",\"");
				insertCaseValues.clear();

				long old_case_id = Long.valueOf(values[0]);

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

				// change parent_key link in IMAGES table
				if(imageCount > 0)
				{
					ContentValues updateImageValues = new ContentValues();
					updateImageValues.clear();

					// replace with new parent_id obtained from newly inserted case row
					updateImageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, parent_id);
					getContentResolver().update(CasesProvider.IMAGES_URI, updateImageValues, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String [] {String.valueOf(old_case_id)});
				}

			}
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to open Cases CSV file", Toast.LENGTH_SHORT).show();
			return;
		}

		Toast.makeText(this, "Imported cases", Toast.LENGTH_SHORT).show();

		tempCasesCSV.delete();
		tempImagesCSV.delete();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode)
		{
			// Restore DB
			case REQUEST_SELECT_BACKUP_FILE:
				if (resultCode == RESULT_OK)
				{
					String restoreFilename;

					// Get the Uri of the selected file
					Uri uri = data.getData();
					//String filename = data.getStringExtra()
					Log.d(TAG, "File Uri: " + uri.toString());
					// Get the path
				//	String path = FileUtils.getPath(this, uri);
					restoreFilename = uri.getPath();
					Log.d(TAG, "File Uri: " + restoreFilename);

					// copy drive file uri content to new local file

					// create new local file
					File tempRestoreFile = null;
					try
					{
						tempRestoreFile = File.createTempFile("RadCases", ".zip", downloadsDir);
					}
					catch (IOException e)
					{
						e.printStackTrace();
						showMessage("Unable to create temporary file.");
					}

					FileOutputStream outputStream = null;
					FileInputStream inputStream = null;
					try
					{
						// Google Drive file
						inputStream = (FileInputStream)getContentResolver().openInputStream(uri);

						// new local file
						outputStream = new FileOutputStream(tempRestoreFile);

						// copy backup file contents to local file
						UtilsFile.copyFile(outputStream, inputStream);
					}
					catch (FileNotFoundException e)
					{
						e.printStackTrace();
						showMessage("local file not found");
					}
					catch (IOException e)
					{
						e.printStackTrace();
						showMessage("Copy backup to Google Drive: IO exception");
						showMessage("cannot open input stream from selected uri");
					}

				//	Log.d(TAG, "File Path: " + path);
					// Get the file instance
					// File file = new File(path);
					// Initiate the upload

					//File restoreFile = new File(restoreFilename);
					//importCasesCSV(restoreFile);

					restoreDB(tempRestoreFile);

					// delete the temporary file
					tempRestoreFile.delete();

				}
				break;

			// Import CSV
			case REQUEST_SELECT_CSV_FILE:
				if (resultCode == RESULT_OK)
				{
					String CSV_filename;

					// Get the Uri of the selected file
					Uri uri = data.getData();
					//String filename = data.getStringExtra()
					Log.d(TAG, "File Uri: " + uri.toString());
					// Get the path
					//	String path = FileUtils.getPath(this, uri);
					CSV_filename = uri.getPath();
					Log.d(TAG, "File Uri: " + CSV_filename);

					// copy drive file uri content to new local file

					// create new local file
					File tempCSV_File = null;
					try
					{
						tempCSV_File = File.createTempFile("RadCases", ".zip", downloadsDir);
					}
					catch (IOException e)
					{
						e.printStackTrace();
						showMessage("Unable to create temporary file.");
					}

					FileOutputStream outputStream = null;
					FileInputStream inputStream = null;
					try
					{
						// Google Drive file
						inputStream = (FileInputStream)getContentResolver().openInputStream(uri);

						// new local file
						outputStream = new FileOutputStream(tempCSV_File);

						// copy backup file contents to local file
						UtilsFile.copyFile(outputStream, inputStream);
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
						showMessage("cannot open input stream from selected uri");
					}

					// process file: unzip images, add csv info to database
					importCasesCSV(tempCSV_File);

					// delete the temporary file
					tempCSV_File.delete();
				}
				break;

			case REQUEST_CREATE_GOOGLE_DRIVE_FILE:
				if (resultCode == RESULT_OK) {
					driveId = (DriveId) data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

					/*
					if(GOOGLE_DRIVE_FOLDER_ID == null)
					{
					//	DriveFolder driveFolder = Drive.DriveApi.getFolder(getGoogleApiClient(), driveId);

						GOOGLE_DRIVE_FOLDER_ID = Drive.DriveApi.getAppFolder(getGoogleApiClient()).getDriveId();
					}
					*/

					//showMessage("File created with ID: " + driveId);
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
	private void restoreDB(File inFile /*restoreFilename*/)
	{
		// unzip image files and db files
		try
		{
			// unzip image files to android pictures directory
			UtilsFile.unzip(inFile.getPath(), picturesDir.getPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to open zip file:", Toast.LENGTH_SHORT).show();
			return;
		}

		File tempBackupDB = null;
		try
		{
			// open existing file that should have been unzipped
			tempBackupDB = new File(picturesDir, DB_FILENAME);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to copy CSV file", Toast.LENGTH_SHORT).show();
			return;
		}

		try
		{
			//File sd = Environment.getExternalStorageDirectory();
			//File appDir = getApplication().getExternalFilesDir(null);   // internal app data directory
			//File data  = Environment.getDataDirectory();

			String currentDBDirPath = "//data//" + getPackageName() + "//databases//";
			File currentDBDir  = new File(dataDir, currentDBDirPath);

			if (currentDBDir.canWrite())
			{
				// If successfully opened file, // todo delete old image files

				String currentDBPath = "//data//" + getPackageName() + "//databases//" + CasesProvider.DATABASE_NAME;
				File currentDB  = new File(dataDir, currentDBPath);
				//File  backupDB = new File(picturesDir, DB_FILENAME);

				FileChannel src = new FileInputStream(tempBackupDB).getChannel();
				FileChannel dst = new FileOutputStream(currentDB).getChannel();

				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			}
		}
		catch (Exception e)
		{

			Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG)
					.show();

		}
		tempBackupDB.delete();

	}
	//exporting database
	private File backupDB(String backupFilename) {
		// TODO Auto-generated method stub

		File returnFile = null;

		try
		{
			if (appDir.canWrite()) {
				String  currentDBPath= "//data//" + getPackageName() + "//databases//" + CasesProvider.DATABASE_NAME;
				String backupDBPath  = "/Backup/" + DB_FILENAME;
				File currentDB_file = new File(dataDir, currentDBPath);
				File backupDB_file = new File(appDir, backupDBPath);


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

				FileChannel src = new FileInputStream(currentDB_file).getChannel();
				FileChannel dst = new FileOutputStream(backupDB_file).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
				Toast.makeText(getBaseContext(), backupDB_file.toString(), Toast.LENGTH_LONG).show();


				// to zip all images into a file for backup
				String zip_files_array[] = new String[0];

				// get image filenames
				//String [] image_args = {String.valueOf(caseCursor.getInt(CasesProvider.COL_ROWID))};
				Cursor imageCursor = getContentResolver().query(CasesProvider.IMAGES_URI, null, null, null, CasesProvider.KEY_ORDER);

				// store image filenames in string array for zip
				if(imageCursor.moveToFirst())
				{
					do
					{
						zip_files_array = UtilClass.addArrayElement(zip_files_array, imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME));

					} while (imageCursor.moveToNext());
				}

				imageCursor.close();

				// add the SQL database file
				zip_files_array = UtilClass.addArrayElement(zip_files_array, backupDB_file.getPath());

				// zip image and csv files
				String zip_filename = backupDir.getPath() + "/" + backupFilename + ".zip";
				// return the zip file
				returnFile = UtilsFile.zip(zip_files_array, zip_filename);

				// delete db file (copy in zip)
				backupDB_file.delete();

			}
		} catch (Exception e) {

			Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
			return null;

		}

		// success
		return returnFile;
	}


	// Create new Google Drive file
	final ResultCallback<DriveApi.DriveContentsResult> driveCreateCopyCallback =
			new ResultCallback<DriveApi.DriveContentsResult>() {
				@Override
				public void onResult(DriveApi.DriveContentsResult result)
				{
					if (!result.getStatus().isSuccess()) {
						showMessage("Error while trying to create new file contents");
						return;
					}

					final DriveContents driveContents = result.getDriveContents();
					final File inFile = local_file_to_cloud;
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
								showMessage("local file not found");
							}
							catch (IOException e)
							{
								e.printStackTrace();
								showMessage("Copy file to Google Drive: IO exception");
							}


							//todo default folder

							// setup google drive file
							MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
									                              .setTitle(filename)
									                              .setMimeType("application/zip")
									                              .build();

							// create a file on root folder
							/*
							Drive.DriveApi.getRootFolder(getGoogleApiClient())
									.createFile(getGoogleApiClient(), changeSet, driveContents)
									.setResultCallback(fileCallback);
									*/


							// todo user selectable default folder
							//GOOGLE_DRIVE_FOLDER_ID = Drive.DriveApi.getAppFolder(getGoogleApiClient()).getDriveId();

							// open up google drive file chooser and create file
							IntentSender intentSender;
							if(GOOGLE_DRIVE_FOLDER_ID != null)
							{
								intentSender = Drive.DriveApi
										                            .newCreateFileActivityBuilder()
										                            .setActivityStartFolder(GOOGLE_DRIVE_FOLDER_ID)
										                            .setInitialMetadata(changeSet)
										                            .setInitialDriveContents(driveContents)
										                            .build(getGoogleApiClient());
							}
							else
							{
								intentSender = Drive.DriveApi
										                            .newCreateFileActivityBuilder()
										                            .setInitialMetadata(changeSet)
										                            .setInitialDriveContents(driveContents)
										                            .build(getGoogleApiClient());
							}
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

	/*
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
*/

	/////////////
	// copy from google drive

	final private ResultCallback<DriveIdResult> driveDownloadCallback = new ResultCallback<DriveIdResult>() {
		@Override
		public void onResult(DriveIdResult result) {
			new RetrieveDriveFileContentsAsyncTask(CloudStorageActivity.this).execute(result.getDriveId());
		}
	};

	final private class RetrieveDriveFileContentsAsyncTask
			extends ApiClientAsyncTask<DriveId, Boolean, String> {

		public RetrieveDriveFileContentsAsyncTask(Context context) {
			super(context);
		}

		@Override
		protected String doInBackgroundConnected(DriveId... params) {
		String contents = null;
		DriveFile file = Drive.DriveApi.getFile(getGoogleApiClient(), params[0]);
		DriveApi.DriveContentsResult driveContentsResult =
				file.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null).await();
		if (!driveContentsResult.getStatus().isSuccess()) {
			return null;
		}
		DriveContents driveContents = driveContentsResult.getDriveContents();
		BufferedReader reader = new BufferedReader(new InputStreamReader(driveContents.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line;
		try
		{
			while ((line = reader.readLine()) != null)
			{
				builder.append(line);
			}
			contents = builder.toString();
		} catch (IOException e)
		{
			Log.e(TAG, "IOException while reading from the stream", e);
		}

		driveContents.discard(getGoogleApiClient());
		return contents;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result == null) {
				showMessage("Error while reading from the file");
				return;
			}
			showMessage("File contents: " + result);
		}
	}


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
