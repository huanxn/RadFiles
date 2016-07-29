package com.radicalpeas.radfiles.app;

import android.app.AlertDialog;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.FrameLayout;

import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class NavigationDrawerActivity extends AppCompatActivity
		implements NavigationDrawerFragment.NavigationDrawerCallbacks
{

	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	protected NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */
	protected SpannableString mTitle;

	final static protected int POS_CASE_LIST = 0;
	//final static protected int POS_CASE_LOG = 1;
	//final static protected int POS_FAVORITES = 2;
	final static protected int POS_CLOUD_STORAGE = 1;
	final static protected int POS_MANAGE_LISTS = 2;
	final static protected int POS_SETTINGS = 3;
	final static protected int POS_INFO = 4;
	final static protected int POS_NONE = -1;

	private int drawerPosition = POS_NONE;

	protected Toolbar mToolbar = null;
	protected boolean hasTransparentStatusbar = false;
	protected View mOverflowTarget = null;
/*
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		onCreate(savedInstanceState, true);
	}

	protected void onCreate(Bundle savedInstanceState, boolean showDrawerIndicator)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigation_drawer);


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
*/
	/**
	 *
	 *  savedInstanceState
	 *  navigation_drawer_layout: default: activity_navigation_drawer
	 *  showDrawerIndicator: default: true
	 */
	protected void onCreate(Bundle savedInstanceState)
	{
		onCreate(savedInstanceState, true, R.layout.toolbar_default, false);
	}

	protected void onCreate(Bundle savedInstanceState, boolean showDrawerIndicator)
	{
		onCreate(savedInstanceState, showDrawerIndicator, R.layout.toolbar_default, false);
	}

	protected void onCreate(Bundle savedInstanceState, boolean showDrawerIndicator, boolean isTransparentToolbar)
	{
		onCreate(savedInstanceState, showDrawerIndicator, R.layout.toolbar_default, isTransparentToolbar);
	}

	protected void onCreate(Bundle savedInstanceState, int toolbar_layout)
	{
		onCreate(savedInstanceState, true, toolbar_layout, false);
	}

	protected void onCreate(Bundle savedInstanceState, int toolbar_layout, boolean isTransparentToolbar)
	{
		onCreate(savedInstanceState, true, toolbar_layout, isTransparentToolbar);
	}

	protected void onCreate(Bundle savedInstanceState, boolean showDrawerIndicator, int toolbar_layout, boolean isTransparentToolbar)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigation_drawer_fab);

		// set appropriate toolbar view
		FrameLayout toolbarContainer = (FrameLayout) findViewById(R.id.toolbar_container);
		if(toolbarContainer != null)
		{
			toolbarContainer.addView(getLayoutInflater().inflate(toolbar_layout, null, false));

		}

		// set the toolbar layout element
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		if (mToolbar != null)
		{
			setSupportActionBar(mToolbar);
			//toolbar.setElevation(4);
			//getSupportActionBar().setElevation(10);
		}


		// transparent toolbar
		if(isTransparentToolbar)
		{
			// show picture under transparent toolbar, ie no margin
			DrawerLayout.LayoutParams params = new DrawerLayout.LayoutParams(
					                                                                DrawerLayout.LayoutParams.WRAP_CONTENT,
					                                                                DrawerLayout.LayoutParams.WRAP_CONTENT
			);
			params.setMargins(0, 0, 0, 0);
			findViewById(R.id.container).setLayoutParams(params);

			// must adjust navigation drawer in NavigationDrawerFragment to account for zero margin, which would layer drawer over statusbar
			hasTransparentStatusbar = true;

			//mNavigationDrawerFragment.getDrawerListView().setPadding(0, 0, 0, UtilClass.getStatusBarHeight(this));
		}


		// toolbar title
		mToolbar.setTitleTextColor(UtilClass.get_attr(this, R.attr.actionMenuTextColor));
		mTitle = new SpannableString(getTitle());
		//	if((mTitle.subSequence(0,3)).toString().equals("RAD"))
		{
			mTitle.setSpan(new TypefaceSpan(this, "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			mTitle.setSpan(new TypefaceSpan(this, "RobotoCondensed-Bold.ttf"), "RAD".length(), mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		// for ShowcaseView tutorial
		mOverflowTarget = findViewById(R.id.overflow_menu_target);

		// Set up the drawer.
		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), showDrawerIndicator);

		// transparent toolbar
		if(isTransparentToolbar)
		{
			mNavigationDrawerFragment.mDrawerLinearLayout.setPadding(0, UtilClass.getStatusBarHeight(this), 0, 0);

		}
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
		Intent intent = null;

		// update the main content by replacing fragments
		switch (position)
		{
			// Interesting Case File
			case POS_CASE_LIST:
			{
				intent = new Intent(this, CaseCardListActivity.class);
				break;
			}
			// Cloud Storage
			case POS_CLOUD_STORAGE:
			{
				intent = new Intent(this, ImportExportActivity.class);
				break;
			}
			// Manage Lists
			case POS_MANAGE_LISTS:
			{
				intent = new Intent(this, ManageListsActivity.class);
				break;
			}
			// Settings
			case POS_SETTINGS:
			{
				intent = new Intent(this, SettingsActivity.class);
				break;
			}
			// App Info
			case POS_INFO:
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
					builder.setTitle("RadFiles");
					builder.setMessage("Developed by Huan T. Nguyen\n\nBuild date: " + buildDate);
					AlertDialog alert = builder.create();
					alert.show();

				}catch(Exception e){
					UtilClass.showMessage(this, e.getMessage());
				}

				//setDrawerPosition(POS_CASE_LIST);

				////
				break;
			}

			// No position
			case POS_NONE:
				break;

			default:
				break;
		}

		if(intent != null)
		{
			final Intent navigationIntent = intent;
			new Handler().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					startActivity(navigationIntent);
				}

			}, 250);
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
