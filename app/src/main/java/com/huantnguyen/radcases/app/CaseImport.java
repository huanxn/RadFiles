package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CaseImport extends ActionBarActivity
{

	RecyclerView mRecyclerView;
	CaseCardAdapter mCardAdapter;

	// standard directories
	private static File downloadsDir = CaseCardListActivity.downloadsDir;
	private static File picturesDir = CaseCardListActivity.picturesDir;
	private static File appDir  = CaseCardListActivity.appDir;             // internal app data directory
	private static File dataDir  = CaseCardListActivity.dataDir;            // private data directory (with SQL database)
	private static File CSV_dir  = CaseCardListActivity.CSV_dir;            // contains created zip files with CSV files and images

	private static File backupDir;

	final static String CASES_CSV_FILENAME = "cases_table.csv";
	final static String IMAGES_CSV_FILENAME = "images_table.csv";

	File importFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_import);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


	public List<Case> getImportedCases(File inFile)
	{
		if(inFile == null)
		{
			return null;
		}

		BufferedReader br = null;
		String line;

		List<Case> importCaseList = new ArrayList<Case>();


		// unzip image files and csv files
		try
		{
			// unzip files to android pictures directory
			UtilsFile.unzip(inFile.getPath(),picturesDir.getPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to open zip file:", Toast.LENGTH_SHORT).show();
			return null;
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
			return null;
		}

			/*
			//////////////////parent ids will change in new database!!!!
			// IMAGES TABLE
			try
			{
				br = new BufferedReader(new FileReader(tempImagesCSV));


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
*/
		// CASES TABLE
		try
		{
			br = new BufferedReader(new FileReader(tempCasesCSV));

			br.readLine(); // header

			while ( (line=br.readLine()) != null)
			{
				line = line.substring(1, line.length()-1);  // trim the double-quotes off
				String[] values = line.split("\",\"");
				//insertCaseValues.clear();

				Case mCase = new Case();

				long old_case_id = Long.valueOf(values[0]);

				mCase.patient_id = values[1];
				mCase.diagnosis = values[2];
				mCase.findings = values[4];

				importCaseList.add(mCase);

					/*
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
					*/




/*
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
*/
			}
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Unable to open Cases CSV file", Toast.LENGTH_SHORT).show();
			return null;
		}

		Toast.makeText(this, "Imported cases", Toast.LENGTH_SHORT).show();

		tempCasesCSV.delete();
		tempImagesCSV.delete();

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
					UtilClass.importCasesCSV(this, importFile);
					importFile.delete();
				}

				finish();
				break;
		}
	}
}
