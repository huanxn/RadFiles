package com.radicalpeas.radfiles.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;


import com.mikepenz.fastadapter.utils.RecyclerViewCacheUtil;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by huanx on 8/5/2016.
 */
public class NavDrawerActivity extends AppCompatActivity
{
    private static final int PROFILE_SETTING = 100000;

    final static protected int POS_CASE_LIST_ALL = 1;
    final static protected int POS_CASE_LIST_FAV = 2;
    final static protected int POS_CASE_LIST_FOLLOWUP = 3;
    final static protected int POS_CASE_LIST_SECTION = 4;
    final static protected int POS_CASE_LIST_SUBSECTION = 400;
    final static protected int POS_CASE_LIST_DETAIL_IMAGE = 51;
    final static protected int POS_CASE_LIST_DETAIL_NOIMAGE = 52;
    final static protected int POS_CLOUD_STORAGE = 6;
    final static protected int POS_MANAGE_LISTS = 7;
    final static protected int POS_SETTINGS = 8;
    final static protected int POS_ABOUT = 9;
    final static protected int POS_NONE = -1;

    static protected int drawerPosition = POS_NONE;    // later set by inherited class to remember its position and determine onCreate parameters

    //save our header or drawerResult
    private AccountHeader headerResult = null;
    private Drawer drawerResult = null;

    protected SpannableString mTitle;
    protected Toolbar mToolbar = null;
    protected View mOverflowTarget = null;
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_drawer);

        // Handle Toolbar
        int toolbar_layout;
        boolean isTransparentToolbar;

        if(drawerPosition == POS_CASE_LIST_ALL)
        {
            toolbar_layout = R.layout.toolbar_spinner;
            isTransparentToolbar = false;
        }
        else if(drawerPosition == POS_CASE_LIST_DETAIL_IMAGE)
        {
            toolbar_layout = R.layout.toolbar_fading;
            isTransparentToolbar = true;
        }
        else if(drawerPosition == POS_CASE_LIST_DETAIL_NOIMAGE)
        {
            toolbar_layout = R.layout.toolbar_default;
            isTransparentToolbar = false;
        }
        else
        {
            toolbar_layout = R.layout.toolbar_default;
            isTransparentToolbar = false;

        }

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

            // toolbar title
            mToolbar.setTitleTextColor(UtilClass.get_attr(this, R.attr.actionMenuTextColor));
            mTitle = new SpannableString(getTitle());
            mTitle.setSpan(new TypefaceSpan(this, "Roboto-BlackItalic.ttf"), 0, "RAD".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTitle.setSpan(new TypefaceSpan(this, "RobotoCondensed-Bold.ttf"), "RAD".length(), mTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            getSupportActionBar().setTitle(mTitle);
        }

        // transparent toolbar
        if(isTransparentToolbar)
        {
            // show picture under transparent toolbar, ie no margin
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 0);
            findViewById(R.id.container).setLayoutParams(params);

        }

        // for ShowcaseView tutorial
        mOverflowTarget = findViewById(R.id.overflow_menu_target);


        // Create a few sample profile
        // NOTE you have to define the loader logic too. See the CustomApplication for more details
        final IProfile profile = new ProfileDrawerItem().withName("Mike Penz").withEmail("mikepenz@gmail.com").withIcon("https://avatars3.githubusercontent.com/u/1476232?v=3&s=460").withIdentifier(100);
        final IProfile profile2 = new ProfileDrawerItem().withName("Bernat Borras").withEmail("alorma@github.com").withIcon(FontAwesome.Icon.faw_user_md).withIdentifier(101);

        // Create the AccountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(true)
                .withHeaderBackground(R.drawable.drawer_header)
                .addProfiles(
                        profile,
                        profile2,

                        //don't ask but google uses 14dp for the add account icon in gmail but 20dp for the normal icons (like manage account)
                        new ProfileSettingDrawerItem().withName("Add Account").withDescription("Add new account").withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_add).actionBar().paddingDp(5).colorRes(R.color.material_drawer_primary_text)).withIdentifier(PROFILE_SETTING),
                        new ProfileSettingDrawerItem().withName("Manage Account").withIcon(GoogleMaterial.Icon.gmd_settings).withIdentifier(100001)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {

                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current)
                    {
                        //sample usage of the onProfileChanged listener
                        //if the clicked item has the identifier 1 add a new profile ;)
                        if (profile instanceof IDrawerItem && profile.getIdentifier() == PROFILE_SETTING)
                        {
/*
                            int count = 100 + headerResult.getProfiles().size() + 1;

                            IProfile newProfile = new ProfileDrawerItem().withNameShown(true).withName("Batman" + count).withEmail("batman" + count + "@gmail.com").withIcon(R.drawable.profile5).withIdentifier(count);

                            if (headerResult.getProfiles() != null) {

                                //we know that there are 2 setting elements. set the new profile above them ;)

                                headerResult.addProfile(newProfile, headerResult.getProfiles().size() - 2);

                            } else {

                                headerResult.addProfiles(newProfile);

                            }
*/
                        }

                        //false if you have not consumed the event and it should close the drawer
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        // get cursor of "Radiology Section List", in order determined by user list preferences
        Cursor section_cursor = getBaseContext().getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);

        List<IDrawerItem> sectionList = new ArrayList<IDrawerItem>();

        if(section_cursor.moveToFirst())
        {
            int i = 0;  // section/group counter

            do
            {
                // get the KEY_SECTION name
                String mSection = section_cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE);
                sectionList.add(new SecondaryDrawerItem().withName(mSection).withLevel(2).withIcon(FontAwesome.Icon.faw_minus).withIdentifier(POS_CASE_LIST_SUBSECTION + i));

                i = i + 1;
            } while (section_cursor.moveToNext());
        }
        section_cursor.close();

        //Create the drawer
        drawerResult = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(mToolbar)
                .withHasStableIds(true)
                .withItemAnimator(new AlphaCrossFadeAnimator())
                .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                .addDrawerItems(
                        new SectionDrawerItem().withName(R.string.navigation_drawer_item_cases_header),

                        new PrimaryDrawerItem().withName(R.string.navigation_drawer_item_cases_all).withIcon(FontAwesome.Icon.faw_book).withIdentifier(POS_CASE_LIST_ALL).withSelectable(false),

                        new ExpandableDrawerItem().withName(R.string.navigation_drawer_item_cases_section).withIcon(FontAwesome.Icon.faw_folder_open).withIdentifier(POS_CASE_LIST_SECTION).withSelectable(false).withSubItems(sectionList),
                        new PrimaryDrawerItem().withName(R.string.navigation_drawer_item_cases_fav).withIcon(GoogleMaterial.Icon.gmd_favorite).withIdentifier(POS_CASE_LIST_FAV).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.navigation_drawer_item_cases_followup).withIcon(GoogleMaterial.Icon.gmd_flag).withIdentifier(POS_CASE_LIST_FOLLOWUP).withSelectable(false),

                        new DividerDrawerItem(),

                        new PrimaryDrawerItem().withName(R.string.navigation_drawer_item_import_export).withIcon(FontAwesome.Icon.faw_cloud_upload).withIdentifier(POS_CLOUD_STORAGE).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.navigation_drawer_item_lists).withIcon(FontAwesome.Icon.faw_list).withIdentifier(POS_MANAGE_LISTS).withSelectable(false),

                        new DividerDrawerItem(),

                        new SecondaryDrawerItem().withName(R.string.navigation_drawer_item_settings).withIcon(GoogleMaterial.Icon.gmd_settings).withIdentifier(POS_SETTINGS).withSelectable(false),
                        new SecondaryDrawerItem().withName(R.string.navigation_drawer_item_about).withIcon(GoogleMaterial.Icon.gmd_info).withIdentifier(POS_ABOUT).withSelectable(false).withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.md_red_700))
                ) // add the items we want to use with our Drawer
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {

                        //check if the drawerItem is set.
                        //there are different reasons for the drawerItem to be null
                        //--> click on the header
                        //--> click on the footer
                        //those items don't contain a drawerItem

                        if (drawerItem != null)
                        {
                            Intent intent = null;

                            if(drawerItem.getIdentifier() == drawerPosition || drawerItem.getIdentifier() == POS_NONE)
                            {
                                // same position, do nothing
                                intent = null;
                            }
                            else if (drawerItem.getIdentifier() == POS_CASE_LIST_ALL)
                            {
                                intent = new Intent(NavDrawerActivity.this, CaseCardListActivity.class);
                            }
                            else if (drawerItem.getIdentifier() == POS_CASE_LIST_FAV)
                            {
                                intent = new Intent(NavDrawerActivity.this, CaseCardListActivity.class);
                                intent.putExtra(CaseCardListActivity.ARG_CASE_SUBSET, drawerItem.getIdentifier());
                            }
                            else if (drawerItem.getIdentifier() == POS_CASE_LIST_FOLLOWUP)
                            {
                                intent = new Intent(NavDrawerActivity.this, CaseCardListActivity.class);
                                intent.putExtra(CaseCardListActivity.ARG_CASE_SUBSET, drawerItem.getIdentifier());
                            }
                            else if(drawerItem.getIdentifier() >= POS_CASE_LIST_SUBSECTION)
                            {
                                intent = new Intent(NavDrawerActivity.this, CaseCardListActivity.class);
                                intent.putExtra(CaseCardListActivity.ARG_CASE_SUBSET, drawerItem.getIdentifier());
                            }
                            else if (drawerItem.getIdentifier() == POS_CLOUD_STORAGE)
                            {
                                intent = new Intent(NavDrawerActivity.this, ImportExportActivity.class);
                            }
                            else if (drawerItem.getIdentifier() == POS_MANAGE_LISTS)
                            {
                                intent = new Intent(NavDrawerActivity.this, ManageListsActivity.class);
                            }
                            else if (drawerItem.getIdentifier() == POS_SETTINGS)
                            {
                                intent = new Intent(NavDrawerActivity.this, SettingsActivity.class);
                            }
                            else if (drawerItem.getIdentifier() == POS_ABOUT)
                            {
                                // info alert

                                String buildDate = null;
                                try
                                {
                                    ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
                                    ZipFile zf = new ZipFile(ai.sourceDir);
                                    ZipEntry ze = zf.getEntry("classes.dex");
                                    long time = ze.getTime();
                                    buildDate = SimpleDateFormat.getInstance().format(new java.util.Date(time));
                                    zf.close();

                                    AlertDialog.Builder builder = new AlertDialog.Builder(NavDrawerActivity.this);
                                    builder.setTitle("RadFiles");
                                    builder.setMessage("Developed by Huan T. Nguyen\n\nBuild date: " + buildDate);
                                    AlertDialog alert = builder.create();
                                    alert.show();

                                }
                                catch(Exception e)
                                {
                                    UtilClass.showMessage(NavDrawerActivity.this, e.getMessage());
                                }

                            }


                            if (intent != null)
                            {
                                drawerResult.closeDrawer();
                               // NavDrawerActivity.this.startActivity(intent);

                               // delay opening next activity to avoid stuttering
                                final Intent navigationIntent = intent;
                                new Handler().postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        startActivity(navigationIntent);
                                    }

                                },200);
                            }
                        }

                        return false;
                    }

                })
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(true)
                .build();


        //drawerResult.addItemAtPosition(new SecondaryDrawerItem().withName("test insert 5").withLevel(2).withIcon(GoogleMaterial.Icon.gmd_8tracks).withIdentifier(2000), 5);

        //if you have many different types of DrawerItems you can magically pre-cache those items to get a better scroll performance
        //make sure to init the cache after the DrawerBuilder was created as this will first clear the cache to make sure no old elements are in
        //RecyclerViewCacheUtil.getInstance().withCacheSize(2).init(drawerResult);
        new RecyclerViewCacheUtil<IDrawerItem>().withCacheSize(2).apply(drawerResult.getRecyclerView(), drawerResult.getDrawerItems());

        //only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null)
        {
            // set the selection to the item with the identifier 11
            drawerResult.setSelection(POS_NONE, false);

            //set the active profile
            headerResult.setActiveProfile(profile); // first profile
        }
        //drawerResult.updateBadge(POS_CASE_LIST_ALL, new StringHolder(10 + ""));
    }

    public void setDrawerPosition(int position)
    {
        drawerPosition = position;
    }

    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked)
        {
            if (drawerItem instanceof Nameable)
            {
                Log.i("material-drawer", "DrawerItem: " + ((Nameable) drawerItem).getName() + " - toggleChecked: " + isChecked);
            }
            else
            {
                Log.i("material-drawer", "toggleChecked: " + isChecked);
            }
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        //add the values which need to be saved from the drawer to the bundle
        outState = drawerResult.saveInstanceState(outState);

        //add the values which need to be saved from the accountHeader to the bundle
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed()
    {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (drawerResult != null && drawerResult.isDrawerOpen())
        {
            drawerResult.closeDrawer();
        }
        else
        {
            super.onBackPressed();
        }
    }
}