package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Huan on 9/23/2014.
 */
public class ImageGridView
{
	private Activity activity;
	private GridView gridView;
	private long case_key_id;
	private ImageAdapter mAdapter;

	private ArrayList<String> deletedImageList;

	public static final int EDIT_ACTIVITY = 0;
	public static final int DETAIL_ACTIVITY = 1;

	private int mode = EDIT_ACTIVITY;

	private int thumbnail = 0;

	// Constructor for ImageGridView without any intial images
	public ImageGridView(Activity activity, GridView gView)
	{
		this.activity = activity;
		gridView = gView;
		case_key_id = -1;

		deletedImageList = new ArrayList<String>();

		mAdapter = new ImageAdapter(activity);
		gridView.setAdapter(mAdapter);

		SetClickListeners();

	}
	// Constructor for ImageGridView, set up with images
	public ImageGridView(Activity activity, GridView gView, long _id, Cursor image_cursor)
	{
		this.activity = activity;
		gridView = gView;
		case_key_id = _id;

		deletedImageList = new ArrayList<String>();

		mAdapter = new ImageAdapter(activity);
		gridView.setAdapter(mAdapter);

		mAdapter.setImages(image_cursor);
		Resize();

		SetClickListeners();
	}

	public void setMode(int m)
	{
		mode = m;
	}

	private void SetClickListeners()
	{
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

				Intent imageGalleryIntent = new Intent(activity, ImageGalleryActivity.class);
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_IMAGE_FILES, mAdapter.getImageFilepaths());
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_POSITION, position);
				activity.startActivity(imageGalleryIntent);

			}
		});

		gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
			{
				// Delete image
				//final long image_key_id = id;
				final long case_id = case_key_id;
				final int image_position = position;
				final Context context = parent.getContext();
				final Activity act = activity;

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

								if (mode == EDIT_ACTIVITY)
								{
									// get image row ID of deleted image, for actual delete if user presses "SAVE"
									deletedImageList.add(mAdapter.getImageFilepath(image_position));

									// delete from gridview adapter
									mAdapter.deleteImage(image_position);

									notifyDataSetChanged();
									Resize();

									// adjust thumbnail index or reset to first image
									if(thumbnail > image_position)
									{
										thumbnail = thumbnail - 1;
									}
									else if(thumbnail == image_position)
									{
										thumbnail = 0;
									}
								}
								else if (mode == DETAIL_ACTIVITY)
								{

									// confirm delete image
									AlertDialog.Builder alert = new AlertDialog.Builder(context);

									alert.setTitle("Delete this image?");
									alert.setMessage("Are you sure?");

									alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton)
										{
											// delete actual image file
											File deleteFile = new File(mAdapter.getImageFilepath(image_position));
											if(deleteFile.exists())
											{
												deleteFile.delete();
											}

											// delete from database
											Uri row_uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, mAdapter.getImageID(image_position));
											context.getContentResolver().delete(row_uri, null, null);

											// delete from gridview adapter
											mAdapter.deleteImage(image_position);

											notifyDataSetChanged();
											Resize();

											// adjust thumbnail index or reset to first image
											if(thumbnail > image_position)
											{
												thumbnail = thumbnail - 1;
											}
											else if(thumbnail == image_position)
											{
												thumbnail = 0;
											}

											if (mode == DETAIL_ACTIVITY)
											{
												((CaseDetailActivity) act).reloadHeaderView(thumbnail);
											}
											act.setResult(CaseCardListActivity.RESULT_EDITED);
										}
									});

									alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											// Canceled.
										}
									});

									alert.show();


								}

								break;

							// Set thumbnail
							case 1:
								// if thumbnail changed
								if(thumbnail != image_position)
								{
									thumbnail = image_position;

									ContentValues values = new ContentValues();
									values.put(CasesProvider.KEY_THUMBNAIL, thumbnail);
									Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, case_id);
									context.getContentResolver().update(row_uri, values, null, null);

									if (mode == DETAIL_ACTIVITY)
									{
										((CaseDetailActivity) act).reloadHeaderView(thumbnail);
									}

									act.setResult(CaseCardListActivity.RESULT_EDITED);
								}

								break;

							// Cancel.  Do Nothing.
							case 2:
								break;

						}
					}
				});

				AlertDialog alert = builder.create();
				alert.show();

				return true;
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

	public int getThumbnail()
	{
		return thumbnail;
	}

}
