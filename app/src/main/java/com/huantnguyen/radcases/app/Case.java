package com.huantnguyen.radcases.app;

/**
 * Created by Huan on 10/21/2014.
 */
public class Case
{
	public long key_id;
	public String patient_id;
	public String diagnosis;
	public String findings;
	public String section;
	public String comments;
	public String key_words;
	public String biopsy;
	public String followup;
	public int thumbnail;

	public String study_type;
	public String db_date_str;

	public int favorite;


	String thumbnail_filename;


	public boolean isSelected = false;

}
