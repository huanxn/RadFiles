package com.huantnguyen.radcases.app;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.UnderlinePageIndicator;


public class ImageGalleryActivity extends ActionBarActivity
{
	
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
		if (toolbar != null)
		{
			setSupportActionBar(toolbar);
			//toolbar.setElevation(4);
			//getSupportActionBar().setElevation(10);

			toolbar.setBackgroundColor(getResources().getColor(R.color.transparent));   // transparent
			//toolbar.setTitleTextColor(getResources().getColor(R.color.transparent));    // transparent

			// set back icon
			getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setTitle("");
		}

		ExtendedViewPager mViewPager = (ExtendedViewPager) findViewById(R.id.viewpager);
		TouchImageAdapter mAdapter = new TouchImageAdapter();

		//if(case_id == -1)   //using imageGrid instead of using db
		{
		//	Log.e(TAG, "Did not get usable case id for key image gallery.");

			String [] filepaths = getIntent().getStringArrayExtra(ARG_IMAGE_FILES);
			mAdapter.setImages(filepaths);
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
    }

    static class TouchImageAdapter extends PagerAdapter {

      //  private static int[] images = { R.drawable.nature_1, R.drawable.nature_2, R.drawable.nature_3, R.drawable.nature_4, R.drawable.nature_5 };
	    private static String[] imageFilepaths;
	    private String[] imageCaptions;

        @Override
        public int getCount() {
        	return imageFilepaths.length;
        }

        @Override
        public View instantiateItem(ViewGroup container, int position)
        {
/*
            TouchImageView img = new TouchImageView(container.getContext());
            //img.setImageResource(images[position]);
	        UtilClass.setPic(img, imageFilepaths[position], 500);
            container.addView(img, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

	        if(imageCaptions != null)
	        {
		        TextView caption = new TextView(container.getContext());
		        caption.setText(imageCaptions[position]);
		        container.addView(caption, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	        }

            return img;

*/
			LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View view = inflater.inflate(R.layout.key_image_full, container, false);

	        TouchImageView imageView = (TouchImageView)view.findViewById(R.id.key_image);
	        UtilClass.setPic(imageView, imageFilepaths[position], 500);

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
		    if(in_filenames == null)
		    {
			    return;
		    }

		    imageFilepaths = new String[in_filenames.length];
		    int image_counter = 0;
		    for(int i = 0; i < in_filenames.length; i++)
		    {
			    imageFilepaths[i] = in_filenames[i];
		    }
		    return;
	    }
    }

}
