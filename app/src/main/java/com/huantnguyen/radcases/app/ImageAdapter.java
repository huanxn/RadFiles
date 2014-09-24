package com.huantnguyen.radcases.app;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Created by Huan on 6/28/2014.
 */
public class ImageAdapter extends BaseAdapter
{
	private Context mContext;
	private String[] imageFilenames;
	private int imageSizePx;

	private long[] imageIDs;

	public ImageAdapter(Context c)
	{
		mContext = c;
		setImageSize(c);

		imageFilenames = new String[0];     // cause memory leak in set images?
		imageIDs = new long[0];
	}

	public int getCount()
	{
		return imageFilenames.length;
	}

	public Object getItem(int position)
	{
		return imageFilenames[position];
	}

	public long getItemId(int position)
	{
		return imageIDs[position];
	}

	// create a new ImageView for each item referenced by the Adapter
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ImageView imageView;

		if (convertView == null)
		{  // if it's not recycled, initialize some attributes
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(imageSizePx, imageSizePx));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(1, 1, 1, 1);
		}
		else
		{
			imageView = (ImageView) convertView;
		}

		UtilClass.setPic(imageView, imageFilenames[position], UtilClass.IMAGE_SIZE);

		return imageView;
	}

	// set initial images with cursor
	public void setImages(Cursor cursor)
	{
		if (cursor.moveToFirst())
		{
			imageFilenames = new String[cursor.getCount()];
			imageIDs = new long[cursor.getCount()];

			int i = 0;
			do
			{
				imageFilenames[i] = cursor.getString(CasesProvider.COL_IMAGE_FILENAME);
				imageIDs[i] = cursor.getInt(CasesProvider.COL_ROWID);

				i = i + 1;

			} while (cursor.moveToNext());
		}
		else
		{
//			initialize();
		}
	}

	/*
	// initial settings with no images yet
	public void initialize()
	{
		imageFilenames = new String[0];
		imageIDs = new long[0];
	}

*/
	// add temporary images to grid into string array
	public void addImage(String newImageFilename)
	{
		imageFilenames = UtilClass.addArrayElement(imageFilenames, newImageFilename);
		imageIDs = UtilClass.addArrayElement(imageIDs, -1);  // temporary image, with no rowID in the database table

		notifyDataSetChanged();
	}

	// add temporary images to grid into string array
	public void deleteImage(int delete_position)
	{
		imageFilenames = UtilClass.deleteArrayElement(imageFilenames, delete_position);
		imageIDs = UtilClass.deleteArrayElement(imageIDs, delete_position);

		notifyDataSetChanged();
	}

	private void setImageSize(Context context)
	{
		//get margin/padding from layout
		int padding = Math.round(UtilClass.convertDpToPixels(context, 20)); // approx 40 on Galaxy Nexus
		int numCols = 2;
		float dp_width = UtilClass.getDisplayWidth(context);
		float px_width = UtilClass.convertDpToPx(context, dp_width);
		imageSizePx = Math.round((px_width-padding)/numCols);
	}

	public int getImageSizePx()
	{
		return imageSizePx;
	}

	// get the image filename at the specified index within the string array
	public String getImageFilename(int index)
	{
		if(index < getCount())
		{
			return imageFilenames[index];
		}
		else
		{
			return null;
		}
	}

	// get the image filename at the specified index within the string array
	public String [] getImageFilenames()
	{
		return imageFilenames;
	}

}
