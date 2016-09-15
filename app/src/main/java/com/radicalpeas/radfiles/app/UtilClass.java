package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

	private static final String TAG = "UtilClass";

	// standard directories
	//private static File downloadsDir = CaseCardListActivity.downloadsDir;
	//private static File picturesDir = CaseCardListActivity.picturesDir;
	private static File appDir  = CaseCardListActivity.appDir;             // internal app data directory
	private static File dataDir  = CaseCardListActivity.dataDir;            // private data directory (with SQL database)

	/**
	 * Shows a toast message.
	 */
	public static void showToast(Context context, String message)
	{
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	public static void showToast(Context context, int message)
	{
		Toast.makeText(context, String.valueOf(message), Toast.LENGTH_LONG).show();
	}
	public static void showSnackbar(Activity activity, String message)
	{

		showSnackbar(activity, message, Snackbar.LENGTH_SHORT);

		//Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	public static void showSnackbar(Activity activity, String message, int length)
	{

		Snackbar snackbar = Snackbar
				.make(activity.findViewById(android.R.id.content), message, length);
		snackbar.show();

		//Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}



	/**
	 * Zoom image from thumbnail image view
	 */
/*
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
	*/
	/* end zoom from thumb */

/*
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
*/

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

	/*
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
	*/
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

	public static int getDisplayWidthPx(Context context)
	{
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		return displayMetrics.widthPixels;
	}
	public static int getDisplayHeightPx(Context context)
	{
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		return displayMetrics.heightPixels;
	}

	public static int getStatusBarHeight(Context context)
	{
		int result = 0;
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static int getToolbarHeight(Context context)
	{
		//return (int)activity.getResources().getDimension(R.dimen.toolbar_size);
		TypedValue tv = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true))
		{
			return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
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


	public static File getScaledImageFile(Context context, Uri old_uri, String new_filename) throws IOException
	{
		// Get the private application storage directory for pictures
		File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

		//File resultFile = File.createTempFile(new_filename, ".png", storageDir);
		File resultFile = new File(storageDir + "/" + new_filename);

		Bitmap scaledBitmap = getScaledBitmap(context, old_uri);
		saveBitmap(scaledBitmap, resultFile.getPath());

		return resultFile;
	}

	public static void saveBitmap(Bitmap bitmap, String path)
	{
		if(bitmap!=null){
			try {
				FileOutputStream outputStream = null;
				try {
					outputStream = new FileOutputStream(path); //here is set your file path where you want to save or also here you can set file object directly

					bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // bitmap is your Bitmap instance, if you want to compress it you can compress reduce percentage
					// PNG is a lossless format, the compression factor (100) is ignored
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if (outputStream != null) {
							outputStream.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static Bitmap getScaledBitmap(Context context, Uri uri) //, String path)
	{
		ContentResolver contentResolver = context.getContentResolver();

		//Uri uri = Uri.fromFile(new File(path));
		InputStream in = null;
		try {
			final int IMAGE_MAX_SIZE = 300000;
			//final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
			//final int IMAGE_MAX_SIZE = 512; //single dimension


			in = contentResolver.openInputStream(uri);

			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(in, null, o);
			in.close();

			int scale = 1;

			while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >	IMAGE_MAX_SIZE) {
				scale++;
			}

			/*
			if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
				scale = (int) Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
			}
			*/

			Log.d(TAG, "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

			Bitmap bitmap = null;
			in = contentResolver.openInputStream(uri);
			if (scale > 1) {
				scale--;
				// scale to max possible inSampleSize that still yields an image
				// larger than target
				o = new BitmapFactory.Options();
				o.inSampleSize = scale;
				bitmap = BitmapFactory.decodeStream(in, null, o);

				/*
				// resize to desired dimensions
				int height = bitmap.getHeight();
				int width = bitmap.getWidth();
				Log.d(TAG, "1th scale operation dimenions - width: " + width + ", height: " + height);

				double y = Math.sqrt(IMAGE_MAX_SIZE
						/ (((double) width) / height));
				double x = (y / height) * width;

				Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) x,	(int) y, true);
				bitmap.recycle();
				bitmap = scaledBitmap;

				*/

				System.gc();
			} else {
				bitmap = BitmapFactory.decodeStream(in);
			}
			in.close();

			Log.d(TAG, "bitmap size - width: " +bitmap.getWidth() + ", height: " + bitmap.getHeight());
			return bitmap;
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(),e);
			return null;
		}
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
							//Uri case_delete_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
							//context.getContentResolver().delete(case_delete_uri, null, null);
							UtilsDatabase.deleteCase(context, key_id);

							// delete all linked images files
							Cursor image_cursor = context.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)}, CasesProvider.KEY_ORDER);
							File imageFile = null;
							if (image_cursor.moveToFirst())
							{
								do
								{
									imageFile = new File(image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME));
									imageFile.delete();

									UtilsDatabase.deleteCaseImageFile(context, image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME));
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
							//Uri case_delete_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
							//context.getContentResolver().delete(case_delete_uri, null, null);
							UtilsDatabase.deleteCase(context, key_id);

							// delete all linked images files
							Cursor image_cursor = context.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)}, CasesProvider.KEY_ORDER);
							File imageFile = null;
							if (image_cursor.moveToFirst())
							{
								do
								{
									imageFile = new File(image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME));
									imageFile.delete();

									UtilsDatabase.deleteCaseImageFile(context, image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME));
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

