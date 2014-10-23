package com.huantnguyen.radcases.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardThumbnail;

/**
 * Created by Huan on 6/21/2014.
 */
public class CaseCard_Kitkat extends Card
{

	protected TextView mTitle_view;
	protected TextView mText1_view;
	protected TextView mText2_view;
	protected TextView mText3_view;
	protected CheckBox star_view;

	protected int filter_group;
	private String groupHeader;

	private String mTitle;
	private String mText1;
	private String mText2;

	private boolean isStarred = false;

	/**
	 * Constructor with a custom inner layout
	 * @param context
	 */
	public CaseCard_Kitkat(Context context) {
		this(context, R.layout.card_case_kitkat);
	}

	/**
	 *
	 * @param context
	 * @param innerLayout
	 */
	public CaseCard_Kitkat(Context context, int innerLayout) {
		super(context, innerLayout);
		init();
	}

	/**
	 * Init
	 */
	private void init(){

		//No Header

		//Set a OnClickListener listener
		setOnClickListener(new OnCardClickListener() {
			@Override
			public void onClick(Card card, View view) {
				Toast.makeText(getContext(), "Click Listener card=", Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public void setupInnerViewElements(ViewGroup parent, View view) {


		//Retrieve elements
		mTitle_view = (TextView) view.findViewById(R.id.card_main_inner_title);
		mText1_view = (TextView) view.findViewById(R.id.card_main_inner_text1);
		mText2_view = (TextView) view.findViewById(R.id.card_main_inner_text2);
		//mText3_view = (TextView) view.findViewById(R.id.card_main_inner_text3);
		star_view = (CheckBox) view.findViewById(R.id.card_star);

		mTitle_view.setText(mTitle);
		mText1_view.setText(mText1);
		mText2_view.setText(mText2);
		star_view.setChecked(isStarred);

		if(!isStarred)
			star_view.setVisibility(View.GONE);

	}

	public void setTitle(String title)
	{
		if(title == null)
		{
			mTitle = "";
		}
		else
		{
			mTitle = title;
		}
	}

	public void setText1(String text)
	{
		if(text == null)
		{
			mText1 = "";
		}
		else
		{
			mText1 = text;
		}
	}

	public void setText2(String text)
	{
		if(text == null)
		{
			mText2 = "";
		}
		else
		{
			mText2 = text;
		}
	}
	public void setGroupHeader(String text)
	{
		if(text == null)
		{
			groupHeader = "";
		}
		else
		{
			groupHeader = text;
		}
	}

	public void setStar(boolean bool)
	{
		if(bool)
		{
			isStarred = true;
		}
		else
		{
			isStarred = false;
		}
	}

	static public class MyThumbnail extends CardThumbnail
	{
		private String thumbFilename;

		public MyThumbnail(Context context, String filename)
		{
			super(context);
			thumbFilename = new String(filename);
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View viewImage)
		{

			ImageView image = (ImageView) viewImage;
			UtilClass.setPic(image, thumbFilename, UtilClass.IMAGE_THUMB_SIZE);
		}
	}

	public String getGroupHeader()
	{
		return groupHeader;
	}

	// group id for StickyCardListView
	public void setGroup(int group)
	{
		filter_group = group;
	}
	public int getGroup()
	{
		return filter_group;
	}
}
