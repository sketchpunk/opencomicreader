package sage.ui;

//https://gist.githubusercontent.com/alexfu/0f464fc3742f134ccd1e/raw/abe729359e5b3691f2fe56445644baf0e40b35ba/DividerItemDecoration.java
//https://gist.github.com/fatfingers/233abbae200b5e87297b
//https://gist.github.com/polbins/e37206fbc444207c0e92

//https://gist.github.com/yrom/3b4bcbc2370ca2290434  //Example to use getItemOffsets to change the spacing between the Views

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerDivider extends RecyclerView.ItemDecoration{
	private Paint mPaint = null;
	public static enum Style{ DOTTED, DASHED };
	public static final int DOTTED = 1;
	public static final int DASHED = 2;

	//Region Constructor
	public RecyclerDivider(){ init("#e0e0e0",1,0,0); }//func
	public RecyclerDivider(String color){ init(color,1,0,0); }//func
	public RecyclerDivider(String color, int w){ init(color,w,0,0); }//func
	public RecyclerDivider(String color, int w, float wDash, float wGap){ init(color,w,wDash,wGap); }//func
	public RecyclerDivider(String color,int w, Style style){
		switch(style){
			case DOTTED: init(color,w,2,5); break;
			case DASHED: init(color,w,7,11); break;
			default: init(color,w,0,0); break;
		}//switch
	}//func

	private void init(String color,int w, float wDash, float wGap){
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(Color.parseColor(color));
		mPaint.setStrokeWidth(w);
		if(wDash > 0 && wGap > 0) mPaint.setPathEffect(new DashPathEffect(new float[] {wDash,wGap},0));
	}//func
	//endregion

	@Override
	public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state){
		int left = parent.getPaddingLeft();
		int right = parent.getWidth() - parent.getPaddingRight();
		int top = 0, bottom = 0;
		View child;
		int childCount = parent.getChildCount();
		for(int i = 0; i < childCount - 1; i++){
			child = parent.getChildAt(i);
			RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

			top = child.getBottom() + params.bottomMargin;
			bottom = top;

			c.drawLine(left, top, right, bottom, mPaint);
		}//for
	}//func

}//cls
