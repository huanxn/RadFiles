package com.huantnguyen.radcases.app;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

/**
 * Created by Huan on 9/23/2014.
 */
public class ImageGridView
{
	private Context context;
	private GridView gridView;
	private long case_key_id;
	private ImageAdapter mAdapter;

	// Constructor for ImageGridView without any intial images
	// TODO how to handle image click gallery without case image cursor
	public ImageGridView(Context ctx, GridView gView)
	{
		context = ctx;
		gridView = gView;
		case_key_id = -1;

		mAdapter = new ImageAdapter(context);

	}
	// Constructor for ImageGridView, set up with images
	public ImageGridView(Context ctx, GridView gView, long _id, Cursor image_cursor)
	{
		context = ctx;
		gridView = gView;
		case_key_id = _id;

		mAdapter = new ImageAdapter(context);

		mAdapter.setImages(image_cursor);
		gridView.setAdapter(mAdapter);
		Resize();

		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

				Intent imageGalleryIntent = new Intent(context, ImageGalleryActivity.class);
				imageGalleryIntent.putExtra(CaseCardListActivity.ARG_KEY_ID, case_key_id);  //TODO pass imageadapter or string of image file names instead of a cursor
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

								// if image_key_id = -1, then it is a temporary image, not in database
								if(image_key_id != -1)
								{
									// delete from IMAGES table by unique row id
									Uri image_row_uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, image_key_id);
									context.getContentResolver().delete(image_row_uri, null, null);

								}

								// delete from gridview adapter
								mAdapter.deleteImage(image_position);

								// TODO if delete last image, change HAS_IMAGE to false
								if(mAdapter.getCount()==0)
								{
									// Update the CASES table, with image count decremented by 1
									//ContentValues case_values = new ContentValues();
									//case_values.put(CasesProvider.KEY_IMAGE_COUNT, numImages--);


									//Uri case_row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, case_key_id);
									//context.getContentResolver().update(case_row_uri, case_values, null, null);
								}


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

		int itemHeight = mAdapter.getImageSizePx();

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
		Resize();
	}

}
