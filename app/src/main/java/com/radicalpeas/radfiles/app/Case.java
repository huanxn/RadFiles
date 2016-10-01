package com.radicalpeas.radfiles.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.widget.EditText;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

	@SerializedName(CasesProvider.KEY_SECTION)
	public String section;

	@SerializedName(CasesProvider.KEY_FINDINGS)
	public String findings;

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

	@SerializedName(CasesProvider.KEY_CASE_INFO1)
	public String case_info1;

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

	public boolean setCase(Context context, long key_id)
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

	public void setCaseFromCursor(Context context, Cursor caseCursor)
	{
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

		setCaseFromCloud(context, key_id);

	}


	public void setCaseFromCloud(final Context context, final long key_id)
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
				// use SQL key _id as node of case in firebase database
				DatabaseReference caseRef = databaseRef.child("Cases/" + key_id);
				DatabaseReference caseImagesRef = caseRef.child("Images");

				caseRef.child(CasesProvider.KEY_DIAGNOSIS).addValueEventListener(new ValueEventListener()
				{
					@Override
					public void onDataChange(DataSnapshot dataSnapshot)
					{
						String cloud_data = (String) dataSnapshot.getValue();
						if(diagnosis != cloud_data )
						{
							diagnosis = cloud_data;
							// put data into "values" for database insert/update
							ContentValues values = new ContentValues();
							values.put(CasesProvider.KEY_DIAGNOSIS, diagnosis);

							Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
							context.getContentResolver().update(row_uri, values, null, null);

						}
					}

					@Override
					public void onCancelled(DatabaseError databaseError)
					{

					}
				});

				caseRef.child(CasesProvider.KEY_FINDINGS).addValueEventListener(new ValueEventListener()
				{
					@Override
					public void onDataChange(DataSnapshot dataSnapshot)
					{
						String cloud_data = (String) dataSnapshot.getValue();
						if(findings != cloud_data )
						{
							findings = cloud_data;
							// put data into "values" for database insert/update
							ContentValues values = new ContentValues();
							values.put(CasesProvider.KEY_FINDINGS, findings);

							Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
							context.getContentResolver().update(row_uri, values, null, null);

						}
					}

					@Override
					public void onCancelled(DatabaseError databaseError)
					{

					}
				});

				caseRef.child(CasesProvider.KEY_COMMENTS).addValueEventListener(new ValueEventListener()
				{
					@Override
					public void onDataChange(DataSnapshot dataSnapshot)
					{
						String cloud_data = (String) dataSnapshot.getValue();
						if(comments != cloud_data )
						{
							comments = cloud_data;
							// put data into "values" for database insert/update
							ContentValues values = new ContentValues();
							values.put(CasesProvider.KEY_COMMENTS, comments);

							Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
							context.getContentResolver().update(row_uri, values, null, null);

						}
					}

					@Override
					public void onCancelled(DatabaseError databaseError)
					{

					}
				});

				caseRef.child(CasesProvider.KEY_FOLLOWUP_COMMENT).addValueEventListener(new ValueEventListener()
				{
					@Override
					public void onDataChange(DataSnapshot dataSnapshot)
					{
						String cloud_data = (String) dataSnapshot.getValue();
						if(followup_comment != cloud_data )
						{
							followup_comment = cloud_data;
							// put data into "values" for database insert/update
							ContentValues values = new ContentValues();
							values.put(CasesProvider.KEY_FOLLOWUP_COMMENT, followup_comment);

							if(followup_comment != null && !followup_comment.isEmpty())
							{
								values.put(CasesProvider.KEY_FOLLOWUP, 1);	// set true
							}
							else
							{
								values.put(CasesProvider.KEY_FOLLOWUP, 0);	// set false
							}

							Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
							context.getContentResolver().update(row_uri, values, null, null);

						}
					}

					@Override
					public void onCancelled(DatabaseError databaseError)
					{

					}
				});

				caseRef.child(CasesProvider.KEY_BIOPSY).addValueEventListener(new ValueEventListener()
				{
					@Override
					public void onDataChange(DataSnapshot dataSnapshot)
					{
						String cloud_data = (String) dataSnapshot.getValue();
						if(biopsy != cloud_data )
						{
							biopsy = cloud_data;
							// put data into "values" for database insert/update
							ContentValues values = new ContentValues();
							values.put(CasesProvider.KEY_BIOPSY, biopsy);

							Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
							context.getContentResolver().update(row_uri, values, null, null);

						}
					}

					@Override
					public void onCancelled(DatabaseError databaseError)
					{

					}
				});

				//// TODO: 9/26/2016 section, keywords

				caseImagesRef.addListenerForSingleValueEvent(new ValueEventListener()
				{
					@Override
					public void onDataChange(DataSnapshot dataSnapshot)
					{
						Iterable<DataSnapshot> imageList = dataSnapshot.getChildren();

						for(DataSnapshot imageSnapshot: imageList)
						{
							final String filename = (String) imageSnapshot.child(CasesProvider.KEY_IMAGE_FILENAME).getValue();
							boolean found = false;

							for(CaseImage caseImage: caseImageList)
							{
								if(filename.equals(caseImage.getFilename()))
								{
									found = true;
									break;
								}
							}

							if(found == false)
							{
								// not found in local SQL database.  add firebase data to local SQL database
								int new_image_index = image_count;	// row order, put at end
								image_count += 1;

								//store in image table
								ContentValues imageValues = new ContentValues();
								imageValues.put(CasesProvider.KEY_IMAGE_PARENT_CASE_ID, key_id);
								imageValues.put(CasesProvider.KEY_IMAGE_FILENAME, filename);
								imageValues.put(CasesProvider.KEY_ORDER, new_image_index);      // set order to display images.  new files last.  //todo user reodering

								// insert into local SQL database
								Uri row_uri = context.getContentResolver().insert(CasesProvider.IMAGES_URI, imageValues);
								//Uri row_uri = UtilsDatabase.insertImage(context, imageValues);
								long new_image_id = Long.parseLong(row_uri.getLastPathSegment());

								// add to this case instatiation
								CaseImage caseImage = new CaseImage();
								caseImage.set_id((int)new_image_id);
								caseImage.setParent_id((int)key_id);
								caseImage.setFilename(filename);
								//caseImage.setCaption(caption);
								//caseImage.setDetails(details);

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
