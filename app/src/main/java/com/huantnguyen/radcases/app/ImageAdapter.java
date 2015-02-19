package com.huantnguyen.radcases.app;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;

/**
 * Created by Huan on 6/28/2014.
 */
public class ImageAdapter extends BaseAdapter
{
	private Context mContext;
	private String[] imageFilepaths;
	private String[] imageCaptions;
	private int imageSizePx;

	private long[] imageIDs;

	private ImageView firstImageView = null;

	public ImageAdapter(Context c)
	{
		mContext = c;
		setImageSize(c);

		imageFilepaths = new String[0];     // cause memory leak in set images?
		imageCaptions = new String[0];
		imageIDs = new long[0];
	}

	public int getCount()
	{
		return imageFilepaths.length;
	}

	public Object getItem(int position)
	{
		return imageFilepaths[position];
	}

	public long getItemId(int position)
	{
		return imageIDs[position];
	}


	public class Holder
	{
		TextView captionTextView;
		ImageView imageView;
	}

	// create a new ImageView for each item referenced by the Adapter
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Holder holder = new Holder();
		View view;

		if (convertView == null)
		{
			// if it's not recycled, initialize some attributes
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.key_image_caption, parent, false);
			holder.captionTextView = (TextView) view.findViewById(R.id.imageCaption);

			holder.imageView = (ImageView) view.findViewById(R.id.image);

			//holder.imageView.setLayoutParams(new GridView.LayoutParams(imageSizePx, imageSizePx));

			view.setLayoutParams(new GridView.LayoutParams(imageSizePx,imageSizePx));

			/*
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(imageSizePx, imageSizePx));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(1, 1, 1, 1);
			*/

		}
		else
		{
			//imageView = (ImageView) convertView;

			view = convertView;
			holder.captionTextView = (TextView) view.findViewById(R.id.imageCaption);
			holder.imageView = (ImageView) view.findViewById(R.id.image);
		}

		if(firstImageView == null)
		{
			firstImageView = holder.imageView;
		}

		// hide if no caption
		if(getImageCaption(position) != null && !getImageCaption(position).contentEquals(""))
		{
			holder.captionTextView.setVisibility(View.VISIBLE);
		}
		else
		{
			holder.captionTextView.setVisibility(View.GONE);
		}

		UtilClass.setPic(holder.imageView, imageFilepaths[position], UtilClass.IMAGE_SIZE);
		holder.captionTextView.setText(getImageCaption(position));

		return view;
	}

	// set initial images with cursor
	public void setImages(Cursor cursor)
	{
		if (cursor.moveToFirst())
		{
			imageFilepaths = new String[cursor.getCount()];
			imageCaptions = new String[cursor.getCount()];
			imageIDs = new long[cursor.getCount()];

			int i = 0;
			do
			{
				imageFilepaths[i] = CaseCardListActivity.picturesDir + "/" + cursor.getString(CasesProvider.COL_IMAGE_FILENAME);
				imageCaptions[i] = cursor.getString(CasesProvider.COL_IMAGE_CAPTION);
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
		imageFilepaths = new String[0];
		imageIDs = new long[0];
	}

*/
	// add temporary images to grid into string array
	public void addImage(String newImageFilename)
	{
		addImage(newImageFilename, -1);
	}

	// add permanent image to grid into string array
	public void addImage(String newImageFilename, long imageID)
	{
		imageFilepaths = UtilClass.addArrayElement(imageFilepaths, newImageFilename);
		imageCaptions = UtilClass.addArrayElement(imageCaptions, null);
		imageIDs = UtilClass.addArrayElement(imageIDs, imageID);

		notifyDataSetChanged();
	}

	// delete image from grid
	public void deleteImage(int delete_position)
	{
		imageFilepaths = UtilClass.deleteArrayElement(imageFilepaths, delete_position);
		imageCaptions = UtilClass.deleteArrayElement(imageCaptions, delete_position);
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
	public String getImageFilepath(int index)
	{
		if(index < getCount())
		{
			return imageFilepaths[index];
		}
		else
		{
			return null;
		}
	}

	// get the image filepath at the specified index within the string array
	public String getImageFilename(int index)
	{
		if(index < getCount())
		{
			return new File(imageFilepaths[index]).getName();
		}
		else
		{
			return null;
		}
	}

	// get the image caption at the specified index within the string array
	public String getImageCaption(int index)
	{
		if(index < getCount())
		{
			return imageCaptions[index];
		}
		else
		{
			return null;
		}
	}


	// get the image filename array
	public String [] getImageFilepaths()
	{
		return imageFilepaths;
	}

	// get the image caption array
	public String [] getImageCaptions()
	{
		return imageCaptions;
	}

	// get the image ID
	public long getImageID(int index)
	{
		if(index < getCount())
		{
			return imageIDs[index];
		}
		else
		{
			return -1;
		}
	}

	public void setImageCaption(int index, String s)
	{
		if(index < getCount())
		{
			imageCaptions[index] = s;
		}

		return;
	}

	public ImageView getFirstImageView()
	{
		return firstImageView;
	}
}
