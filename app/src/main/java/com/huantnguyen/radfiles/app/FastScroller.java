package com.huantnguyen.radfiles.app;

import android.widget.LinearLayout;

/*
 * StylingAndroid
 * LollipopContactsRecyclerViewFastScroller
 *
 * */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import static android.support.v7.widget.RecyclerView.OnScrollListener;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class FastScroller extends LinearLayout
{
	private static final int HANDLE_HIDE_DELAY = 1000;
	private static final int HANDLE_ANIMATION_DURATION = 100;
	private static final String SCALE_X = "scaleX";
	private static final String SCALE_Y = "scaleY";
	private static final String ALPHA = "alpha";

	private static final int MIN_LIST_SIZE_FOR_FAST_SCROLL = 20;


	private static final int BUBBLE_ANIMATION_DURATION=100;
	private static final int TRACK_SNAP_RANGE=5;

	private TextView bubble;
	private View handle;
	private RecyclerView recyclerView;
	private final ScrollListener scrollListener=new ScrollListener();
	private final HandleHider handleHider = new HandleHider();
	private int height; // height of fast scroll track
	private int handle_position; // y position

	private ObjectAnimator currentAnimator=null;

	public FastScroller(final Context context,final AttributeSet attrs,final int defStyleAttr)
	{
		super(context,attrs,defStyleAttr);
		initialise(context);
	}

	public FastScroller(final Context context)
	{
		super(context);
		initialise(context);
	}

	public FastScroller(final Context context,final AttributeSet attrs)
	{
		super(context,attrs);
		initialise(context);
	}

	private void initialise(Context context)
	{
		setOrientation(HORIZONTAL);
		setClipChildren(false);
		LayoutInflater inflater=LayoutInflater.from(context);
		inflater.inflate(R.layout.recycler_view_fast_scroller__fast_scroller,this,true);
		bubble=(TextView)findViewById(R.id.fastscroller_bubble);
		handle=findViewById(R.id.fastscroller_handle);
		bubble.setVisibility(INVISIBLE);
	}

	@Override
	protected void onSizeChanged(int w,int h,int oldw,int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		height=h;
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event)
	{
		final int action=event.getAction();
		switch(action)
		{
			case MotionEvent.ACTION_DOWN:
				if(handle.getVisibility() == INVISIBLE || event.getX()<handle.getX()-5)    // extra padding of 5 so it's not too narrow to grab
					return false;

				// only activate if clicked on handle
				final float down_y=event.getY();
				if((down_y > handle_position-handle.getHeight() && down_y < handle_position+handle.getHeight()) ) // only if visible
				{

				/*	if(currentAnimator!=null)
						currentAnimator.cancel();

					getHandler().removeCallbacks(handleHider);

					if (handle.getVisibility() == INVISIBLE)
					{
						showHandle();
					}

					*//*if(bubble.getVisibility()==INVISIBLE)
						showBubble();*/

					handle.setSelected(true);
				}

				// don't return, continue to ACTION_MOVE
			case MotionEvent.ACTION_MOVE:
				final float y=event.getY();

				if(handle.isSelected() || (y > handle_position-handle.getHeight() && y < handle_position+handle.getHeight()))
				{
					if(currentAnimator!=null)
						currentAnimator.cancel();
					getHandler().removeCallbacks(handleHider);
					if (handle.getVisibility() == INVISIBLE)
					{
						showHandle();
					}

					setBubbleAndHandlePosition(y);
					setRecyclerViewPosition(y);

					return true;
				}
				else
				{
					return false;   // pass touch event through
				}

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				handle.setSelected(false);
				hideBubble();
				getHandler().postDelayed(handleHider, HANDLE_HIDE_DELAY);
				return true;
		}
		return super.onTouchEvent(event);
	}

	public void setRecyclerView(RecyclerView recyclerView)
	{
		this.recyclerView=recyclerView;
		recyclerView.addOnScrollListener(scrollListener);
	}

	private void setRecyclerViewPosition(float y)
	{
		if(recyclerView!=null)
		{
			int itemCount=recyclerView.getAdapter().getItemCount();
			float proportion;
			if(handle.getY()==0)
				proportion=0f;
			else if(handle.getY()+handle.getHeight()>=height-TRACK_SNAP_RANGE)
				proportion=1f;
			else
				proportion=y/(float)height;
			int targetPos=getValueInRange(0,itemCount-1,(int)(proportion*(float)itemCount));
			((LinearLayoutManager)recyclerView.getLayoutManager()).scrollToPositionWithOffset(targetPos, 0);
			//      recyclerView.oPositionWithOffset(targetPos);

		//	String bubbleText=((BubbleTextGetter)recyclerView.getAdapter()).getTextToShowInBubble(targetPos);
		//	bubble.setText(bubbleText);
		}
	}

	private int getValueInRange(int min,int max,int value)
	{
		int minimum=Math.max(min,value);
		return Math.min(minimum,max);
	}

	private void setBubbleAndHandlePosition(float y)
	{
		int bubbleHeight=bubble.getHeight();
		int handleHeight=handle.getHeight();

		handle_position = getValueInRange(0,height-handleHeight,(int)(y-handleHeight/2.0));
		handle.setY(getValueInRange(0,height-handleHeight,(int)(y-handleHeight/2.0)));

	/*	bubble.setText(Integer.toString(getValueInRange(0,height-handleHeight,(int)(y-handleHeight/2.0))));
		showBubble();*/

		bubble.setY(getValueInRange(0,height-bubbleHeight-handleHeight/2,(int)(y-bubbleHeight)));
	}

	private void showBubble()
	{
		AnimatorSet animatorSet=new AnimatorSet();
		bubble.setVisibility(VISIBLE);
		if(currentAnimator!=null)
			currentAnimator.cancel();
		currentAnimator=ObjectAnimator.ofFloat(bubble,"alpha",0f,1f).setDuration(BUBBLE_ANIMATION_DURATION);
		currentAnimator.start();
	}

	private void hideBubble()
	{
		if (currentAnimator != null)
			currentAnimator.cancel();
		currentAnimator = ObjectAnimator.ofFloat(bubble, "alpha", 1f, 0f).setDuration(BUBBLE_ANIMATION_DURATION);
		currentAnimator.addListener(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator animation)
			{
				super.onAnimationEnd(animation);
				bubble.setVisibility(INVISIBLE);
				currentAnimator=null;
			}

			@Override
			public void onAnimationCancel(Animator animation)
			{
				super.onAnimationCancel(animation);
				bubble.setVisibility(INVISIBLE);
				currentAnimator=null;
			}
		});
		currentAnimator.start();
	}

	private void showHandle()
	{
		AnimatorSet animatorSet = new AnimatorSet();
		handle.setPivotX(handle.getWidth());
		handle.setPivotY(handle.getHeight());
		handle.setVisibility(VISIBLE);
		Animator growerX = ObjectAnimator.ofFloat(handle, SCALE_X, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION);
		Animator growerY = ObjectAnimator.ofFloat(handle, SCALE_Y, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION);
		Animator alpha = ObjectAnimator.ofFloat(handle, ALPHA, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION);
		animatorSet.playTogether(growerX, growerY, alpha);
		animatorSet.start();
	}

	private void hideHandle()
	{
		if(currentAnimator!=null)
			currentAnimator.cancel();
		currentAnimator=ObjectAnimator.ofFloat(handle,"alpha",1f,0f).setDuration(BUBBLE_ANIMATION_DURATION);
		currentAnimator.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation)
			{
				super.onAnimationEnd(animation);
				handle.setVisibility(INVISIBLE);
				currentAnimator=null;
			}

			@Override
			public void onAnimationCancel(Animator animation)
			{
				super.onAnimationCancel(animation);
				handle.setVisibility(INVISIBLE);
				currentAnimator=null;
			}
		});
		currentAnimator.start();
	}

	private class ScrollListener extends OnScrollListener
	{
		private int scrollState = SCROLL_STATE_IDLE;
		private float cumulative_dy = 0;
		private int handle_click_position;

		private int old_position;

		@Override
		public void onScrolled(RecyclerView rv,int dx,int dy)
		{
			View firstVisibleView=recyclerView.getChildAt(0);
			int firstVisiblePosition=recyclerView.getChildPosition(firstVisibleView);   //adapter or layout position?
			int visibleRange=recyclerView.getChildCount();
			int lastVisiblePosition=firstVisiblePosition+visibleRange;
			int itemCount=recyclerView.getAdapter().getItemCount();
			int position;
			int item_height;

			// don't use fast scroll if item count is too small
			if(itemCount < MIN_LIST_SIZE_FOR_FAST_SCROLL)
			{
				hideHandle();
				hideBubble();
				return;
			}

			item_height = firstVisibleView.getHeight()+4;   //add for padding between cards

			if(firstVisiblePosition==0)
				position=0;
			else if(lastVisiblePosition==itemCount)
				position=itemCount;
			else
				position=(int)(((float)firstVisiblePosition/(((float)itemCount-(float)visibleRange)))*(float)itemCount);

			if(cumulative_dy == 0)
			{
				handle_click_position = position;   //item position (number in list)
			}

			cumulative_dy += dy;

			float handle_click_proportion=(float)handle_click_position/(float)itemCount;    // proportion of height of recycler view

			// do not change handle if dragging manually (fast scroll)
			if(scrollState != SCROLL_STATE_IDLE)
			{
				//setBubbleAndHandlePosition((float)height * proportion);
				// x * height = item_height * item_count
				// dy / x
				setBubbleAndHandlePosition((float) height * handle_click_proportion + cumulative_dy * (float)(height)/(float)item_height/(float)itemCount);

			}
		}

		@Override
		public void onScrollStateChanged(RecyclerView rv, int newState)
		{
			scrollState = newState;

			// let go of drag
			if (newState == SCROLL_STATE_IDLE)
			{
				// hide after short delay
				getHandler().postDelayed(handleHider, HANDLE_HIDE_DELAY);

				// reset dy
				cumulative_dy = 0;
			}
			else
			{
				// dragging or settling
				getHandler().removeCallbacks(handleHider);
				if (handle.getVisibility() == INVISIBLE)
				{
					showHandle();
				}
			}
		}
	}

	private class HandleHider implements Runnable {
		@Override
		public void run() {
			hideHandle();
		}
	}

}