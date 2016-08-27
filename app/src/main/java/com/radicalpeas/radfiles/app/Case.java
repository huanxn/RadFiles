package com.radicalpeas.radfiles.app;

import android.content.Context;
import android.database.Cursor;

import com.google.gson.annotations.SerializedName;

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

	public void setCaseFromCursor(Context context, Cursor caseCursor)
	{
		case_id = caseCursor.getString(CasesProvider.COL_CASE_NUMBER);
		diagnosis = caseCursor.getString(CasesProvider.COL_DIAGNOSIS);
		section = caseCursor.getString(CasesProvider.COL_SECTION);
		findings = caseCursor.getString(CasesProvider.COL_FINDINGS);
		biopsy = caseCursor.getString(CasesProvider.COL_BIOPSY);
		followup = caseCursor.getInt(CasesProvider.COL_FOLLOWUP);
		followup_comment = caseCursor.getString(CasesProvider.COL_FOLLOWUP_COMMENT);
		key_words = caseCursor.getString(CasesProvider.COL_KEYWORDS);
		comments = caseCursor.getString(CasesProvider.COL_COMMENTS);
		study_type = caseCursor.getString(CasesProvider.COL_STUDY_TYPE);
		db_date_str = caseCursor.getString(CasesProvider.COL_DATE);
		image_count = caseCursor.getInt(CasesProvider.COL_IMAGE_COUNT);;
		thumbnail = caseCursor.getInt(CasesProvider.COL_THUMBNAIL);
		favorite = caseCursor.getString(CasesProvider.COL_FAVORITE);
		clinical_history = caseCursor.getString(CasesProvider.COL_CLINICAL_HISTORY);
		last_modified_date = caseCursor.getString(CasesProvider.COL_LAST_MODIFIED_DATE);
		userID = caseCursor.getString(CasesProvider.COL_USER_ID);
		original_creator = caseCursor.getString(CasesProvider.COL_ORIGINAL_CREATOR);
		is_shared = caseCursor.getInt(CasesProvider.COL_IS_SHARED);

		String [] image_args = {String.valueOf(key_id)};
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
		}

		if(thumbnail >= 0)
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
