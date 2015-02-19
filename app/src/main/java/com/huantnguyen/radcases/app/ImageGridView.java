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
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;

import com.nispok.snackbar.Snackbar;

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

	// Constructor for ImageGridView without any initial images
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

	public void setThumbnailPosition(int thumb)
	{
		thumbnail = thumb;
	}

	public void setMode(int m)
	{
		mode = m;
	}

	public void getLocationInWindow(int[] location)
	{
		gridView.getLocationInWindow(location);
	}

	public View getView()
	{
		//return gridView;
		return mAdapter.getFirstImageView();
	}

	private void SetClickListeners()
	{
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

				Intent imageGalleryIntent = new Intent(activity, ImageGalleryActivity.class);
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_IMAGE_FILES, mAdapter.getImageFilepaths());
				imageGalleryIntent.putExtra(ImageGalleryActivity.ARG_IMAGE_CAPTIONS, mAdapter.getImageCaptions());
				imageGalleryIntent.putExtra(CaseCardListActivity.ARG_KEY_ID, case_key_id);
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
				final String image_caption = mAdapter.getImageCaption(position);
				final long image_id = mAdapter.getImageID(position);
				final Context context = parent.getContext();
				final Activity act = activity;

				AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());

				CharSequence[] choices = {"Edit image caption", "Set as thumbnail", "Delete image"};
				builder.setTitle(image_caption)
						.setItems(choices, new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int index)
							{
								switch (index)
								{
									// Set caption
									case 0:
										// open EditText box to get user input for caption
										AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);

										// Set an EditText view to get user input
										final EditText input = new EditText(activity);
										input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
										input.setHighlightColor(UtilClass.get_attr(activity, R.attr.colorControlHighlight));

										alertBuilder.setTitle("Image Caption");

										// show current text in the edit box
										input.setText(image_caption);

										alertBuilder.setView(input);

										alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
										{
											public void onClick(DialogInterface dialog, int whichButton)
											{
												// Save changes to the image caption
												ContentValues captionValue = new ContentValues();
												captionValue.put(CasesProvider.KEY_IMAGE_CAPTION, input.getText().toString());

												// Update database
												if (mode == DETAIL_ACTIVITY)
												{
													Uri caption_row_uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, image_id);
													context.getContentResolver().update(caption_row_uri, captionValue, null, null);

													// update last modified date field
													UtilClass.updateLastModifiedDate(act, case_id);
												}

												// update mAdapter
												mAdapter.setImageCaption(image_position, input.getText().toString());
												notifyDataSetChanged();
											}
										});

										alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
										{
											public void onClick(DialogInterface dialog, int whichButton)
											{
												// Canceled.
											}
										});

										AlertDialog editTextDialog = alertBuilder.create();
										// Show keyboard
										editTextDialog.setOnShowListener(new DialogInterface.OnShowListener()
										{
											@Override
											public void onShow(DialogInterface dialog)
											{
												InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
												imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
											}
										});
										editTextDialog.show();


										act.setResult(CaseCardListActivity.RESULT_EDITED);

										break;

									// Set thumbnail
									case 1:
										// if thumbnail changed
										if (thumbnail != image_position)
										{

											thumbnail = image_position;

											if (mode == DETAIL_ACTIVITY)
											{
												ContentValues thumbnailValue = new ContentValues();
												thumbnailValue.put(CasesProvider.KEY_THUMBNAIL, thumbnail);
												Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, case_id);
												context.getContentResolver().update(row_uri, thumbnailValue, null, null);

												((CaseDetailActivity) act).reloadHeaderView(thumbnail);

												// update last modified date field
												UtilClass.updateLastModifiedDate(act, case_id);
											}

											act.setResult(CaseCardListActivity.RESULT_EDITED);
										}

										break;

									// Delete the photo.
									case 2:

										if (mode == EDIT_ACTIVITY)
										{
											// get image row ID of deleted image, for actual delete if user presses "SAVE"
											deletedImageList.add(mAdapter.getImageFilepath(image_position));

											// delete from gridview adapter
											mAdapter.deleteImage(image_position);

											notifyDataSetChanged();
											Resize();

											// adjust thumbnail index or reset to first image
											if (thumbnail > image_position)
											{
												thumbnail = thumbnail - 1;
											}
											else if (thumbnail == image_position)
											{
												thumbnail = 0;
											}
										}
										else if (mode == DETAIL_ACTIVITY)
										{

											// confirm delete image
											AlertDialog.Builder alert = new AlertDialog.Builder(context);

											alert.setTitle("Delete image");
											alert.setMessage("Are you sure?");

											alert.setPositiveButton("Delete", new DialogInterface.OnClickListener()
											{
												public void onClick(DialogInterface dialog, int whichButton)
												{
													// delete actual image file
													File deleteFile = new File(mAdapter.getImageFilepath(image_position));
													if (deleteFile.exists())
													{
														deleteFile.delete();
													}

													// delete from database
													if(mAdapter.getImageID(image_position)!=-1)
													{
														Uri row_uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, mAdapter.getImageID(image_position));
														context.getContentResolver().delete(row_uri, null, null);
													}

													// delete from gridview adapter
													mAdapter.deleteImage(image_position);

													notifyDataSetChanged();
													Resize();

													// adjust thumbnail index or reset to first image
													if (thumbnail > image_position)
													{
														thumbnail = thumbnail - 1;
													}
													else if (thumbnail == image_position)
													{
														thumbnail = 0;
													}

													if (mode == DETAIL_ACTIVITY)
													{
														((CaseDetailActivity) act).reloadHeaderView(thumbnail);
													}

													// update last modified date field
													UtilClass.updateLastModifiedDate(act, case_id);

													UtilClass.showMessage(activity, "Image deleted.");

													act.setResult(CaseCardListActivity.RESULT_EDITED);
												}
											});

											alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
											{
												public void onClick(DialogInterface dialog, int whichButton)
												{
													// Canceled.
												}
											});

											alert.show();


										}

										break;


									// Do Nothing.
									default:
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

		int itemHeight = mAdapter.getImageSizePx();
		//int itemHeight = UtilClass.IMAGE_GRID_SIZE;

		int numRows = itemCount/numCols;
		if(itemCount%numCols > 0)
			numRows += 1;

		ViewGroup.LayoutParams layoutParams = gridView.getLayoutParams();


		//layoutParams.height = convertDpToPixels(context, IMAGE_GRID_SIZE*numRows); //this is in pixels
		layoutParams.height = (itemHeight)*numRows; //this is in pixels


		gridView.setLayoutParams(layoutParams);
	}

	public void notifyDataSetChanged()
	{
		mAdapter.notifyDataSetChanged();
	}

	public void addImage(String newImage)
	{
		addImage(newImage, -1);
	}

	public void addImage(String newImage, long newID)
	{
		mAdapter.addImage(newImage, newID);
		notifyDataSetChanged();
		Resize();

		UtilClass.showMessage(activity, "New image saved.");
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

	public String getImageCaption(int index)
	{
		return mAdapter.getImageCaption(index);
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

	public String [] getImageFilepaths()
	{
		return mAdapter.getImageFilepaths();
	}

	public String [] getImageCaptions()
	{
		return mAdapter.getImageCaptions();
	}

}
