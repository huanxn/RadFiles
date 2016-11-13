package com.radicalpeas.radfiles.app;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
//import android.support.v7.view.ActionMode;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 10/21/2014.
 */

public class CaseCardAdapter extends RecyclerView.Adapter<CaseCardAdapter.ViewHolder> implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder>
{
	private static String TAG = "CaseCardAdapter";

	public List<Case> caseList;
	private int card_layout_id;
	private Activity activity;

	private File imageDir = CaseCardListActivity.picturesDir;

	// StickyRecyclerHeadersAdapter
	//private List<Integer> header_id;    // if different than previous (ie in a different group), then it will display it's header
	private List<String> header;
	private List<Integer> group_position;

	// contextual action mode set in activity
	private android.view.ActionMode.Callback mActionModeCallback = null;
	public android.view.ActionMode mActionMode = null;

	// contextual multi-select list
	private List<Long> multiselectList;

	// click listeners set in activity
	private View.OnClickListener mOnClickListener;
	private View.OnLongClickListener mOnLongClickListener;

	public CaseCardAdapter(Activity activity, Cursor caseCursor, int card_layout)
	{
		caseList = new ArrayList<Case>();
		if(caseCursor != null)
		{
			loadCases(caseCursor);
			notifyDataSetChanged();
		}

		this.card_layout_id = card_layout;
		this.activity = activity;

		multiselectList = new ArrayList<Long>();

		setHasStableIds(true);
	}

	public void loadCases(Cursor cursor)
	{
		// loop through SQL case cursor and put info into cards
		if (cursor != null && cursor.moveToFirst())
		{
			// clear old list
			if(!caseList.isEmpty())
				caseList.clear();

			do
			{
				Case new_case = new Case();

				new_case.unique_id = cursor.getString(CasesProvider.COL_UNIQUE_ID);
				new_case.key_id = cursor.getInt(CasesProvider.COL_ROWID);

				new_case.case_id = cursor.getString(CasesProvider.COL_CASE_NUMBER);
				new_case.diagnosis = cursor.getString(CasesProvider.COL_DIAGNOSIS);
				new_case.findings = cursor.getString(CasesProvider.COL_FINDINGS);
				new_case.section = cursor.getString(CasesProvider.COL_SECTION);
				new_case.study_type = cursor.getString(CasesProvider.COL_STUDY_TYPE);
				new_case.db_date_str = cursor.getString(CasesProvider.COL_DATE);
				new_case.key_words = cursor.getString(CasesProvider.COL_KEYWORDS);
				new_case.favorite = cursor.getString(CasesProvider.COL_FAVORITE);

				new_case.thumbnail = 0;
				String thumbnailString = cursor.getString(CasesProvider.COL_THUMBNAIL);
				if (thumbnailString != null && !thumbnailString.isEmpty())
					new_case.thumbnail = Integer.parseInt(thumbnailString);

				// get images for this case
				String[] image_args = {String.valueOf(new_case.key_id)};
				Cursor image_cursor = activity.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

				if(image_cursor.getCount() > 0 && image_cursor.moveToFirst())
				{
					if(new_case.thumbnail < image_cursor.getCount())
					{
						image_cursor.move(new_case.thumbnail);
					}

					//new_case.thumbnail_filename = CaseCardListActivity.picturesDir + "/" + image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);
					new_case.thumbnail_filename = imageDir + "/" + image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME);
				}
				else
				{
					new_case.thumbnail_filename = null;
				}
				image_cursor.close();

				caseList.add(new_case);

			} while(cursor.moveToNext());

		}
		else
		{
			caseList.clear();
		}

		syncCloudCases();

//		notifyDataSetChanged();
	}

	private void syncCloudCases()
	{

		// loop through firebase database cases
		//  see if there are any cases that are not found in the SQL database
		//  if so, then case was added on web
		//  need to add case to SQL and the current caseList

		// build list of firebase database cases
		final List<Case> cloudCaseList = new ArrayList<Case>();
		// build list of all unique local cases
		final List<String> localCaseList = new ArrayList<String>();

		final Cursor caseCursor = activity.getContentResolver().query(CasesProvider.CASES_URI, null, null, null, CasesProvider.KEY_UNIQUE_ID + " ASC", null);
		if (caseCursor.getCount() > 0 && caseCursor.moveToFirst())
		{
			do
			{
				localCaseList.add(caseCursor.getString(CasesProvider.COL_UNIQUE_ID));
			} while (caseCursor.moveToNext());
		}
		caseCursor.close();

		final FirebaseAuth mAuth = FirebaseAuth.getInstance();
		if (mAuth != null)
		{
			final FirebaseUser firebaseUser = mAuth.getCurrentUser();
			final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

			if (firebaseUser != null && firebaseDatabase != null)
			{
				DatabaseReference databaseRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid());
				// order cases by unique_id
				final Query casesQuery = databaseRef.child("Cases").orderByChild(CasesProvider.KEY_UNIQUE_ID);

				casesQuery.addListenerForSingleValueEvent(new ValueEventListener()
				{
					@Override
					public void onDataChange(DataSnapshot dataSnapshot)
					{
						// loop through cloud cases
						for (DataSnapshot postSnapshot: dataSnapshot.getChildren())
						{
							//Log.d(TAG, "found: " + postSnapshot.getKey() + " - " + postSnapshot.child("DIAGNOSIS"));

							boolean found = false;

							// loop through local cases, already built in loadCases()
							for( String local_case_ID: localCaseList )
							{
								// if cloud case is found in caseList, it is up to date synced.  remove from list.  move on to nex postSnapshot
								if (postSnapshot.child(CasesProvider.KEY_UNIQUE_ID) != null && postSnapshot.child(CasesProvider.KEY_UNIQUE_ID).getValue().equals(local_case_ID))
								{
									Log.d(TAG, "found: " + postSnapshot.getKey() + " - " + postSnapshot.child("DIAGNOSIS"));

									// when case is found, remove from list
									found = true;
									localCaseList.remove(local_case_ID);
									break;
								}
							}

							// if postSnapshot is not in localCaseList, then it should be added to caseList and SQL
							if(!found)
							{
								Log.d(TAG, "not found: " + postSnapshot.getKey() + " - " + postSnapshot.child("DIAGNOSIS"));

								Case new_case = new Case();
								//new_case.key_id = postSnapshot.
								new_case.unique_id = getDatabaseValue(postSnapshot, CasesProvider.KEY_UNIQUE_ID);
								new_case.case_id = getDatabaseValue(postSnapshot, CasesProvider.KEY_CASE_NUMBER);
								new_case.diagnosis = getDatabaseValue(postSnapshot, CasesProvider.KEY_DIAGNOSIS);
								new_case.findings = getDatabaseValue(postSnapshot, CasesProvider.KEY_FINDINGS);
								new_case.section = getDatabaseValue(postSnapshot, CasesProvider.KEY_SECTION);

								new_case.biopsy = getDatabaseValue(postSnapshot, CasesProvider.KEY_BIOPSY);
								new_case.followup_comment = getDatabaseValue(postSnapshot, CasesProvider.KEY_FOLLOWUP_COMMENT);

								new_case.followup = getDatabaseValueInteger(postSnapshot, CasesProvider.KEY_FOLLOWUP); //int

								new_case.key_words = getDatabaseValue(postSnapshot, CasesProvider.KEY_KEYWORDS);
								new_case.comments = getDatabaseValue(postSnapshot, CasesProvider.KEY_COMMENTS);

								new_case.study_type = getDatabaseValue(postSnapshot, CasesProvider.KEY_STUDY_TYPE);
								new_case.db_date_str = getDatabaseValue(postSnapshot, CasesProvider.KEY_STUDY_DATE);

								new_case.image_count = getDatabaseValueInteger(postSnapshot, CasesProvider.KEY_IMAGE_COUNT);	//int

								new_case.thumbnail = getDatabaseValueInteger(postSnapshot, CasesProvider.KEY_THUMBNAIL);	//int
								new_case.favorite = getDatabaseValue(postSnapshot, CasesProvider.KEY_FAVORITE);

								new_case.clinical_history = getDatabaseValue(postSnapshot, CasesProvider.KEY_CLINICAL_HISTORY);
								new_case.last_modified_date = getDatabaseValue(postSnapshot, CasesProvider.KEY_LAST_MODIFIED_DATE);
								new_case.userID = getDatabaseValue(postSnapshot, CasesProvider.KEY_USER_ID);
								new_case.original_creator = getDatabaseValue(postSnapshot, CasesProvider.KEY_ORIGINAL_CREATOR);

								new_case.is_shared = getDatabaseValueInteger(postSnapshot, CasesProvider.KEY_IS_SHARED);	//int

								ContentValues insertCaseValues = new ContentValues();
								insertCaseValues.clear();

								// insert into local SQL database
								insertCaseValues.put(CasesProvider.KEY_UNIQUE_ID, new_case.unique_id);
								insertCaseValues.put(CasesProvider.KEY_CASE_NUMBER, new_case.case_id);
								insertCaseValues.put(CasesProvider.KEY_DIAGNOSIS, new_case.diagnosis);
								insertCaseValues.put(CasesProvider.KEY_FINDINGS, new_case.findings);
								insertCaseValues.put(CasesProvider.KEY_SECTION, new_case.section);
								insertCaseValues.put(CasesProvider.KEY_STUDY_TYPE, new_case.study_type);
								insertCaseValues.put(CasesProvider.KEY_STUDY_DATE, new_case.db_date_str);
								insertCaseValues.put(CasesProvider.KEY_KEYWORDS, new_case.key_words);
								insertCaseValues.put(CasesProvider.KEY_BIOPSY, new_case.biopsy);
								insertCaseValues.put(CasesProvider.KEY_FOLLOWUP, new_case.followup);
								insertCaseValues.put(CasesProvider.KEY_FOLLOWUP_COMMENT, new_case.followup_comment);
								insertCaseValues.put(CasesProvider.KEY_CLINICAL_HISTORY, new_case.clinical_history);
								insertCaseValues.put(CasesProvider.KEY_COMMENTS, new_case.comments);
								insertCaseValues.put(CasesProvider.KEY_FAVORITE, new_case.favorite);
								insertCaseValues.put(CasesProvider.KEY_IMAGE_COUNT, new_case.image_count);
								insertCaseValues.put(CasesProvider.KEY_THUMBNAIL, new_case.thumbnail);
								insertCaseValues.put(CasesProvider.KEY_LAST_MODIFIED_DATE, new_case.last_modified_date);
								insertCaseValues.put(CasesProvider.KEY_ORIGINAL_CREATOR, new_case.original_creator);
								insertCaseValues.put(CasesProvider.KEY_IS_SHARED, new_case.is_shared);

								// use current userID, may have been shared from another user
								insertCaseValues.put(CasesProvider.KEY_USER_ID, new_case.userID);

								// insert into local SQL database
								Uri new_case_uri = activity.getContentResolver().insert(CasesProvider.CASES_URI, insertCaseValues);
								long newCase_KEYID = Integer.valueOf(new_case_uri.getLastPathSegment());

								new_case.key_id = newCase_KEYID;


								//IMAGES
								if(new_case.image_count > 0)
								{
									// loop through images of this case snapshot
									for (DataSnapshot imageSnapshot: postSnapshot.child("Images").getChildren())
									{
										Log.d(TAG, "image: " + imageSnapshot.child(CasesProvider.KEY_IMAGE_FILENAME).getValue().toString());

										// add to this case instatiation
										CaseImage caseImage = new CaseImage();
										//caseImage.set_id((int)new_image_id);
										caseImage.setParent_id((int)new_case.key_id);
										caseImage.setFilename(getDatabaseValue(imageSnapshot, CasesProvider.KEY_IMAGE_FILENAME));
										caseImage.setCaption(getDatabaseValue(imageSnapshot, CasesProvider.KEY_IMAGE_CAPTION));
										caseImage.setDetails(getDatabaseValue(imageSnapshot, CasesProvider.KEY_IMAGE_DETAILS));
										caseImage.setOrder(getDatabaseValueInteger(imageSnapshot, CasesProvider.KEY_ORDER));

										//store in SQL image table
										ContentValues imageValues = new ContentValues();
										imageValues.clear();
										imageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, new_case.key_id);
										imageValues.put(CasesProvider.KEY_IMAGE_FILENAME, caseImage.getFilename());
										imageValues.put(CasesProvider.KEY_IMAGE_CAPTION, caseImage.getCaption());
										imageValues.put(CasesProvider.KEY_IMAGE_DETAILS, caseImage.getDetails());
										imageValues.put(CasesProvider.KEY_ORDER, caseImage.getOrder());      // set order to display images.  new files last.  //todo user reodering

										// insert into local SQL database
										Uri image_row_uri = activity.getContentResolver().insert(CasesProvider.IMAGES_URI, imageValues);
										long new_image_id = Long.parseLong(image_row_uri.getLastPathSegment());

										caseImage.set_id((int)new_image_id);
										caseImage.foundInCloudDatabase = true;	//probably don't need this, but just in case...

										// add image to new_case caseImageList
										new_case.caseImageList.add(caseImage);

										// DOWNLOAD IMAGE FILES
										final String filename = caseImage.getFilename();

										// set up Firebase storage reference
										final FirebaseStorage mStorage = FirebaseStorage.getInstance();
										final StorageReference mStorageRef = mStorage.getReferenceFromUrl("gs://rad-files.appspot.com");

										final File picturesDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
										final File localFilePath = new File(picturesDir + "/" + filename);
										final StorageReference storageImage = mStorageRef.child(firebaseUser.getUid() + "/pictures/" + filename);

										File localFile = new File(localFilePath.getPath());//File.createTempFile("images", "jpg");

										storageImage.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {

											@Override
											public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
											{
												// Local image file has been created
												UtilClass.showToast(activity, "Downloaded file " + filename);
												Log.d(TAG, "Downloaded file " + filename);

												//todo refresh casedetail view
												//todo or show snackbar with refresh button

												//notifyDataSetChanged();

											}
										}).addOnFailureListener(new OnFailureListener() {
											@Override
											public void onFailure(@NonNull Exception exception) {
												// Handle any errors
												UtilClass.showToast(activity, "Unable to download file " + filename);
												Log.d(TAG, "Unable to download file " + filename);
											}
										});
									}
								}


								caseList.add(new_case);


								// refresh
								notifyDataSetChanged();
							}

						}

						// delete left over local_cases that were not found in cloud
						for( String local_case_ID: localCaseList )
						{
							Log.d(TAG, "todo delete: " + local_case_ID);

							final Cursor deleteCaseCursor = activity.getContentResolver().query(CasesProvider.CASES_URI, null, CasesProvider.KEY_UNIQUE_ID + " is not null and " + CasesProvider.KEY_UNIQUE_ID + " = ?", new String[]{local_case_ID}, null, null);
							if (deleteCaseCursor.moveToFirst())
							{
								Log.d(TAG, "delete diagnosis: " + deleteCaseCursor.getString(CasesProvider.COL_DIAGNOSIS));
								UtilsDatabase.deleteCase(activity, deleteCaseCursor.getInt(CasesProvider.COL_ROWID), false);

							}
							deleteCaseCursor.close();

							// refresh
							notifyDataSetChanged();
						}

					}

					@Override
					public void onCancelled(DatabaseError databaseError)
					{
						Log.d(TAG, "firebase query failed");

					}
				});

			}
		}
	}

	private String getDatabaseValue(DataSnapshot data, String key)
	{
		if(data.child(key) == null || data.child(key).getValue() == null)
		{
			return null;
		}
		else
		{
			return data.child(key).getValue().toString();
		}
	}
	private int getDatabaseValueInteger(DataSnapshot data, String key)
	{
		if(data.child(key) == null || data.child(key).getValue() == null)
		{
			return 0;
		}
		else
		{
			return Integer.valueOf(data.child(key).getValue().toString());
		}
	}

	/**
	 * used for import JSON
	 * @param inputCases
	 */
	public void loadCaseList(List<Case> inputCases)
	{
		// loop through case cursor and put info into cards
		if (inputCases != null && !inputCases.isEmpty())
		{
			caseList = inputCases;
		}
		else
		{
			caseList.clear();
		}

		//notifyDataSetChanged();
	}

	public void removeCase(long key_id)
	{
		for(int i = 0; i < caseList.size(); i++)
		{
			if(caseList.get(i).key_id == key_id)
			{
				caseList.remove(i);
				return;
			}
		}
	}

	public void setActionModeCallback(android.view.ActionMode.Callback actionModeCallback)
	{
		if(actionModeCallback != null)
		{
			mActionModeCallback = actionModeCallback;
		}
	}

	public void setActionMode(android.view.ActionMode actionMode)
	{
		mActionMode = actionMode;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(card_layout_id, viewGroup, false);

		ViewHolder holder = new ViewHolder(v);

		holder.cardView.setOnClickListener(mOnClickListener);
		holder.cardView.setOnLongClickListener(mOnLongClickListener);

		holder.cardView.setTag(holder);

		return holder;
	}

	@Override
	/**
	 * redraws when cards scroll into view
	 */
	public void onBindViewHolder(ViewHolder viewHolder, int i)
	{
		if(caseList != null)
		{
			Case mCase = caseList.get(i);

			View cardView = viewHolder.cardView;//.findViewById(R.id.container);

			// not used
//			int height = cardView.getLayoutParams().height;
//			int maxCardHeight = (int)activity.getResources().getDimension(R.dimen.card_thumbnail_height);

			if(header != null && group_position != null)
			{
				if(mCase.isHidden)
				{
					//viewHolder.cardView.setVisibility(View.GONE);
					/*
					ViewGroup.LayoutParams layoutParams = viewHolder.cardView.getLayoutParams();
					layoutParams.height = 0;
					viewHolder.cardView.setLayoutParams(layoutParams);
					*/
					//animateViewHeightChange(cardContainer, maxCardHeight, 0);
					setViewHeight(cardView, 0);

					//collapseViewHolder(viewHolder, group_position.get(i));
				}
				else
				{
					viewHolder.cardView.setVisibility(View.VISIBLE);
					/*
					ViewGroup.LayoutParams layoutParams = viewHolder.cardView.getLayoutParams();
					layoutParams.height = (int)activity.getResources().getDimension(R.dimen.card_thumbnail_height);
					viewHolder.cardView.setLayoutParams(layoutParams);
					*/
					//animateViewHeightChange(cardContainer, 0, maxCardHeight);
					setViewHeight(cardView, ViewGroup.LayoutParams.WRAP_CONTENT);

					//expandViewHolder(viewHolder, group_position.get(i));
				}
			}

			viewHolder.key_id = mCase.key_id;

			/*
			viewHolder.card_title.setText(mCase.case_id);
			viewHolder.card_text1.setText(mCase.diagnosis);
			viewHolder.card_text2.setText(mCase.findings);
			viewHolder.card_text3.setText(mCase.key_words);
			*/
			viewHolder.card_title.setText(mCase.diagnosis);
			viewHolder.card_text1.setVisibility(View.GONE);
			viewHolder.card_text2.setText(mCase.findings);
			viewHolder.card_text3.setText(mCase.key_words);


			//viewHolder.thumbnail.setImageDrawable(activity.getDrawable(country.getImageResourceId(mContext)));

			Glide.with(activity).load(mCase.thumbnail_filename)
					/*
					.placeholder(new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_error_outline))
					.error(new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_error_outline))
					.fallback(new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_sync))	//null
					*/
					.into(viewHolder.thumbnail)
					;
			/*
			if(UtilClass.setPic(viewHolder.thumbnail, mCase.thumbnail_filename, UtilClass.IMAGE_THUMB_SIZE))
			{
				viewHolder.thumbnail.setVisibility(View.VISIBLE);
			}
			else
			{
				viewHolder.thumbnail.setVisibility(View.GONE);
			}
			*/

			if(mActionMode == null || !mCase.isSelected)
			{
				// clear color highlight of unselected items
				//viewHolder.container.setBackgroundColor(activity.getResources().getColor(R.color.default_card_background));
				viewHolder.container.setBackgroundColor(ContextCompat.getColor(activity, R.color.default_card_background));
				viewHolder.thumbnail.setColorFilter(0x00000000);

				/*
				cardView.setMaxCardElevation(20);
				cardView.setCardElevation(20);
				*/
			}
			else // mCase.isSelected == true
			{
				//viewHolder.container.setBackgroundColor(activity.getResources().getColor(R.attr.colorAccent));

				// color highlight the selected items
				viewHolder.container.setBackgroundColor(ContextCompat.getColor(activity, R.color.default_colorSelected));
				viewHolder.thumbnail.setColorFilter(ContextCompat.getColor(activity, R.color.default_colorSelected));
				//viewHolder.thumbnail.setColorFilter(UtilClass.get_attr(activity, R.attr.colorControlHighlight)); // too opaque
			}
		}
	}

	// collapse animations
	private void animateViewHeightChange(final View view, int start, int end)
	{
		if (view == null)
		{
			return;
		}

		final ValueAnimator animator = ValueAnimator.ofInt(start, end);

		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator)
			{
				int value = (Integer) valueAnimator.getAnimatedValue();
				setViewHeight(view, value);
			}
		});
		animator.setDuration(300).start();
	}

	private void collapseViewHolder(ViewHolder viewHolder, int position_in_group)
	{
		//viewHolder.thumbnail.setVisibility(View.GONE);
		viewHolder.offset = (int) (position_in_group*(activity.getResources().getDimension(R.dimen.card_thumbnail_height)));
		viewHolder.cardView.setTranslationY(viewHolder.offset);
	}

	private void expandViewHolder(ViewHolder viewHolder, int position_in_group)
	{
		//viewHolder.thumbnail.setVisibility(View.VISIBLE);
		if(viewHolder.offset != 0)
		{
			viewHolder.cardView.setTranslationY( 0-viewHolder.offset );
			viewHolder.offset = 0;
		}
	}

	private void setViewHeight(View view, int height)
	{
		if (view == null)
		{
			return;
		}

		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		layoutParams.height = height;
		view.setLayoutParams(layoutParams);
	}

	@Override
	public int getItemCount()
	{
		return caseList == null ? 0 : caseList.size();
	}

	@Override
	public long getItemId(int position)
	{
		return caseList.get(position).key_id;
	}

	public Case getItem(int position)
	{
		return caseList.get(position);
	}

	public void toggleSelected(Case mCase)
	{
	//	int backgroundColor = 0xffb3e5fc;

		if(mCase.isSelected)
		{
			mCase.isSelected = false;
			removeFromMultiselectList(mCase.key_id);
		}
		else
		{
			mCase.isSelected = true;
			addToMultiselectList(mCase.key_id);

			//API21
			//highlight.setBackgroundColor(activity.getTheme().getResources().getColor(R.attr.colorAccent));
			//cardview-highlight.setBackgroundColor(activity.getResources().getColor(R.color.default_colorAccent));
		}

		if( multiselectList.size() <= 0 )
			mActionMode.finish();
		else
			mActionMode.setTitle(multiselectList.size() + " selected");

		notifyDataSetChanged();
	}

	public void clearSelected()
	{
		for(Case mCase : caseList)
		{
			//if(clearList.contains(mCase.key_id))
			{
				mCase.isSelected = false;
			}
		}

		multiselectList.clear();

		notifyDataSetChanged();
	}

	public void addToMultiselectList(long key_id)
	{
		if(!multiselectList.contains(key_id))
			multiselectList.add(key_id);
	}
	public void removeFromMultiselectList(long key_id)
	{
		if(multiselectList.contains(key_id))
			multiselectList.remove(key_id);
	}

	public void addAllToMultiselectList()
	{
		for(int i = 0; i < caseList.size(); i++)
		{
			Case mCase = caseList.get(i);

			// if not yet selected, then toggle
			if(mCase.isSelected == false)
			{
				mCase.isSelected = true;
				addToMultiselectList(mCase.key_id);
			}
		}

		mActionMode.setTitle(multiselectList.size() + " selected");
		notifyDataSetChanged();
	}
/*
	public void clearMultiselectList()
	{
		multiselectList.clear();
	}
*/
	public List<Long> getMultiselectList()
	{
		return multiselectList;
	}

	/*
	public int getMultiselectCount()
	{
		return multiselectList.size();
	}*/

	/**
	 * Stickyheaders
	 */

	/**
	 * setHeaderLiest
	 * //@param text: headers for each item in list
	 * //@param IDs
	 */
/*
	public void setHeaderList(List<String> text, List<Integer> IDs)
	{
		if(header != null)
			header.clear();

		header = text;

		notifyDataSetChanged();
	}
	*/

	public void setHeaderList(List<String> text)
	{
		if(header != null)
			header.clear();

		header = text;


		// set position numbers within group (for collapse translation Y)
		//if(group_position != null)
		//	group_position = null;

		group_position = new ArrayList<Integer>();
		long previous_header = 0;
		int pos = 0;
		for(int i = 0; i < header.size(); i++)
		{
			if(getHeaderId(i) != previous_header)
			{
				// new group
				previous_header = getHeaderId(i);
				pos = 0;

			}

			group_position.add(pos);
			pos += 1;
		}

//		notifyDataSetChanged();
	}

	public void toggleCollapseHeader(Long headerId)
	{
		// find all cases with this headerId
		for(int i = 0; i < header.size(); i++)
		{
			if(getHeaderId(i) == headerId)
			{
				// toggle Case hidden flag
				if(caseList.get(i).isHidden)
				{
					caseList.get(i).isHidden = false;
				}
				else
				{
					caseList.get(i).isHidden = true;
				}
			}
		}

		notifyDataSetChanged();
	}

	@Override
	public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent)
	{
		View view = LayoutInflater.from(parent.getContext())
				            .inflate(R.layout.sticky_header, parent, false);
		return new RecyclerView.ViewHolder(view) { };
	}

	@Override
	public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int position)
	{
		if(header != null && position >= 0 && position < header.size())
		{
			TextView textView = (TextView) holder.itemView;
			//textView.setText(String.valueOf(getItem(position)));
			textView.setText(header.get(position));
		}
		/*
		else
		{
			if(header == null)
				UtilClass.showToast(activity, "Debug: CardCaseAdapter.header is null");
		}
		*/
	}
	@Override
	public long getHeaderId(int position) {
		//return getItem(position).hashCode();
		if(header != null && position >= 0 && position < header.size() )
		{
			//return header_id.get(position);
			return header.get(position).hashCode();
		}
		else
		{
			return -1; // TODO detect invalid number?
		}
	}

	public void setOnClickListeners(View.OnClickListener cardOnClickListener, View.OnLongClickListener cardOnLongClickListener)
	{
		mOnClickListener = cardOnClickListener;
		mOnLongClickListener = cardOnLongClickListener;
	}

	/**
	 *
	 */
	public static class ViewHolder extends RecyclerView.ViewHolder
	{
		public long key_id;
		public CardView cardView;
		public View container;

		public TextView card_title;
		public TextView card_text1;
		public TextView card_text2;
		public TextView card_text3;
		public ImageView thumbnail;

		int offset = 0;

		public ViewHolder(View itemView)
		{
			super(itemView);

			cardView = (CardView) itemView.findViewById(R.id.case_card_view);

			container = itemView.findViewById(R.id.container);

			card_title = (TextView) itemView.findViewById(R.id.card_title);
			card_text1 = (TextView) itemView.findViewById(R.id.case_info1);
			card_text2 = (TextView) itemView.findViewById(R.id.case_info2);
			card_text3 = (TextView) itemView.findViewById(R.id.case_info3);
			thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
		}
	}
}

