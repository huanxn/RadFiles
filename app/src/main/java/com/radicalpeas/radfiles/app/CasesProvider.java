package com.radicalpeas.radfiles.app;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import net.sqlcipher.database.SQLiteDatabase;  //note the import of net.sqlcipher.database.SQLiteDatabase instead of android.database.sqlite.SQLiteDatabase as well as the call to SQLiteDatabase.loadLibs(this). The call to SQLiteDatabase.loadLibs(this) must occur before any other database operation.
import net.sqlcipher.database.SQLiteOpenHelper;
import net.sqlcipher.database.SQLiteQueryBuilder;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class CasesProvider extends ContentProvider
{

	/////////////////////////////////////////////////////////////////////
	//	Constants & Data
	/////////////////////////////////////////////////////////////////////
	// For logging:
	private static final String TAG = "CasesProvider";
	private static final String FTS_SEARCH = "FTS_SEARCH_INTENT";

	// projection map for a query
	private static HashMap<String, String> CaseMap;

	// integer values used in content URI
	static final int CASES = 1;
	static final int CASES_ID = 2;

	static final int IMAGES = 3;
	static final int IMAGES_ID = 4;

	static final int SECTION_LIST = 5;
	static final int SECTION_LIST_ID = 6;

	static final int KEYWORD_LIST = 7;
	static final int KEYWORD_LIST_ID = 8;

	static final int STUDYTYPE_LIST = 9;
	static final int STUDYTYPE_LIST_ID = 10;



	// fields for my content provider
	static final String PROVIDER_NAME = "com.radicalpeas.radfiles.app.CasesProvider";

	// CASES TABLE
	static final String cases_URL = "content://" + PROVIDER_NAME + "/cases";
	static final Uri CASES_URI = Uri.parse(cases_URL);

	// IMAGES TABLE: linked to a row in CASES TABLE
	static final String images_URL = "content://" + PROVIDER_NAME + "/images";
	static final Uri IMAGES_URI = Uri.parse(images_URL);

	// SECTION LIST TABLE
	static final String sectionList_URL = "content://" + PROVIDER_NAME + "/section_list";
	static final Uri SECTION_LIST_URI = Uri.parse(sectionList_URL);

	// SECTION LIST TABLE
	static final String keyWordList_URL = "content://" + PROVIDER_NAME + "/keyWord_list";
	static final Uri KEYWORD_LIST_URI = Uri.parse(keyWordList_URL);

	// SECTION LIST TABLE
	static final String studyTypeList_URL = "content://" + PROVIDER_NAME + "/studyType_list";
	static final Uri STUDYTYPE_LIST_URI = Uri.parse(studyTypeList_URL);


	// maps content URI "patterns" to the integer values that were set above
	static final UriMatcher uriMatcher;

	static
	{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "cases", CASES);
		uriMatcher.addURI(PROVIDER_NAME, "cases/#", CASES_ID);

		uriMatcher.addURI(PROVIDER_NAME, "images", IMAGES);
		uriMatcher.addURI(PROVIDER_NAME, "images/#", IMAGES_ID);

		uriMatcher.addURI(PROVIDER_NAME, "section_list", SECTION_LIST);
		uriMatcher.addURI(PROVIDER_NAME, "section_list/#", SECTION_LIST_ID);

		uriMatcher.addURI(PROVIDER_NAME, "keyWord_list", KEYWORD_LIST);
		uriMatcher.addURI(PROVIDER_NAME, "keyWord_list/#", KEYWORD_LIST_ID);

		uriMatcher.addURI(PROVIDER_NAME, "studyType_list", STUDYTYPE_LIST);
		uriMatcher.addURI(PROVIDER_NAME, "studyType_list/#", STUDYTYPE_LIST_ID);
	}


	// DB Fields.  unique KEY_ROWID is first column in all tables
	public static final String KEY_ROWID = "_id";
	public static final int COL_ROWID = 0;


	// Cases Table
	// 1. define String KEY_ = "..."
	public static final String KEY_CASE_NUMBER = "CASE_NUMBER";		// changed from PATIENT_ID for HIPAA reasons
	public static final String KEY_DIAGNOSIS = "DIAGNOSIS";
	public static final String KEY_SECTION = "SECTION";     // re-used in section_list table
	public static final String KEY_FINDINGS = "FINDINGS";       // comma-separated list
	public static final String KEY_BIOPSY = "BIOPSY";           // null or pathology
	public static final String KEY_FOLLOWUP = "FOLLOWUP";       // boolean
	public static final String KEY_FOLLOWUP_COMMENT = "FOLLOWUP_COMMENT";
	public static final String KEY_KEYWORDS = "KEYWORDS";   // re-used in keyword_list table, // comma-separated list
	public static final String KEY_COMMENTS = "COMMENTS";

	public static final String KEY_STUDY_TYPE = "STUDY_TYPE";    // re-used in studytype_list table
	public static final String KEY_STUDY_DATE = "STUDY_DATE";               // comma-separated list??
	public static final String KEY_IMAGE_COUNT = "IMAGE_COUNT";
	public static final String KEY_THUMBNAIL = "THUMBNAIL";
	public static final String KEY_FAVORITE = "FAVORITE";
	public static final String KEY_CLINICAL_HISTORY = "CLINICAL";

	public static final String KEY_LAST_MODIFIED_DATE = "LAST_MODIFIED_DATE";

	public static final String KEY_USER_ID = "USER_ID";	// RadFiles userID
	public static final String KEY_ORIGINAL_CREATOR = "ORIGINAL_CREATOR";	// RadFiles user name who first submitted this case
	public static final String KEY_IS_SHARED = "IS_SHARED";	// flag if shared publicly

	public static final String KEY_UNIQUE_ID = "CASE_INFO1";	// unique ID across firebase and SQL of multiple devices

	public static final String KEY_CASE_INFO2 = "CASE_INFO2"; // extra rows in case need for future
	public static final String KEY_CASE_INFO3 = "CASE_INFO3";
	public static final String KEY_CASE_INFO4 = "CASE_INFO4";
	public static final String KEY_CASE_INFO5 = "CASE_INFO5";

	// Images Table
	// 1. define String KEY_ = "..."
	public static final String KEY_IMAGE_PARENT_CASE_ID = "IMAGE_PARENT_CASE_ID";
	public static final String KEY_IMAGE_FILENAME = "IMAGE_FILENAME";
	public static final String KEY_ORDER = "ROW_ORDER";    // re-used in images,  section_list, studytype_list, keyword_list tables
	public static final String KEY_IMAGE_DETAILS = "IMAGE_DETAILS";
	public static final String KEY_IMAGE_CAPTION = "IMAGE_CAPTION";

	public static final String KEY_IMAGE_INFO1 = "CASE_INFO1";
	public static final String KEY_IMAGE_INFO2 = "CASE_INFO2";
	public static final String KEY_IMAGE_INFO3 = "CASE_INFO3";

	// list tables
	// KEY_ // keywords, modality, section
	// KEY_ORDER
	public static final String KEY_LIST_ITEM_IS_HIDDEN = "LIST_ITEM_IS_HIDDEN";


	/*
	 * modified date
	 * keywords list
	 * diagnosis list
	 * followup bool
	 * followup comment
	 */

	// Setup field numbers here (0 = KEY_ROWID, 1=...)
	// 2. define int COL_ = "..."
	public static final int COL_CASE_NUMBER = 1;
	public static final int COL_DIAGNOSIS = 2;
	public static final int COL_SECTION = 3;
	public static final int COL_FINDINGS = 4;
	public static final int COL_BIOPSY = 5;
	public static final int COL_FOLLOWUP = 6;
	public static final int COL_FOLLOWUP_COMMENT = 7;
	public static final int COL_KEYWORDS = 8;
	public static final int COL_COMMENTS = 9;
	public static final int COL_STUDY_TYPE = 10;
	public static final int COL_DATE = 11;
	public static final int COL_IMAGE_COUNT = 12;
	public static final int COL_THUMBNAIL = 13;
	public static final int COL_FAVORITE = 14;
	public static final int COL_CLINICAL_HISTORY = 15;
	public static final int COL_LAST_MODIFIED_DATE = 16;
	public static final int COL_USER_ID = 17;	// RadFiles user
	public static final int COL_ORIGINAL_CREATOR = 18;	// RadFiles user who first submitted this case
	public static final int COL_IS_SHARED = 19;	// flag if shared publicly

	public static final int COL_UNIQUE_ID = 20;	// extra rows in case need for future
	public static final int COL_CASE_INFO2 = 21;
	public static final int COL_CASE_INFO3 = 22;
	public static final int COL_CASE_INFO4 = 23;
	public static final int COL_CASE_INFO5 = 24;

	// for secondary list tables.  study_type tables
	public static final int COL_LIST_ITEM_VALUE = 1;
	public static final int COL_LIST_ITEM_ORDER = 2;  // sorting
	public static final int COL_LIST_ITEM_IS_HIDDEN = 3;

	// for images table
	public static final int COL_IMAGE_PARENT_CASE_ID = 1; // from Cases table
	public static final int COL_IMAGE_FILENAME = 2;
	public static final int COL_IMAGE_ORDER = 3;  // order to display images.  0 is main thumbnail
	public static final int COL_IMAGE_DETAILS = 4;
	public static final int COL_IMAGE_CAPTION = 5;

	public static final int COL_IMAGE_INFO1 = 6;
	public static final int COL_IMAGE_INFO2 = 7;
	public static final int COL_IMAGE_INFO3 = 8;


	// array of all table key strings
	// 3. add KEY_ string to _ALL_KEYS array
	public static final String[] CASES_TABLE_ALL_KEYS = new String[]{KEY_ROWID, KEY_CASE_NUMBER, KEY_DIAGNOSIS,
		KEY_SECTION, KEY_FINDINGS, KEY_BIOPSY, KEY_FOLLOWUP, KEY_FOLLOWUP_COMMENT, KEY_KEYWORDS, KEY_COMMENTS, KEY_STUDY_TYPE,
			KEY_STUDY_DATE, KEY_IMAGE_COUNT, KEY_THUMBNAIL, KEY_FAVORITE, KEY_CLINICAL_HISTORY, KEY_LAST_MODIFIED_DATE,
			KEY_USER_ID, KEY_ORIGINAL_CREATOR, KEY_IS_SHARED,
			KEY_UNIQUE_ID, KEY_CASE_INFO2, KEY_CASE_INFO3, KEY_CASE_INFO4, KEY_CASE_INFO5};

	// DB info: it's name, and the table we are using.
	public static final String DATABASE_NAME = "MyDB";
	public static final String CASES_TABLE = "CasesTable";
	public static final String IMAGES_TABLE = "ImagesTable";

	/*
	private static final String FTS_VIRTUAL_CASES_TABLE = "FTS_Cases_Table";

    //Create a FTS3 Virtual Table for fast searches
    private static final String DATABASE_CREATE =
        "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_CASES_TABLE + " USING fts3(" +
        		CASES_TABLE_ALL_KEYS[0] + "," +
        		CASES_TABLE_ALL_KEYS[1] + "," +
        		CASES_TABLE_ALL_KEYS[2] + "," +
        		CASES_TABLE_ALL_KEYS[3] + "," +
        		" UNIQUE (" + CASES_TABLE_ALL_KEYS[0] + "));";
    */

	// for custom dropdown spinner lists
	public static final String SECTION_LIST_TABLE = "SectionTable";
	public static final String KEYWORD_LIST_TABLE = "KeyWordListTable";
	public static final String STUDYTYPE_LIST_TABLE = "StudyTypeListTable";

	// 4. SQL string to create _TABLE with _ALL_KEYS matched with COL_ defined above
	private static final String CASES_TABLE_CREATE_SQL =
			"create table " + CASES_TABLE
					+ " (" + KEY_ROWID + " integer primary key autoincrement, "

					// + KEY_{...} + " {type} not null"
					//	- Key is the column name you created above.
					//	- {type} is one of: text, integer, real, blob
					//		(http://www.sqlite.org/datatype3.html)
					//  - "not null" means it is a required field (must be given a value).
					// NOTE: All must be comma separated (end of line!) Last one must have NO comma!!
					+ CASES_TABLE_ALL_KEYS[COL_CASE_NUMBER] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_DIAGNOSIS] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_SECTION] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_FINDINGS] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_BIOPSY] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_FOLLOWUP] + " integer, "    // boolean
					+ CASES_TABLE_ALL_KEYS[COL_FOLLOWUP_COMMENT] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_KEYWORDS] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_COMMENTS] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_STUDY_TYPE] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_DATE] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_IMAGE_COUNT] + " integer, " 	// int
					+ CASES_TABLE_ALL_KEYS[COL_THUMBNAIL] + " integer, "	// int image list position to be used as thumbnail
					+ CASES_TABLE_ALL_KEYS[COL_FAVORITE] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_CLINICAL_HISTORY] + " text, "
					+ CASES_TABLE_ALL_KEYS[COL_LAST_MODIFIED_DATE] + " text, "
					+ KEY_USER_ID + " text, "
					+ KEY_ORIGINAL_CREATOR + " text, "
					+ KEY_IS_SHARED + " integer, "							// boolean

					+ KEY_UNIQUE_ID + " text, "
					+ KEY_CASE_INFO2 + " text, "
					+ KEY_CASE_INFO3 + " text, "
					+ KEY_CASE_INFO4 + " integer, "	//int
					+ KEY_CASE_INFO5 + " integer"	//int

					// Rest  of creation:
					+ ");";

	// list of image files, with links to parent "Cases" table
	// 3. add KEY_ string to _ALL_KEYS array
	public static final String[] IMAGES_TABLE_ALL_KEYS = new String[]{KEY_ROWID, KEY_IMAGE_PARENT_CASE_ID, KEY_IMAGE_FILENAME, KEY_ORDER, KEY_IMAGE_DETAILS, KEY_IMAGE_CAPTION, KEY_IMAGE_INFO1, KEY_IMAGE_INFO2, KEY_IMAGE_INFO3};
	// 4. SQL string to create _TABLE with _KEYS defined above
	private static final String IMAGES_TABLE_CREATE_SQL =
			"create table " + IMAGES_TABLE
					+ " (" + KEY_ROWID + " integer primary key autoincrement, "

					// + KEY_{...} + " {type} not null"
					//	- Key is the column name you created above.
					//	- {type} is one of: text, integer, real, blob
					//		(http://www.sqlite.org/datatype3.html)
					//  - "not null" means it is a required field (must be given a value).
					// NOTE: All must be comma separated (end of line!) Last one must have NO comma!!
					+ KEY_IMAGE_PARENT_CASE_ID + " integer, "
					+ KEY_IMAGE_FILENAME + " text, "
					+ KEY_ORDER + " integer, "
					+ KEY_IMAGE_DETAILS + " text, "
					+ KEY_IMAGE_CAPTION + " text, "
					+ KEY_IMAGE_INFO1 + " text, "
					+ KEY_IMAGE_INFO2 + " text, "
					+ KEY_IMAGE_INFO3 + " integer"

					+ ");";

	// to generate list of available Sections
	private static final String SECTION_LIST_TABLE_CREATE_SQL =
			"create table " + SECTION_LIST_TABLE
					+ " (" + KEY_ROWID + " integer primary key autoincrement, "
					+ KEY_SECTION + " integer, " // COL_LIST_ITEM_VALUE
					+ KEY_ORDER + " integer, "
					+ KEY_LIST_ITEM_IS_HIDDEN + " integer DEFAULT 0"
					+ ");";

	// to generate list of available Key Words
	private static final String KEYWORD_LIST_TABLE_CREATE_SQL =
			"create table " + KEYWORD_LIST_TABLE
					+ " (" + KEY_ROWID + " integer primary key autoincrement, "
					+ KEY_KEYWORDS + " text, " // COL_LIST_ITEM_VALUE
					+ KEY_ORDER + " integer, "
					+ KEY_LIST_ITEM_IS_HIDDEN + " integer DEFAULT 0"
					+ ");";

	// to generate list of available Study Types
	private static final String STUDYTYPE_LIST_TABLE_CREATE_SQL =
			"create table " + STUDYTYPE_LIST_TABLE
					+ " (" + KEY_ROWID + " integer primary key autoincrement, "
					+ KEY_STUDY_TYPE + " text, " // COL_LIST_ITEM_VALUE
					+ KEY_ORDER + " integer, "
					+ KEY_LIST_ITEM_IS_HIDDEN + " integer DEFAULT 0"
					+ ");";


	// Track DB version if a new version of your app changes the format.
	// 5. update DATABASE_VERSION
	public static final int DATABASE_VERSION = 46;

	// Context of application who uses us.
	//	private final Context context;

	private DatabaseHelper myDBHelper;
	private SQLiteDatabase db;

	private String db_key;

	/////////////////////////////////////////////////////////////////////
	//	Public methods:
	/////////////////////////////////////////////////////////////////////

	@Override
	public boolean onCreate()
	{
		Context context = getContext();

		// set up SQLCipher
		SQLiteDatabase.loadLibs(context);
		db_key = "test password";

		myDBHelper = new DatabaseHelper(context);
		//myDBHelper = DatabaseHelper.getInstance(context);

		// permissions to be writable
		db = myDBHelper.getWritableDatabase(db_key);

		if (db == null)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		//	queryBuilder.setTables(CASES_TABLE);

		switch (uriMatcher.match(uri))
		{
			// maps all database column names
			case CASES:
				queryBuilder.setTables(CASES_TABLE);
				queryBuilder.setProjectionMap(CaseMap);
				break;
			case CASES_ID:
				queryBuilder.setTables(CASES_TABLE);
				queryBuilder.appendWhere(KEY_ROWID + "=" + uri.getLastPathSegment());
				break;
			case IMAGES:
				queryBuilder.setTables(IMAGES_TABLE);
				queryBuilder.setProjectionMap(CaseMap);
				break;
			case IMAGES_ID:
				queryBuilder.setTables(IMAGES_TABLE);
				queryBuilder.appendWhere(KEY_ROWID + "=" + uri.getLastPathSegment());
				break;
			case SECTION_LIST:
				queryBuilder.setTables(SECTION_LIST_TABLE);
				queryBuilder.setProjectionMap(CaseMap);
				break;
			case SECTION_LIST_ID:
				queryBuilder.setTables(SECTION_LIST_TABLE);
				queryBuilder.appendWhere(KEY_ROWID + "=" + uri.getLastPathSegment());
				break;
			case KEYWORD_LIST:
				queryBuilder.setTables(KEYWORD_LIST_TABLE);
				queryBuilder.setProjectionMap(CaseMap);
				break;
			case KEYWORD_LIST_ID:
				queryBuilder.setTables(KEYWORD_LIST_TABLE);
				queryBuilder.appendWhere(KEY_ROWID + "=" + uri.getLastPathSegment());
				break;
			case STUDYTYPE_LIST:
				queryBuilder.setTables(STUDYTYPE_LIST_TABLE);
				queryBuilder.setProjectionMap(CaseMap);
				break;
			case STUDYTYPE_LIST_ID:
				queryBuilder.setTables(STUDYTYPE_LIST_TABLE);
				queryBuilder.appendWhere(KEY_ROWID + "=" + uri.getLastPathSegment());
				break;


			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (sortOrder == null || sortOrder == "")
		{
			// No sorting specified
			sortOrder = KEY_ROWID;
		}

		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		// register to watch a content URI for changes
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		String table;
		Uri content_uri;

		switch (uriMatcher.match(uri))
		{
			// maps all database column names
			case CASES:
			case CASES_ID:
				table = CASES_TABLE;
				content_uri = CASES_URI;
				break;

			case IMAGES:
			case IMAGES_ID:
				table = IMAGES_TABLE;
				content_uri = IMAGES_URI;
				break;

			case SECTION_LIST:
			case SECTION_LIST_ID:
				table = SECTION_LIST_TABLE;
				content_uri = SECTION_LIST_URI;
				break;

			case KEYWORD_LIST:
			case KEYWORD_LIST_ID:
				table = KEYWORD_LIST_TABLE;
				content_uri = KEYWORD_LIST_URI;
				break;

			case STUDYTYPE_LIST:
			case STUDYTYPE_LIST_ID:
				table = STUDYTYPE_LIST_TABLE;
				content_uri = STUDYTYPE_LIST_URI;
				break;


			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		long row = db.insert(table, null, values);

		// If record is added successfully
		if (row > 0)
		{
			Uri rowUri = ContentUris.withAppendedId(content_uri, row);
			getContext().getContentResolver().notifyChange(rowUri, null);
			return rowUri;
		}
		throw new SQLException("Insert row failed. Fail to add a new record into " + uri);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{

		int count = 0;

		switch (uriMatcher.match(uri))
		{
			// maps all database column names
			case CASES:
				count = db.update(CASES_TABLE, values, selection, selectionArgs);
				break;
			case CASES_ID:
				count = db.update(CASES_TABLE, values, KEY_ROWID + " = " + uri.getLastPathSegment() + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;
			case IMAGES:
				count = db.update(IMAGES_TABLE, values, selection, selectionArgs);
				break;
			case IMAGES_ID:
				count = db.update(IMAGES_TABLE, values, KEY_ROWID + " = " + uri.getLastPathSegment() + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;
			case SECTION_LIST:
				count = db.update(SECTION_LIST_TABLE, values, selection, selectionArgs);
				break;
			case SECTION_LIST_ID:
				count = db.update(SECTION_LIST_TABLE, values, KEY_ROWID + " = " + uri.getLastPathSegment() + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;
			case KEYWORD_LIST:
				count = db.update(KEYWORD_LIST_TABLE, values, selection, selectionArgs);
				break;
			case KEYWORD_LIST_ID:
				count = db.update(KEYWORD_LIST_TABLE, values, KEY_ROWID + " = " + uri.getLastPathSegment() + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;
			case STUDYTYPE_LIST:
				count = db.update(STUDYTYPE_LIST_TABLE, values, selection, selectionArgs);
				break;
			case STUDYTYPE_LIST_ID:
				count = db.update(STUDYTYPE_LIST_TABLE, values, KEY_ROWID + " = " + uri.getLastPathSegment() + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;

			default:
				throw new IllegalArgumentException("Update row failed. Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		int count = 0;
		String uri_id;

		switch (uriMatcher.match(uri))
		{
			case CASES:
				count = db.delete(CASES_TABLE, selection, selectionArgs);
				break;
			case CASES_ID:
				uri_id = uri.getLastPathSegment();    //gets the id
				count = db.delete(CASES_TABLE, KEY_ROWID + " = " + uri_id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;

			case IMAGES:
				count = db.delete(IMAGES_TABLE, selection, selectionArgs);
				break;
			case IMAGES_ID:
				uri_id = uri.getLastPathSegment();    //gets the id
				count = db.delete(IMAGES_TABLE, KEY_ROWID + " = " + uri_id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;

			case SECTION_LIST:
				count = db.delete(SECTION_LIST_TABLE, selection, selectionArgs);
				break;
			case SECTION_LIST_ID:
				uri_id = uri.getLastPathSegment();    //gets the id
				count = db.delete(SECTION_LIST_TABLE, KEY_ROWID + " = " + uri_id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;

			case KEYWORD_LIST:
				count = db.delete(KEYWORD_LIST_TABLE, selection, selectionArgs);
				break;
			case KEYWORD_LIST_ID:
				uri_id = uri.getLastPathSegment();    //gets the id
				count = db.delete(KEYWORD_LIST_TABLE, KEY_ROWID + " = " + uri_id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;

			case STUDYTYPE_LIST:
				count = db.delete(STUDYTYPE_LIST_TABLE, selection, selectionArgs);
				break;
			case STUDYTYPE_LIST_ID:
				uri_id = uri.getLastPathSegment();    //gets the id
				count = db.delete(STUDYTYPE_LIST_TABLE, KEY_ROWID + " = " + uri_id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;

			default:
				throw new IllegalArgumentException("Delete failed. Unsupported URI " + uri);
		}


		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}


	@Override
	public String getType(Uri uri)
	{
		switch (uriMatcher.match(uri))
		{
			case CASES:
				return "debug: getType CASES";
			case CASES_ID:
				return "debug: getType CASES_ID";

			case IMAGES:
				return "debug: getType IMAGES";
			case IMAGES_ID:
				return "debug: getType IMAGES_ID";

			case SECTION_LIST:
				return "debug: getType SECTION_LIST";
			case SECTION_LIST_ID:
				return "debug: getType SECTION_LIST_ID";

			case KEYWORD_LIST:
				return "debug: getType KEYWORD_LIST";
			case KEYWORD_LIST_ID:
				return "debug: getType KEYWORD_LIST_ID";

			case STUDYTYPE_LIST:
				return "debug: getType STUDYTYPE_LIST";
			case STUDYTYPE_LIST_ID:
				return "debug: getType STUDYTYPE_LIST_ID";



			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}


	// Open the database connection.
	public CasesProvider open()
	{
		SQLiteDatabase.loadLibs(getContext());	// TODO: do i need this here?
		db = myDBHelper.getWritableDatabase(db_key);
		return this;
	}


	// Close the database connection.
	public void close()
	{
		myDBHelper.close();
	}


	/////////////////////////////////////////////////////////////////////
	//	Private Helper Classes:
	/////////////////////////////////////////////////////////////////////

	/**
	 * Private class which handles database creation and upgrading.
	 * Used to handle low-level database access.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		private static DatabaseHelper sInstance;    // only one database adapter

		public static DatabaseHelper getInstance(Context context)
		{

			// Use the application context, which will ensure that you
			// don't accidentally leak an Activity's context.
			// See this article for more information: http://bit.ly/6LRzfx
			if (sInstance == null)
			{
				sInstance = new DatabaseHelper(context.getApplicationContext());
			}
			return sInstance;
		}

		private DatabaseHelper(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase _db)
		{
			_db.execSQL(CASES_TABLE_CREATE_SQL);
			_db.execSQL(IMAGES_TABLE_CREATE_SQL);

			//TODO remove initial db data
			ContentValues initialValues = new ContentValues();


			// Create StudyTypes Table
			_db.execSQL(STUDYTYPE_LIST_TABLE_CREATE_SQL);

			ContentValues values = new ContentValues();

			values.clear();
			values.put(KEY_STUDY_TYPE, "CT Head");
			values.put(KEY_ORDER, 1);
			_db.insert(STUDYTYPE_LIST_TABLE, null, values);

			values.clear();
			values.put(KEY_STUDY_TYPE, "MR Brain");
			values.put(KEY_ORDER, 2);
			_db.insert(STUDYTYPE_LIST_TABLE, null, values);

			values.clear();
			values.put(KEY_STUDY_TYPE, "CT C-Spine");
			values.put(KEY_ORDER, 8);
			_db.insert(STUDYTYPE_LIST_TABLE, null, values);

			values.clear();
			values.put(KEY_STUDY_TYPE, "CT Chest");
			values.put(KEY_ORDER, 7);
			_db.insert(STUDYTYPE_LIST_TABLE, null, values);

			values.clear();
			values.put(KEY_STUDY_TYPE, "CT Abdomen and Pelvis");
			values.put(KEY_ORDER, 3);
			_db.insert(STUDYTYPE_LIST_TABLE, null, values);

			values.clear();
			values.put(KEY_STUDY_TYPE, "Ultrasound Pelvis");
			values.put(KEY_ORDER, 4);
			_db.insert(STUDYTYPE_LIST_TABLE, null, values);

			values.clear();
			values.put(KEY_STUDY_TYPE, "Ultrasound Renal");
			values.put(KEY_ORDER, 5);
			_db.insert(STUDYTYPE_LIST_TABLE, null, values);

			values.clear();
			values.put(KEY_STUDY_TYPE, "V/Q Scan");
			values.put(KEY_ORDER, 6);
			_db.insert(STUDYTYPE_LIST_TABLE, null, values);

			values.clear();
			values.put(KEY_STUDY_TYPE, "PET/CT");
			values.put(KEY_ORDER, 0);
			_db.insert(STUDYTYPE_LIST_TABLE, null, values);


			// Create KeyWords LIST Table
			_db.execSQL(KEYWORD_LIST_TABLE_CREATE_SQL);

			values.clear();
			values.put(KEY_KEYWORDS, "Normal Variant");
			values.put(KEY_ORDER, 8);
			_db.insert(KEYWORD_LIST_TABLE, null, values);

			values.put(KEY_KEYWORDS, "Misses");
			values.put(KEY_ORDER, 1);
			_db.insert(KEYWORD_LIST_TABLE, null, values);

			values.put(KEY_KEYWORDS, "Trauma");
			values.put(KEY_ORDER, 2);
			_db.insert(KEYWORD_LIST_TABLE, null, values);

			values.put(KEY_KEYWORDS, "Foreign bodies");
			values.put(KEY_ORDER, 3);
			_db.insert(KEYWORD_LIST_TABLE, null, values);

			values.put(KEY_KEYWORDS, "VA Call");
			values.put(KEY_ORDER, 0);
			_db.insert(KEYWORD_LIST_TABLE, null, values);

			values.put(KEY_KEYWORDS, "Tumor");
			values.put(KEY_ORDER, 4);
			_db.insert(KEYWORD_LIST_TABLE, null, values);

			values.put(KEY_KEYWORDS, "Lines");
			values.put(KEY_ORDER, 7);
			_db.insert(KEYWORD_LIST_TABLE, null, values);

			values.put(KEY_KEYWORDS, "Cancer");
			values.put(KEY_ORDER, 5);
			_db.insert(KEYWORD_LIST_TABLE, null, values);

			values.put(KEY_KEYWORDS, "Bowel Obstruction");
			values.put(KEY_ORDER, 8);
			_db.insert(KEYWORD_LIST_TABLE, null, values);


			_db.execSQL(SECTION_LIST_TABLE_CREATE_SQL);
			initialValues.clear();
			initialValues.put(KEY_SECTION, "MSK");
			initialValues.put(KEY_ORDER, 3);
			_db.insert(SECTION_LIST_TABLE, null, initialValues);

			initialValues.clear();
			initialValues.put(KEY_SECTION, "Neuro");
			initialValues.put(KEY_ORDER, 0);
			_db.insert(SECTION_LIST_TABLE, null, initialValues);

			initialValues.clear();
			initialValues.put(KEY_SECTION, "Body");
			initialValues.put(KEY_ORDER, 2);
			_db.insert(SECTION_LIST_TABLE, null, initialValues);

			initialValues.clear();
			initialValues.put(KEY_SECTION, "Nuclear Medicine");
			initialValues.put(KEY_ORDER, 4);
			_db.insert(SECTION_LIST_TABLE, null, initialValues);

			initialValues.clear();
			initialValues.put(KEY_SECTION, "Ultrasound");
			initialValues.put(KEY_ORDER, 5);
			_db.insert(SECTION_LIST_TABLE, null, initialValues);

			initialValues.clear();
			initialValues.put(KEY_SECTION, "Chest");
			initialValues.put(KEY_ORDER, 1);
			_db.insert(SECTION_LIST_TABLE, null, initialValues);

			initialValues.clear();
			initialValues.put(KEY_SECTION, "Vascular");
			initialValues.put(KEY_ORDER, 6);
			_db.insert(SECTION_LIST_TABLE, null, initialValues);

			initialValues.clear();
			initialValues.put(KEY_SECTION, "IR");
			initialValues.put(KEY_ORDER, 7);
			_db.insert(SECTION_LIST_TABLE, null, initialValues);

			initialValues.clear();
			initialValues.put(KEY_SECTION, "Pediatric");
			initialValues.put(KEY_ORDER, 7);
			_db.insert(SECTION_LIST_TABLE, null, initialValues);
		}

		@Override
		public void onUpgrade(SQLiteDatabase _db, int oldVersion, int newVersion)
		{
			Log.w(TAG, "Upgrading application's database from version " + oldVersion
					           + " to " + newVersion + ", which will destroy all old data!");

			// Destroy old database:
			_db.execSQL("DROP TABLE IF EXISTS " + CASES_TABLE);
			_db.execSQL("DROP TABLE IF EXISTS " + IMAGES_TABLE);
			_db.execSQL("DROP TABLE IF EXISTS " + SECTION_LIST_TABLE);
			_db.execSQL("DROP TABLE IF EXISTS " + KEYWORD_LIST_TABLE);
			_db.execSQL("DROP TABLE IF EXISTS " + STUDYTYPE_LIST_TABLE);


			// Recreate new database:
			onCreate(_db);
		}
	}


}

