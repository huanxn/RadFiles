package com.huantnguyen.radcases.app;

import android.app.Activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;


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
	protected CharSequence mTitle;

	final static protected int POS_CASE_LIST = 0;
	final static protected int POS_CLOUD_STORAGE = 3;

	private int drawerPosition = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
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
		mTitle = getTitle();
		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mNavigationDrawerFragment.setDrawerPosition(drawerPosition);
	}

	protected void onCreate_for_FAB(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigation_drawer_fab);

/*
		// set the toolbar layout element as the FadingActionBar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null)
		{
			setSupportActionBar(toolbar);
		}
*/

		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();
		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
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
		if (id == R.id.menu_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
