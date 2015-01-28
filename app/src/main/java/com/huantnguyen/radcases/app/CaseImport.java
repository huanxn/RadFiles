package com.huantnguyen.radcases.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.JsonToken;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CaseImport extends ActionBarActivity
{

	RecyclerView mRecyclerView;
	CaseCardAdapter mCardAdapter;

	// standard directories
	private static File downloadsDir = UtilClass.getDownloadsDir();

	final static String CASES_CSV_FILENAME = "cases_table.csv";
	final static String IMAGES_CSV_FILENAME = "images_table.csv";

	File importFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_import);

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

	    // Setup CaseCardAdapter
	    mCardAdapter = new CaseCardAdapter(this, null, R.layout.card_case);

	    mRecyclerView.setAdapter(mCardAdapter);

			/*
			//sticky headers
			StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mCardAdapter);

			mRecyclerView.addItemDecoration(headersDecor);


			mRecyclerView.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					UtilClass.hideKeyboard(getActivity());
					return false;
				}
			});

			*/

	    // Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

	    if (Intent.ACTION_VIEW.equals(action) && type != null)
	    {
		    if (type.equals("application/zip") || type.equals("*/*") || type.equals("application/octet-stream") )
		    {
			    Uri import_uri = intent.getData();

			    try
			    {
				    importFile = UtilsFile.makeLocalFile(this, downloadsDir, import_uri);
				    mCardAdapter.loadCaseList(getImportedCases(importFile));
				    //importFile.delete();
			    }
			    catch (IOException e)
			    {
				    e.printStackTrace();
			    }
		    }
	    }
	    else
	    {
		    // Show error message

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


	public List<Case> getImportedCases(File inFile)
	{
		if(inFile == null)
		{
			return null;
		}

		List<Case> importCaseList = new ArrayList<Case>();

		// unzip image files and csv files
		try
		{
			// unzip files to android pictures directory
			UtilsFile.unzip(inFile.getPath(), getCacheDir().getAbsolutePath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to open zip file:", Toast.LENGTH_SHORT).show();
			return null;
		}

		File tempCasesJSON = null;
		JsonReader reader = null;

		try
		{	// open existing file that should have been unzipped
			tempCasesJSON = new File(getCacheDir(), CloudStorageActivity.CASES_JSON_FILENAME);
			FileInputStream cases_in = new FileInputStream(tempCasesJSON);
			reader = new JsonReader(new InputStreamReader(cases_in, "UTF-8"));

		}
		catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to copy CSV file", Toast.LENGTH_SHORT).show();
			return null;
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

					if(reader.peek() == JsonToken.NULL || field_name.contentEquals(CasesProvider.KEY_ROWID))
					{
						reader.skipValue();
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
			Toast.makeText(this, "Unable to open Cases JSON file", Toast.LENGTH_SHORT).show();
			return null;
		}

		tempCasesJSON.delete();

		return importCaseList;
	}

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
					UtilClass.importCasesJSON(this, importFile);
					importFile.delete();
				}

				finish();
				break;
		}
	}
}
