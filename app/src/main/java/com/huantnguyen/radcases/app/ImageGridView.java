package com.huantnguyen.radcases.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;

/**
 * Created by Huan on 9/23/2014.
 */
public class ImageGridView
{
	private Context context;
	private GridView gridView;
	private long case_key_id;
	private ImageAdapter mAdapter;

	private ArrayList<String> deletedImageList;

	// Constructor for ImageGridView without any intial images
	public ImageGridView(Context ctx, GridView gView)
	{
		context = ctx;
		gridView = gView;
		case_key_id = -1;

		deletedImageList = new ArrayList<String>();

		mAdapter = new ImageAdapter(context);
		gridView.setAdapter(mAdapter);

		SetClickListeners();

	}
	// Constructor for ImageGridView, set up with images
	public ImageGridView(Context ctx, GridView gView, long _id, Cursor image_cursor)
	{
		context = ctx;
		gridView = gView;
		case_key_id = _id;

		deletedImageList = new ArrayList<String>();

		mAdapter = new ImageAdapter(context);
		gridView.setAdapter(mAdapter);

		mAdapter.setImages(image_cursor);
		Resize();

		SetClickListeners();
	}

	private void SetClickListeners()
	{
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

				Intent imageGalleryIntent = new Intent(context, ImageGalleryActivity.class);
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_IMAGE_FILES, mAdapter.getImageFilepaths());
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_POSITION, position);
				context.startActivity(imageGalleryIntent);

			}
		});

		gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
			{
				// Delete image
				final long image_key_id = id;
				final int image_position = position;

				AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
				CharSequence[] imageSources = {"Delete Image", "Set Thumbnail", "Cancel"};
				builder.setItems(imageSources, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int index)
					{
						switch (index)
						{
							// Delete the photo.
							case 0:

								/*
								// if image_key_id = -1, then it is a temporary image, not in database
								if(image_key_id != -1)
								{
									// delete from IMAGES table by unique row id
									Uri image_row_uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, image_key_id);
									context.getContentResolver().delete(image_row_uri, null, null);

								}*/

								// get image row ID of deleted image, for actual delete if user presses "SAVE"
								deletedImageList.add(mAdapter.getImageFilepath(image_position));

								// delete from gridview adapter
								mAdapter.deleteImage(image_position);

								notifyDataSetChanged();

								Resize();

								// TODO Delete the actual file


								break;

							// Set thumbnail
							case 1:
								//TODO set thumbnail
								break;

							// Cancel.  Do Nothing.
							case 2:
								break;

						}
					}
				});

				AlertDialog alert = builder.create();
				alert.show();

				return false;
			}
		});
	}

	public void Resize()
	{
		//UtilClass.expandGridView(gridView, mAdapter.getImageSizePx());
		//int numCols = gridView.getNumColumns(); //doesn't work
		int numCols = 2;
		int itemCount = mAdapter.getCount();

		//int itemHeight = mAdapter.getImageSizePx();
		int itemHeight = UtilClass.IMAGE_GRID_SIZE;

		int numRows = itemCount/numCols;
		if(itemCount%numCols > 0)
			numRows += 1;

		ViewGroup.LayoutParams layoutParams = gridView.getLayoutParams();


		//layoutParams.height = convertDpToPixels(context, IMAGE_GRID_SIZE*numRows); //this is in pixels
		layoutParams.height = itemHeight*numRows; //this is in pixels


		gridView.setLayoutParams(layoutParams);
	}

	public void notifyDataSetChanged()
	{
		mAdapter.notifyDataSetChanged();
	}

	public void addImage(String newImage)
	{
		mAdapter.addImage(newImage);
		notifyDataSetChanged();
		Resize();
	}

	public int getCount()
	{
		return mAdapter.getCount();
	}

	public String getImageFilepath(int index)
	{
		return mAdapter.getImageFilepath(index);
	}

	public String getImageFilename(int index)
	{
		return mAdapter.getImageFilename(index);
	}

	public long getImageID(int index)
	{
		return mAdapter.getImageID(index);
	}

	public ArrayList<String> getDeletedImageList()
	{
		return deletedImageList;
	}

}
