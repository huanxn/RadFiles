package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;

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
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huan on 10/21/2014.
 */
public class Case
{
	@SerializedName(CasesProvider.KEY_ROWID)
	public long key_id;

	@SerializedName(CasesProvider.KEY_CASE_NUMBER)
	//@SerializedName("PATIENT_ID")
	public String case_id;

	@SerializedName(CasesProvider.KEY_DIAGNOSIS)
	public String diagnosis;

	@SerializedName(CasesProvider.KEY_FINDINGS)
	public String findings;

	@SerializedName(CasesProvider.KEY_SECTION)
	public String section;

	@SerializedName(CasesProvider.KEY_BIOPSY)
	public String biopsy;

	@SerializedName(CasesProvider.KEY_FOLLOWUP)
	public int followup;

	@SerializedName(CasesProvider.KEY_FOLLOWUP_COMMENT)
	public String followup_comment;

	@SerializedName(CasesProvider.KEY_KEYWORDS)
	public String key_words;

	@SerializedName(CasesProvider.KEY_COMMENTS)
	public String comments;

	@SerializedName(CasesProvider.KEY_STUDY_TYPE)
	//@SerializedName("STUDYTYPE")
	public String study_type;

	@SerializedName(CasesProvider.KEY_STUDY_DATE)
	//@SerializedName("DATE")
	public String db_date_str;

	@SerializedName(CasesProvider.KEY_IMAGE_COUNT)
	public int image_count;

	@SerializedName(CasesProvider.KEY_THUMBNAIL)
	public int thumbnail;
	public String thumbnail_filename;

	@SerializedName(CasesProvider.KEY_FAVORITE)
	public String favorite;

	@SerializedName(CasesProvider.KEY_CLINICAL_HISTORY)
	public String clinical_history;

	@SerializedName(CasesProvider.KEY_LAST_MODIFIED_DATE)
	public String last_modified_date;

	@SerializedName(CasesProvider.KEY_USER_ID)
	public String userID;

	@SerializedName(CasesProvider.KEY_ORIGINAL_CREATOR)
	public String original_creator;

	@SerializedName(CasesProvider.KEY_IS_SHARED)
	public int is_shared;

	@SerializedName(CasesProvider.KEY_UNIQUE_ID)
	public String unique_id;

	@SerializedName(CasesProvider.KEY_CASE_INFO2)
	public String case_info2;

	@SerializedName(CasesProvider.KEY_CASE_INFO3)
	public String case_info3;

	@SerializedName(CasesProvider.KEY_CASE_INFO4)
	public int case_info4;

	@SerializedName(CasesProvider.KEY_CASE_INFO5)
	public int case_info5;

	@SerializedName("IMAGES")
	public List<CaseImage> caseImageList;

	public boolean isSelected = false;
	public boolean isHidden = false;

	public Case()
	{
		caseImageList = new ArrayList<CaseImage>();
	}

	public boolean setCase(Activity context, long key_id)
	{
		this.key_id = key_id;

		// get db row of clicked case
		Uri uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
		Cursor caseCursor = context.getContentResolver().query(uri, null, null, null, null, null);

		if (caseCursor != null && caseCursor.moveToFirst())
		{
			setCaseFromCursor(context, caseCursor);

			caseCursor.close();

			return true;
		}
		else
		{
			return false;	// failure
		}

	}

	public void setCaseFromCursor(Activity context, Cursor caseCursor)
	{
		unique_id = caseCursor.getString(CasesProvider.COL_UNIQUE_ID);

		case_id = caseCursor.getString(CasesProvider.COL_CASE_NUMBER);
		diagnosis = caseCursor.getString(CasesProvider.COL_DIAGNOSIS);
		findings = caseCursor.getString(CasesProvider.COL_FINDINGS);
		section = caseCursor.getString(CasesProvider.COL_SECTION);
		study_type = caseCursor.getString(CasesProvider.COL_STUDY_TYPE);
		key_words = caseCursor.getString(CasesProvider.COL_KEYWORDS);
		biopsy = caseCursor.getString(CasesProvider.COL_BIOPSY);
		followup = caseCursor.getInt(CasesProvider.COL_FOLLOWUP);
		followup_comment = caseCursor.getString(CasesProvider.COL_FOLLOWUP_COMMENT);
		comments = caseCursor.getString(CasesProvider.COL_COMMENTS);
		favorite = caseCursor.getString(CasesProvider.COL_FAVORITE);
		image_count = caseCursor.getInt(CasesProvider.COL_IMAGE_COUNT);
		thumbnail = caseCursor.getInt(CasesProvider.COL_THUMBNAIL);
		last_modified_date = caseCursor.getString(CasesProvider.COL_LAST_MODIFIED_DATE);

		db_date_str = caseCursor.getString(CasesProvider.COL_DATE);					// not included in firebase
		clinical_history = caseCursor.getString(CasesProvider.COL_CLINICAL_HISTORY);// not included in firebase

		userID = caseCursor.getString(CasesProvider.COL_USER_ID);
		original_creator = caseCursor.getString(CasesProvider.COL_ORIGINAL_CREATOR);
		is_shared = caseCursor.getInt(CasesProvider.COL_IS_SHARED);


		if(image_count > 0)
		{
			String[] image_args = {String.valueOf(key_id)};
			Cursor imageCursor = context.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);
			if (imageCursor != null && imageCursor.moveToFirst())
			{
				caseImageList = new ArrayList<CaseImage>();

				do
				{
					CaseImage caseImage = new CaseImage();
					caseImage.set_id(imageCursor.getInt(CasesProvider.COL_ROWID));
					caseImage.setParent_id(imageCursor.getInt(CasesProvider.COL_IMAGE_PARENT_CASE_ID));
					caseImage.setFilename(imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME));
					caseImage.setCaption(imageCursor.getString(CasesProvider.COL_IMAGE_CAPTION));
					caseImage.setDetails(imageCursor.getString(CasesProvider.COL_IMAGE_DETAILS));

					caseImageList.add(caseImage);

				} while (imageCursor.moveToNext());

				imageCursor.close();

				if (thumbnail >= 0)
				{
					thumbnail_filename = caseImageList.get(thumbnail).getFilename();
				}
				else
				{
					// default is first image used as thumbnail
					thumbnail_filename = caseImageList.get(0).getFilename();
				}

			}


		}

		// syncCaseWithCloud(context);

	}

	//todo create method to set new case to SQL from a mCase (as currently done in CaseCardAdapter.java)

	// uses this cases key_id and unique_id
	public void syncCaseWithCloud(final Activity context)
	{
		// get data fromFirebase database
		final FirebaseAuth mAuth = FirebaseAuth.getInstance();
		if (mAuth != null)
		{
			final FirebaseUser firebaseUser = mAuth.getCurrentUser();
			final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

			if (firebaseUser != null && firebaseDatabase != null)
			{
				DatabaseReference databaseRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid());

				// get node of case in firebase database, based on unique_ID
				final Query caseQuery = databaseRef.child("Cases").orderByChild(CasesProvider.KEY_UNIQUE_ID).equalTo(unique_id);

				caseQuery.addListenerForSingleValueEvent(new ValueEventListener()
				{
					@Override
					public void onDataChange(DataSnapshot parentDataSnapshot)
					{
						DataSnapshot dataSnapshot = parentDataSnapshot.getChildren().iterator().next();

						ContentValues values = new ContentValues();
						values.clear();

						String cloud_data;

						boolean madeChanges = false;

						cloud_data = UtilsDatabase.getDataSnapshotValue(dataSnapshot, CasesProvider.KEY_DIAGNOSIS);
						if(!UtilClass.compare(diagnosis, cloud_data))
						{
							diagnosis = cloud_data;
							values.put(CasesProvider.KEY_DIAGNOSIS, cloud_data);
							madeChanges = true;
						}

						cloud_data = UtilsDatabase.getDataSnapshotValue(dataSnapshot, CasesProvider.KEY_FINDINGS);
						if(!UtilClass.compare(findings, cloud_data))
						{
							findings = cloud_data;
							values.put(CasesProvider.KEY_FINDINGS, cloud_data);
							madeChanges = true;
						}

						cloud_data = UtilsDatabase.getDataSnapshotValue(dataSnapshot, CasesProvider.KEY_SECTION);
						if(!UtilClass.compare(section, cloud_data))
						{
							section = cloud_data;
							values.put(CasesProvider.KEY_SECTION, cloud_data);
							madeChanges = true;
						}

						cloud_data = UtilsDatabase.getDataSnapshotValue(dataSnapshot, CasesProvider.KEY_KEYWORDS);
						if(!UtilClass.compare(key_words, cloud_data))
						{
							key_words = cloud_data;
							values.put(CasesProvider.KEY_KEYWORDS, cloud_data);
							madeChanges = true;
						}

						cloud_data = UtilsDatabase.getDataSnapshotValue(dataSnapshot, CasesProvider.KEY_COMMENTS);
						if(!UtilClass.compare(comments, cloud_data))
						{
							comments = cloud_data;
							values.put(CasesProvider.KEY_COMMENTS, cloud_data);
							madeChanges = true;
						}

						cloud_data = UtilsDatabase.getDataSnapshotValue(dataSnapshot, CasesProvider.KEY_FOLLOWUP_COMMENT);
						if(!UtilClass.compare(followup_comment, cloud_data))
						{
							followup_comment = cloud_data;
							values.put(CasesProvider.KEY_FOLLOWUP_COMMENT, cloud_data);
							madeChanges = true;
						}

						int cloud_data_integer = UtilsDatabase.getDataSnapshotInteger(dataSnapshot, CasesProvider.KEY_FOLLOWUP);
						if(followup != cloud_data_integer)
						{
							followup = cloud_data_integer;
							values.put(CasesProvider.KEY_FOLLOWUP, cloud_data_integer);
							madeChanges = true;
						}

						cloud_data = UtilsDatabase.getDataSnapshotValue(dataSnapshot, CasesProvider.KEY_BIOPSY);
						if(!UtilClass.compare(biopsy, cloud_data))
						{
							biopsy = cloud_data;
							values.put(CasesProvider.KEY_BIOPSY, cloud_data);
							madeChanges = true;
						}

						//todo sync favorite, thumbnail, clinical history

						// loop through imageList (all images in firebase database)
						for(DataSnapshot imageSnapshot: dataSnapshot.child("Images").getChildren())
						{
							final String filename = UtilsDatabase.getDataSnapshotValue(imageSnapshot, CasesProvider.KEY_IMAGE_FILENAME);
							boolean found = false;

							// compare to caseImageList (images in the local SQL database)
							for(CaseImage caseImage: caseImageList)
							{
								if(filename.equals(caseImage.getFilename()))
								{
									found = true;
									caseImage.foundInCloudDatabase = true;
									break;
								}
							}

							if(!found)
							{
								// not found in local SQL database.  add firebase data to local SQL database
								int new_image_index = image_count;	// row order, put at end
								String caption = UtilsDatabase.getDataSnapshotValue(imageSnapshot, CasesProvider.KEY_IMAGE_CAPTION);
								String details = UtilsDatabase.getDataSnapshotValue(imageSnapshot, CasesProvider.KEY_IMAGE_DETAILS);

								// update image count
								image_count = image_count + 1;
								values.put(CasesProvider.KEY_IMAGE_COUNT, image_count);
								madeChanges = true;

								//store in image table
								ContentValues imageValues = new ContentValues();
								imageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, key_id);
								imageValues.put(CasesProvider.KEY_IMAGE_FILENAME, filename);
								imageValues.put(CasesProvider.KEY_IMAGE_CAPTION, caption);
								imageValues.put(CasesProvider.KEY_IMAGE_DETAILS, details);
								imageValues.put(CasesProvider.KEY_ORDER, new_image_index);      // set order to display images.  new files last.  //todo user reodering

								// insert into local SQL database
								Uri image_row_uri = context.getContentResolver().insert(CasesProvider.IMAGES_URI, imageValues);
								//Uri row_uri = UtilsDatabase.insertImage(context, imageValues);
								long new_image_id = Long.parseLong(image_row_uri.getLastPathSegment());

								// add to this case instatiation
								CaseImage caseImage = new CaseImage();
								caseImage.set_id((int)new_image_id);
								caseImage.setParent_id((int)key_id);
								caseImage.setFilename(filename);
								caseImage.setCaption(caption);
								caseImage.setDetails(details);

								caseImage.foundInCloudDatabase = true;

								caseImageList.add(caseImage);


								//download image file
								// set up Firebase storage reference
								final FirebaseStorage mStorage = FirebaseStorage.getInstance();
								final StorageReference mStorageRef = mStorage.getReferenceFromUrl("gs://rad-files.appspot.com");

								final File picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
								final File localFilePath = new File(picturesDir + "/" + filename);
								final StorageReference storageImage = mStorageRef.child(firebaseUser.getUid() + "/pictures/" + filename);

								File localFile = new File(localFilePath.getPath());//File.createTempFile("images", "jpg");

								storageImage.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {

									@Override
									public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
									{
										// Local image file has been created
										UtilClass.showToast(context, "Downloaded file " + filename);

										//todo refresh casedetail view
										//todo or show snackbar with refresh button

									}
								}).addOnFailureListener(new OnFailureListener() {
									@Override
									public void onFailure(@NonNull Exception exception) {
										// Handle any errors
										UtilClass.showToast(context, "Unable to download file " + filename);
									}
								});
							}
						}

						// loop through local case images again
						// if image was not found in cloud database, then it is deleted from local database
						for(CaseImage caseImage: caseImageList)
						{
							if(caseImage.foundInCloudDatabase == false)
							{
								// delete from SQL database
								//UtilsDatabase.deleteImage(context, key_id, caseImage.get_id(), caseImage.getFilename());
								Uri row_uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, caseImage.get_id());
								context.getContentResolver().delete(row_uri, null, null);

								// delete local image file: caseImage.getFilename()
								final File picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
								final File localFilePath = new File(picturesDir + "/" + caseImage.getFilename());
								localFilePath.delete();

								// update image_count
								image_count = image_count - 1;
								values.put(CasesProvider.KEY_IMAGE_COUNT, image_count);
								madeChanges = true;
							}
						}

						if(madeChanges)
						{
							Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
							context.getContentResolver().update(row_uri, values, null, null);

							//todo notifydatachange listener
							((CaseDetailActivity) context).populateFields();
							UtilClass.showSnackbar(context, "Updated info from cloud");

						}
					}

					@Override
					public void onCancelled(DatabaseError databaseError)
					{

					}
				});



			}
		}

		/*
		case_id = caseCursor.getString(CasesProvider.COL_CASE_NUMBER);
		diagnosis = caseCursor.getString(CasesProvider.COL_DIAGNOSIS);
		findings = caseCursor.getString(CasesProvider.COL_FINDINGS);
		section = caseCursor.getString(CasesProvider.COL_SECTION);
		study_type = caseCursor.getString(CasesProvider.COL_STUDY_TYPE);
		key_words = caseCursor.getString(CasesProvider.COL_KEYWORDS);
		biopsy = caseCursor.getString(CasesProvider.COL_BIOPSY);
		followup = caseCursor.getInt(CasesProvider.COL_FOLLOWUP);
		followup_comment = caseCursor.getString(CasesProvider.COL_FOLLOWUP_COMMENT);
		comments = caseCursor.getString(CasesProvider.COL_COMMENTS);
		favorite = caseCursor.getString(CasesProvider.COL_FAVORITE);
		image_count = caseCursor.getInt(CasesProvider.COL_IMAGE_COUNT);;
		thumbnail = caseCursor.getInt(CasesProvider.COL_THUMBNAIL);
		last_modified_date = caseCursor.getString(CasesProvider.COL_LAST_MODIFIED_DATE);

		db_date_str = caseCursor.getString(CasesProvider.COL_DATE);					// not included in firebase
		clinical_history = caseCursor.getString(CasesProvider.COL_CLINICAL_HISTORY);// not included in firebase

		userID = caseCursor.getString(CasesProvider.COL_USER_ID);
		original_creator = caseCursor.getString(CasesProvider.COL_ORIGINAL_CREATOR);
		is_shared = caseCursor.getInt(CasesProvider.COL_IS_SHARED);

		if(image_count > 0)
		{
			String[] image_args = {String.valueOf(key_id)};
			Cursor imageCursor = context.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);
			if (imageCursor != null && imageCursor.moveToFirst())
			{
				caseImageList = new ArrayList<CaseImage>();

				do
				{
					CaseImage caseImage = new CaseImage();
					caseImage.set_id(imageCursor.getInt(CasesProvider.COL_ROWID));
					caseImage.setParent_id(imageCursor.getInt(CasesProvider.COL_IMAGE_PARENT_CASE_ID));
					caseImage.setFilename(imageCursor.getString(CasesProvider.COL_IMAGE_FILENAME));
					caseImage.setCaption(imageCursor.getString(CasesProvider.COL_IMAGE_CAPTION));
					caseImage.setDetails(imageCursor.getString(CasesProvider.COL_IMAGE_DETAILS));

					caseImageList.add(caseImage);

				} while (imageCursor.moveToNext());

				imageCursor.close();

				if (thumbnail >= 0)
				{
					thumbnail_filename = caseImageList.get(thumbnail).getFilename();
				}
				else
				{
					// default is first image used as thumbnail
					thumbnail_filename = caseImageList.get(0).getFilename();
				}

			}


		}
		*/

	}


}
