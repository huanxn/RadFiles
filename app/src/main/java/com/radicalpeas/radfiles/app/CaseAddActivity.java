package com.radicalpeas.radfiles.app;

import android.os.Bundle;
import android.view.Menu;


public class CaseAddActivity extends CaseEditActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_add_case);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		getMenuInflater().inflate(R.menu.case_add, menu);

		return true;
	}

}
