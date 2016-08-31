package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

//import com.gc.materialdesign.widgets.ProgressDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CaseImport extends AppCompatActivity
{
	public static final String TAG = "CaseImport";

	RecyclerView mRecyclerView;
	CaseCardAdapter mCardAdapter;

	// standard directories
	//private static File downloadsDir = UtilClass.getDownloadsDir();

	final static String CASES_CSV_FILENAME = "cases_table.csv";
	final static String IMAGES_CSV_FILENAME = "images_table.csv";

	File importFile = null;
	String password = null;

	private Activity activity;

	private String userID;  // firebase userID
	private String userEmail;

	private android.app.ProgressDialog progressDialog = null;

	private CasesDB cases_info;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_import);

	    activity = this;
	    File downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

	    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

	    // Set an OnMenuItemClickListener to handle menu item clicks
	    toolbar.setOnMenuItemClickListener(
			                                      new Toolbar.OnMenuItemClickListener() {
				                                      @Override
				                                      public boolean onMenuItemClick(MenuItem item) {
					                                      // Handle the menu item
					                                      return true;
				                                      }
			                                      });

	    // Inflate a menu to be displayed in the toolbar
	    toolbar.inflateMenu(R.menu.case_import);

		// Firebase user log in
		FirebaseAuth mAuth = FirebaseAuth.getInstance();
		if(mAuth != null)
		{
			FirebaseUser firebaseUser = mAuth.getCurrentUser();
			if(firebaseUser != null)
			{
				userID = firebaseUser.getUid();
				userEmail = firebaseUser.getEmail();
			}
		}

	    // Find RecyclerView
	    mRecyclerView = (RecyclerView)findViewById(R.id.cards_list);

	    // Setup RecyclerView
	    mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
	    mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		// Setup FastScroller
		FastScroller fastScroller=(FastScroller)findViewById(R.id.case_import_fastscroller);
		fastScroller.setRecyclerView(mRecyclerView);

	    // Setup CaseCardAdapter
	    mCardAdapter = new CaseCardAdapter(this, null, R.layout.card_case);

	    mRecyclerView.setAdapter(mCardAdapter);

	    // Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();
	    Uri uri = intent.getParcelableExtra(ImportExportActivity.ARG_IMPORT_URI);

	    if (Intent.ACTION_VIEW.equals(action) && type != null)
	    {
		    // IMPORT from external source intent
		    if (type.equals("application/zip") || type.equals("*/*") || type.equals("application/octet-stream") )
		    {
			    Uri import_uri = intent.getData();

			    try
			    {
				    importFile = UtilsFile.makeLocalFile(this, downloadsDir, "RadFiles import", "rcs", import_uri);
			    }
			    catch (IOException e)
			    {
				    e.printStackTrace();
			    }
		    }
	    }
	    else if(uri != null)
	    {
		    // IMPORT from app ImportExportActivity
		    try
		    {
			    importFile = UtilsFile.makeLocalFile(this, downloadsDir, "RadFiles import", "rcs", uri);
		    }
		    catch (IOException e)
		    {
			    e.printStackTrace();
		    }
	    }
	    else
	    {
		    // Show error message and end activity

		    AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setMessage("Unable to open file").setTitle("Error")
				    .setPositiveButton(this.getResources().getString(R.string.button_OK), new DialogInterface.OnClickListener()
				    {
					    public void onClick(DialogInterface dialog, int id)
					    {
						   finish();
					    }
				    });
		    AlertDialog alert = builder.create();
		    alert.show();
	    }

		if(importFile != null)
		{
			// User prompt for passkey for decryption of JSON file
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);

			// Set an EditText view to get user input
			final EditText input = new EditText(activity);
			input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			//input.setHighlightColor(activity.getResources().getColor(R.color.default_colorControlHighlight));
			input.setHighlightColor(UtilClass.get_attr(activity, R.attr.colorControlHighlight));

			alertBuilder.setTitle("Enter password");

			alertBuilder.setView(input);

			alertBuilder.setPositiveButton(R.string.button_OK, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					password = input.getText().toString();
					new LoadImportListTask().execute(importFile.getPath(), password);
				//	new ImportCasesTask().execute(importFile.getPath(), password);
				}
			});

			alertBuilder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					// Canceled.
					//todo what to do, clean up
					finish();
				}
			});

			AlertDialog dialog = alertBuilder.create();
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
		else
		{
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
			alertBuilder.setTitle("File not found. Check internet connection.");

			alertBuilder.setPositiveButton(R.string.button_OK, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					  finish();
				}
			});

			AlertDialog dialog = alertBuilder.create();
			dialog.show();
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.case_import, menu);
        return true;
    }

	/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


        int id = item.getItemId();
        if (id == R.id.menu_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */


	/**
	 * UI clicks
	 */

	public void onClick_Button(View view) throws IOException
	{
		switch(view.getId())
		{
			case R.id.cancelButton:
				//TODO delete files that we don't need
				setResult(CaseCardListActivity.RESULT_NOCHANGE);

				if(importFile != null)
					importFile.delete();

				finish();
				break;

			case R.id.importButton:

				if(importFile != null)
				{
					setResult(CaseCardListActivity.REQUEST_ADD_CASE);
					//UtilClass.importCasesJSON(this, importFile);
					new ImportCasesTask().execute(importFile);
				}
				else
				{
					UtilClass.showSnackbar(this, "Error: file not found.");
				}
				break;
		}
	}

	@Override
	public void onBackPressed()
	{
		// pop up dialog

		// Alert dialog to confirm save
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage("Discard these cases?")
				.setPositiveButton(getResources().getString(R.string.button_discard), new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						finish();
					}
				})
				.setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						// cancel
					}
				});

		AlertDialog alert = builder.create();
		alert.show();

	}


	/**
	 * show list of cases from import file
	 */
	private class LoadImportListTask extends AsyncTask<String, Integer, String>
	{
		protected void onPreExecute()
		{
			progressDialog = new ProgressDialog(activity, R.style.ProgressDialogTheme);
			progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
			progressDialog.setMessage("Opening import file");
			progressDialog.setCancelable(false);
			progressDialog.show();
		}

		@Override
		protected String doInBackground(String... params)
		{
			String file_path = params[0];
			String password = params[1];
			int numCases = 0;

			List<Case> importCaseList = new ArrayList<Case>();

			File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

			if(picturesDir != null)
			{
				// unzip image files and JSON files into pictures dir
				try
				{
					// unzip image files to android pictures directory
					UtilsFile.unzip(file_path, picturesDir.getPath());
				}
				catch (IOException e)
				{
					e.printStackTrace();
					Toast.makeText(activity, "Unable to open zip file.", Toast.LENGTH_SHORT).show();
					return "Unable to open rcs zip file.";
				}
			}
			else
			{
				Toast.makeText(activity, "Unable to open pictures folder.", Toast.LENGTH_SHORT).show();
				return "Unable to open pictures folder.";
			}

			File tempCasesJSON = null;

			try
			{	// open existing file that should have been unzipped
				tempCasesJSON = new File(picturesDir, ImportExportActivity.CASES_JSON_FILENAME);

				// decrypt JSON file unless blank password given
				if (!password.contentEquals(""))
				{
					try
					{
						byte[] passkey = UtilsFile.generateKey(password);
						UtilsFile.decryptFile(passkey, tempCasesJSON);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						return "Unable to generate encryption key.";
					}
				}

				// read data from JSON
				cases_info = new CasesDB().parseJSON(tempCasesJSON);

				// get case list to pass to UI card adapter
				importCaseList = cases_info.getCaseList();

				if(importCaseList == null)    // no cases
				{
					publishProgress(ImportExportActivity.PROGRESS_MSG_FINISHED, 0);
					return "No cases found.";
				}

				// get thumbnail image for each case
				for(int c = 0; c < importCaseList.size(); c++)
				{
					Case mCase = importCaseList.get(c);

					if(mCase.caseImageList.size() > 0)
					{
						if (mCase.thumbnail == -1 || mCase.thumbnail >= mCase.caseImageList.size())   // default: use first image
						{
							mCase.thumbnail_filename = picturesDir + "/" + mCase.caseImageList.get(0);
						}
						else
						{
							mCase.thumbnail_filename = picturesDir + "/" + mCase.caseImageList.get(mCase.thumbnail).getFilename();
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return "Unable to open JSON file.";
			}

			tempCasesJSON.delete();

			mCardAdapter.loadCaseList(importCaseList);

			return null; // no error message
		}

		protected void onPostExecute(String error_msg)
		{
			progressDialog.dismiss();

			if(error_msg == null)
			{
				mCardAdapter.notifyDataSetChanged();
			}
			else
			{
				UtilClass.showSnackbar(activity, error_msg);
			}
		}
	}

	/**
	 * Complete import of selected cases into database
	 */
	private class ImportCasesTask extends AsyncTask<File, Integer, Integer>
	{
		private static final String TAG = "ImportCasesTask";
		private static final int DOWNLOADING_CASE_INFO = 0;
		private static final int READING_CASE_INFO = 1;
		private static final int DOWNLOADING_CASES = 2;

		Context context = activity;

		protected void onPreExecute()
		{
			progressDialog = new ProgressDialog(activity, R.style.ProgressDialogTheme);
			progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
			progressDialog.setMessage("Importing cases...");
			progressDialog.setCancelable(false);
			progressDialog.show();
		}

		@Override
		protected Integer doInBackground(File... files)
		{
			int caseCount = 0;

			Uri rowUri = null;
			int newCase_KEYID;

			ContentValues insertCaseValues = new ContentValues();
			ContentValues insertImageValues = new ContentValues();

			// get metadata
			String user = cases_info.getUser();
			String date_created = cases_info.getDateCreated();

			List<Case> caseList = cases_info.getCaseList();

			if (caseList == null)    // no cases
			{
				publishProgress(ImportExportActivity.PROGRESS_MSG_FINISHED, 0);
				return 0;
			}

			for (int c = 0; c < caseList.size(); c++)
			{
				insertCaseValues.clear();

				// insert into local SQL database
				insertCaseValues.put(CasesProvider.KEY_CASE_NUMBER, caseList.get(c).case_id);
				insertCaseValues.put(CasesProvider.KEY_DIAGNOSIS, caseList.get(c).diagnosis);
				insertCaseValues.put(CasesProvider.KEY_FINDINGS, caseList.get(c).findings);
				insertCaseValues.put(CasesProvider.KEY_SECTION, caseList.get(c).section);
				insertCaseValues.put(CasesProvider.KEY_STUDY_TYPE, caseList.get(c).study_type);
				insertCaseValues.put(CasesProvider.KEY_STUDY_DATE, caseList.get(c).db_date_str);
				insertCaseValues.put(CasesProvider.KEY_KEYWORDS, caseList.get(c).key_words);
				insertCaseValues.put(CasesProvider.KEY_BIOPSY, caseList.get(c).biopsy);
				insertCaseValues.put(CasesProvider.KEY_FOLLOWUP, caseList.get(c).followup);
				insertCaseValues.put(CasesProvider.KEY_FOLLOWUP_COMMENT, caseList.get(c).followup_comment);
				insertCaseValues.put(CasesProvider.KEY_CLINICAL_HISTORY, caseList.get(c).clinical_history);
				insertCaseValues.put(CasesProvider.KEY_COMMENTS, caseList.get(c).comments);
				insertCaseValues.put(CasesProvider.KEY_FAVORITE, caseList.get(c).favorite);
				insertCaseValues.put(CasesProvider.KEY_IMAGE_COUNT, caseList.get(c).image_count);
				insertCaseValues.put(CasesProvider.KEY_THUMBNAIL, caseList.get(c).thumbnail);
				insertCaseValues.put(CasesProvider.KEY_LAST_MODIFIED_DATE, caseList.get(c).last_modified_date);
				insertCaseValues.put(CasesProvider.KEY_ORIGINAL_CREATOR, caseList.get(c).original_creator);
				insertCaseValues.put(CasesProvider.KEY_IS_SHARED, caseList.get(c).is_shared);

				// use current userID, may have been shared from another user
				insertCaseValues.put(CasesProvider.KEY_USER_ID, userID);
				rowUri = UtilsDatabase.insertCase(context, insertCaseValues);

				if (rowUri != null)
				{
					// get parent key information
					newCase_KEYID = Integer.valueOf(rowUri.getLastPathSegment());

					// increment count
					caseCount += 1;

					// Images
					if (caseList.get(c).caseImageList != null)
					{
						List<CaseImage> imageList = caseList.get(c).caseImageList;

						for (int i = 0; i < imageList.size(); i++)
						{
							insertImageValues.clear();

							// put new newCase_KEYID of newly added case
							insertImageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, newCase_KEYID);
							insertImageValues.put(CasesProvider.KEY_IMAGE_FILENAME, imageList.get(i).getFilename());
							insertImageValues.put(CasesProvider.KEY_ORDER, imageList.get(i).getOrder());
							insertImageValues.put(CasesProvider.KEY_IMAGE_DETAILS, imageList.get(i).getDetails());
							insertImageValues.put(CasesProvider.KEY_IMAGE_CAPTION, imageList.get(i).getCaption());

							// insert the set of image info into the DB images table
							UtilsDatabase.insertImage(context, insertImageValues, i);
						}
					}

				} // end if successfully placed into SQL db

			} // end for loop caseList

			// delete the temporary downloaded file
			files[0].delete();

			return caseCount;
		}

		protected void onProgressUpdate(Integer... params)
		{
			// 0: type, 1: param
			switch (params[0])
			{
				case ImportExportActivity.PROGRESS_MSG_MAX:
					progressDialog.setMax(params[1]);
					progressDialog.setIndeterminate(false);
					break;

				case ImportExportActivity.PROGRESS_MSG_SET_PROGRESS:
					progressDialog.setProgress(params[1]);
					break;

				case ImportExportActivity.PROGRESS_MSG_FINISHED:
					progressDialog.dismiss();
					//progressDialog.dismiss();
					if (params[1] >= 0)
						UtilClass.showSnackbar(activity, "Downloaded " + params[1] + " cases.");
					else
						UtilClass.showSnackbar(activity, "Error downloading cases.", Snackbar.LENGTH_INDEFINITE);

					break;

				case ImportExportActivity.PROGRESS_MSG_SET_TEXT:
					//progressDialog.setMessage((String)msg.obj);
					if (params[1] == DOWNLOADING_CASE_INFO)
					{
						progressDialog.setMessage("Downloading case info...");
					}
					else if (params[1] == READING_CASE_INFO)
					{
						progressDialog.setMessage("Reading case info...");
					}
					else if (params[1] == DOWNLOADING_CASES)
					{
						progressDialog.setMessage("Loading images...");
					}
					break;

				case ImportExportActivity.PROGRESS_MSG_INCREMENT:
					progressDialog.incrementProgressBy(1);
					break;

				default:
					break;
			}
		}

		protected void onPostExecute(Integer count)
		{
			progressDialog.dismiss();
			UtilClass.showSnackbar(activity, "RadFiles imported " + count + " cases.", Snackbar.LENGTH_INDEFINITE);

			finish();
		}
	}
}
