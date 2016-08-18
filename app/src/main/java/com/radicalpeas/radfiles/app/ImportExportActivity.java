package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.InputType;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImportExportActivity extends NavDrawerActivity // GoogleDriveBaseActivity
{
    String TAG = "CloudStorageActivity";

    private static Activity activity;

    private CloudStorageFragment fragment;

    //final static String ARG_IMPORT_STREAM = "com.radicalpeas.radcases.ARG_IMPORT_STREAM";
    final static String ARG_IMPORT_URI = "com.radicalpeas.radcases.ARG_IMPORT_URI";

    //final static int REQUEST_SELECT_BACKUP_FILE = 0;
    final static int REQUEST_SELECT_IMPORT_CASES_FILE = 1;
    final static int REQUEST_LIST_JSON_FILE = 2;
    final static int REQUEST_IMPORT_CASES = 3;
    final static int REQUEST_EXPORT_CASES_SAF = 4;

    final static int PW_START = 3;
    final static int PW_END = 13;

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

    private android.app.ProgressDialog progressBar = null;
    //private AlertDialog progressBarDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setDrawerPosition(NavDrawerActivity.POS_CLOUD_STORAGE);
        super.onCreate(savedInstanceState);
        activity = this;
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

        switch (id)
        {
            case R.id.menu_clear_cache:

                final File downloadsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                android.app.ProgressDialog progressWheelDialog = new android.app.ProgressDialog(activity, activity.getResources().getColor(R.color.default_colorAccent));
                progressWheelDialog.setTitle("Clearing cache...");
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

    static private String password = null;

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
        final EditText pw_input = new EditText(this);
        final Activity activity = this;

        // user sets filename and encryption password
        input.setText(filename);
        input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint("File name");
        pw_input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        pw_input.setHint("Encryption password");
//		alertBuilder.setView(input);
//		alertBuilder.setView(pw_input);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(input);
        layout.addView(pw_input);

        alertBuilder.setView(layout);

        alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                dialog.dismiss();

                final String value = input.getText().toString();
                exportFilename = value + RCS_EXTENSION;
                exportMIMEtype = RCS_MIMETYPE;

                final String password = pw_input.getText().toString();

				/*
				LayoutInflater inflater = activity.getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.alertdialog_progress, null);
				AlertDialog.Builder progressBuilder = new AlertDialog.Builder(activity).setCancelable(false);
				progressBuilder.setView(dialoglayout);
				progressBarDialog = progressBuilder.create();
				progressBarDialog.show();

				progressBar = (ProgressBarIndeterminateDeterminate) dialoglayout.findViewById(R.id.progress_bar);
				progressBar.setMin(0);
*/
                progressBar = new ProgressDialog(activity, R.style.ProgressDialogTheme);
                progressBar.setTitle("Please wait.");
                progressBar.setMessage("Exporting cases...");
                //progressBar.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
                progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                //progressBar.setIndeterminate(true);
                progressBar.setCancelable(false);
                progressBar.show();

                Thread exportThread = new Thread()
                {
                    @Override
                    public void run()
                    {
                        local_file_to_cloud = UtilClass.exportCasesJSON(activity, value, null, password, progressHandler); //exportCasesCSV(value);

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
                            //	UtilClass.showSnackbar(activity, "Unable to export cases to a file.");
                            progressBar.dismiss();
//							Toast.makeText(activity, "Unable to export cases to a file.", Toast.LENGTH_LONG);
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
						UtilClass.showSnackbar(this, uri.toString());
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
                        UtilClass.showSnackbar(this, "Unable to create temporary file.");
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
                        UtilClass.showSnackbar(this, "local CSV file not found");
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        UtilClass.showSnackbar(this, "Copy CSV to Google Drive: IO exception");
                        UtilClass.showSnackbar(this, "cannot open input stream from selected uri");
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
    final public static int PROGRESS_MSG_SET_PROGRESS = 2;
    final public static int PROGRESS_MSG_INCREMENT = 3;
    final public static int PROGRESS_MSG_FINISHED = 5;
    final public static int PROGRESS_MSG_SET_TEXT = 6;
    final public static int PROGRESS_MSG_CASE_COUNT = 7;

    Handler progressHandler = new Handler(new Handler.Callback()
    {
        int progress = 0;

        @Override
        public boolean handleMessage(Message msg)
        {

            switch (msg.arg1)
            {
                default:
                case PROGRESS_MSG_INCREMENT:
                    progressBar.setProgress(progress++);
                    break;
                //			case PROGRESS_MSG_MIN:
                //				progressBar.setMin(msg.arg2);
                //				break;
                case PROGRESS_MSG_MAX:
                    progressBar.setMax(msg.arg2);
                    break;
                case PROGRESS_MSG_SET_PROGRESS:
                    progressBar.setProgress(msg.arg2);
                    break;

                case PROGRESS_MSG_FINISHED:
                    progressBar.dismiss();
                    UtilClass.showSnackbar(activity, "Exported " + msg.arg2 + " cases.");
                    break;

            }


            return false;
        }
    });


    //////////////////////

    public static class CloudStorageFragment extends Fragment
    {
        private ProgressDialog progressDialog = null;

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
            // CASE SAVE / BACKUP TO CLOUD
            view.findViewById(R.id.case_upload_button).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //if(UtilClass.uploadToCloud(activity))
                    //	UtilClass.showSnackbar(activity, "Cases uploaded.");
                    new UploadCasesTask().execute();
                }
            });

            view.findViewById(R.id.case_download_button).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {

                    //int count = UtilClass.downloadFromCloud(activity);
                    //if(count >= 0)
                    //	UtilClass.showSnackbar(activity, count + " cases downloaded.");

                    new DownloadCasesTask().execute();

                }
            });

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
                    Snackbar snackbar = Snackbar
                            .make(v, "In the main case list, long press on a case to start sharing.", Snackbar.LENGTH_LONG)
                            .setActionTextColor(Color.GREEN)
                            .setAction("GO", new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    Intent intent = new Intent(activity, CaseCardListActivity.class);
                                    activity.startActivity(intent);

                                    activity.finish();
                                }
                            });
                    snackbar.show();
					/*
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
*/
                    //UtilClass.showSnackbar(activity, "In the main case list, long press on a case to start sharing.");
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

        /**
         * Complete upload of all cases into firebase storage
         */
        private class UploadCasesTask extends AsyncTask<Void, Integer, Integer>
        {
            private static final String TAG = "UploadCasesTask";
            private static final int READING_CASE_INFO = 0;
            private static final int ENCRYPTING_CASE_INFO = 1;
            private static final int UPLOADING_CASE_IMAGES = 2;

            private final Context context = activity;

            protected void onPreExecute()
            {
                //progressDialog = new android.app.ProgressDialog(activity, "Uploading cases", getResources().getColor(R.color.default_colorAccent)); //UtilClass.get_attr(activity, R.color.default_colorAccent));
                progressDialog = new android.app.ProgressDialog(activity, R.style.ProgressDialogTheme); //UtilClass.get_attr(activity, R.color.default_colorAccent));
                progressDialog.setTitle("Saving to cloud");
                progressDialog.setMessage("");
                progressDialog.setProgressNumberFormat(null);
                //progressDialog.setProgressPercentFormat(null);
                //progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
                progressDialog.setIndeterminate(true);///
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            // UploadCasesTaks
            @Override
            protected Integer doInBackground(Void... params)
            {
                File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                File picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File casesJSON;
                int count = 0;

                // Firebase: upload images and JSON files
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null)
                {
                    try
                    {
                        casesJSON = new File(downloadsDir.getPath(), ImportExportActivity.CASES_JSON_FILENAME);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        //showSnackbar(activity, "Unable to create local backup file.");
                        Log.d(TAG, "Unable to create local backup file.");
                        return -1;
                    }

                    // filepath of images to upload to firebase storage
                    String imageFilepaths[] = new String[0];

                    // create local JSON files
                    publishProgress(PROGRESS_MSG_SET_TEXT, READING_CASE_INFO);

                    try
                    {
                        FileOutputStream cases_out = new FileOutputStream(casesJSON);

                        // Get cases to export from cases table into a cursor
                        Cursor caseCursor;
                        // get all cases
                        caseCursor = context.getContentResolver().query(CasesProvider.CASES_URI, null, null, null, null, null);

                        if (caseCursor != null && caseCursor.moveToFirst())
                        {
                            JsonWriter cases_writer = new JsonWriter(new OutputStreamWriter(cases_out, "UTF-8"));
                            cases_writer.setIndent("  ");

                            // write metadata
                            cases_writer.beginObject();
                            cases_writer.name("NUM_CASES").value(caseCursor.getCount());
                            cases_writer.name("DATE_CREATED").value(new SimpleDateFormat("yyyy-MM-dd HHmm").format(new Date()));
                            cases_writer.name("USER").value(user.getEmail());

                            cases_writer.name("DATA");

                            // loop through all cases
                            cases_writer.beginArray();
                            do
                            {
                                cases_writer.beginObject();

                                // output all case columns/fields for this case
                                for (int i = 0; i < CasesProvider.CASES_TABLE_ALL_KEYS.length; i++)
                                {
                                    if (caseCursor.getString(i) != null && !caseCursor.getString(i).isEmpty())
                                    {
                                        cases_writer.name(CasesProvider.CASES_TABLE_ALL_KEYS[i]).value(caseCursor.getString(i));
                                    }
                                }

                                // output all linked images for this case (via parent_case_id)
                                String[] image_args = {String.valueOf(caseCursor.getInt(CasesProvider.COL_ROWID))};
                                Cursor imageCursor = context.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

                                // loop through all images of this case
                                if (imageCursor.moveToFirst())
                                {
                                    cases_writer.name("IMAGES");
                                    cases_writer.beginArray();
                                    do
                                    {
                                        cases_writer.beginObject();
                                        for (int i = 0; i < CasesProvider.IMAGES_TABLE_ALL_KEYS.length; i++)
                                        {
                                            cases_writer.name(CasesProvider.IMAGES_TABLE_ALL_KEYS[i]).value(imageCursor.getString(i));
                                        }
                                        cases_writer.endObject();

                                        // add image filename to zip list
                                        imageFilepaths = UtilClass.addArrayElement(imageFilepaths, picturesDir + "/" + imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME));

                                    } while (imageCursor.moveToNext());

                                    cases_writer.endArray();
                                }
                                else
                                {
                                    cases_writer.name("IMAGES").nullValue();
                                }

                                imageCursor.close();

                                cases_writer.endObject();

                            } while (caseCursor.moveToNext());

                            // number of cases
                            count = caseCursor.getCount();

                            caseCursor.close();

                            cases_writer.endArray();
                            cases_writer.endObject();
                            cases_writer.close();
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        Log.d(TAG, "IOException: " + e.getMessage());
                        return -1;
                    }

                    // encrypt casesJSON file
                    publishProgress(PROGRESS_MSG_SET_TEXT, ENCRYPTING_CASE_INFO);
					try
					{
						byte[] passkey = UtilsFile.generateKey(user.getUid().substring(PW_START, PW_END));
						UtilsFile.encryptFile(passkey, casesJSON);
					}
					catch(Exception e)
					{
						e.printStackTrace();
						Log.d(TAG, "Unable to generate encryption key.");
						return -1;
					}

                    // set up Firebase storage reference
                    FirebaseStorage mStorage = FirebaseStorage.getInstance();
                    StorageReference mStorageRef = mStorage.getReferenceFromUrl("gs://rad-files.appspot.com");

                    // put in userID folder
                    String userID = user.getUid();
                    StorageReference storageCasesJSON = mStorageRef.child(userID + "/" + ImportExportActivity.CASES_JSON_FILENAME);    // filename entered by user in alertdialog

                    // upload encrypted casesJSON file to Firebase server
                    UploadTask casesJSON_uploadTask = storageCasesJSON.putFile(Uri.fromFile(casesJSON));

                    // Register observers to listen for when the upload is done or if it fails
                    casesJSON_uploadTask.addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception exception)
                        {
                            // Handle unsuccessful uploads
                            //				showSnackbar(context, "Failed uploading case info.");
                            Log.d(TAG, "Failed uploading case info.");

                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
                    {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                        {
                            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            //				showSnackbar(context, "Successfully uploaded case info.");
                            Log.d(TAG, "Successfully uploaded case info.");
                        }
                    });

                    // upload case images
                    publishProgress(PROGRESS_MSG_SET_TEXT, UPLOADING_CASE_IMAGES);
                    publishProgress(PROGRESS_MSG_MAX, imageFilepaths.length);

                    // image file fullpath stored in imageFilepaths[]
                    StorageReference storageImages;
                    // Create the file metadata
                    StorageMetadata metadata = new StorageMetadata.Builder()
                            .setContentType("image/jpeg")
                            .build();

                    for (int i = 0; i < imageFilepaths.length; i++)
                    {
                        publishProgress(PROGRESS_MSG_SET_PROGRESS, i);

                        // put in userID/pictures folder
                        final File filePath = new File(imageFilepaths[i]);
                        storageImages = mStorageRef.child(userID + "/pictures/" + filePath.getName());

                        // open FileInputStream based on image fullpath stored in zip_files_array[], and upload to Firebase server
                        try
                        {
                            final FileInputStream fi = new FileInputStream(imageFilepaths[i]);
                            UploadTask image_uploadTask = storageImages.putStream(fi, metadata);

                            // limit of 128 total asynctasks
                            List tasks = mStorageRef.getActiveUploadTasks();
                            while (tasks.size() > 100)
                            {
                                Thread.sleep(500);
                                tasks = mStorageRef.getActiveUploadTasks();
                            }
                            // Register observers to listen for when the download is done or if it fails
                            image_uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
                            {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    // Handle successful uploads on complete
                                    Uri downloadUrl = taskSnapshot.getMetadata().getDownloadUrl();
//							showSnackbar(context, "Upload images successful.");

                                    System.out.println("Successfully uploaded image: " + filePath.getName());
                                    Log.d(TAG, "Successfully uploaded image: " + filePath.getName());

                                }
                            }).addOnFailureListener(new OnFailureListener()
                            {
                                @Override
                                public void onFailure(@NonNull Exception exception)
                                {
                                    // Handle unsuccessful uploads
                                    //				showSnackbar(activity, "Failed uploading images.");

//							showSnackbar(context, "Failed uploading images.");
                                    System.out.println("Failed uploading image: " + filePath.getName());
                                    Log.d(TAG, "Failed uploading image: " + filePath.getName());
                                }
                            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
                            {
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                    System.out.println("Upload is " + progress + "% done");
                                }
                            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>()
                            {
                                @Override
                                public void onPaused(UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    System.out.println("Upload is paused");
                                }
                            });


                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            Log.d(TAG, "IOException: " + e.getMessage());
                            return -1;
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                            Log.d(TAG, "InterruptedException: " + e.getMessage());
                            return -1;
                        }
                    } //loop imageFilepaths[]

                    // delete temporary files
                    casesJSON.delete();

                    return count;

                } // end if Firebase user != null
                else
                {
                    return -1;
                }
            }

            protected void onProgressUpdate(Integer... params)
            {
                // 0: type, 1: param
                switch (params[0])
                {
                    case PROGRESS_MSG_MAX:
                        progressDialog.setMax(params[1]);
                        progressDialog.setIndeterminate(false);
                        break;

                    case PROGRESS_MSG_SET_PROGRESS:
                        progressDialog.setProgress(params[1]);
                        break;

                    case PROGRESS_MSG_FINISHED:
                        progressDialog.dismiss();
                        //progressDialog.dismiss();

                        if (params[1] >= 0)
                            UtilClass.showSnackbar(activity, "Uploaded " + params[1] + " cases.");
                        break;

                    case PROGRESS_MSG_SET_TEXT:
                        //progressDialog.setMessage((String)msg.obj);
                        if (params[1] == READING_CASE_INFO)
                        {
                            progressDialog.setMessage("Reading case info...");
                        }
                        else if (params[1] == ENCRYPTING_CASE_INFO)
                        {
                            progressDialog.setMessage("Encrypting case info...");
                        }
                        else if (params[1] == UPLOADING_CASE_IMAGES)
                        {
                            progressDialog.setMessage("Uploading case images...");
                        }
                        break;

                    default:
                        break;
                }
            }

            protected void onPostExecute(Integer count)
            {
                progressDialog.dismiss();

                if (count >= 0)
                {
                    UtilClass.showSnackbar(activity, "Finished uploading " + count + " cases.", Snackbar.LENGTH_INDEFINITE);
                }
                else
                {
                    UtilClass.showSnackbar(activity, "Error uploading cases.", Snackbar.LENGTH_LONG);
                }
            }
        }

        /**
         * Complete download of all cases into firebase storage
         */
        private class DownloadCasesTask extends AsyncTask<Void, Integer, Integer>
        {
            private static final String TAG = "DownloadCasesTask";
            private static final int DOWNLOADING_CASE_INFO = 0;
            private static final int UNENCRYPTING_CASE_INFO = 1;
            private static final int DOWNLOADING_CASES = 2;

            private final Context context = activity;
            private int pendingTaskCount;


            protected void onPreExecute()
            {
                //progressDialog = new android.app.ProgressDialog(activity, "Uploading cases", getResources().getColor(R.color.default_colorAccent)); //UtilClass.get_attr(activity, R.color.default_colorAccent));
                progressDialog = new android.app.ProgressDialog(activity, R.style.ProgressDialogTheme); //UtilClass.get_attr(activity, R.color.default_colorAccent));
                progressDialog.setTitle("Restoring cases");
                progressDialog.setMessage("");
                progressDialog.setProgressNumberFormat(null);
                //progressDialog.setProgressPercentFormat(null);
                //progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
                progressDialog.setIndeterminate(true);///
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//
                progressDialog.setCancelable(false);
                progressDialog.show();

            }

            // DownloadCasesTask
            @Override
            protected Integer doInBackground(Void... params)
            {
                // upload to firebase storage
                //int count =  UtilClass.downloadFromCloud(activity, downloadHandler);
                //return count;	// count not updated within downloadtask thread

                final File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                final File picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                int caseCount = 0;
                pendingTaskCount = 0;

                // Firebase: upload images and JSON files
                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null)
                {
                    // set up Firebase storage reference
                    final FirebaseStorage mStorage = FirebaseStorage.getInstance();
                    final StorageReference mStorageRef = mStorage.getReferenceFromUrl("gs://rad-files.appspot.com");

                    // get casesJSON file in userID folder
                    final String userID = user.getUid();
                    final StorageReference storageCasesJSON = mStorageRef.child(userID + "/" + ImportExportActivity.CASES_JSON_FILENAME);    // filename entered by user in alertdialog

                    try
                    {
                        publishProgress(PROGRESS_MSG_SET_TEXT, DOWNLOADING_CASE_INFO);

                        // download casesJSON from Firebase storage
                        //final File localCasesJSON = File.createTempFile(ImportExportActivity.CASES_JSON_FILENAME, "_temp");

                        final File localCasesJSON = new File(downloadsDir, ImportExportActivity.CASES_JSON_FILENAME);
                        //final File testFile = new File(downloadsDir, "testjson.txt");

                        storageCasesJSON.getFile(localCasesJSON).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>()
                        {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
                            {
                                int caseCount = 0;
                                int numCases = 0;

                                // success
                                // Local temp file has been created
                                // decrypt casesJSON file
                                try
                                {
                                    publishProgress(PROGRESS_MSG_SET_TEXT, UNENCRYPTING_CASE_INFO);

									byte[] passkey = UtilsFile.generateKey(user.getUid().substring(PW_START,PW_END));
									UtilsFile.decryptFile(passkey, localCasesJSON);

                                    // set up JSON reader from decrypted casesJSON
                                    //final FileInputStream cases_in = new FileInputStream(localCasesJSON);
                    //                final JsonReader reader = new JsonReader(new InputStreamReader(cases_in, "UTF-8"));
                                    /*
                                    final Gson gsonReader = new Gson();
                                    gsonReader.fromJson(cases_in.toString(), CasesDB.class);
*/
                                    // pass JSON File to be parsed by GSON in CasesDB class
                                    //CasesDB cases_info = new CasesDB().parseJSON(new InputStreamReader(new FileInputStream(localCasesJSON)));
                                    CasesDB cases_info = new CasesDB().parseJSON(localCasesJSON);


                                    ///////////
                                    // CASES TABLE

                                    Uri rowUri = null;
                                    int parent_id;

                                    ContentValues insertCaseValues = new ContentValues();
                                    ContentValues insertImageValues = new ContentValues();

                                    // get metadata
                                    String user = cases_info.getUser();
                                    String date_created = cases_info.getDateCreated();

                                    List<Case> caseList = cases_info.getCaseList();

                                    publishProgress(PROGRESS_MSG_SET_TEXT, DOWNLOADING_CASES);
                                    publishProgress(PROGRESS_MSG_MAX, caseList.size());

                                    for(int c = 0; c < caseList.size(); c++)
                                    {
                                        insertCaseValues.clear();

                                        insertCaseValues.put(CasesProvider.KEY_PATIENT_ID, caseList.get(c).patient_id);
                                        insertCaseValues.put(CasesProvider.KEY_DIAGNOSIS, caseList.get(c).diagnosis);
                                        insertCaseValues.put(CasesProvider.KEY_SECTION, caseList.get(c).section);
                                        insertCaseValues.put(CasesProvider.KEY_FINDINGS, caseList.get(c).findings);
                                        insertCaseValues.put(CasesProvider.KEY_BIOPSY, caseList.get(c).biopsy);
                                        insertCaseValues.put(CasesProvider.KEY_FOLLOWUP, caseList.get(c).followup);
                                        insertCaseValues.put(CasesProvider.KEY_FOLLOWUP_COMMENT, caseList.get(c).followup_comment);
                                        insertCaseValues.put(CasesProvider.KEY_KEYWORDS, caseList.get(c).key_words);
                                        insertCaseValues.put(CasesProvider.KEY_COMMENTS, caseList.get(c).comments);
                                        insertCaseValues.put(CasesProvider.KEY_STUDY_TYPE, caseList.get(c).study_type);
                                        insertCaseValues.put(CasesProvider.KEY_DATE, caseList.get(c).db_date_str);
                                        insertCaseValues.put(CasesProvider.KEY_IMAGE_COUNT, caseList.get(c).image_count);
                                        insertCaseValues.put(CasesProvider.KEY_THUMBNAIL, caseList.get(c).thumbnail);
                                        insertCaseValues.put(CasesProvider.KEY_FAVORITE, caseList.get(c).favorite);
                                        insertCaseValues.put(CasesProvider.KEY_CLINICAL_HISTORY, caseList.get(c).clinical_history);
                                        insertCaseValues.put(CasesProvider.KEY_LAST_MODIFIED_DATE, caseList.get(c).last_modified_date);

                                        rowUri = context.getContentResolver().insert(CasesProvider.CASES_URI, insertCaseValues);

                                        if (rowUri != null)
                                        {
                                            // get parent key information
                                            parent_id = Integer.valueOf(rowUri.getLastPathSegment());

                                            // increment count
                                            caseCount += 1;

                                            // Images
                                            List<CaseImage> imageList = caseList.get(c).caseImageList;
                                            for (int i = 0; i < imageList.size(); i++)
                                            {
                                                insertImageValues.clear();

                                                // put new parent_id of newly added case
                                                insertImageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, parent_id);
                                                insertImageValues.put(CasesProvider.KEY_IMAGE_FILENAME, imageList.get(i).getFilename());
                                                insertImageValues.put(CasesProvider.KEY_ORDER, imageList.get(i).getOrder());
                                                insertImageValues.put(CasesProvider.KEY_IMAGE_DETAILS, imageList.get(i).getDetails());
                                                insertImageValues.put(CasesProvider.KEY_IMAGE_CAPTION, imageList.get(i).getCaption());

                                                // insert the set of image info into the DB images table
                                                context.getContentResolver().insert(CasesProvider.IMAGES_URI, insertImageValues);


                                                // download image file

                                                // image filename
                                                final String image_filename = imageList.get(i).getFilename();

                                                // download file from Firebase storage
                                                StorageReference storageImageRef = mStorageRef.child(userID + "/pictures/" + image_filename);
                                                try
                                                {
                                                    // limit of 128 total asynctasks, wait while other tasks finish
                                                    /*
                                                    List tasks = mStorageRef.getActiveDownloadTasks();
                                                    while (pendingTaskCount > 100)
                                                    {
                                                        Thread.sleep(500);
                                                        tasks = mStorageRef.getActiveDownloadTasks();
                                                    }
                                                    */

                                                    // download image file from Firebase storage
                                                    pendingTaskCount += 1;
                                                    final File localImageFile = new File(picturesDir, image_filename);

                                                    storageImageRef.getFile(localImageFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>()
                                                    {
                                                        @Override
                                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
                                                        {
                                                            pendingTaskCount -= 1;

                                                            System.out.println("Successfully downloaded image: " + image_filename);
                                                            Log.d(TAG, "Successfully downloaded image: " + image_filename);
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener()
                                                    {
                                                        @Override
                                                        public void onFailure(@NonNull Exception exception)
                                                        {
                                                            pendingTaskCount -= 1;

                                                            // Handle any errors
                                                            System.out.println("Failed to download image: " + image_filename);
                                                            Log.d(TAG, "Failed to download image: " + image_filename);

                                                        }
                                                    });

                                                }
                                                catch (Exception e)
                                                {
                                                    e.printStackTrace();
                                                    Log.d(TAG, "Exception: " + e.getMessage() + ". Failed to create file for image: " + image_filename);
                                                    System.out.println("Failed to create file for image: " + image_filename);

                                                    return;
                                                }

                                            }

                                        }

                                        publishProgress(PROGRESS_MSG_SET_PROGRESS, caseCount);
                                    } // end for loop caseList

                                    // finished looped through all cases
                                    publishProgress(PROGRESS_MSG_FINISHED, caseCount);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                    //					Toast.makeText(activity, "Unable to read Cases JSON file", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Exception: " + e.getMessage() + ". Unable to read Cases JSON file.");

                                    // Exception, but need to close progressDialog
                                    publishProgress(PROGRESS_MSG_FINISHED, -1);

                                    return;
                                }

    //                            localCasesJSON.delete();

                            }
                        }).addOnFailureListener(new OnFailureListener()
                        {
                            @Override
                            public void onFailure(@NonNull Exception exception)
                            {
                                // Handle any errors

                                // Exception, but need to close progressDialog
                                publishProgress(PROGRESS_MSG_FINISHED, -1);
                            }
                        }); // download JSON from firebase storage

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        Log.d(TAG, "IOException: " + e.getMessage());
                        //		Toast.makeText(activity, "Unable to create local Cases JSON file", Toast.LENGTH_SHORT).show();

                        // Exception, but need to close progressDialog
                        publishProgress(PROGRESS_MSG_FINISHED, -1);

                        return -1;
                    } //localJSON file open/create

			/*
			// wait until almost finished downloading
			try
			{
				List tasks = mStorageRef.getActiveDownloadTasks();
				while (tasks.size() > 1)
				{
					Thread.sleep(500);
					tasks = mStorageRef.getActiveUploadTasks();
				}
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				Log.d(TAG, "InterruptedException: " + e.getMessage());
				return -1;
			}
			*/

                }
                else
                {
                    // user is null
                    //		Toast.makeText(activity, "User not logged in", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User not logged in.");
                    // Exception, but need to close progressDialog
                    publishProgress(PROGRESS_MSG_FINISHED, -1);

                    return -1;
                }

                return caseCount;
            } // end DownloadTask doInBackground

            protected void onProgressUpdate(Integer... params)
            {
                // 0: type, 1: param
                switch (params[0])
                {
                    case PROGRESS_MSG_MAX:
                        progressDialog.setMax(params[1]);
                        progressDialog.setIndeterminate(false);
                        break;

                    case PROGRESS_MSG_SET_PROGRESS:
                        progressDialog.setProgress(params[1]);
                        break;

                    case PROGRESS_MSG_FINISHED:
                        progressDialog.dismiss();
                        //progressDialog.dismiss();
                        if (params[1] >= 0)
                            UtilClass.showSnackbar(activity, "Downloaded " + params[1] + " cases.");
                        else
                            UtilClass.showSnackbar(activity, "Error downloading cases.", Snackbar.LENGTH_INDEFINITE);

                        break;

                    case PROGRESS_MSG_SET_TEXT:
                        //progressDialog.setMessage((String)msg.obj);
                        if (params[1] == DOWNLOADING_CASE_INFO)
                        {
                            progressDialog.setMessage("Downloading case info...");
                        }
                        else if (params[1] == UNENCRYPTING_CASE_INFO)
                        {
                            progressDialog.setMessage("Unencrypting case info...");
                        }
                        else if (params[1] == DOWNLOADING_CASES)
                        {
                            progressDialog.setMessage("Loading cases...");
                        }
                        break;

                    default:
                        break;
                }
            }

            protected void onPostExecute(Integer count)
            {
                //progressDialog.dismiss();
                //UtilClass.showToast(activity, "Finished downloading " + caseCount + " cases.");

                //finish();
                if (count == -1)    //error occured
                {
                    progressDialog.dismiss();
                    UtilClass.showSnackbar(activity, "Error occurred during download.", Snackbar.LENGTH_INDEFINITE);
                }
            }
        } // end Class DownloadCasesTask
    }

    private class ClearDirectoryTask extends AsyncTask<File, Integer, Boolean>
    {
        private Activity activity;
        private android.app.ProgressDialog progressWheelDialog;

        ClearDirectoryTask(Activity activity, android.app.ProgressDialog progressWheelDialog)
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

            if (isSuccessful)
            {
                UtilClass.showSnackbar(activity, "Deleted cache files.");
            }
            else
            {
                UtilClass.showSnackbar(activity, "Clear directory failed.");
            }
        }
    }


}