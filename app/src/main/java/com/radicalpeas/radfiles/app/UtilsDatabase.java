package com.radicalpeas.radfiles.app;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;
import android.widget.Toast;

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
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by huanx on 8/27/2016.
 */
public class UtilsDatabase extends Activity
{

    private static final String TAG = "UtilsDatabase";


    /**
     * Creates zip file of images and JSON of database rows of select cases
     * used by CloudStorageActivity, CardCaseListActivity
     *
     * @param activity:         context of calling activity
     * @param filename:         filename of zip to be created (do not include path or extension)
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
            //showSnackbar(activity, "Unable to create local backup file.");
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
            if (selectedCaseList == null)
            {
                // get all cases
                caseCursor = activity.getContentResolver().query(CasesProvider.CASES_URI, null, null, null, null, null);
            }
            else
            {
                // get select cases
                // convert list of integers into array
                int j = 0;
                String[] selectionArgs;
                selectionArgs = new String[selectedCaseList.size()];

                String selection = CasesProvider.KEY_ROWID + " = ?";

                for (Long case_id : selectedCaseList)
                {
                    if (j > 0)
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


                // write metadata
                cases_writer.beginObject();
                cases_writer.name(CasesDB.NUM_CASES).value(caseCursor.getCount());
                cases_writer.name(CasesDB.DATE_CREATED).value(new SimpleDateFormat("yyyy-MM-dd HHmm").format(new Date()));

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null)
                {
                    cases_writer.name(CasesDB.USER).value(user.getEmail());
                    cases_writer.name(CasesDB.USER_ID).value(user.getUid());
                }
                else
                {
                    cases_writer.name(CasesDB.USER).value("ANONYMOUS");
                }

                cases_writer.name(CasesDB.DATA);

                cases_writer.beginArray();
                // loop through all cases
                do
                {
                    cases_writer.beginObject();

                    // output all case columns/fields for this case
                    for (int i = 0; i < CasesProvider.CASES_TABLE_ALL_KEYS.length; i++)
                    {
                        if (caseCursor.getString(i) != null && !caseCursor.getString(i).isEmpty())
                        {
                            cases_writer.name(CasesProvider.CASES_TABLE_ALL_KEYS[i]).value(caseCursor.getString(i));
                        }
                    }

                    // output all linked images for this case (via parent_case_id)
                    String[] image_args = {String.valueOf(caseCursor.getInt(CasesProvider.COL_ROWID))};
                    Cursor imageCursor = activity.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", image_args, CasesProvider.KEY_ORDER);

                    // loop through all images of this case
                    if (imageCursor.moveToFirst())
                    {
                        cases_writer.name("IMAGES");
                        cases_writer.beginArray();
                        do
                        {
                            cases_writer.beginObject();
                            for (int i = 0; i < CasesProvider.IMAGES_TABLE_ALL_KEYS.length; i++)
                            {
                                if (imageCursor.getString(i) != null && !imageCursor.getString(i).isEmpty())
                                {
                                    cases_writer.name(CasesProvider.IMAGES_TABLE_ALL_KEYS[i]).value(imageCursor.getString(i));
                                }
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
                cases_writer.endObject();
                cases_writer.close();
            }

            // encrypt casesJSON file, if nonblank password
            if (!password.contentEquals(""))
            {
                try
                {
                    byte[] passkey = UtilsFile.generateKey(password);
                    UtilsFile.encryptFile(passkey, casesJSON);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.d(TAG, "Unable to generate encryption key.");
                    return null;
                }
            }

            // zip image and JSON files
            String zip_filename = downloadsDir.getPath() + "/" + filename + ImportExportActivity.RCS_EXTENSION;
            zip_files_array = UtilClass.addArrayElement(zip_files_array, casesJSON.getPath());

            // create zip file.  return link to that file.
            returnFile = UtilsFile.zip(zip_files_array, zip_filename, progressHandler);

            // delete temporary files
            casesJSON.delete();

            if (progressHandler != null)
            {
                Message msg = new Message();
                msg.arg1 = ImportExportActivity.PROGRESS_MSG_FINISHED;
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


    public static File exportListsJSON(Activity activity, String filename)
    {
        // attempt to create json file
        File listsJSON = null;
        File cacheDir = activity.getCacheDir();
        File downloadsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

        try
        {
            listsJSON = new File(downloadsDir.getPath(), filename + ImportExportActivity.LIST_EXTENSION);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            UtilClass.showSnackbar(activity, "Unable to create local JSON backup files.");
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
            while (reader.hasNext())
            {
                reader.beginObject();

                while (reader.hasNext())
                {
                    insertListValues.clear();

                    String list_name = reader.nextName();

                    if (list_name.contentEquals("KEYWORDS"))
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
                    else if (list_name.contentEquals("MODALITY"))
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
                    else if (list_name.contentEquals("SECTION"))
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

    static public final Uri insertCase(Context context, ContentValues values)
    {
        // insert into local SQL database
        Uri new_case_uri = context.getContentResolver().insert(CasesProvider.CASES_URI, values);

        if (new_case_uri != null)
        {
            // get parent key information
            long newCase_KEYID = Integer.valueOf(new_case_uri.getLastPathSegment());

            // make unique id across all SQL databases and firebase
            // use time stamp and this SQL generated key_id
            SimpleDateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmm-ss");
            String timestamp_str = timestamp.format(Calendar.getInstance().getTime());

            ContentValues id_value = new ContentValues();
            id_value.clear();
            id_value.put(CasesProvider.KEY_UNIQUE_ID, timestamp_str + "_" + newCase_KEYID);

            Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, newCase_KEYID);
            context.getContentResolver().update(row_uri, id_value, null, null);

            // insert into firebase database
            values.put(CasesProvider.KEY_UNIQUE_ID, timestamp_str + "_" + newCase_KEYID);
            insertCaseToCloud(values);
        }

        return new_case_uri;
    }

    public static final int updateCase(Context context, long key_id, ContentValues values)
    {
        // update last modified date field
        // format string for database
        SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd-HHmm-ss");
        String today_date_str = db_sdf.format(Calendar.getInstance().getTime());
        values.put(CasesProvider.KEY_LAST_MODIFIED_DATE, today_date_str);

        Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
        context.getContentResolver().update(row_uri, values, null, null);

        // get unique_id
        Cursor caseCursor = context.getContentResolver().query(row_uri, null, null, null, null, null);
        if (caseCursor != null && caseCursor.moveToFirst())
        {
            String unique_id = caseCursor.getString(CasesProvider.COL_UNIQUE_ID);
            values.put(CasesProvider.KEY_UNIQUE_ID, unique_id);

            insertCaseToCloud(values);
        }

        return 0;
    }

    // need key_unique_id to be set in 'values'
    static public void insertCaseToCloud(ContentValues values)
    {
        // insert into Firebase database
        // Create Firebase storage references
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth != null)
        {
            FirebaseUser firebaseUser = mAuth.getCurrentUser();
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

            if (firebaseUser != null && firebaseDatabase != null)
            {

                DatabaseReference databaseRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid());

                // use unique_id as node of case in firebase database
                String unique_id = values.getAsString(CasesProvider.KEY_UNIQUE_ID);
                if(unique_id == null)
                {
                    // not done here because no 'context' for getContentResolver
                    Log.d(TAG, "insertCaseToCloud: unique_id not set");
                    return;
                }
                DatabaseReference caseRef = databaseRef.child("Cases/" + unique_id);

                // flattened data structure for Elastic Search indexing
                DatabaseReference caseSearchIndexRef = firebaseDatabase.getReference("Cases/" + firebaseUser.getUid() + "_" + values.getAsString(CasesProvider.KEY_UNIQUE_ID));

                if (values.getAsString(CasesProvider.KEY_DIAGNOSIS) != null)
                {
                    DatabaseReference diagnosisRef = caseRef.child(CasesProvider.KEY_DIAGNOSIS);
                    diagnosisRef.setValue(values.getAsString(CasesProvider.KEY_DIAGNOSIS));

                    caseSearchIndexRef.child(CasesProvider.KEY_DIAGNOSIS).setValue(values.getAsString(CasesProvider.KEY_DIAGNOSIS));
                }

                if (values.getAsString(CasesProvider.KEY_FINDINGS) != null)
                {
                    DatabaseReference findingsRef = caseRef.child(CasesProvider.KEY_FINDINGS);
                    findingsRef.setValue(values.getAsString(CasesProvider.KEY_FINDINGS));

                    caseSearchIndexRef.child(CasesProvider.KEY_FINDINGS).setValue(values.getAsString(CasesProvider.KEY_FINDINGS));
                }

                if (values.getAsString(CasesProvider.KEY_SECTION) != null)
                {
                    DatabaseReference sectionRef = caseRef.child(CasesProvider.KEY_SECTION);
                    sectionRef.setValue(values.getAsString(CasesProvider.KEY_SECTION));

                    caseSearchIndexRef.child(CasesProvider.KEY_SECTION).setValue(values.getAsString(CasesProvider.KEY_SECTION));
                }

                if (values.getAsString(CasesProvider.KEY_STUDY_TYPE) != null)
                {
                    DatabaseReference studytypeRef = caseRef.child(CasesProvider.KEY_STUDY_TYPE);
                    studytypeRef.setValue(values.getAsString(CasesProvider.KEY_STUDY_TYPE));

                    caseSearchIndexRef.child(CasesProvider.KEY_STUDY_TYPE).setValue(values.getAsString(CasesProvider.KEY_STUDY_TYPE));
                }

                if (values.getAsString(CasesProvider.KEY_KEYWORDS) != null)
                {
                    DatabaseReference keywordsRef = caseRef.child(CasesProvider.KEY_KEYWORDS);
                    keywordsRef.setValue(values.getAsString(CasesProvider.KEY_KEYWORDS));

                    caseSearchIndexRef.child(CasesProvider.KEY_KEYWORDS).setValue(values.getAsString(CasesProvider.KEY_KEYWORDS));
                }

                if (values.getAsString(CasesProvider.KEY_BIOPSY) != null)
                {
                    DatabaseReference biopsyRef = caseRef.child(CasesProvider.KEY_BIOPSY);
                    biopsyRef.setValue(values.getAsString(CasesProvider.KEY_BIOPSY));

                    caseSearchIndexRef.child(CasesProvider.KEY_BIOPSY).setValue(values.getAsString(CasesProvider.KEY_BIOPSY));
                }

                if (values.getAsInteger(CasesProvider.KEY_FOLLOWUP) != null)
                {
                    DatabaseReference followupRef = caseRef.child(CasesProvider.KEY_FOLLOWUP);
                    followupRef.setValue(values.getAsInteger(CasesProvider.KEY_FOLLOWUP));

                    caseSearchIndexRef.child(CasesProvider.KEY_FOLLOWUP).setValue(values.getAsString(CasesProvider.KEY_FOLLOWUP));
                }

                if (values.getAsString(CasesProvider.KEY_FOLLOWUP_COMMENT) != null)
                {
                    DatabaseReference followupCommentRef = caseRef.child(CasesProvider.KEY_FOLLOWUP_COMMENT);
                    followupCommentRef.setValue(values.getAsString(CasesProvider.KEY_FOLLOWUP_COMMENT));

                    caseSearchIndexRef.child(CasesProvider.KEY_FOLLOWUP_COMMENT).setValue(values.getAsString(CasesProvider.KEY_FOLLOWUP_COMMENT));
                }

                if (values.getAsString(CasesProvider.KEY_COMMENTS) != null)
                {
                    DatabaseReference commentsRef = caseRef.child(CasesProvider.KEY_COMMENTS);
                    commentsRef.setValue(values.getAsString(CasesProvider.KEY_COMMENTS));

                    caseSearchIndexRef.child(CasesProvider.KEY_COMMENTS).setValue(values.getAsString(CasesProvider.KEY_COMMENTS));
                }

                if (values.getAsString(CasesProvider.KEY_FAVORITE) != null)
                {
                    DatabaseReference favoriteRef = caseRef.child(CasesProvider.KEY_FAVORITE);
                    favoriteRef.setValue(values.getAsString(CasesProvider.KEY_FAVORITE));

                    caseSearchIndexRef.child(CasesProvider.KEY_FAVORITE).setValue(values.getAsString(CasesProvider.KEY_FAVORITE));
                }

                if (values.getAsInteger(CasesProvider.KEY_IMAGE_COUNT) != null)
                {
                    DatabaseReference imageCountRef = caseRef.child(CasesProvider.KEY_IMAGE_COUNT);
                    imageCountRef.setValue(values.getAsInteger(CasesProvider.KEY_IMAGE_COUNT));

                    caseSearchIndexRef.child(CasesProvider.KEY_IMAGE_COUNT).setValue(values.getAsString(CasesProvider.KEY_IMAGE_COUNT));
                }

                if (values.getAsInteger(CasesProvider.KEY_THUMBNAIL) != null)
                {
                    DatabaseReference thumbnailRef = caseRef.child(CasesProvider.KEY_THUMBNAIL);
                    thumbnailRef.setValue(values.getAsInteger(CasesProvider.KEY_THUMBNAIL));

                    caseSearchIndexRef.child(CasesProvider.KEY_THUMBNAIL).setValue(values.getAsString(CasesProvider.KEY_THUMBNAIL));
                }

                if (values.getAsString(CasesProvider.KEY_LAST_MODIFIED_DATE) != null)
                {
                    DatabaseReference modifiedDateRef = caseRef.child(CasesProvider.KEY_LAST_MODIFIED_DATE);
                    modifiedDateRef.setValue(values.getAsString(CasesProvider.KEY_LAST_MODIFIED_DATE));

                    caseSearchIndexRef.child(CasesProvider.KEY_LAST_MODIFIED_DATE).setValue(values.getAsString(CasesProvider.KEY_LAST_MODIFIED_DATE));
                }

                if (values.getAsString(CasesProvider.KEY_ORIGINAL_CREATOR) != null)
                {
                    DatabaseReference originalCreatorRef = caseRef.child(CasesProvider.KEY_ORIGINAL_CREATOR);
                    originalCreatorRef.setValue(values.getAsString(CasesProvider.KEY_ORIGINAL_CREATOR));

                    caseSearchIndexRef.child(CasesProvider.KEY_ORIGINAL_CREATOR).setValue(values.getAsString(CasesProvider.KEY_ORIGINAL_CREATOR));
                }

                if (values.getAsInteger(CasesProvider.KEY_IS_SHARED) != null)
                {
                    DatabaseReference isSharedRef = caseRef.child(CasesProvider.KEY_IS_SHARED);
                    isSharedRef.setValue(values.getAsInteger(CasesProvider.KEY_IS_SHARED));

                    caseSearchIndexRef.child(CasesProvider.KEY_IS_SHARED).setValue(values.getAsString(CasesProvider.KEY_IS_SHARED));
                }

                if (values.getAsString(CasesProvider.KEY_USER_ID) != null)
                {
                    DatabaseReference UserIdRef = caseRef.child(CasesProvider.KEY_USER_ID);
                    UserIdRef.setValue(values.getAsString(CasesProvider.KEY_USER_ID));

                    caseSearchIndexRef.child(CasesProvider.KEY_USER_ID).setValue(values.getAsString(CasesProvider.KEY_USER_ID));
                }

                if (values.getAsString(CasesProvider.KEY_UNIQUE_ID) != null)
                {
                    caseRef.child(CasesProvider.KEY_UNIQUE_ID).setValue(values.getAsString(CasesProvider.KEY_UNIQUE_ID));

                    // caseSearchIndexRef.child(CasesProvider.KEY_UNIQUE_ID).setValue(values.getAsString(CasesProvider.KEY_UNIQUE_ID));
                }

            }

        }

    }

    static public final Uri insertImage(Context context, ContentValues values)
    {
        return insertImage(context, values, false);
    }

    static public final Uri insertImage(Context context, ContentValues values, boolean isThumbnail)
    {
        // insert into local SQL database
        Uri imageUri = context.getContentResolver().insert(CasesProvider.IMAGES_URI, values);

        long newImage_KEYID = Integer.valueOf(imageUri.getLastPathSegment());

        insertImageToCloud(context, values, newImage_KEYID, isThumbnail);

        // update last modified date field of the parent case
        UtilsDatabase.updateLastModifiedDate(context, values.getAsInteger(CasesProvider.KEY_IMAGE_PARENT_CASE_ID));

        return imageUri;
    }

    static public final int updateImage(Context context, ContentValues values, long image_keyID)
    {
        Uri uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, image_keyID);
        int ret = context.getContentResolver().update(uri, values, null, null);

        insertImageToCloud(context, values, image_keyID);

        // update last modified date field of the parent case
        UtilsDatabase.updateLastModifiedDate(context, values.getAsInteger(CasesProvider.KEY_IMAGE_PARENT_CASE_ID));

        return ret;
    }

    static public int insertImageToCloud(Context context, ContentValues values, long image_keyID)
    {
        // default isThumbnail = false;
        return insertImageToCloud(context, values, image_keyID, false);
    }

    static public int insertImageToCloud(Context context, ContentValues values, long image_keyID, final boolean isThumbnail)
    {
        // insert into Firebase database
        // Create Firebase storage references
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth != null)
        {
            FirebaseUser firebaseUser = mAuth.getCurrentUser();
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

            if (firebaseUser != null && firebaseDatabase != null)
            {
                Uri parent_case_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, values.getAsInteger(CasesProvider.KEY_IMAGE_PARENT_CASE_ID));
                Cursor caseCursor = context.getContentResolver().query(parent_case_uri, null, null, null, null, null);

                if (caseCursor != null && caseCursor.moveToFirst())
                {
                    String unique_id = caseCursor.getString(CasesProvider.COL_UNIQUE_ID);

                    //    DatabaseReference databaseRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid());
                    //    DatabaseReference imageRef = databaseRef.child("Cases/" + values.getAsInteger(CasesProvider.KEY_IMAGE_PARENT_CASE_ID) + "/Images/" + newImage_KEYID);
                    final DatabaseReference databaseRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid());

                    // use unique_id as node of case in firebase database
                    final DatabaseReference caseRef = databaseRef.child("Cases/" + unique_id);
                    final DatabaseReference imageRef = caseRef.child("Images/" + image_keyID);

                    final DatabaseReference caseSearchIndexRef = firebaseDatabase.getReference("Cases/" + firebaseUser.getUid() + "_" + unique_id);
                    final DatabaseReference imageSearchIndexRef = caseSearchIndexRef.child("Images/" + image_keyID);

                    if (values.getAsString(CasesProvider.KEY_IMAGE_FILENAME) != null)
                    {
                        DatabaseReference filenameRef = imageRef.child(CasesProvider.KEY_IMAGE_FILENAME);
                        filenameRef.setValue(values.getAsString(CasesProvider.KEY_IMAGE_FILENAME));
                    }

                    if (values.getAsString(CasesProvider.KEY_IMAGE_CAPTION) != null)
                    {
                        DatabaseReference captionRef = imageRef.child(CasesProvider.KEY_IMAGE_CAPTION);
                        captionRef.setValue(values.getAsString(CasesProvider.KEY_IMAGE_CAPTION));

                        // only caption is searchable for Elastic Search
                        imageSearchIndexRef.child(CasesProvider.KEY_IMAGE_CAPTION).setValue(values.getAsString(CasesProvider.KEY_IMAGE_CAPTION));
                    }

                    if (values.getAsInteger(CasesProvider.KEY_ORDER) != null)
                    {
                        DatabaseReference orderRef = imageRef.child(CasesProvider.KEY_ORDER);
                        orderRef.setValue(values.getAsInteger(CasesProvider.KEY_ORDER));
                    }


                    // upload image files to cloud db
                    // put in userID/pictures folder


                    // set up Firebase storage reference
                    final FirebaseStorage mStorage = FirebaseStorage.getInstance();
                    final StorageReference mStorageRef = mStorage.getReferenceFromUrl("gs://rad-files.appspot.com");

                    final File picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    final File filePath = new File(picturesDir + "/" + values.getAsString(CasesProvider.KEY_IMAGE_FILENAME)); //values.getAsString(CasesProvider.KEY_IMAGE_FILENAME)
                    final StorageReference storageImages = mStorageRef.child(firebaseUser.getUid() + "/pictures/" + filePath.getName());

                    storageImages.getDownloadUrl().addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception exception)
                        {
                            // file does not exist, so upload image file

                            // open FileInputStream and upload to Firebase server
                            try
                            {
                                // Create the file metadata
                                StorageMetadata metadata = new StorageMetadata.Builder()
                                        .setContentType("image/jpeg")
                                        .build();

                                final FileInputStream fi = new FileInputStream(filePath);
                                UploadTask image_uploadTask = storageImages.putStream(fi, metadata);

                                // limit of 128 total asynctasks
                                List tasks = mStorageRef.getActiveUploadTasks();
                                while (tasks.size() > 100)
                                {
                                    Thread.sleep(500);
                                    tasks = mStorageRef.getActiveUploadTasks();
                                }
                                // Register observers to listen for when the download is done or if it fails
                                image_uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
                                {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                                    {
                                        // Handle successful uploads on complete
                                        Uri downloadUrl = taskSnapshot.getMetadata().getDownloadUrl();

                                        // store in db
                                        imageRef.child("image_URL").setValue(downloadUrl.getScheme() + ":" + downloadUrl.getEncodedSchemeSpecificPart());
                                        if (isThumbnail)
                                        {
                                            caseRef.child("thumbnail_URL").setValue(downloadUrl.getScheme() + ":" + downloadUrl.getEncodedSchemeSpecificPart());
                                        }
                                        Log.d(TAG, "Successfully uploaded image: " + filePath.getName());

                                    }
                                }).addOnFailureListener(new OnFailureListener()
                                {
                                    @Override
                                    public void onFailure(@NonNull Exception exception)
                                    {
                                        // Handle unsuccessful uploads
                                        //				showSnackbar(activity, "Failed uploading images.");

//							showSnackbar(context, "Failed uploading images.");
                                        Log.d(TAG, "Failed uploading image: " + filePath.getName());
                                    }
                                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
                                {
                                    @Override
                                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot)
                                    {
                                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                        System.out.println("Upload is " + progress + "% done");
                                    }
                                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>()
                                {
                                    @Override
                                    public void onPaused(UploadTask.TaskSnapshot taskSnapshot)
                                    {
                                        System.out.println("Upload is paused");
                                    }
                                });

                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                                Log.d(TAG, "IOException: " + e.getMessage());

                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                                Log.d(TAG, "InterruptedException: " + e.getMessage());

                            }
                        } // no file found
                    }); // check if image file is already in storage

                } // caseCursor to find unique_id not null
            } // firebase user
        } // auth


        return 0;
    }


    // used when changing Images table
    public static void updateLastModifiedDate(Context context, long key_id)
    {
        // put data into "values" for database insert/update
        ContentValues values = new ContentValues();

        // format string for database
        SimpleDateFormat db_sdf = new SimpleDateFormat("yyyy-MM-dd-HHmm-ss");
        String today_date_str = db_sdf.format(Calendar.getInstance().getTime());
        values.put(CasesProvider.KEY_LAST_MODIFIED_DATE, today_date_str);

        // Update the existing case in the database
        //Uri row_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);
        //activity.getContentResolver().update(row_uri, values, null, null);

        UtilsDatabase.updateCase(context, key_id, values);
    }

    public static void deleteCase(Context context, long key_id)
    {
        // default cloud sync = true
        deleteCase(context, key_id, true);
    }

    public static void deleteCase(Context context, long key_id, boolean cloudSync)
    {
        // get unique_id
        String unique_id;
        // get db row of clicked case
        Uri case_delete_uri = ContentUris.withAppendedId(CasesProvider.CASES_URI, key_id);


        if (cloudSync)
        {
            Cursor caseCursor = context.getContentResolver().query(case_delete_uri, null, null, null, null, null);

            if (caseCursor != null && caseCursor.moveToFirst())
            {
                unique_id = caseCursor.getString(CasesProvider.COL_UNIQUE_ID);

                // delete from Firebase database
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                if (mAuth != null)
                {
                    final FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

                    if (firebaseUser != null && firebaseDatabase != null)
                    {
                        final DatabaseReference databaseRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid());

                        final Query caseQuery = databaseRef.child("Cases").orderByChild(CasesProvider.KEY_UNIQUE_ID).equalTo(unique_id);

                        caseQuery.addListenerForSingleValueEvent(new ValueEventListener()
                        {
                            @Override
                            public void onDataChange(DataSnapshot parentDataSnapshot)
                            {
                                DataSnapshot dataSnapshot = parentDataSnapshot.getChildren().iterator().next();

                                String cloud_key_id = dataSnapshot.getKey();

                                DatabaseReference caseRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid() + "/Cases/" + cloud_key_id);
                                caseRef.removeValue();

                                // 'search index database'
                                DatabaseReference caseSearchIndexRef = firebaseDatabase.getReference("Cases/" + firebaseUser.getUid() + "_" + cloud_key_id);
                                caseSearchIndexRef.removeValue();

                                Log.d(TAG, "Deleted case: " + cloud_key_id);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError)
                            {

                            }
                        });


                    } // firebase user
                } // mAuth not null
            } // cursor not null
        } // cloudSync = true

        // delete local SQL row
        context.getContentResolver().delete(case_delete_uri, null, null);

        // delete all linked images files
        Cursor image_cursor = context.getContentResolver().query(CasesProvider.IMAGES_URI, null, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)}, CasesProvider.KEY_ORDER);
        File imageFile = null;
        if (image_cursor.moveToFirst())
        {
            do
            {
                //           imageFile = new File(image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME));
                //           imageFile.delete();

                UtilsDatabase.deleteCaseImageFile(context, image_cursor.getString(CasesProvider.COL_IMAGE_FILENAME), cloudSync);
            } while (image_cursor.moveToNext());
        }
        image_cursor.close();

        // delete all child rows from IMAGES table, by parent case key_id
        context.getContentResolver().delete(CasesProvider.IMAGES_URI, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ?", new String[]{String.valueOf(key_id)});


    }

        // single image deleted from ImageGridView or CaseEditActivity

    public static void deleteImage(Context context, long case_key_id, final String image_filename)
    {
        // delete from IMAGES table, select by case key_id and fileNAME
        String[] selArgs = {String.valueOf(case_key_id), image_filename};
        context.getContentResolver().delete(CasesProvider.IMAGES_URI, CasesProvider.KEY_IMAGE_PARENT_CASE_ID + " = ? AND " + CasesProvider.KEY_IMAGE_FILENAME + " = ?", selArgs);


        //Uri row_uri = ContentUris.withAppendedId(CasesProvider.IMAGES_URI, image_key_id);
        //context.getContentResolver().delete(row_uri, null, null);

        // delete from Firebase database
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth != null)
        {
            FirebaseUser firebaseUser = mAuth.getCurrentUser();
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

            if (firebaseUser != null && firebaseDatabase != null)
            {
                //DatabaseReference databaseRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid());

                // use SQL key _id as node of case in firebase database
                //        DatabaseReference imageRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid() + "/" + case_key_id + "/Images/" + image_key_id);
                //        imageRef.removeValue();
                //        DatabaseReference imageSearchIndexRef = firebaseDatabase.getReference("Cases/" + firebaseUser.getUid() + "_" + case_key_id + "/Images/" + image_key_id);
                //        imageSearchIndexRef.removeValue();


                ////
                DatabaseReference databaseRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid());
                // use SQL key _id as node of case in firebase database
                DatabaseReference caseRef = databaseRef.child("Cases/" + case_key_id);
                DatabaseReference caseImagesRef = caseRef.child("Images");

                caseImagesRef.addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        Iterable<DataSnapshot> imageList = dataSnapshot.getChildren();

                        for (DataSnapshot imageSnapshot : imageList)
                        {
                            final String filename = (String) imageSnapshot.child(CasesProvider.KEY_IMAGE_FILENAME).getValue();

                            if (filename.equals(image_filename))
                            {
                                // found the image to delete
                                imageSnapshot.getRef().removeValue();
                                break;
                            }

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {

                    }
                });
                ////

                deleteCaseImageFile(context, image_filename); // can probably get filename from imageRef databaseRef

                // remove thumbnail_URL link in case it needs to be refreshed
                DatabaseReference thumbnailRef = firebaseDatabase.getReference("users/" + firebaseUser.getUid() + "/" + case_key_id + "thumbnail_URL");
                thumbnailRef.removeValue();
            }
        }

    }

    public static void deleteCaseImageFile(Context context, String image_filename)
    {
        // default cloudSync = true
        deleteCaseImageFile(context, image_filename, true);
    }

    public static void deleteCaseImageFile(Context context, String image_filename, boolean cloudSync)
    {
        if (cloudSync)
        {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (mAuth != null)
            {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();

                if (firebaseUser != null)
                {
                    // set up Firebase storage reference
                    final FirebaseStorage mStorage = FirebaseStorage.getInstance();
                    final StorageReference mStorageRef = mStorage.getReferenceFromUrl("gs://rad-files.appspot.com");

                    final File picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    final File filePath = new File(picturesDir + "/" + image_filename); //values.getAsString(CasesProvider.KEY_IMAGE_FILENAME)
                    final StorageReference storageImage = mStorageRef.child(firebaseUser.getUid() + "/pictures/" + filePath.getName());

                    // Delete the file
                    storageImage.delete();
                    Log.d(TAG, "Deleting image file: " + firebaseUser.getUid() + "/pictures/" + filePath.getName());
                }
            }
        }

        // delete local image file: caseImage.getFilename()
        final File picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        final File localFilePath = new File(picturesDir + "/" + image_filename);
        if (localFilePath != null && localFilePath.exists())
        {
            localFilePath.delete();
        }
    }


    public static String getDataSnapshotValue(DataSnapshot data, String key)
    {
        if (data.child(key) == null || data.child(key).getValue() == null)
        {
            return null;
        }
        else
        {
            return data.child(key).getValue().toString();
        }
    }

    public static int getDataSnapshotInteger(DataSnapshot data, String key)
    {
        if (data.child(key) == null || data.child(key).getValue() == null)
        {
            return 0;
        }
        else
        {
            return Integer.valueOf(data.child(key).getValue().toString());
        }
    }
}