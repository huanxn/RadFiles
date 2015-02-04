package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.gc.materialdesign.views.ProgressBarIndeterminateDeterminate;
import com.gc.materialdesign.widgets.ProgressDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

//import eu.janmuller.android.simplecropimage.ImageViewTouchBase;


public class ImportExportActivity extends GoogleDriveBaseActivity
{
	String TAG = "CloudStorageActivity";

	private static Activity activity;

	private CloudStorageFragment fragment;

	//final static String ARG_IMPORT_STREAM = "com.huan.t.nguyen.radcases.ARG_IMPORT_STREAM";
	final static String ARG_IMPORT_URI = "com.huan.t.nguyen.radcases.ARG_IMPORT_URI";

	//final static int REQUEST_SELECT_BACKUP_FILE = 0;
	final static int REQUEST_SELECT_CSV_FILE = 1;
	final static int REQUEST_LIST_JSON_FILE = 2;
	final static int REQUEST_IMPORT_CASES = 3;

	// for uploading to cloud
	private File local_file_to_cloud;
	private String exportFilename;
	private String exportMIMEtype;

	final static String CASES_CSV_FILENAME = "cases_table.csv";
	final static String IMAGES_CSV_FILENAME = "images_table.csv";
	final static String STUDIES_CSV_FILENAME = "studies_table.csv";

	final static String CASES_JSON_FILENAME = "cases_table.txt";
	final static String IMAGES_JSON_FILENAME = "images_table.txt";
	final static String LISTS_JSON_FILENAME = "lists_table.txt";

	// Database: backup and restore
	final static String RDB_MIMETYPE = "application/x-7z-compressed";
	final static String RDB_EXTENSION = ".rdb";

	// CSV: import and export select cases
	final static String RCS_MIMETYPE = "application/zip";
	final static String RCS_EXTENSION = ".rcs";

	final static String LIST_MIMETYPE = "text/plain";
	final static String LIST_EXTENSION = ".list";

	final static String DB_FILENAME = "RadCases.db";

	// standard directories
	private static File downloadsDir = CaseCardListActivity.downloadsDir;
	private static File picturesDir = CaseCardListActivity.picturesDir;
	private static File appDir = CaseCardListActivity.appDir;             // internal app data directory
	private static File dataDir = CaseCardListActivity.dataDir;            // private data directory (with SQL database)
	//private static File CSV_dir = CaseCardListActivity.CSV_dir;            // contains created zip files with CSV files and images

	private static File backupDir;

	private ProgressBarIndeterminateDeterminate progressBar = null;
	private AlertDialog progressBarDialog = null;

	// default
	private ProgressDialog progressWheelDialog = null;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		activity = this;
		setDrawerPosition(NavigationDrawerActivity.POS_CLOUD_STORAGE);
		//setContentView(R.layout.activity_cloud_storage);

	    /*
	    downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
	    picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		appDir = getExternalFilesDir(null);
	    dataDir = Environment.getDataDirectory();
	    CSV_dir = new File(appDir, "/CSV/");
	    */

		backupDir = new File(appDir, "/Backup/");


		if (savedInstanceState == null)
		{
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.

			// Get intent, action and MIME type
			Intent intent = getIntent();
			String action = intent.getAction();
			String type = intent.getType();
/*
		    if (Intent.ACTION_VIEW.equals(action) && type != null)
		    {
			    fragment = new ImportFragment();
			    getSupportActionBar().setTitle("Rad Import");

			    if (type.equals("application/zip") || type.equals("**") || type.equals("application/octet-stream") )
			    {
				    Uri import_uri = intent.getData();

				    Bundle arguments = new Bundle();
				    //arguments.putParcelable(ARG_IMPORT_STREAM, intent.getParcelableExtra(Intent.EXTRA_STREAM)); //(ARG_IMPORT_URI, import_uri, null););
				    arguments.putString(ARG_IMPORT_STREAM, import_uri.toString()); //(ARG_IMPORT_URI, import_uri, null););
				    //arguments.putBoolean(ARG_HAS_IMAGE, hasImage);
				    fragment.setArguments(arguments);
			    }
		    }
		    else
*/
			{
				// Handle other intents, such as being started from the home screen
				fragment = new CloudStorageFragment();
				//  getSupportActionBar().setTitle("Rad Backup");
			}


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

		return super.onOptionsItemSelected(item);
	}

	public void onClick_Button(View view) throws IOException
	{
		String filename;

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		AlertDialog dialog;
		alert.setTitle("Create Backup File");
		//alert.setMessage("Filename");

		// Create an image file name based on timestamp
		//String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HHmm").format(new Date());

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		final Context context = this;

		switch (view.getId())
		{
/*
			case R.id.backup_button:

				filename = "RadBackup (" + timeStamp + ")";
				input.setText(filename);
				alert.setView(input);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						exportFilename = value + RDB_EXTENSION;
						exportMIMEtype = RDB_MIMETYPE;

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

				//alert.show();
				dialog = alert.create();
				// Show keyboard
				dialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
					}
				});
				dialog.show();

				break;

			case R.id.restore_button:
				Intent restoreIntent = new Intent();
				//restoreIntent.setType("DOWNLOADS/*.db");
				restoreIntent.setDataAndType(Uri.parse(backupDir.getPath()), RDB_MIMETYPE);
				restoreIntent.setAction(Intent.ACTION_GET_CONTENT);
				//intent.putExtra("image_filename", filename);
				startActivityForResult(Intent.createChooser(restoreIntent,"Select Backup File"), REQUEST_SELECT_BACKUP_FILE);

				break;



			case R.id.exportCSV_button:

				filename = "RadExport (" + timeStamp + ")";
				input.setText(filename);
				alert.setView(input);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						exportFilename = value + RCS_EXTENSION;
						//exportMIMEtype = "application/zip";
						exportMIMEtype = RCS_MIMETYPE;

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

				//alert.show();
				dialog = alert.create();
				// Show keyboard
				dialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
					}
				});
				dialog.show();


				break;



			case R.id.importCSV_button:

				Intent importIntent = new Intent();
				//importIntent.setType("DOWNLOADS/*.csv");
				//importIntent.setDataAndType(Uri.parse(CSV_dir.getPath()), "application/zip");
				importIntent.setDataAndType(Uri.parse(CSV_dir.getPath()), RCS_MIMETYPE);
				importIntent.setAction(Intent.ACTION_GET_CONTENT);
				//intent.putExtra("image_filename", filename);
				startActivityForResult(Intent.createChooser(importIntent,"Select Cases File"), REQUEST_SELECT_CSV_FILE);

				break;

			case R.id.exportList_button:
				UtilClass.exportListsJSON(this, "RadLists.json");
				break;

			case R.id.importList_button:
				Intent importListIntent = new Intent();
				//importIntent.setType("DOWNLOADS/*.csv");
				//importIntent.setDataAndType(Uri.parse(CSV_dir.getPath()), "application/zip");
				importListIntent.setDataAndType(Uri.parse(CSV_dir.getPath()), "text/plain");
				importListIntent.setAction(Intent.ACTION_GET_CONTENT);
				//intent.putExtra("image_filename", filename);
				startActivityForResult(Intent.createChooser(importListIntent,"Select Cases File"), REQUEST_LIST_JSON_FILE);
				break;
*/
			case R.id.fix_DB_button:

				/*
				//change path to just filename
				Cursor imageCursor = getContentResolver().query(CasesProvider.IMAGES_URI, null, null, null, null);
				String path;
				String name;
				ContentValues updateImageValues = new ContentValues();

				String row_id;

				if(imageCursor.moveToFirst())
				{
					do
					{
						row_id = imageCursor.getString(CasesProvider.COL_ROWID);
						path = imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME);
						name = new File(path).getName();


						// input all columns for this case, except row_id
						updateImageValues.clear();
						updateImageValues.put(CasesProvider.KEY_IMAGE_FILENAME, name);

						getContentResolver().update(CasesProvider.IMAGES_URI, updateImageValues, CasesProvider.KEY_ROWID + " = ?", new String [] {row_id});


					} while(imageCursor.moveToNext());

					imageCursor.close();
				}
				*/

				Cursor caseCursor = getContentResolver().query(CasesProvider.CASES_URI, null,
						                                              CasesProvider.KEY_LAST_MODIFIED_DATE + " IS NULL OR " + CasesProvider.KEY_LAST_MODIFIED_DATE + "= ?", new String[]{""},
						                                              null);

				ContentValues updateValues = new ContentValues();
				String date;

				String row_id;

				if (caseCursor.moveToFirst())
				{
					do
					{
						row_id = caseCursor.getString(CasesProvider.COL_ROWID);
						date = caseCursor.getString(CasesProvider.COL_DATE);

						// input all columns for this case, except row_id
						updateValues.clear();
						updateValues.put(CasesProvider.KEY_LAST_MODIFIED_DATE, date);

						getContentResolver().update(CasesProvider.CASES_URI, updateValues, CasesProvider.KEY_ROWID + " = ?", new String[]{row_id});


					} while (caseCursor.moveToNext());

					caseCursor.close();
				}

				UtilClass.showMessage(this, "added modified date");


				break;
		}
	}

	private void exportCases()
	{
		String filename;

		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle("Create Cases File");
		//alert.setMessage("Filename");

		// Create an image file name based on timestamp
		//String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HHmm").format(new Date());

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		final Activity activity = this;

		filename = "RadFiles (" + timeStamp + ")";
		input.setText(filename);
		input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		alertBuilder.setView(input);

		alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				dialog.dismiss();

				final String value = input.getText().toString();
				exportFilename = value + RCS_EXTENSION;
				exportMIMEtype = RCS_MIMETYPE;

				LayoutInflater inflater = activity.getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.alertdialog_progress, null);
				AlertDialog.Builder progressBuilder = new AlertDialog.Builder(activity).setCancelable(false);
				progressBuilder.setView(dialoglayout);
				progressBarDialog = progressBuilder.create();
				progressBarDialog.show();

				progressBar = (ProgressBarIndeterminateDeterminate) dialoglayout.findViewById(R.id.progress_bar);
				progressBar.setMin(0);

				/*
				ProgressDialog progressDialog2 = new ProgressDialog(activity, "Exporting", getResources().getColor(R.color.default_colorAccent));
				progressDialog2.show();
				*/

				Thread exportThread = new Thread() {
					@Override
					public void run()
					{
						local_file_to_cloud = UtilClass.exportCasesJSON(activity, value, null, progressHandler); //exportCasesCSV(value);

						if (local_file_to_cloud != null)
						{
							Uri uriShareFile = Uri.fromFile(local_file_to_cloud);

							Intent shareIntent = new Intent(Intent.ACTION_SEND);
							//shareIntent.setType("message/rfc822");
							shareIntent.setType(RCS_MIMETYPE);
							shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});  // to: field
							//shareIntent.putExtra(Intent.EXTRA_SUBJECT, "New radiology cases!");
							shareIntent.putExtra(Intent.EXTRA_SUBJECT, exportFilename);
							shareIntent.putExtra(Intent.EXTRA_TITLE, exportFilename);
							shareIntent.putExtra(Intent.EXTRA_TEXT, "Please see the attached file.\nOpen it with the RadFiles Android app!"); //todo link to store
							shareIntent.putExtra(Intent.EXTRA_STREAM, uriShareFile);


							//shareIntent.setData(Uri.parse("mailto:")); // or just "mailto:" for blank
							//shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
							try
							{
								startActivity(Intent.createChooser(shareIntent, "Save cases to..."));
							}
							catch (android.content.ActivityNotFoundException ex)
							{
								Toast.makeText(activity, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
							}
						}
						else
						{
							UtilClass.showMessage(activity, "Unable to export cases to a file.");
						}
					}
				};
				exportThread.start();

				// create CSV file
				//local_file_to_cloud = UtilClass.exportCasesJSON(activity, value, null); //exportCasesCSV(value);



			}
		});

		alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				// Canceled.
			}
		});

		AlertDialog alertDialog = alertBuilder.create();
		/*
		// Show keyboard
		alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
		{
			@Override
			public void onShow(DialogInterface dialog)
			{
				InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
			}
		});
		*/
		alertDialog.show();
	}

	private void exportLists()
	{
		String filename;

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		AlertDialog dialog;
		alert.setTitle("Create Lists File");

		// Create an image file name based on timestamp
		//String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HHmm").format(new Date());

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		final Activity activity = this;

		filename = "RadFiles Lists (" + timeStamp + ")";
		input.setText(filename);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				String value = input.getText().toString();
				exportFilename = value + LIST_EXTENSION;
				exportMIMEtype = LIST_MIMETYPE;

				// create CSV file
				local_file_to_cloud = UtilClass.exportListsJSON(activity, value);


				Uri uriShareFile = Uri.fromFile(local_file_to_cloud);

				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				//shareIntent.setType("message/rfc822");
				shareIntent.setType(LIST_MIMETYPE);
				shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});  // to: field
				shareIntent.putExtra(Intent.EXTRA_SUBJECT, exportFilename);
				shareIntent.putExtra(Intent.EXTRA_TEXT, "My lists of Keywords, Modalities, and Sections for the RadFiles Android app!"); //todo link to store
				shareIntent.putExtra(Intent.EXTRA_STREAM, uriShareFile);

				try
				{
					startActivity(Intent.createChooser(shareIntent, "Save cases to..."));
				}
				catch (android.content.ActivityNotFoundException ex)
				{
					Toast.makeText(activity, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
				}

			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				// Canceled.
			}
		});

		//alert.show();
		dialog = alert.create();
		// Show keyboard
		dialog.setOnShowListener(new DialogInterface.OnShowListener()
		{
			@Override
			public void onShow(DialogInterface dialog)
			{
				InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
			}
		});
		dialog.show();

	}

	/////////////////////
/*
	//importing database
	private void restoreDB(File inFile)
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
			Toast.makeText(this, "Unable to open bak file:", Toast.LENGTH_SHORT).show();
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
			File currentDBDir = new File(dataDir, currentDBDirPath);

			if (currentDBDir.canWrite())
			{
				// If successfully opened file, // todo delete old image files

				String currentDBPath = "//data//" + getPackageName() + "//databases//" + CasesProvider.DATABASE_NAME;
				File currentDB = new File(dataDir, currentDBPath);
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
	private File backupDB(String backupFilename)
	{
		// TODO Auto-generated method stub

		File returnFile = null;

		try
		{
			if (appDir.canWrite())
			{
				String currentDBPath = "//data//" + getPackageName() + "//databases//" + CasesProvider.DATABASE_NAME;
				File currentDB_file = new File(dataDir, currentDBPath);

				//String backupDBPath  = "/Backup/" + DB_FILENAME;
				//File backupDB_file = new File(appDir, backupDBPath);
				File backupDB_file = new File(backupDir, DB_FILENAME);

				//String backupDirPath  = "/Backup/";
				//File backupDir = new File(appDir, backupDirPath);

				if (!backupDir.exists())
				{
					if (backupDir.mkdirs())
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
				if (imageCursor.moveToFirst())
				{
					do
					{
						zip_files_array = UtilClass.addArrayElement(zip_files_array, picturesDir + "/" + imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME));

					} while (imageCursor.moveToNext());
				}

				imageCursor.close();

				// add the SQL database file
				zip_files_array = UtilClass.addArrayElement(zip_files_array, backupDB_file.getPath());

				// zip image and csv files
				String zip_filename = backupDir.getPath() + "/" + backupFilename + RDB_EXTENSION;
				// return the zip file
				returnFile = UtilsFile.zip(zip_files_array, zip_filename);

				// delete db file (copy in zip)
				backupDB_file.delete();

			}
		}
		catch (Exception e)
		{

			Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
			return null;

		}

		// success
		return returnFile;
	}
*/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent resultData)
	{
		switch (requestCode)
		{
			case UtilsFile.WRITE_REQUEST_CODE:
				if(resultCode == Activity.RESULT_OK)
				{
					Uri uri = null;
					if (resultData != null)
					{
						uri = resultData.getData();
						Log.i(TAG, "Uri: " + uri.toString());
						UtilClass.showMessage(this, uri.toString());
					}
				}
				break;
			/*
			// Restore DB
			case REQUEST_SELECT_BACKUP_FILE:
				if (resultCode == RESULT_OK)
				{
					String restoreFilename;


					//fragment.progressBar.progressiveStart();

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
						UtilClass.showMessage(this, "Unable to create temporary file.");
					}

					FileOutputStream outputStream = null;
					FileInputStream inputStream = null;
					try
					{
						// Google Drive file
						inputStream = (FileInputStream) getContentResolver().openInputStream(uri);

						// new local file
						outputStream = new FileOutputStream(tempRestoreFile);

						// copy backup file contents to local file
						UtilsFile.copyFile(outputStream, inputStream);
					}
					catch (FileNotFoundException e)
					{
						e.printStackTrace();
						UtilClass.showMessage(this, "local file not found");
					}
					catch (IOException e)
					{
						e.printStackTrace();
						UtilClass.showMessage(this, "Copy backup to Google Drive: IO exception");
						UtilClass.showMessage(this, "cannot open input stream from selected uri");
					}

					//	Log.d(TAG, "File Path: " + path);
					// Get the file instance
					// File file = new File(path);
					// Initiate the upload

					//File restoreFile = new File(restoreFilename);
					//importCasesJSON(restoreFile);

					restoreDB(tempRestoreFile);

					// delete the temporary file
					tempRestoreFile.delete();

					//fragment.progressBar.progressiveStop();

				}
				break;
*/
			// Import CSV
			case REQUEST_SELECT_CSV_FILE:
				if (resultCode == RESULT_OK)
				{
					// Get the Uri of the selected file
					final Uri uri = resultData.getData();
					//String filename = data.getStringExtra()
					Log.d(TAG, "File Uri: " + uri.toString());

					//new ImportCasesTask().execute(uri);

					Intent intent = new Intent(this, CaseImport.class);
					intent.putExtra(ARG_IMPORT_URI, uri);
					startActivity(intent);

				}
				break;
			/*
			case REQUEST_IMPORT_CASES:

				//setResult();??

				break;
			*/

			// import list JSON file
			case REQUEST_LIST_JSON_FILE:
				if (resultCode == RESULT_OK)
				{
					String CSV_filename;

					// Get the Uri of the selected file
					Uri uri = resultData.getData();
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
						tempCSV_File = File.createTempFile("lists", ".json", downloadsDir);
					}
					catch (IOException e)
					{
						e.printStackTrace();
						UtilClass.showMessage(this, "Unable to create temporary file.");
					}

					FileOutputStream outputStream = null;
					FileInputStream inputStream = null;
					try
					{
						// Google Drive file
						inputStream = (FileInputStream) getContentResolver().openInputStream(uri);

						// new local file
						outputStream = new FileOutputStream(tempCSV_File);

						// copy backup file contents to local file
						UtilsFile.copyFile(outputStream, inputStream);
					}
					catch (FileNotFoundException e)
					{
						e.printStackTrace();
						UtilClass.showMessage(this, "local CSV file not found");
					}
					catch (IOException e)
					{
						e.printStackTrace();
						UtilClass.showMessage(this, "Copy CSV to Google Drive: IO exception");
						UtilClass.showMessage(this, "cannot open input stream from selected uri");
					}


					// process file: unzip images, add csv info to database
					UtilClass.importListsJSON(this, tempCSV_File);

					// delete the temporary file
					tempCSV_File.delete();
				}

				break;

			default:
				super.onActivityResult(requestCode, resultCode, resultData);
				break;
		}

	}


	final public static int PROGRESS_MSG_MIN = 0;
	final public static int PROGRESS_MSG_MAX = 1;
	final public static int PROGRESS_MSG_SET = 2;
	final public static int PROGRESS_MSG_INCREMENT = 3;
	final public static int PROGRESS_MSG_EXPORT_FINISHED = 5;

	Handler progressHandler = new Handler(new Handler.Callback()
	{
		int progress = 0;

		@Override
		public boolean handleMessage(Message msg)
		{

			switch(msg.arg1)
			{
				default:
				case PROGRESS_MSG_INCREMENT:
					progressBar.setProgress(progress++);
					break;
				case PROGRESS_MSG_MIN:
					progressBar.setMin(msg.arg2);
					break;
				case PROGRESS_MSG_MAX:
					progressBar.setMax(msg.arg2);
					break;
				case PROGRESS_MSG_SET:
					progressBar.setProgress(msg.arg2);
					break;

				case PROGRESS_MSG_EXPORT_FINISHED:
					progressBarDialog.dismiss();
					UtilClass.showMessage(activity, "Exported " + msg.arg2 + " cases.");
					break;

			}


			return false;
		}
	});


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
			setOnClickListeners(rootView);

			return rootView;
		}

		public void setOnClickListeners(View view)
		{
			// CASE IMPORT
			view.findViewById(R.id.case_import_button).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Intent importIntent = new Intent();
					//importIntent.setType("DOWNLOADS/*.csv");
					//importIntent.setDataAndType(Uri.parse(CSV_dir.getPath()), "application/zip");
					importIntent.setDataAndType(Uri.parse(activity.getCacheDir().getPath()), RCS_MIMETYPE);
					importIntent.setAction(Intent.ACTION_GET_CONTENT);
					//intent.putExtra("image_filename", filename);
					getActivity().startActivityForResult(Intent.createChooser(importIntent, "Select Cases File"), REQUEST_SELECT_CSV_FILE);
				}
			});

			// CASE EXPORT
			view.findViewById(R.id.case_export_button).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					((ImportExportActivity) getActivity()).exportCases();
/*
					String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
					String filename = "RadFiles (" + timeStamp + ")" + RCS_EXTENSION;
					UtilsFile.createFile(getActivity(), RCS_MIMETYPE, filename);
					*/
				}
			});

			// CASE EXPORT MULTISELECT
			view.findViewById(R.id.case_export_multiselect_button).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					UtilClass.showMessage(activity, "In the main case list, long press on a case to start sharing.");
				}
			});

			// LIST IMPORT
			view.findViewById(R.id.list_import_button).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Intent importListIntent = new Intent();
					//importIntent.setType("DOWNLOADS/*.csv");
					//importIntent.setDataAndType(Uri.parse(CSV_dir.getPath()), "application/zip");
					importListIntent.setDataAndType(Uri.parse(activity.getCacheDir().getPath()), "text/plain");
					importListIntent.setAction(Intent.ACTION_GET_CONTENT);
					//intent.putExtra("image_filename", filename);
					getActivity().startActivityForResult(Intent.createChooser(importListIntent, "Select Lists File"), REQUEST_LIST_JSON_FILE);
				}
			});

			// LIST EXPORT
			view.findViewById(R.id.list_export_button).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					((ImportExportActivity) getActivity()).exportLists();
				}
			});

		}
	}


}