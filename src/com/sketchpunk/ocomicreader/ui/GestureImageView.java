package com.sketchpunk.ocomicreader.ui;

import sage.Util;
import sage.listener.MultiFingerGestureDetector;

import com.sketchpunk.ocomicreader.lib.ImgTransform;
import com.sketchpunk.ocomicreader.ui.ComicPageView.CallBack;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
 //Transition idea, First image crushes by width, then new image slides in while old is crushed widthwise.
import android.widget.Toast;


public class GestureImageView extends View
	implements OnScaleGestureListener
		,GestureDetector.OnGestureListener
		,GestureDetector.OnDoubleTapListener
		,MultiFingerGestureDetector.OnGestureListener{
	
	public static interface OnImageGestureListener{
		public void onImageGesture(int gType);
	}//func
	
	//Gesture Types
	public static final int	TAPLEFT		= 1,
							TAPRIGHT	= 4,
							FLINGLEFT	= 2,
							FLINGRIGHT	= 8,
							TWOFINGTAP	= 16,
							TWOFINGHOLD	= 32,
			
							MASKLEFT	= 3,
							MASKRIGHT	= 12;
						
	//========================================================================
	private Bitmap mBitmap = null;
	private ImgTransform mImgTrans = new ImgTransform();
	private Context mContext = null;
	
	private Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG); //make scaled image less pixelated
	private int mTapBoundary = 35; //Left/Right boundary to denote page change
	
	private ScaleGestureDetector mScaleGesture;
	private GestureDetector mGesture;
	private MultiFingerGestureDetector mFingerGesture;
	
	private OnImageGestureListener mListener = null;
	private int mGestureMode = 0; //TODO, See if this helps with Panning taking over Scale if lifting off one finger, else remove this.
	private float mLastY=0;
	
	//========================================================================
	public GestureImageView(Context context){ super(context); init(context); }//cls	
	public GestureImageView(Context context, AttributeSet attrs){ super(context, attrs); init(context); }
	public GestureImageView(Context context, AttributeSet attrs, int defStyle){ super(context, attrs, defStyle); init(context); }
	
	private void init(Context context){		
		mScaleGesture = new ScaleGestureDetector(context,this);
		mGesture = new GestureDetector(context,this);
		mGesture.setOnDoubleTapListener(this);
		mFingerGesture = new MultiFingerGestureDetector(this);
		mContext = context;
		if(context instanceof OnImageGestureListener) mListener = (OnImageGestureListener)context;
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mTapBoundary = prefs.getInt("viewTapBoundary",35);
		mTapBoundary = Util.dp2px(mTapBoundary,context); //Get the proper Pixel Size depending on density.
	}//func
	
	@Override
	public void onSizeChanged(int newW, int newH, int oldW, int oldH){
		super.onSizeChanged(newW, newH, oldW, oldH);
		mImgTrans.configChange(this);
		invalidate();
	}//func
	
	//========================================================================
	//Methods
	public void setImageBitmap(Bitmap bmp){
		if(bmp == null){
			if(mBitmap != null){
				mBitmap.recycle();
				mBitmap = null;
			}//if
			return;
		}//if
		
		//TODO : temporary, will not recycle in final version
		if(mBitmap != null){
			mBitmap.recycle();
			mBitmap = null;
		}//if

		mBitmap = bmp;
		mImgTrans.applyTo(bmp,this);
		mGestureMode = 0;

		invalidate();
	}//func

	public int getScaleMode(){ return mImgTrans.getScaleMode(); }
	public void setScaleMode(int sm){
		mImgTrans.setScaleMode(sm);
		invalidate();
	}//func
	
	public void setPanState(int i){
		mImgTrans.setPanSate(i);
	}//func

	
	//========================================================================
	//functions
	private void invokeGesture(int g){ if(mListener != null) mListener.onImageGesture(g); }
	
	public boolean shiftRight(){ //going right but down
		boolean isRight = mImgTrans.isOnRight(),isBottom = mImgTrans.isOnBottom(), isDone = false;
				
		if(isRight && isBottom) return false; //No more shifting
		else if(!isRight){ //Shift to the right
			isDone = mImgTrans.appyPanAnimate(this,mImgTrans.srcRect.width(),0,false);
		}else if(isRight){ //Shift all the way to the left, then shift down.
			isDone = mImgTrans.appyPanAnimate(this,mImgTrans.srcRect.left*-1,mImgTrans.srcRect.height(),false);
		}//if
		
		if(isDone) invalidate();
		return isDone;
	}//func
	
	public boolean shiftRight_rev(){ //going right but up
		boolean isLeft = mImgTrans.isOnLeft(),isTop = mImgTrans.isOnTop(), isDone = false;
				
		if(isLeft && isTop) return false; //No more shifting
		else if(!isLeft){ //Shift to the right
			isDone = mImgTrans.appyPanAnimate(this,mImgTrans.srcRect.width()*-1,0,false);
		}else if(isLeft){ //Shift all the way to the left, then shift down.
			isDone = mImgTrans.appyPanAnimate(this,mImgTrans.getRightBoundary(),mImgTrans.srcRect.height()*-1,false);
		}//if
		
		if(isDone) invalidate();
		return isDone;
	}//func
	
	public boolean shiftLeft(){ //going left by down
		boolean isLeft = mImgTrans.isOnLeft(),isBottom = mImgTrans.isOnBottom(), isDone = false;
		
		if(isLeft && isBottom) return false; //No more shifting
		else if(!isLeft){ //Shift to the Left
			isDone = mImgTrans.appyPanAnimate(this,mImgTrans.srcRect.width()*-1,0,false);
		}else if(isLeft){ //Shift all the way to the right, then shift down.
			isDone = mImgTrans.appyPanAnimate(this,mImgTrans.getRightBoundary(),mImgTrans.srcRect.height(),false);
		}//if
		
		if(isDone) invalidate();
		return isDone;
	}//func
	
	public boolean shiftLeft_rev(){  //Going left but going up
		boolean isRight = mImgTrans.isOnRight(),isTop = mImgTrans.isOnTop(), isDone = false;
				
		if(isRight && isTop) return false; //No more shifting
		else if(!isRight){
			isDone = mImgTrans.appyPanAnimate(this,mImgTrans.srcRect.width(),0,false);
		}else if(isRight){
			isDone = mImgTrans.appyPanAnimate(this,mImgTrans.srcRect.left*-1,mImgTrans.srcRect.height()*-1,false);
		}//if
		
		if(isDone) invalidate();
		return isDone;
	}//func
	
	
	//========================================================================
	//Drawing Events
	@Override
	protected void onDraw(Canvas canvas){
		if(mBitmap != null && ! mBitmap.isRecycled()){
			canvas.drawBitmap(mBitmap,mImgTrans.srcRect,mImgTrans.viewRect,mPaint);
		}else System.out.println("Bitmap is null");
	}//func	

	
	//========================================================================
	//Main Touch Event Handling
	@Override
	public boolean onTouchEvent(MotionEvent e){
		//Reset Action Mode when any motion is done.
		//When finishing a zoom, sometimes one finger lifts off before the other is off causing the scroll to start triggering.
		//So once a gesture starts, no other gesture should be allowed to run.
		int action = e.getActionMasked();
		if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_CANCEL) mGestureMode = 0;
		
		//...............................................................
		//One finger Zoom
		if(mGestureMode == 3){
			if(action == MotionEvent.ACTION_MOVE){
				float y = e.getY();
				float ratio = (float)Math.sqrt(mLastY/y);
				ratio *= (ratio >= 1)? 1.02 : 0.98; //Speed up the scaling a bit.
				
				if(mImgTrans.appyScaleRatio( ratio )) invalidate();
				
				mLastY = y;
			}//if
			return true;
		}//if
		
		//...............................................................
		//Only do scale gesture checking if there is more then one finger
		if(e.getPointerCount() > 1){
			if(mFingerGesture.onTouchEvent(e)) return true;
			mScaleGesture.onTouchEvent(e);
		}else mGesture.onTouchEvent(e);
		
		return true;
	}//func
	
	@Override
	public boolean onScale(ScaleGestureDetector dScale){
		if(mGestureMode == 0) mGestureMode = 1;
		else if(mGestureMode != 1) return false;
		
		float ratio = dScale.getScaleFactor();
		if(mImgTrans.appyScaleRatio(ratio)){ invalidate(); return true; }
		return false;
	}//func
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,float distanceY){
		if(mGestureMode == 0) mGestureMode = 2;
		else if(mGestureMode != 2) return false;
		
		//Can not start panning from the side boundaries. Will make the fling gesture unusable.
		float pos = e1.getX();
		if(pos <= mTapBoundary || pos >= this.getWidth()-mTapBoundary) return false;
		
		//Pane image
		if(mImgTrans.applyPan(distanceX,distanceY,true)) invalidate();
		return true;
	}//func
	
	@Override
	public void onLongPress(MotionEvent e){
		if(e.getPointerCount() > 1 || !(mGestureMode == 0 || mGestureMode == 3)) return; //One finger zooming, other wise other multi finger gesture get interrupted.
		if(mGestureMode == 0) mGestureMode = 3;
		mLastY = e.getY();
		
		Toast toast= Toast.makeText(mContext,"One Touch Zoom Enabled", Toast.LENGTH_SHORT);  
		toast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 0);
		toast.show();
	}//func
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,float velocityY){
		//System.out.format("FLing %d %d %d %n", (int)Math.abs(e1.getY() - e2.getY()), (int)Math.abs(e1.getX() - e2.getX()), (int)Math.abs(velocityX));
		if(Math.abs(velocityX) < 500) return false; //To slow, don't register as a fling.
		if(Math.abs(e1.getY() - e2.getY()) > 250) return false; //the swipe wasn't horizontal.
		if(Math.abs(e1.getX() - e2.getX()) < 200) return false; //the travel distance to small
		
		float x = e1.getX();
		if(x >= this.getWidth()-mTapBoundary){
			invokeGesture(FLINGRIGHT);
			return true;
		}else if(x <= mTapBoundary){
			invokeGesture(FLINGLEFT);
			return true;
		}//if
		
		return false; 
	}//func
		
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e){
		float x = e.getX();

		//System.out.format("TAP %f %d %n",x,this.getWidth());
		
		if(x >= this.getWidth()-mTapBoundary){
			invokeGesture(TAPRIGHT);
			return true;
		}else if(x <= mTapBoundary){
			invokeGesture(TAPLEFT);
			return true;
		}//if
		return false;
	}//func

	@Override
	public void onMFingGesture(int fingerCnt, boolean isLongPress){
		if(fingerCnt == 2 && isLongPress){
			invokeGesture(TWOFINGHOLD);
		}else if(fingerCnt == 2 && !isLongPress){
			invokeGesture(TWOFINGTAP); 
		}//if
	}//func
	
	@Override
	public boolean onDoubleTap(MotionEvent e){
		if(mImgTrans.hasScaleChanged()) mImgTrans.resetScale();
		else mImgTrans.zoomScale(4);
		
		invalidate();
		return true;
	}//func

	
	//========================================================================
	//Unused Events
	@Override public boolean onDown(MotionEvent e){ return true; }
	@Override public boolean onScaleBegin(ScaleGestureDetector dScale){ return true; }
	@Override public void onScaleEnd(ScaleGestureDetector arg0){}
	@Override public void onShowPress(MotionEvent e){}
	@Override public boolean onSingleTapUp(MotionEvent e){ return false; }//func
	@Override public boolean onDoubleTapEvent(MotionEvent e){ return false; }//func
}//cls
