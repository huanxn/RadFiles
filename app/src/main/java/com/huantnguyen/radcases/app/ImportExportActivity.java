package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.JsonReader;
import android.util.JsonToken;
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
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.ActionClickListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//import eu.janmuller.android.simplecropimage.ImageViewTouchBase;


public class ImportExportActivity extends GoogleDriveBaseActivity
{
	String TAG = "CloudStorageActivity";

	private static Activity activity;

	private CloudStorageFragment fragment;

	//final static String ARG_IMPORT_STREAM = "com.huan.t.nguyen.radcases.ARG_IMPORT_STREAM";
	final static String ARG_IMPORT_URI = "com.huan.t.nguyen.radcases.ARG_IMPORT_URI";

	//final static int REQUEST_SELECT_BACKUP_FILE = 0;
	final static int REQUEST_SELECT_IMPORT_CASES_FILE = 1;
	final static int REQUEST_LIST_JSON_FILE = 2;
	final static int REQUEST_IMPORT_CASES = 3;
	final static int REQUEST_EXPORT_CASES_SAF = 4;

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
	//private ProgressDialog progressWheelDialog = null;


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
		getMenuInflater().inflate(R.menu.import_export, menu);

		return super.onCreateOptionsMenu(menu);     // nav drawer
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch(id)
		{
			case R.id.menu_clear_cache:

				final File downloadsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
				ProgressDialog progressWheelDialog = new ProgressDialog(activity, "Clearing cache...", activity.getResources().getColor(R.color.default_colorAccent));
				new ClearDirectoryTask(this, progressWheelDialog).execute(downloadsDir);

				break;

		}

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

			case R.id.fix_DB_button:



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
		filename = "RadFiles (" + timeStamp + ")";

		/*
		// would need to disable ACTION_GET_CONTENT in manifext for 4.4+
		// https://developer.android.com/guide/topics/providers/document-provider.html
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
		{
			exportFilename = filename + RCS_EXTENSION;
			exportMIMEtype = RCS_MIMETYPE;

			// Storage Access Framework
			Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

			// Filter to only show results that can be "opened", such as
			// a file (as opposed to a list of contacts or timezones).
			intent.addCategory(Intent.CATEGORY_OPENABLE);

			// Create a file with the requested MIME type.
			exportFilename = "" + RCS_EXTENSION;
			exportMIMEtype = RCS_MIMETYPE;

			intent.setType(exportMIMEtype);
			intent.putExtra(Intent.EXTRA_TITLE, exportFilename);
			startActivityForResult(intent, REQUEST_EXPORT_CASES_SAF);
		}
		*/

		// no Storage Access Framework pre KITKAT
		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		final Activity activity = this;

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
					startActivity(Intent.createChooser(shareIntent, "Save lists to..."));
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent resultData)
	{
		switch (requestCode)
		{
			/*
			case REQUEST_EXPORT_CASES_SAF:
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
				*/

			// Import JSON
			case REQUEST_SELECT_IMPORT_CASES_FILE:
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
			View rootView = inflater.inflate(R.layout.fragment_import_export, container, false);
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
					getActivity().startActivityForResult(Intent.createChooser(importIntent, "Select Cases File"), REQUEST_SELECT_IMPORT_CASES_FILE);
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
					SnackbarManager.show(Snackbar.with(activity)
							                     .type(SnackbarType.MULTI_LINE)
							                     .text("In the main case list, long press on a case to start sharing.")
							                     .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
							                     .actionLabel("GO")
							                     .actionColor(Color.GREEN) // action button label color
							                     .actionListener(new ActionClickListener()
							                     {
								                     @Override
								                     public void onActionClicked(Snackbar snackbar)
								                     {
									                     Intent intent = new Intent(activity, CaseCardListActivity.class);
									                     activity.startActivity(intent);

									                     activity.finish();
								                     }
							                     }));

					//UtilClass.showMessage(activity, "In the main case list, long press on a case to start sharing.");
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

	private class ClearDirectoryTask extends AsyncTask<File, Integer, Boolean>
	{
		private Activity activity;
		private ProgressDialog progressWheelDialog;

		ClearDirectoryTask(Activity activity, ProgressDialog progressWheelDialog)
		{
			this.activity = activity;
			this.progressWheelDialog = progressWheelDialog;
		}
		protected void onPreExecute()
		{
			progressWheelDialog.setCancelable(false);
			progressWheelDialog.setCanceledOnTouchOutside(false);
			progressWheelDialog.show();
		}

		@Override
		protected Boolean doInBackground(File... dir)
		{
			return UtilsFile.clearDir(dir[0]);
		}

		protected void onPostExecute(Boolean isSuccessful)
		{
			progressWheelDialog.dismiss();

			if(isSuccessful)
			{
				UtilClass.showMessage(activity, "Deleted cache files.");
			}
			else
			{
				UtilClass.showMessage(activity, "Clear directory failed.");
			}
		}
	}
}