package com.huantnguyen.radcases.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.drive.Drive;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import eu.janmuller.android.simplecropimage.CropImage;

/**
 * Created by Huan on 6/10/2014.
 */
public class UtilClass extends Activity
{

	public static final int IMAGE_SIZE = 500;
	public static final int IMAGE_THUMB_SIZE = 150;
	public static final int IMAGE_GRID_SIZE = 330;

	public static final String TAG = "UtilClass";

	/**
	 * Shows a toast message.
	 */
	public static void showMessage(Context context, String message)
	{
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	/**
	 * Sets ImageView picture from file
	 * @param mImageView: Layout ImageView that will display the picture
	 * @param mCurrentPhotoPath: full file path of image to be displayed
	 * @param size: scale down size of displayed picture (square)
	 */
	static public void setPic(ImageView mImageView, String mCurrentPhotoPath, int size)
	{
		// Get the dimensions of the View
		int targetW = mImageView.getWidth();
		int targetH = mImageView.getHeight();

		if (targetW == 0 || targetH == 0)
		{
			targetW = targetH = size;
		}

		// Get the dimensions of the bitmap
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		// Determine how much to scale down the image
		int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

		// Decode the image file into a Bitmap sized to fill the View
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		mImageView.setImageBitmap(bitmap);
	}

	static public Bitmap getBitmapFromFile(String mCurrentPhotoPath, int size)
	{
		// Scale down to given size
		int targetW = size;
		int targetH = size;

		// Get the dimensions of the bitmap
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		// Determine how much to scale down the image
		int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

		// Decode the image file into a Bitmap sized to fill the View
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		return BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
	}

	static private int BITMAP_DRAWABLE_SIZE = 100;
/*
	static public void setPic(BitmapDrawable bitmapDrawable, String mCurrentPhotoPath)
	{
		int size = BITMAP_DRAWABLE_SIZE;

		int targetW = size;
		int targetH = size;


		// Get the dimensions of the bitmap
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		// Determine how much to scale down the image
		int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

		// Decode the image file into a Bitmap sized to fill the View
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		return new BitmapDrawable(getResources(), bitmap);
	}*/


	/**
	 * Zoom image from thumbnail image view
	 */

	static Animator mCurrentAnimator;

	static public void zoomImageFromThumb(View view, Animator mAnimator, final int mShortAnimationDuration, final View container, final View thumbView, final View expandedImageView)
	{
		mCurrentAnimator = mAnimator;

		// If there's an animation in progress, cancel it
		// immediately and proceed with this one.
		if (mCurrentAnimator != null)
		{
			mCurrentAnimator.cancel();
		}

		// Load the high-resolution "zoomed-in" image.
		//final ImageView expandedImageView = (ImageView) view.findViewById(R.id.expanded_image);
		//expandedImageView.setImageResource(imageResId);

		// Calculate the starting and ending bounds for the zoomed-in image.
		// This step involves lots of math. Yay, math.
		final Rect startBounds = new Rect();
		final Rect finalBounds = new Rect();
		final Point globalOffset = new Point();

		// The start bounds are the global visible rectangle of the thumbnail,
		// and the final bounds are the global visible rectangle of the container
		// view. Also set the container view's offset as the origin for the
		// bounds, since that's the origin for the positioning animation
		// properties (X, Y).
		thumbView.getGlobalVisibleRect(startBounds);
		//view.findViewById(R.id.container).getGlobalVisibleRect(finalBounds, globalOffset);
		container.getGlobalVisibleRect(finalBounds, globalOffset);
		startBounds.offset(-globalOffset.x, -globalOffset.y);
		finalBounds.offset(-globalOffset.x, -globalOffset.y);

		// Adjust the start bounds to be the same aspect ratio as the final
		// bounds using the "center crop" technique. This prevents undesirable
		// stretching during the animation. Also calculate the start scaling
		// factor (the end scaling factor is always 1.0).
		float startScale;
		if ((float) finalBounds.width() / finalBounds.height()
				    > (float) startBounds.width() / startBounds.height())
		{
			// Extend start bounds horizontally
			startScale = (float) startBounds.height() / finalBounds.height();
			float startWidth = startScale * finalBounds.width();
			float deltaWidth = (startWidth - startBounds.width()) / 2;
			startBounds.left -= deltaWidth;
			startBounds.right += deltaWidth;
		}
		else
		{
			// Extend start bounds vertically
			startScale = (float) startBounds.width() / finalBounds.width();
			float startHeight = startScale * finalBounds.height();
			float deltaHeight = (startHeight - startBounds.height()) / 2;
			startBounds.top -= deltaHeight;
			startBounds.bottom += deltaHeight;
		}

		// Hide the thumbnail and show the zoomed-in view. When the animation
		// begins, it will position the zoomed-in view in the place of the
		// thumbnail.
		thumbView.setAlpha(0f);
		expandedImageView.setVisibility(View.VISIBLE);

		// Set the pivot point for SCALE_X and SCALE_Y transformations
		// to the top-left corner of the zoomed-in view (the default
		// is the center of the view).
		expandedImageView.setPivotX(0f);
		expandedImageView.setPivotY(0f);

		// Construct and run the parallel animation of the four translation and
		// scale properties (X, Y, SCALE_X, and SCALE_Y).
		AnimatorSet set = new AnimatorSet();
		set
				.play(ObjectAnimator.ofFloat(expandedImageView, View.X,
						                            startBounds.left, finalBounds.left))
				.with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
						                            startBounds.top, finalBounds.top))
				.with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
						                            startScale, 1f)).with(ObjectAnimator.ofFloat(expandedImageView,
								                                                                        View.SCALE_Y, startScale, 1f));
		set.setDuration(mShortAnimationDuration);
		set.setInterpolator(new DecelerateInterpolator());
		set.addListener(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator animation)
			{
				mCurrentAnimator = null;
			}

			@Override
			public void onAnimationCancel(Animator animation)
			{
				mCurrentAnimator = null;
			}
		});
		set.start();
		mCurrentAnimator = set;

		// Upon clicking the zoomed-in image, it should zoom back down
		// to the original bounds and show the thumbnail instead of
		// the expanded image.
		final float startScaleFinal = startScale;
		expandedImageView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (mCurrentAnimator != null)
				{
					mCurrentAnimator.cancel();
				}

				// Animate the four positioning/sizing properties in parallel,
				// back to their original values.
				AnimatorSet set = new AnimatorSet();
				set.play(ObjectAnimator
						         .ofFloat(expandedImageView, View.X, startBounds.left))
						.with(ObjectAnimator
								      .ofFloat(expandedImageView,
										              View.Y, startBounds.top))
						.with(ObjectAnimator
								      .ofFloat(expandedImageView,
										              View.SCALE_X, startScaleFinal))
						.with(ObjectAnimator
								      .ofFloat(expandedImageView,
										              View.SCALE_Y, startScaleFinal));
				set.setDuration(mShortAnimationDuration);
				set.setInterpolator(new DecelerateInterpolator());
				set.addListener(new AnimatorListenerAdapter()
				{
					@Override
					public void onAnimationEnd(Animator animation)
					{
						thumbView.setAlpha(1f);
						expandedImageView.setVisibility(View.GONE);
						mCurrentAnimator = null;
					}

					@Override
					public void onAnimationCancel(Animator animation)
					{
						thumbView.setAlpha(1f);
						expandedImageView.setVisibility(View.GONE);
						mCurrentAnimator = null;
					}
				});
				set.start();
				mCurrentAnimator = set;
			}
		});
	}

	/* end zoom from thumb */

	/**
	 * getPictureFromCamera()
	 *
	 * @param activity: calling activity, will return to this activity's <onActivityResult()>
	 * @param REQUEST_IMAGE_CAPTURE: the onActivityResult() code
	 * @return
	 */
	static public File getPictureFromCamera(Activity activity, int REQUEST_IMAGE_CAPTURE)
	{
		File imageFile = null;

		// opens phone's camera
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null)
		{
			// Create the Filename where the photo should go
			String tempFilename = null;

			// Create an image file name based on timestamp
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			tempFilename = "JPEG_" + timeStamp + "_";

			// Get the private application storage directory for pictures
			File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

			// Create the file
			try
			{
				imageFile = File.createTempFile(tempFilename, ".jpg", storageDir);
			}
			catch (IOException ex)
			{
				Log.e("UtilClass.getPictureFromCamera", "Could not open new image file.");
			}

			// Continue only if the File was successfully created
			if (imageFile != null)
			{
				// get uri of newly created File and pass to takePictureIntent
				Uri mImageCaptureUri = Uri.fromFile(imageFile);
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);

				// open phone's camera and pass result to the calling activity's onActivityResult()
				// which will then run UtilClass.CropPicture() if successful
				activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
			}
		}

		// returns the File of the new jpg image
		return imageFile;
	}

	// called from onActivityResult
	static public void CropPicture(Activity activity, File imageFile, int REQUEST_CROP_IMAGE)
	{
		// new Intent from CropImage class, included as imported library project: simple-crop-image-lib
		Intent cropPictureIntent = new Intent(activity, CropImage.class);

		// tell CropImage activity to look for image to crop
		String filePath = imageFile.getPath();
		cropPictureIntent.putExtra(CropImage.IMAGE_PATH, filePath);

		// allow CropImage activity to rescale image
		cropPictureIntent.putExtra(CropImage.SCALE, true);

		// aspect ratio is fixed to ratio 1:1
		cropPictureIntent.putExtra(CropImage.ASPECT_X, 1);
		cropPictureIntent.putExtra(CropImage.ASPECT_Y, 1);

		// start activity CropImage with request code and listen for result
		activity.startActivityForResult(cropPictureIntent, REQUEST_CROP_IMAGE);
	}

	// get file path from data resulted from IO file manager or gallery.  [Uri selectedImageUri = data.getData();]
	static public String getFilePathFromResult(Activity activity, Uri selectedImageUri)
	{
		String selectedImagePath;
		String filemanagerstring;

		//OI FILE Manager
		filemanagerstring = selectedImageUri.getPath();

		//MEDIA GALLERY
		selectedImagePath = getPath(activity, selectedImageUri);

		//DEBUG PURPOSE - you can delete this if you want
		if (selectedImagePath != null)
			System.out.println(selectedImagePath);
		else
			System.out.println("selectedImagePath is null");
		if (filemanagerstring != null)
			System.out.println(filemanagerstring);
		else
			System.out.println("filemanagerstring is null");

		//NOW WE HAVE OUR WANTED STRING
		if (selectedImagePath != null)
			return selectedImagePath;
		else
			return filemanagerstring;

	}

/*
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{

		else if(requestCode == REQUEST_SELECT_IMAGE_FROM_FILE && resultCode == activity_requesting_image.RESULT_OK)
		{
			Uri selectedImageUri = data.getData();

			String selectedImagePath;
			String filemanagerstring;

			//OI FILE Manager
			filemanagerstring = selectedImageUri.getPath();

			//MEDIA GALLERY
			selectedImagePath = getPath(selectedImageUri);

			//DEBUG PURPOSE - you can delete this if you want
			if(selectedImagePath!=null)
				System.out.println(selectedImagePath);
			else System.out.println("selectedImagePath is null");
			if(filemanagerstring!=null)
				System.out.println(filemanagerstring);
			else System.out.println("filemanagerstring is null");

			//NOW WE HAVE OUR WANTED STRING
			if(selectedImagePath!=null)
				imageFilename = selectedImagePath;
			else
				imageFilename = filemanagerstring;
		}
	}

*/

	// get path for image selector
	static private String getPath(Activity activity, Uri uri)
	{
		String[] projection = {MediaStore.Images.Media.DATA};
		Cursor cursor = activity.managedQuery(uri, projection, null, null, null);
		if (cursor != null)
		{
			//HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
			//THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
			int column_index = cursor
					                   .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		}
		else
			return null;
	}
/*
	public static String getPath(Context context, Uri uri) throws URISyntaxException {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection, null, null, null);
				int column_index = cursor
						                   .getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
				// Eat it
			}
		}

		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}
*/

	static public String[] addArrayElement(String[] original, String added)
	{
		String[] result = Arrays.copyOf(original, original.length + 1);
		result[original.length] = added;
		return result;
	}

	static public String[] deleteArrayElement(String[] original, int delete_position)
	{
		String[] segment1 = Arrays.copyOfRange(original, 0, delete_position);
		String[] segment2 = Arrays.copyOfRange(original, delete_position+1, original.length);

		//String[] result = Arrays.copyOf(segment1, original.length-1);
		List<String> combined = new ArrayList<String>(Arrays.asList(segment1));
		combined.addAll(Arrays.asList(segment2));

		String[] result = combined.toArray(new String[original.length-1]);

		return result;
	}

	static public int[] addArrayElement(int[] original, int added)
	{
		int[] result = Arrays.copyOf(original, original.length + 1);
		result[original.length] = added;
		return result;
	}

	static public long[] addArrayElement(long[] original, long added)
	{
		long[] result = Arrays.copyOf(original, original.length + 1);
		result[original.length] = added;
		return result;
	}

	static public long[] deleteArrayElement(long[] original, int delete_position)
	{
		long[] segment1 = Arrays.copyOfRange(original, 0, delete_position);
		long[] segment2 = Arrays.copyOfRange(original, delete_position+1, original.length);

		long[] result = new long[original.length-1];
		System.arraycopy(original, 0, result, 0, delete_position);
		System.arraycopy(original, delete_position+1, result, delete_position, original.length-delete_position-1);

		return result;
	}


	public static int convertDpToPixels(Context context, float dp)
	{
		Resources resources = context.getResources();
		return (int) TypedValue.applyDimension(
				                                      TypedValue.COMPLEX_UNIT_DIP,
				                                      dp,
				                                      resources.getDisplayMetrics()
		);

	}

	public static float convertDpToPx(Context context, float dp)
	{
		//Context context = getApplication().getBaseContext();
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		float px = dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
		return px;
	}

	public static float convertPxToDp(Context context, float px)
	{
		//Context context = getApplication().getBaseContext();
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		float dp = px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
		return dp;
	}

	public static float getDisplayWidth(Context context)
	{
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		return displayMetrics.widthPixels / displayMetrics.density;
	}
	public static float getDisplayHeight(Context context)
	{
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		return displayMetrics.heightPixels / displayMetrics.density;
	}

	public static String convertDateString(String original_date_str, SimpleDateFormat original_sdf, SimpleDateFormat display_sdf)
	{
		if(original_date_str == null)
			return "Unspecified";

		Calendar selected_date = Calendar.getInstance();

		try
		{
			selected_date.setTime(original_sdf.parse(original_date_str));
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return display_sdf.format(selected_date.getTime());
	}

	public static String convertDateString(String original_date_str, String original_sdf_str, String display_sdf_str)
	{
		SimpleDateFormat original_sdf = new SimpleDateFormat(original_sdf_str);
		SimpleDateFormat display_sdf = new SimpleDateFormat(display_sdf_str);

		return convertDateString(original_date_str, original_sdf, display_sdf);
	}

	/**
	 * Action Bar menu item: Delete the case from the action bar menu
	 * @param activity: calling activity for context
	 * @param case_id: unique row id of the case to be deleted
	 */
	public static void menuItem_deleteCase(Activity activity, long case_id)
	{
		final long key_id = case_id;
		final Activity context = activity;

		// Alert dialog to confirm delete
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setMessage("Delete this case")
				.setPositiveButton(context.getResources().getString(R.string.button_OK), new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						// delete confirmed

						// delete case from CASES table
						Uri case_delete_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
						context.getContentResolver().delete(case_delete_uri, null, null);

						// delete all linked images files
						Cursor image_cursor = context.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)}, CasesProvider.KEY_ORDER);
						File imageFile = null;
						if (image_cursor.moveToFirst())
						{
							do
							{
								imageFile = new File(image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME));
								imageFile.delete();
							} while (image_cursor.moveToNext());
						}
						image_cursor.close();

						// delete all child rows from IMAGES table, by parent case key_id
						context.getContentResolver().delete(CasesProvider.IMAGES_URI, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)});

						// update CaseCardListActivity
						context.setResult(CaseCardListActivity.RESULT_DELETED);
						context.finish();
					}
				})
				.setNegativeButton(context.getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						// cancel
					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 *
	 * @param activity
	 * @param case_id
	 */
	public static void menuItem_shareCase(Activity activity, long case_id)
	{
		final long key_id = case_id;
		final Activity context = activity;

	}

	public void buildEditTextAlert(Context context, String title, String message)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(context);


		if(title != null)
			alert.setTitle(title);
		if(message != null)
			alert.setMessage(message);

		// Set an EditText view to get user input
		final EditText input = new EditText(context);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String result = input.getText().toString();

			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();
	}


	/**
	 * Creates zip file of images and CSV of database rows of select cases
	 * used by CloudStorageActivity, CardCaseListActivity
	 * @param activity: context of calling activity
	 * @param filename: filename of zip to be created (do not include path or extension)
	 * @param selectedCaseList: list of unique case id of cases to be included
	 * @return
	 */

	public static File exportCasesCSV(Activity activity, String filename, List<Long> selectedCaseList)
	{
		File returnFile;

		// attempt to create CSV file
		File casesCSV = null;
		File imagesCSV = null;

		// CSV subdirectory within internal app data directory
		File CSV_dir = new File(activity.getApplication().getExternalFilesDir(null), "/CSV/");

		/*
		// create CSV dir if doesn't already exist
		if(!CSV_dir.exists())
		{
			if(CSV_dir.mkdirs())
			{
				showMessage(activity, "Created directory: " + CSV_dir.getPath());
			}
			else
			{
				showMessage(activity, "Unable to create directory: " + CSV_dir.getPath());
			}
		}
*/
		try
		{
			casesCSV = new File(CSV_dir.getPath(), CloudStorageActivity.CASES_CSV_FILENAME);
			imagesCSV = new File(CSV_dir.getPath(), CloudStorageActivity.IMAGES_CSV_FILENAME);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			showMessage(activity, "Unable to create local CSV backup files.");
			return null;
		}

		String csvHeader = "";
		String case_csvValues = "";
		String image_csvValues = "";

		// to zip all images into a file for backup
		String zip_files_array[] = new String[0];

		String value = "";

		for (int i = 0; i < CasesProvider.ALL_KEYS.length; i++) {
			if (csvHeader.length() > 0) {
				csvHeader += ",";
			}
			csvHeader += "\"" + CasesProvider.ALL_KEYS[i] + "\"";
		}

		//csvHeader += ",\"Image Files\"\n";
		csvHeader += "\n";

		Log.d(TAG, "header=" + csvHeader);

		// create local CSV files
		try
		{
			FileWriter casesWriter = new FileWriter(casesCSV);
			BufferedWriter casesOut = new BufferedWriter(casesWriter);

			FileWriter imagesWriter = new FileWriter(imagesCSV);
			BufferedWriter imagesOut = new BufferedWriter(imagesWriter);

			// Cases table
			Cursor caseCursor;
			if(selectedCaseList == null)
			{
				// get all cases
				caseCursor = activity.getContentResolver().query(CasesProvider.CASES_URI, null, null, null, null, null);
			}
			else
			{
				// get select cases
				// convert list of integers into array
				int j=0;
				String[] selectionArgs;
				selectionArgs = new String[selectedCaseList.size()];
				for(Long case_id : selectedCaseList)
				{
					selectionArgs[j++] = String.valueOf(case_id);
				}

				caseCursor = activity.getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_ROWID + " = ?", selectionArgs, null);
			}

			if (caseCursor.moveToFirst())
			{
				casesOut.write(csvHeader);
				//	outputStream.write(csvHeader.getBytes());

				// loop through all cases
				do
				{
					case_csvValues = "";
					// output all case columns for this case
					for (int i = 0; i < CasesProvider.ALL_KEYS.length; i++)
					{
						if (i > 0)
						{
							case_csvValues += ",";
						}

						if (i == 6 || i == 11)
							case_csvValues += "\"" + String.valueOf(caseCursor.getInt(i)) + "\"";
						else
						{

							value = caseCursor.getString(i);

							if(value == null)
								value = "";

							case_csvValues += "\"" + value + "\"";
						}
					}

					// Image Table CSV
					String [] image_args = {String.valueOf(caseCursor.getInt(CasesProvider.COL_ROWID))};
					Cursor imageCursor = activity.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);


					// loop through all images of this case
					if(imageCursor.moveToFirst())
					{
						do
						{
							image_csvValues = imageCursor.getString(0) + "," + imageCursor.getString(1) + "," + imageCursor.getString(2) + "," + imageCursor.getString(3) + "\n";
							imagesOut.write(image_csvValues);

							zip_files_array = UtilClass.addArrayElement(zip_files_array, imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME));

						} while (imageCursor.moveToNext());
					}

					imageCursor.close();

					casesOut.write(case_csvValues + "\n");
					//			outputStream.write(case_csvValues.getBytes());
				} while (caseCursor.moveToNext());
				caseCursor.close();

			}
			casesOut.close();
			imagesOut.close();

			// zip image and csv files
			String zip_filename = CSV_dir.getPath() + "/" + filename + ".zip";
			zip_files_array = UtilClass.addArrayElement(zip_files_array, casesCSV.getPath());
			zip_files_array = UtilClass.addArrayElement(zip_files_array, imagesCSV.getPath());

			// create zip file.  return link to that file.
			returnFile = UtilsFile.zip(zip_files_array, zip_filename);

			// delete temporary files
			casesCSV.delete();
			imagesCSV.delete();

		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.d(TAG, "IOException: " + e.getMessage());
			return null;
		}

		return returnFile;
	}

}
