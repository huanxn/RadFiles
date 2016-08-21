package com.radicalpeas.radfiles.app;

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
	public String followup;

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
}
