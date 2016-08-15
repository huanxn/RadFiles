package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.JsonReader;
import android.util.JsonToken;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

//import com.gc.materialdesign.widgets.ProgressDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class CaseImport extends AppCompatActivity
{

	RecyclerView mRecyclerView;
	CaseCardAdapter mCardAdapter;

	// standard directories
	//private static File downloadsDir = UtilClass.getDownloadsDir();

	final static String CASES_CSV_FILENAME = "cases_table.csv";
	final static String IMAGES_CSV_FILENAME = "images_table.csv";

	File importFile = null;
	String password = null;

	private Activity activity;

	private android.app.ProgressDialog progressWheelDialog = null;

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
					UtilClass.showMessage(this, "Error: file not found.");
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
			progressWheelDialog = new android.app.ProgressDialog(activity, R.style.ProgressDialogTheme);
			progressWheelDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
			progressWheelDialog.setMessage("Opening import file");
			progressWheelDialog.setCancelable(false);
			progressWheelDialog.show();
		}

		@Override
		protected String doInBackground(String... params)
		{
			String file_path = params[0];
			String password = params[1];

			List<Case> importCaseList = new ArrayList<Case>();

			// unzip image files and csv files
			try
			{
				// unzip files to android pictures directory
				UtilsFile.unzip(file_path, getCacheDir().getAbsolutePath());
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return "Unable to open rcs zip file.";
			}

			File tempCasesJSON = null;
			JsonReader reader = null;

			try
			{	// open existing file that should have been unzipped
				tempCasesJSON = new File(getCacheDir(), ImportExportActivity.CASES_JSON_FILENAME);

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

				FileInputStream cases_in = new FileInputStream(tempCasesJSON);
				reader = new JsonReader(new InputStreamReader(cases_in, "UTF-8"));

			}
			catch (Exception e)
			{
				e.printStackTrace();
				return "Unable to open JSON file.";
			}


			// CASES TABLE
			try
			{
				reader.beginArray();

				// loop through all cases
				while(reader.hasNext())
				{
					reader.beginObject();

					Case mCase = new Case();
					mCase.thumbnail = -1;    //default

					while(reader.hasNext())
					{
						String field_name = reader.nextName();

						if(reader.peek() == JsonToken.NULL)
						{
							reader.skipValue();
						}
						else if(field_name.contentEquals(CasesProvider.KEY_ROWID))
						{
							mCase.key_id = Long.valueOf(reader.nextString());
						}
						else if(field_name.contentEquals(CasesProvider.KEY_PATIENT_ID))
						{
							mCase.patient_id = reader.nextString();
						}
						else if(field_name.contentEquals(CasesProvider.KEY_DIAGNOSIS))
						{
							mCase.diagnosis = reader.nextString();
						}
						else if(field_name.contentEquals(CasesProvider.KEY_FINDINGS))
						{
							mCase.findings = reader.nextString();
						}
						else if(field_name.contentEquals(CasesProvider.KEY_THUMBNAIL))
						{
							mCase.thumbnail = Integer.valueOf(reader.nextString());
						}
						else if(field_name.contentEquals("IMAGES"))
						{
							List<String> image_filenames = new ArrayList<String>();
							int image_order;
							reader.beginArray();

							// loop through all images of this case
							while(reader.hasNext())
							{
								reader.beginObject();

								while(reader.hasNext())
								{
									String image_field_name = reader.nextName();

									if(reader.peek() == JsonToken.NULL || image_field_name.contentEquals(CasesProvider.KEY_ROWID))
									{
										reader.skipValue();
									}
									else if(image_field_name.contentEquals(CasesProvider.KEY_IMAGE_FILENAME))
									{
										image_filenames.add(reader.nextString());
									}
									else if(image_field_name.contentEquals(CasesProvider.KEY_ORDER))
									{
										image_order = Integer.valueOf(reader.nextString());

										if(mCase.thumbnail == image_order)
										{
											mCase.thumbnail_filename = getCacheDir().getAbsolutePath() + "/" + image_filenames.get(image_filenames.size()-1);   // use last one that was just added
										}
									}
									else
									{
										reader.skipValue();
									}
								}

								if(mCase.thumbnail == -1)   // default: use first image
								{
									mCase.thumbnail_filename = getCacheDir().getAbsolutePath() + "/" + image_filenames.get(0);
								}

								reader.endObject();
							}

							reader.endArray();

						}
						else
						{
							reader.skipValue();
						}
					}

					reader.endObject();

					importCaseList.add(mCase);
				}

				reader.endArray();

			}
			catch (IOException e)
			{
				e.printStackTrace();
				return "Unable to open case file";
			}

			// leave decrypted json file for actual import function (into database)
			//tempCasesJSON.delete();

			mCardAdapter.loadCaseList(importCaseList);

			return null; // no error message
		}

		protected void onPostExecute(String error_msg)
		{
			progressWheelDialog.dismiss();

			if(error_msg == null)
			{
				mCardAdapter.notifyDataSetChanged();
			}
			else
			{
				UtilClass.showMessage(activity, error_msg);
			}
		}
	}

	/**
	 * Complete import of selected cases into database
	 */
	private class ImportCasesTask extends AsyncTask<File, Integer, Integer>
	{
		protected void onPreExecute()
		{
			progressWheelDialog = new ProgressDialog(activity, R.style.ProgressDialogTheme);
			progressWheelDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
			progressWheelDialog.setMessage("Importing cases...");
			progressWheelDialog.setCancelable(false);
			progressWheelDialog.setCanceledOnTouchOutside(false);
			progressWheelDialog.show();
		}

		@Override
		protected Integer doInBackground(File... files)
		{
			// process file: unzip images, add csv info to database
			int count = UtilClass.importCasesJSON(activity, files[0]);

			// delete the temporary file
			files[0].delete();

			return count;
		}

		protected void onPostExecute(Integer count)
		{
			progressWheelDialog.dismiss();
			UtilClass.showToast(activity, "RadFiles imported " + count + " cases.");

			finish();
		}
	}
}