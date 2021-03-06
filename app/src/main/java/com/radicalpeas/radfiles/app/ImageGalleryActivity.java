package com.radicalpeas.radfiles.app;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.Touch;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.jrejaud.viewpagerindicator2.PageIndicator;
import com.github.jrejaud.viewpagerindicator2.UnderlinePageIndicator;

public class ImageGalleryActivity extends AppCompatActivity
{

	/**
	 * Step 1: Download and set up v4 support library: http://developer.android.com/tools/support-library/setup.html
	 * Step 2: Create ExtendedViewPager wrapper which calls TouchImageView.canScrollHorizontallyFroyo
	 * Step 3: ExtendedViewPager is a custom view and must be referred to by its full package name in XML
	 * Step 4: Write TouchImageAdapter, located below
	 * Step 5. The ViewPager in the XML should be ExtendedViewPager
	 */

	private static long case_id;
	private static String TAG = "ImageGallery Activity";

	public static final String ARG_POSITION = "com.radicalpeas.radcases.INITIAL_POSITION";
	public final static String ARG_IMAGE_FILES = "com.radicalpeas.radcases.ARG_IMAGE_FILES";
	public final static String ARG_IMAGE_CAPTIONS = "com.radicalpeas.radcases.ARG_IMAGE_CAPTIONS";

	ViewPager mPager;
	PageIndicator mIndicator;

	// ACTION MENU
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.keyimage_gallery, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.

		// Handle presses on the action bar items
		switch (item.getItemId())
		{
			case android.R.id.home:
				//NavUtils.navigateUpFromSameTask(this);
				finish();
				return true;

			case R.id.menu_edit_caption:
				Toast.makeText(this, "debug: Add caption function...", Toast.LENGTH_SHORT).show();
				return true;

			case R.id.menu_camera:
				Toast.makeText(this, "debug: Camera function...", Toast.LENGTH_SHORT).show();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// black status bar
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			Window window = getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
		}

		case_id = getIntent().getLongExtra(CaseCardListActivity.ARG_KEY_ID, -1);
		int init_pos = getIntent().getIntExtra(ARG_POSITION, 0);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_keyimage_gallery);

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
		setSupportActionBar(toolbar);
		//toolbar.setElevation(4);
		//getSupportActionBar().setElevation(10);

		toolbar.setBackgroundColor(getResources().getColor(R.color.transparent));   // transparent
		//toolbar.setTitleTextColor(getResources().getColor(R.color.transparent));    // transparent

		// set back icon
		getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle("");

		ExtendedViewPager mViewPager = (ExtendedViewPager) findViewById(R.id.viewpager);
		TouchImageAdapter mAdapter = new TouchImageAdapter(this);

		//if(case_id == -1)   //using imageGrid instead of using db
		{
		//	Log.e(TAG, "Did not get usable case id for key image gallery.");

			String [] filepaths = getIntent().getStringArrayExtra(ARG_IMAGE_FILES);
			String [] captions = getIntent().getStringArrayExtra(ARG_IMAGE_CAPTIONS);
			mAdapter.setImages(filepaths, captions);
		}
		/*
		else
		{
			// get all of the images linked to this case _id
			String[] image_args = {String.valueOf(case_id)};
			Cursor cursor = getApplication().getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

			//Set the adapter to get images from cursor
			mAdapter.setImages(cursor);
		}
		*/


		mViewPager.setAdapter(mAdapter);

		mIndicator = (UnderlinePageIndicator)findViewById(R.id.indicator);
		mIndicator.setViewPager(mViewPager);

		mViewPager.setCurrentItem(init_pos);

		/*
		// lollipop transitions
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			getWindow().setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.shared_element));

			TransitionSet transitionSet = new TransitionSet();

			Transition fade = new Fade();
			fade.excludeTarget(toolbar, true);
			fade.excludeTarget(android.R.id.statusBarBackground, true);
			fade.excludeTarget(android.R.id.navigationBarBackground, true);
			transitionSet.addTransition(fade);

			getWindow().setEnterTransition(transitionSet);
			getWindow().setExitTransition(transitionSet);
		}
		*/
    }

    static class TouchImageAdapter extends PagerAdapter
	{

      //  private static int[] images = { R.drawable.nature_1, R.drawable.nature_2, R.drawable.nature_3, R.drawable.nature_4, R.drawable.nature_5 };
	    private static String[] imageFilepaths;
	    private String[] imageCaptions;
		private Context mContext;

		public TouchImageAdapter(Context context)
		{
			mContext = context;
		}

        @Override
        public int getCount() {
        	return imageFilepaths.length;
        }

        @Override
        public View instantiateItem(ViewGroup container, int position)
        {

			LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View view = inflater.inflate(R.layout.key_image_full, container, false);

	        TouchImageView imageView = (TouchImageView)view.findViewById(R.id.key_image);

			// resize to full screen
			imageView.requestLayout();
			imageView.getLayoutParams().height = (int) UtilClass.getDisplayHeightPx(mContext);
			imageView.getLayoutParams().width = (int) UtilClass.getDisplayWidthPx(mContext);

			Glide.with(mContext).load(imageFilepaths[position]).into(imageView);

	        if(imageCaptions != null)
	        {
		        TextView caption = (TextView)view.findViewById(R.id.imageCaption);
		        caption.setText(imageCaptions[position]);
	        }

	        container.addView(view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

	        return view;

        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }


	    public void setImages(Cursor image_cursor)
	    {
		    imageFilepaths = new String[image_cursor.getCount()];
		    imageCaptions = new String[image_cursor.getCount()];

		    if(image_cursor.moveToFirst())
		    {
			    int image_counter = 0;
			    do
			    {
				    imageFilepaths[image_counter] = CaseCardListActivity.picturesDir + "/" + image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);
				    imageCaptions[image_counter] = image_cursor.getString(CasesProvider.COL_IMAGE_CAPTION);

				    image_counter += 1;
			    } while (image_cursor.moveToNext());
		    }

		    return;
	    }

	    // set the ImageGalleryActivity images by String array)
	    public void setImages(String [] in_filenames)
	    {
		    setImages(in_filenames, null);
	    }

	    // set the ImageGalleryActivity images by String array)
	    public void setImages(String [] in_filenames, String [] in_captions)
	    {
		    if(in_filenames == null)
		    {
			    return;
		    }

		    imageFilepaths = new String[in_filenames.length];

		    System.arraycopy(in_filenames, 0, imageFilepaths, 0, in_filenames.length);

		    if(in_captions != null && in_captions.length == in_filenames.length)
		    {
			    imageCaptions = new String[in_captions.length];

			    System.arraycopy(in_captions, 0, imageCaptions, 0, in_captions.length);
		    }

		    return;
	    }

    }

}
