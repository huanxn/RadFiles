package com.huantnguyen.radcases.app;

import android.app.AlertDialog;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class NavigationDrawerActivity extends ActionBarActivity
		implements NavigationDrawerFragment.NavigationDrawerCallbacks
{

	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */
	protected SpannableString mTitle;

	final static protected int POS_CASE_LIST = 0;
	final static protected int POS_CLOUD_STORAGE = 3;
	final static protected int POS_MANAGE_LISTS = 4;
	final static protected int POS_SETTINGS = 5;
	final static protected int POS_HELP = 6;

	private int drawerPosition = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		onCreate(savedInstanceState, true);
	}

	protected void onCreate(Bundle savedInstanceState, boolean showDrawerIndicator)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigation_drawer);

		/*

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
		}
		*/

		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = new SpannableString(getTitle());

		//	if((mTitle.subSequence(0,3)).toString().equals("RAD"))
		{
			mTitle.setSpan(new TypefaceSpan(this, "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			mTitle.setSpan(new TypefaceSpan(this, "RobotoCondensed-Bold.ttf"), "RAD".length(), mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), showDrawerIndicator);
	}


	protected void onCreate_for_FAB(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigation_drawer_fab);


		// set the toolbar layout element as the FadingActionBar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null)
		{
			setSupportActionBar(toolbar);
			//toolbar.setElevation(4);
			//getSupportActionBar().setElevation(10);
		}

		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = new SpannableString(getTitle());
		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), false);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mNavigationDrawerFragment.setDrawerPosition(drawerPosition);
	}

	public void setDrawerPosition(int position)
	{
		drawerPosition = position;
		mNavigationDrawerFragment.setDrawerPosition(position);
	}

	@Override
	public void onNavigationDrawerItemSelected(int position)
	{
		// update the main content by replacing fragments
		switch (position)
		{
			// Interesting Case File
			case POS_CASE_LIST:
			{
				Intent intent = new Intent(this, CaseCardListActivity.class);
				startActivity(intent);
				break;
			}
			// Cloud Storage
			case POS_CLOUD_STORAGE:
			{
				Intent intent = new Intent(this, CloudStorageActivity.class);
				startActivity(intent);
				break;
			}
			// Manage Lists
			case POS_MANAGE_LISTS:
			{
				Intent intent = new Intent(this, ManageListsActivity.class);
				startActivity(intent);
				break;
			}
			// Manage Lists
			case POS_SETTINGS:
			{
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
			}
			// Manage Lists
			case POS_HELP:
			{
				// info alert

				String buildDate = null;
				try{
					ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
					ZipFile zf = new ZipFile(ai.sourceDir);
					ZipEntry ze = zf.getEntry("classes.dex");
					long time = ze.getTime();
					buildDate = SimpleDateFormat.getInstance().format(new java.util.Date(time));
					zf.close();

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage(buildDate).setTitle("Build Date");
					AlertDialog alert = builder.create();
					alert.show();

				}catch(Exception e){
					UtilClass.showMessage(getApplicationContext(), e.getMessage());
				}

				setDrawerPosition(POS_CASE_LIST);

				////
				break;
			}
			default:
				break;
		}
	}

	public void restoreActionBar()
	{
		android.support.v7.app.ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(actionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (!mNavigationDrawerFragment.isDrawerOpen())
		{
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			//getMenuInflater().inflate(R_menu_ID, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
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

	public void setDrawerListener(DrawerLayout.DrawerListener listener)
	{
		mNavigationDrawerFragment.getDrawerLayout().setDrawerListener(listener);
	}

	public DrawerLayout getDrawerLayout()
	{
		return mNavigationDrawerFragment.getDrawerLayout();
	}

}
