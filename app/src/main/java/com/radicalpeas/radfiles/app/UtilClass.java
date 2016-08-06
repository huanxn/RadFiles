package com.radicalpeas.radfiles.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
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
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.view.ViewCompat;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Huan on 6/10/2014.
 */
public class UtilClass extends Activity
{

	public static final int IMAGE_SIZE = 500;
	public static final int IMAGE_THUMB_SIZE = 125;
	public static final int IMAGE_GRID_SIZE = 330;

	public static final String TAG = "UtilClass";

	// standard directories
	//private static File downloadsDir = CaseCardListActivity.downloadsDir;
	//private static File picturesDir = CaseCardListActivity.picturesDir;
	private static File appDir  = CaseCardListActivity.appDir;             // internal app data directory
	private static File dataDir  = CaseCardListActivity.dataDir;            // private data directory (with SQL database)

	/**
	 * Shows a toast message.
	 */
	public static void showToast(Activity activity, String message)
	{
		Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
	}
	public static void showToast(Activity activity, int message)
	{
		Toast.makeText(activity, String.valueOf(message), Toast.LENGTH_LONG).show();
	}
	public static void showMessage(Activity activity, String message)
	{
		SnackbarManager.show(Snackbar.with(activity)
				       .text(message));



		//Toast.makeText(context, message, Toast.LENGTH_LONG).show();
		/*
		SnackBar snackbar = new SnackBar(activity, message, null, null);
		snackbar.show();
		*/

	}


	/**
	 * Sets ImageView picture from file
	 * @param mImageView: Layout ImageView that will display the picture
	 * @param mCurrentPhotoPath: full file path of image to be displayed
	 * @param size: scale down size of displayed picture (square)
	 * @return boolean: false if no image is set
	 */

	static public boolean setPic(ImageView mImageView, String mCurrentPhotoPath, int size)
	{
		if(mCurrentPhotoPath== null || mCurrentPhotoPath.isEmpty())
		{
			mImageView.setImageBitmap(null);
			return false;
		}

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

		return true;
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

	public static int getDisplayWidthPx(Activity activity)
	{
		Point size = new Point();
		activity.getWindowManager().getDefaultDisplay().getSize(size);

		return size.x;
	}

	public static int getDisplayHeightPx(Activity activity)
	{
		Point size = new Point();
		activity.getWindowManager().getDefaultDisplay().getSize(size);

		return size.y;
	}

	public static int getStatusBarHeight(Activity activity)
	{
		int result = 0;
		int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = activity.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static int getToolbarHeight(Activity activity)
	{
		//return (int)activity.getResources().getDimension(R.dimen.toolbar_size);
		TypedValue tv = new TypedValue();
		if (activity.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true))
		{
			return TypedValue.complexToDimensionPixelSize(tv.data, activity.getResources().getDisplayMetrics());
		}
		else
		{
			return 0;
		}

	}

	public static int getNavigationBarHeight(Activity activity) {
		Resources resources = activity.getResources();
		int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			return resources.getDimensionPixelSize(resourceId);
		}
		return 0;
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

		builder.setMessage("Delete this case?")
				.setPositiveButton(context.getResources().getString(R.string.button_OK), new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						// delete confirmed

						if (key_id != -1)
						{
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
						}
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
	 * Action Bar menu item: Delete the case from the action bar menu
	 * @param activity: calling activity for context
	 * @param case_id_list: list array of unique row id of the cases to be deleted
	 */
	public static void menuItem_deleteCases(Activity activity, List<Long> case_id_list)
	{
		final List<Long> caseList = new ArrayList<Long>(case_id_list);
		final Activity context = activity;

		// Alert dialog to confirm delete
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setMessage("Delete " + caseList.size() + " cases?")
				.setPositiveButton(context.getResources().getString(R.string.button_OK), new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						// delete confirmed

						for (int i = 0; i < caseList.size(); i++)
						{
							// get key_id from list array
							long key_id = caseList.get(i);

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
						}

						// update CaseCardListActivity
						//context.setResult(CaseCardListActivity.RESULT_DELETED);
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

	public static void updateLastModifiedDate(Activity activity, long key_id)
	{
		// put data into "values" for database insert/update
		ContentValues values = new ContentValues();

		// format string for database
		SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd-HHmm-ss");
		String today_date_str = db_sdf.format(Calendar.getInstance().getTime());
		values.put(CasesProvider.KEY_LAST_MODIFIED_DATE, today_date_str);

		// Update the existing case in the database
		Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
		activity.getContentResolver().update(row_uri, values, null, null);
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

	/*
	public void buildConfirmAlert(Context context, String title, String message)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(context);

		if(title != null)
			alert.setTitle(title);
		if(message != null)
			alert.setMessage(message);

		// Set an EditText view to get user input


		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {


			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();
	}
*/

	public static void hideKeyboard(Activity activity)
	{
		InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

		// check if no view has focus:
		View view = activity.getCurrentFocus();
		if (view != null) {
			inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}


	/**
	 * Creates zip file of images and JSON of database rows of select cases
	 * used by CloudStorageActivity, CardCaseListActivity
	 * @param activity: context of calling activity
	 * @param filename: filename of zip to be created (do not include path or extension)
	 * @param selectedCaseList: list of unique case id of cases to be included
	 * @return
	 */

	public static File exportCasesJSON(Activity activity, String filename, List<Long> selectedCaseList, String password)
	{
		return exportCasesJSON(activity, filename, selectedCaseList, password, null);
	}

	public static File exportCasesJSON(final Activity activity, String filename, List<Long> selectedCaseList, String password, Handler progressHandler)
	{
		File returnFile;
		int count = 0;

		// attempt to create json file
		File casesJSON = null;

		File cacheDir = activity.getCacheDir();
		File downloadsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		File picturesDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);


		///

		try
		{
			casesJSON = new File(downloadsDir.getPath(), ImportExportActivity.CASES_JSON_FILENAME);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			//showMessage(activity, "Unable to create local backup file.");
			Log.d(TAG, "Unable to create local backup file.");
			return null;
		}

		// to zip all images into a file for backup
		String zip_files_array[] = new String[0];

		// create local JSON files
		try
		{
			FileOutputStream cases_out = new FileOutputStream(casesJSON);

			// Get cases to export from cases table into a cursor
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

				String selection = CasesProvider.KEY_ROWID + " = ?";

				for(Long case_id : selectedCaseList)
				{
					if(j>0)
						selection += " OR " + CasesProvider.KEY_ROWID + " = ?";

					selectionArgs[j] = String.valueOf(case_id);

					j++;

				}

				caseCursor = activity.getContentResolver().query(CasesProvider.CASES_URI, null, selection, selectionArgs, null);
			}

			if (caseCursor != null && caseCursor.moveToFirst())
			{
				JsonWriter cases_writer = new JsonWriter(new OutputStreamWriter(cases_out, "UTF-8"));
				cases_writer.setIndent("  ");

				cases_writer.beginArray();

				// loop through all cases
				do
				{
					cases_writer.beginObject();

					// output all case columns/fields for this case
					for (int i = 0; i < CasesProvider.CASES_TABLE_ALL_KEYS.length; i++)
					{
						if(caseCursor.getString(i) != null && !caseCursor.getString(i).isEmpty())
						{
							cases_writer.name(CasesProvider.CASES_TABLE_ALL_KEYS[i]).value(caseCursor.getString(i));
						}
					}

					// output all linked images for this case (via parent_case_id)
					String [] image_args = {String.valueOf(caseCursor.getInt(CasesProvider.COL_ROWID))};
					Cursor imageCursor = activity.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

					// loop through all images of this case
					if(imageCursor.moveToFirst())
					{
						cases_writer.name("IMAGES");
						cases_writer.beginArray();
						do
						{
							cases_writer.beginObject();
							for(int i = 0; i < CasesProvider.IMAGES_TABLE_ALL_KEYS.length; i++)
							{
								cases_writer.name(CasesProvider.IMAGES_TABLE_ALL_KEYS[i]).value(imageCursor.getString(i));
							}
							cases_writer.endObject();

							// add image filename to zip list
							zip_files_array = UtilClass.addArrayElement(zip_files_array, picturesDir + "/" + imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME));

						} while (imageCursor.moveToNext());

						cases_writer.endArray();
					}
					else
					{
						cases_writer.name("IMAGES").nullValue();
					}

					imageCursor.close();

					cases_writer.endObject();

				} while (caseCursor.moveToNext());

				count = caseCursor.getCount();
				caseCursor.close();

				cases_writer.endArray();
				cases_writer.close();
			}

			// TODO get user passkey, test encrypt JSON file
			try
			{
				byte[] passkey = UtilsFile.generateKey(password);
				UtilsFile.encryptFile(passkey, casesJSON);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				Log.d(TAG, "Unable to generate encryption key.");
				return null;
			}

			// zip image and JSON files
			String zip_filename = downloadsDir.getPath() + "/" + filename + ImportExportActivity.RCS_EXTENSION;
			zip_files_array = UtilClass.addArrayElement(zip_files_array, casesJSON.getPath());

			// create zip file.  return link to that file.
			returnFile = UtilsFile.zip(zip_files_array, zip_filename, progressHandler);

			// delete temporary files
			casesJSON.delete();

			if(progressHandler != null)
			{
				Message msg = new Message();
				msg.arg1 = ImportExportActivity.PROGRESS_MSG_EXPORT_FINISHED;
				msg.arg2 = count;
				progressHandler.sendMessage(msg);
			}

		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.d(TAG, "IOException: " + e.getMessage());
			return null;
		}

		//alertDialog.dismiss();

		return returnFile;
	}

	/**
	 * Called from async task of CaseImport.java
	 *
	 * @param activity
	 * @param inFile
	 */
	public static int importCasesJSON(Activity activity, File inFile)
	{
		BufferedReader br = null;
		String line;
		Uri rowUri = null;
		int parent_id;
		int caseCount = 0;

		File picturesDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);


		// unzip image files and JSON files into pictures dir

		try
		{
			// unzip image files to android pictures directory
			UtilsFile.unzip(inFile.getPath(),picturesDir.getPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(activity, "Unable to open zip file:", Toast.LENGTH_SHORT).show();
			return 0;
		}


		File tempCasesJSON = null;
		File decryptedCasesJSON = null;
		JsonReader reader = null;

		try
		{
			// open existing file that should have been unzipped and decrypted
			decryptedCasesJSON = new File(activity.getCacheDir(), ImportExportActivity.CASES_JSON_FILENAME);
			tempCasesJSON = new File(picturesDir, ImportExportActivity.CASES_JSON_FILENAME);

			FileInputStream cases_in = new FileInputStream(decryptedCasesJSON);
			reader = new JsonReader(new InputStreamReader(cases_in, "UTF-8"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(activity, "Unable to open JSON file", Toast.LENGTH_SHORT).show();
			return 0;
		}

		// CASES TABLE
		try
		{
			ContentValues insertCaseValues = new ContentValues();
			ContentValues insertImageValues = new ContentValues();

			reader.beginArray();

			// loop through all cases
			while(reader.hasNext())
			{
				insertCaseValues.clear();
				reader.beginObject();

				while(reader.hasNext())
				{
					String field_name = reader.nextName();

					if(field_name.contentEquals("IMAGES"))	// IMAGES are at the end of the row, after other fields
					{
						// update LAST_MODIFIED_DATE
						SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd-HHmm-ss");
						String today_date_str = db_sdf.format(Calendar.getInstance().getTime());
						insertCaseValues.put(CasesProvider.KEY_LAST_MODIFIED_DATE, today_date_str);

						// insert the set of case info into the DB cases table
						rowUri = activity.getContentResolver().insert(CasesProvider.CASES_URI, insertCaseValues);

						// get parent key information
						parent_id = Integer.valueOf(rowUri.getLastPathSegment());

						// increment count
						caseCount += 1;

						// insert images ("IMAGES" is not null)
						if(reader.peek() != JsonToken.NULL)
						{
							reader.beginArray();

							// loop through all images of this case
							while (reader.hasNext())
							{
								insertImageValues.clear();
								reader.beginObject();

								while (reader.hasNext())
								{
									String image_field_name = reader.nextName();

									if (reader.peek() == JsonToken.NULL || image_field_name.contentEquals(CasesProvider.KEY_ROWID))
									{
										reader.skipValue();
									}
									else if (image_field_name.contentEquals(CasesProvider.KEY_IMAGE_PARENT_CASE_ID))
									{
										// put new parent_id of newly added case
										insertImageValues.put(image_field_name, parent_id);
										reader.skipValue();
									}
									else if (Arrays.asList(CasesProvider.IMAGES_TABLE_ALL_KEYS).contains(image_field_name))
									{
										insertImageValues.put(image_field_name, reader.nextString());
									}
									else
									{
										reader.skipValue();
									}
								}

								reader.endObject();

								// insert the set of image info into the DB images table
								activity.getContentResolver().insert(CasesProvider.IMAGES_URI, insertImageValues);

							}

							reader.endArray();

						}
						else
						{
							// skip "IMAGES" NULL value
							reader.skipValue();
						}

					}
					else if(reader.peek() == JsonToken.NULL || field_name.contentEquals(CasesProvider.KEY_ROWID))
					{
						// ignore NULL values (except if "IMAGES) and row_id
						reader.skipValue();
					}
					else if(Arrays.asList(CasesProvider.CASES_TABLE_ALL_KEYS).contains(field_name))
					{
						// valid field name, enter in database
						insertCaseValues.put(field_name, reader.nextString());
					}
					else
					{
						// unrecognized field name
						reader.skipValue();
					}
				}

				reader.endObject();
			}

			reader.endArray();

		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(activity, "Unable to open Cases JSON file", Toast.LENGTH_SHORT).show();
			return 0;
		}

		//UtilClass.showMessage(activity, "Imported " + caseCount + " cases");
		//Toast.makeText(activity, "Imported " + caseCount + " cases", Toast.LENGTH_SHORT).show();
		tempCasesJSON.delete();
		decryptedCasesJSON.delete();

		return caseCount;
	}

	public static File exportListsJSON(Activity activity, String filename)
	{
		// attempt to create json file
		File listsJSON = null;
		File cacheDir = activity.getCacheDir();
		File downloadsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

		try
		{
			listsJSON = new File(downloadsDir.getPath(), filename+ ImportExportActivity.LIST_EXTENSION);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			showMessage(activity, "Unable to create local JSON backup files.");
			return null;
		}

		// create local JSON files
		try
		{
			FileOutputStream lists_out = new FileOutputStream(listsJSON);

			JsonWriter lists_writer = new JsonWriter(new OutputStreamWriter(lists_out, "UTF-8"));
			lists_writer.setIndent("  ");

			Cursor cursor;

			lists_writer.beginArray();

			// get KEYWORD list
			cursor = activity.getContentResolver().query(CasesProvider.KEYWORD_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);
			lists_writer.beginObject();
			lists_writer.name("KEYWORDS");
			lists_writer.beginArray();

			if (cursor != null && cursor.moveToFirst())
			{
				// loop through all rows
				do
				{
					lists_writer.beginObject();

					lists_writer.name(CasesProvider.KEY_KEYWORDS).value(cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE));
					lists_writer.name(CasesProvider.KEY_ORDER).value(cursor.getString(CasesProvider.COL_LIST_ITEM_ORDER));
					lists_writer.name(CasesProvider.KEY_LIST_ITEM_IS_HIDDEN).value(cursor.getString(CasesProvider.COL_LIST_ITEM_IS_HIDDEN));

					lists_writer.endObject();

				} while (cursor.moveToNext());

				cursor.close();
			}

			lists_writer.endArray();
			lists_writer.endObject();

			// get modality list
			cursor = activity.getContentResolver().query(CasesProvider.STUDYTYPE_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);
			lists_writer.beginObject();
			lists_writer.name("MODALITY");
			lists_writer.beginArray();

			if (cursor != null && cursor.moveToFirst())
			{
				// loop through all rows
				do
				{
					lists_writer.beginObject();

					lists_writer.name(CasesProvider.KEY_STUDY_TYPE).value(cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE));
					lists_writer.name(CasesProvider.KEY_ORDER).value(cursor.getString(CasesProvider.COL_LIST_ITEM_ORDER));
					lists_writer.name(CasesProvider.KEY_LIST_ITEM_IS_HIDDEN).value(cursor.getString(CasesProvider.COL_LIST_ITEM_IS_HIDDEN));

					lists_writer.endObject();

				} while (cursor.moveToNext());

				cursor.close();
			}

			lists_writer.endArray();
			lists_writer.endObject();

			// get KEYWORD list
			cursor = activity.getContentResolver().query(CasesProvider.SECTION_LIST_URI, null, null, null, CasesProvider.KEY_ORDER, null);
			lists_writer.beginObject();
			lists_writer.name("SECTION");
			lists_writer.beginArray();

			if (cursor != null && cursor.moveToFirst())
			{
				// loop through all rows
				do
				{
					lists_writer.beginObject();

					lists_writer.name(CasesProvider.KEY_SECTION).value(cursor.getString(CasesProvider.COL_LIST_ITEM_VALUE));
					lists_writer.name(CasesProvider.KEY_ORDER).value(cursor.getString(CasesProvider.COL_LIST_ITEM_ORDER));
					lists_writer.name(CasesProvider.KEY_LIST_ITEM_IS_HIDDEN).value(cursor.getString(CasesProvider.COL_LIST_ITEM_IS_HIDDEN));

					lists_writer.endObject();

				} while (cursor.moveToNext());

				cursor.close();
			}

			lists_writer.endArray();
			lists_writer.endObject();


			lists_writer.endArray();
			lists_writer.close();

		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.d(TAG, "IOException: " + e.getMessage());
			return null;
		}

		return listsJSON;
	}

	public static void importListsJSON(Activity activity, File inFile)
	{
		BufferedReader br = null;
		String line;
		Uri rowUri = null;
		int parent_id;
		int imageCount = 0;


		File tempListsJSON = null;
		JsonReader reader = null;

		try
		{
			// open existing file that should have been unzipped
			tempListsJSON = inFile;
			FileInputStream cases_in = new FileInputStream(tempListsJSON);
			reader = new JsonReader(new InputStreamReader(cases_in, "UTF-8"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(activity, "Unable to copy JSON file", Toast.LENGTH_SHORT).show();
			return;
		}

		// LIST TABLES
		try
		{
			ContentValues insertListValues = new ContentValues();

			reader.beginArray();

			// loop through all lists
			while(reader.hasNext())
			{
				reader.beginObject();

				while(reader.hasNext())
				{
					insertListValues.clear();

					String list_name = reader.nextName();

					if(list_name.contentEquals("KEYWORDS"))
					{
						activity.getContentResolver().delete(CasesProvider.KEYWORD_LIST_URI, null, null);

						reader.beginArray();

						while (reader.hasNext())
						{
							reader.beginObject();

							while (reader.hasNext())
							{

								String field_name = reader.nextName();

								if (reader.peek() == JsonToken.NULL || field_name.contentEquals(CasesProvider.KEY_ROWID))
								{
									// ignore NULL values and row_id
									reader.skipValue();
								}
								else if (field_name.contentEquals(CasesProvider.KEY_KEYWORDS) || field_name.contentEquals(CasesProvider.KEY_ORDER))
								{
									// valid field name, enter in database
									insertListValues.put(field_name, reader.nextString());
								}
								else
								{
									// unrecognized field name
									reader.skipValue();
								}
							}

							reader.endObject();

							// insert the set of case info into the DB cases table
							rowUri = activity.getContentResolver().insert(CasesProvider.KEYWORD_LIST_URI, insertListValues);
						}

						reader.endArray();


					}
					else if(list_name.contentEquals("MODALITY"))
					{
						activity.getContentResolver().delete(CasesProvider.STUDYTYPE_LIST_URI, null, null);

						reader.beginArray();

						while (reader.hasNext())
						{
							reader.beginObject();

							while (reader.hasNext())
							{

								String field_name = reader.nextName();

								if (reader.peek() == JsonToken.NULL || field_name.contentEquals(CasesProvider.KEY_ROWID))
								{
									// ignore NULL values and row_id
									reader.skipValue();
								}
								else if (field_name.contentEquals(CasesProvider.KEY_STUDY_TYPE) || field_name.contentEquals(CasesProvider.KEY_ORDER))
								{
									// valid field name, enter in database
									insertListValues.put(field_name, reader.nextString());
								}
								else
								{
									// unrecognized field name
									reader.skipValue();
								}
							}

							reader.endObject();

							// insert the set of case info into the DB cases table
							rowUri = activity.getContentResolver().insert(CasesProvider.STUDYTYPE_LIST_URI, insertListValues);
						}

						reader.endArray();
					}
					else if(list_name.contentEquals("SECTION"))
					{
						activity.getContentResolver().delete(CasesProvider.SECTION_LIST_URI, null, null);

						reader.beginArray();

						while (reader.hasNext())
						{
							reader.beginObject();

							while (reader.hasNext())
							{

								String field_name = reader.nextName();

								if (reader.peek() == JsonToken.NULL || field_name.contentEquals(CasesProvider.KEY_ROWID))
								{
									// ignore NULL values and row_id
									reader.skipValue();
								}
								else if (field_name.contentEquals(CasesProvider.KEY_SECTION) || field_name.contentEquals(CasesProvider.KEY_ORDER))
								{
									// valid field name, enter in database
									insertListValues.put(field_name, reader.nextString());
								}
								else
								{
									// unrecognized field name
									reader.skipValue();
								}
							}

							reader.endObject();

							// insert the set of case info into the DB cases table
							rowUri = activity.getContentResolver().insert(CasesProvider.SECTION_LIST_URI, insertListValues);

						}

						reader.endArray();
					}

				}

				reader.endObject();
			}

			reader.endArray();

		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(activity, "Unable to open Cases JSON file", Toast.LENGTH_SHORT).show();
			return;
		}

		Toast.makeText(activity, "Imported lists", Toast.LENGTH_SHORT).show();
		//tempListsJSON.delete();
	}


	// attr values
	public static int get_attr(Context context, int value)
	{
		Resources.Theme theme = context.getTheme();
		TypedValue typedValue = new TypedValue();

		theme.resolveAttribute(value, typedValue, true);

		return typedValue.data;
	}

	public static boolean hitTest(View v, int x, int y)
	{
		final int tx = (int) (ViewCompat.getTranslationX(v) + 0.5f);
		final int ty = (int) (ViewCompat.getTranslationY(v) + 0.5f);
		final int left = v.getLeft() + tx;
		final int right = v.getRight() + tx;
		final int top = v.getTop() + ty;
		final int bottom = v.getBottom() + ty;

		return (x >= left) && (x <= right) && (y >= top) && (y <= bottom);
	}

	// FILE DIRECTORIES
	public static File getDownloadsDir()
	{
		return CaseCardListActivity.downloadsDir;
	}

	public static File getPicturesDir()
	{
		return CaseCardListActivity.picturesDir;
	}

	public static File getAppDir()
	{
		return CaseCardListActivity.appDir;
	}

	/*
	// after sdk24, error: cannot override getDataDir() in Context, overriding method is static
	public static File getDataDir()
	{
		return CaseCardListActivity.dataDir;
	}
	*/


	public static void changeSearchViewTextColor(View view, int color) {
		if (view != null) {
			if (view instanceof TextView) {
				((TextView) view).setTextColor(color);
				return;
			} else if (view instanceof ViewGroup) {
				ViewGroup viewGroup = (ViewGroup) view;
				for (int i = 0; i < viewGroup.getChildCount(); i++) {
					changeSearchViewTextColor(viewGroup.getChildAt(i), color);
				}
			}
		}
	}

/*
	public static void setMargins(View view, int left, int top, int right, int bottom)
	{
		android.support.v4.widget.DrawerLayout.LayoutParams params = new android.support.v4.widget.DrawerLayout.LayoutParams(
				                                                                                                                    android.support.v4.widget.DrawerLayout.LayoutParams.WRAP_CONTENT,
				                                                                                                                    android.support.v4.widget.DrawerLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(left, top, right, bottom);
		view.setLayoutParams(params);
	}
*/
}

