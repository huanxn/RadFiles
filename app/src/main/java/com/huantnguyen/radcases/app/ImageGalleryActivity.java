package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.UnderlinePageIndicator;


public class ImageGalleryActivity extends Activity {
	
	/**
	 * Step 1: Download and set up v4 support library: http://developer.android.com/tools/support-library/setup.html
	 * Step 2: Create ExtendedViewPager wrapper which calls TouchImageView.canScrollHorizontallyFroyo
	 * Step 3: ExtendedViewPager is a custom view and must be referred to by its full package name in XML
	 * Step 4: Write TouchImageAdapter, located below
	 * Step 5. The ViewPager in the XML should be ExtendedViewPager
	 */

	private static long case_id;
	private static String TAG = "KeyImageGallery Activity";

	public static final String ARG_POSITION = "com.huantnguyen.radcases.INITIAL_POSITION";
	public final static String ARG_IMAGE_FILES = "com.huantnguyen.radcases.ARG_IMAGE_FILES";

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

			case R.id.menu_camera:
				Toast.makeText(this, "debug: Camera function...", Toast.LENGTH_SHORT).show();
				return true;

			case R.id.menu_settings:
				//openSettings();
				Toast.makeText(this, "debug: Settings function...", Toast.LENGTH_SHORT).show();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		case_id = getIntent().getLongExtra(CaseCardListActivity.ARG_KEY_ID, -1);
		int init_pos = getIntent().getIntExtra(ARG_POSITION, 0);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_keyimage_gallery);
		ExtendedViewPager mViewPager = (ExtendedViewPager) findViewById(R.id.view_pager);
		TouchImageAdapter mAdapter = new TouchImageAdapter();

		if(case_id == -1)   //error getting case id in intent extra
		{
			Log.e(TAG, "Did not get usable case id for key image gallery.");

			String [] imageFilenames = getIntent().getStringArrayExtra(ARG_IMAGE_FILES);
			mAdapter.setImages(imageFilenames);
		}
		else
		{
			// get all of the images linked to this case _id
			String[] image_args = {String.valueOf(case_id)};
			Cursor cursor = getApplication().getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

			//Set the adapter to get images from cursor
			mAdapter.setImages(cursor);
		}

		mViewPager.setAdapter(mAdapter);

		mIndicator = (UnderlinePageIndicator)findViewById(R.id.indicator);
		mIndicator.setViewPager(mViewPager);

		mViewPager.setCurrentItem(init_pos);

    }

    static class TouchImageAdapter extends PagerAdapter {

      //  private static int[] images = { R.drawable.nature_1, R.drawable.nature_2, R.drawable.nature_3, R.drawable.nature_4, R.drawable.nature_5 };
	    private static String[] imageFilename;

        @Override
        public int getCount() {
        	return imageFilename.length;
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            TouchImageView img = new TouchImageView(container.getContext());
            //img.setImageResource(images[position]);
	        UtilClass.setPic(img, imageFilename[position], 500);
            container.addView(img, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            return img;
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
		    imageFilename = new String[image_cursor.getCount()];
		    if(image_cursor.moveToFirst())
		    {

			    int image_counter = 0;
			    do
			    {
				    imageFilename[image_counter] = image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);

				    image_counter += 1;
			    } while (image_cursor.moveToNext());

		    }
		    return;
	    }

	    // set the ImageGalleryActivity images by String array)
	    public void setImages(String [] in_filenames)
	    {
		    imageFilename = new String[in_filenames.length];
		    int image_counter = 0;
		    for(int i = 0; i < in_filenames.length; i++)
		    {
			    imageFilename[i] = in_filenames[i];
		    }
		    return;

	    }

    }
}
