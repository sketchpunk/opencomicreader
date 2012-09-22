package com.sketchpunk.ocomicreader.lib;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;


public class ComicPageView extends View implements GestureDetector.OnGestureListener,GestureDetector.OnDoubleTapListener{
	private Bitmap mBitmap; //WeakReference Change to this
	private Matrix mMatrix;
	private GestureDetector mGesture;
	private float mScale;
	private RectF mBound;
	private boolean mIsScrollH = false;
	private boolean mIsScrollV = false;
	
	public ComicPageView(Context context){ super(context); init(context); }//func
	public ComicPageView(Context context, AttributeSet attrs){ super(context, attrs); init(context); }
	public ComicPageView(Context context, AttributeSet attrs, int defStyle){ super(context, attrs, defStyle); init(context); }
	
	private void init(Context context){
		mMatrix = new Matrix();
		mGesture = new GestureDetector(context,this);
		mScale = 1;
		mBound = new RectF();
		mBound.left = 0;
		mBound.top = 0;
	}//func

	private void doImageCalc(){
		if(mBitmap == null) return;
		mBound.right = ((mBitmap.getWidth()*mScale) - this.getWidth()) * -1;
		mBound.bottom = ((mBitmap.getHeight()*mScale) - this.getHeight()) * -1;
		
		mIsScrollH = (mBitmap.getWidth()*mScale > this.getWidth());
		mIsScrollV = (mBitmap.getHeight()*mScale > this.getHeight());
		
		System.out.println(String.format("%d : %d : %f : %f",mBitmap.getWidth(),this.getWidth(),mBound.right,mScale));
		System.out.println(String.format("%d : %d : %f : %f",mBitmap.getHeight(),this.getHeight(),mBound.bottom,mScale));
	}//func
	
	public void setImageBitmap(Bitmap bmp){
		mBitmap = bmp;
		doImageCalc();
		this.invalidate();
	}//func
	
	/*========================================================
	*/
	@Override
	public boolean onTouchEvent(MotionEvent e){
		return mGesture.onTouchEvent(e);
	}//func
	
	@Override
	protected void onDraw(Canvas canvas){
		System.out.println("ONDRAW");
		if(mBitmap != null){
			canvas.drawBitmap(mBitmap,mMatrix,null);
			System.out.println("draw bmp");
		}//func
	}//func	

	
	/*========================================================
	*/
	@Override
	public boolean onDown(MotionEvent e){//needed to return tree to get most of the gestures to work.
		System.out.println("onDown");
		return true;
	}//func
	
	
	/*========================================================
	*/
	@Override
	public void onLongPress(MotionEvent e) {
		System.out.println("onLongPress");
		// TODO Auto-generated method stub
	}//func
	
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e){
		System.out.println("onSingleTap");
		return true;
	}//func
	
	@Override
	public boolean onDoubleTap(MotionEvent e){
		System.out.println("onDoubleTap");
		mMatrix.reset();
		invalidate();
		return true;
	}//func

	//don't need it but it must be override.


	/*========================================================
	*/	
	@Override
	public boolean onFling(MotionEvent e1,MotionEvent e2,final float xVelocity,final float yVelocity) {
		//System.out.println("onFling");
		
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run(){onFlingAnimate(xVelocity,yVelocity);}
		});
		
		//thread.start();
	
		return true;
	}//func
	
	private void onFlingAnimate(float xVelocity,float yVelocity){
		System.out.println("onFligAnimate");
		float timeRatio = 0.2f; //40% of a second = 400 milliseconds
		float xDistance = (timeRatio * xVelocity/5); //overshoot is pretty large
		float yDistance = (timeRatio * yVelocity/5);
		
		Matrix mxStart = new Matrix(mMatrix);
		OvershootInterpolator easing = new OvershootInterpolator();
		//LinearInterpolator easing = new LinearInterpolator();
		
		long sTime = System.currentTimeMillis();
		long eTime = (sTime + (long)(1000*timeRatio));
		float lenTime = (float)(eTime - sTime);
		
		System.out.println(sTime);
		System.out.println(eTime);
		System.out.println(lenTime);
		System.out.println((1000*timeRatio));
		
		float percTime = 0;
		long cTime = 0;
		while(percTime < 1.0f){
			cTime = System.currentTimeMillis();
			
			percTime = (float)(cTime - sTime) / lenTime;
			float percDistance = easing.getInterpolation(percTime);
			
			final float xPos = percDistance * xDistance;
			final float yPos = percDistance * yDistance;
			
			
			mMatrix.set(mxStart);
			mMatrix.postTranslate(xPos,yPos);
			this.postInvalidate();
			
			
			System.out.println(String.format("%f %f",xPos,yPos));
			
			if(percTime >= 1.0f) break;
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			}
		}//while

		System.out.println("animateend");
	}//func
	

	@Override
	public boolean onScroll(MotionEvent e1,MotionEvent e2,float xDistance,float yDistance){
		if(!mIsScrollH && !mIsScrollV) return true;
		xDistance *= -1;
		yDistance *= -1;

    	//..................................
		//Get current y,x
    	float[] m = new float[9];
		mMatrix.getValues(m);
		float xmatrix = m[Matrix.MTRANS_X];
        float ymatrix = m[Matrix.MTRANS_Y];
        
        //..................................
      	//Horizontal Bound
        if(mIsScrollH){
        	if(xmatrix + xDistance > mBound.left) xDistance = (xmatrix > 0)? xmatrix*-1 : 0;
        	else if(xmatrix + xDistance < mBound.right) xDistance = mBound.right-xmatrix;
		}else xDistance = 0;//if

        //.................................. 
  		//Vertical Bound        
        //System.out.println(String.format("%f - %f - %f - %f",mBound.bottom,ymatrix,yDistance,ymatrix+yDistance));
        
        if(mIsScrollV){
        	if(ymatrix + yDistance > mBound.top) yDistance = (ymatrix < 0)? ymatrix*-1 : 0;
        	else if(ymatrix + yDistance < mBound.bottom) yDistance = (ymatrix < mBound.bottom)? ymatrix*-1 : 0;
        }else yDistance = 0;

  		//..................................
        //don't refresh if there is not need.
        if(xDistance != 0 || yDistance != 0){
        	mMatrix.postTranslate(xDistance,yDistance);
        	invalidate();
        }//if
        
		return true;
	}//func

	
	/*========================================================
	*/
	@Override
	public boolean onDoubleTapEvent(MotionEvent e){ return false; }
	
	@Override
	public void onShowPress(MotionEvent e){}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0){ return false; }
}//cls
