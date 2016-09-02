package com.sketchpunk.ocomicreader.lib;

import com.sketchpunk.ocomicreader.ui.GestureImageView;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;

public class ImgTransform{
	public static final int SCALE_NONE = 0;
	public static final int SCALE_HEIGHT = 1;
	public static final int SCALE_WIDTH = 2;
	public static final int SCALE_AUTO = 3;
	
	public static final int INITPAN_LEFT = 1;
	public static final int INITPAN_RIGHT = 2;
	
	private float mSrcWidth,mSrcHeight,mSrcScale
	,mViewWidth,mViewHeight,mOrgSrcScale;
	private int mScaleMode = 0, mPanState = INITPAN_RIGHT;
	private boolean mIsInit = false;
	public Rect srcRect = new Rect(), viewRect = new Rect();
	
	//TODO, need to do an initial state, where once applyTo is called its set, after the scale has been applied, unset
	// The initial state will change the LEFT position if the image is zoomed in. Also a new switch for StartingPos = 1 left, 2 right.
	
	public void applyTo(Bitmap bmp,View view){
		mSrcWidth = bmp.getWidth();
		mSrcHeight = bmp.getHeight();
		mViewWidth = view.getWidth();
		mViewHeight = view.getHeight();
		
		srcRect.setEmpty();
		viewRect.setEmpty();
		mIsInit = true;
		calcScale();
	}//func
	
	public void configChange(View view){
		System.out.println(mViewWidth);
		mViewWidth = view.getWidth();
		mViewHeight = view.getHeight();
		System.out.println(mViewWidth);
		
		srcRect.setEmpty();
		viewRect.setEmpty();
		mIsInit = true;
		calcScale();
	}//func
	
	//========================================================================	
	public boolean isOnLeft(){ return (srcRect.left == 0); }
	public boolean isOnRight(){ return (srcRect.left >= mSrcWidth - srcRect.width()); }
	public boolean isOnTop(){ return (srcRect.top == 0); }
	public boolean isOnBottom(){ return (srcRect.top >= mSrcHeight - srcRect.height()); }
	public int getRightBoundary(){ return (int)(mSrcWidth - srcRect.width()); }
	
	public void setPanSate(int i){ mPanState = i; }
	
	//========================================================================
	//scale methods
	public void zoomScale(int x){
		applyScale(mSrcScale * x);
	}//func
	
	public void setScaleMode(int sm){
		mScaleMode = sm;
		srcRect.setEmpty();
		viewRect.setEmpty();
		calcScale();
	}//if
	
	public int getScaleMode(){ return mScaleMode; }
	
	public void resetScale(){
		srcRect.setEmpty();
		viewRect.setEmpty();
		mIsInit = true;
		applyScale(mOrgSrcScale);
	}//func
	
	public boolean hasScaleChanged(){ return mSrcScale != mOrgSrcScale; }
	
	public float getReverseScale(){ return srcRect.width() / mViewWidth; }
	public float getScale(){ return mSrcScale; }
	
	//========================================================================
	//calculations
	private void calcScale(){
		int scaleMode = mScaleMode;
		if(scaleMode == SCALE_AUTO) scaleMode = (mViewWidth > mViewHeight)?SCALE_WIDTH:SCALE_HEIGHT; //Auto Width/Height 
	
		switch(scaleMode){
			//No Scale
			case SCALE_NONE:	mOrgSrcScale = mSrcScale = 1; break;

			//Scale Width
			case SCALE_WIDTH:	mOrgSrcScale = mSrcScale = mViewWidth / mSrcWidth; break;

			//Scale Height
			case SCALE_HEIGHT:	mOrgSrcScale = mSrcScale = mViewHeight / mSrcHeight; break;
		}//switch
		
		applyScale(mSrcScale);
	}//func
	
	private boolean calcPan(float distanceX, float distanceY, boolean useScale, int[] outX, int[] outY){
		System.out.println("calcPan");
		int x=0, y=0, h = srcRect.height(), w = srcRect.width();
		
		//Adjust the panning to how much the image is scaled.
		//When using onScroll gesture, the we need to apply the scale, but if doing direct control then don't apply.
		if(useScale){
			distanceX = (float)Math.ceil(distanceX / mSrcScale);
			distanceY = (float)Math.ceil(distanceY / mSrcScale);
		}//if

		//...........................................................
		if(h < mSrcHeight){
			int newY = (int)(srcRect.top + distanceY);
			int yBound = Math.round(mSrcHeight - h);
			
			//If new position is between the boundary, let it pass.
			if(newY >= 0 && newY <= yBound) y = (int)distanceY;
			
			//if the new position is less then zero, but the current top is still greater then zero, calc the remaining distance to reach zero.
			else if(newY < 0 && srcRect.top > 0) y = srcRect.top * -1;
			
			//if the new position is over the Y limit but the current top is less then the limit, cal the remaining distance to reach the limit.
			else if(newY > yBound  && srcRect.top < yBound) y = yBound - srcRect.top;
		}//if
	
		//...........................................................
		if(w < mSrcWidth){
			int newX = (int)(srcRect.left + distanceX);
			int xBound = Math.round(mSrcWidth - w);
			
			//If new position is between the boundary, let it pass.
			if(newX >= 0 && newX <= xBound) x = (int)distanceX;
			
			//if the new position is less then zero, but the current top is still greater then zero, calc the remaining distance to reach zero.
			else if(newX < 0 && srcRect.left > 0) x = srcRect.left * -1;
			
			//if the new position is over the Y limit but the current top is less then the limit, cal the remaining distance to reach the limit.
			else if(newX > xBound  && srcRect.left < xBound) x = xBound - srcRect.left;
		}//if
		
		//...........................................................
		if(x != 0 || y != 0){
			outX[0] = x;
			outY[0] = y;
			return true;
		}//if
		
		return false;
	}//func
	
	//========================================================================
	//Apply Transformations
	public boolean appyScaleRatio(float ratio){ return applyScale(mSrcScale * ratio); }//func
	
	public boolean applyScale(float srcScale){
		int srcRectWidth,srcRectHeight,viewRectWidth,viewRectHeight
			,srcRectLeft,srcRectTop,viewRectLeft,viewRectTop
			,srcScaleWidth	= Math.round(mSrcWidth * srcScale)
			,srcScaleHeight	= Math.round(mSrcHeight * srcScale);
		
		//.................................................
		//Put a limit of how small to scale the image
		if(srcScaleWidth < 200 || srcScaleHeight < 200) return false;
		
		viewRectLeft = viewRectTop = 0;
		srcRectLeft = srcRect.left;
		srcRectTop = srcRect.top;
		
		//.................................................
		//Calc the size of the rectangles
		if(srcScaleWidth < mViewWidth){
			//source too small, shrink view rect to match
			srcRectWidth = (int)mSrcWidth;
			viewRectWidth = srcScaleWidth;
			
			viewRectLeft = Math.round((mViewWidth - viewRectWidth) / 2);
		}else{ 
			//source too big, crop the image rect to the right scale
			srcRectWidth = Math.round(mSrcWidth * (mViewWidth/srcScaleWidth));
			viewRectWidth = (int)mViewWidth;
			
			if(mIsInit){
				srcRectLeft = (mPanState == INITPAN_LEFT)? 0 : (int)mSrcWidth - srcRectWidth;
			}else{
				//Keep the scaled image centered to the viewport
				srcRectLeft = Math.round(((srcRect.width() - srcRectWidth)/2) + srcRectLeft);
				
				//Keep widthin bounds
				if(srcRectLeft < 0) srcRectLeft = 0;
				else if(srcRectLeft > mSrcWidth - srcRectWidth) srcRectLeft = (int)mSrcWidth - srcRectWidth;
			}//if
		}//if
		
		if(srcScaleHeight < mViewHeight){
			srcRectHeight = (int)mSrcHeight;
			viewRectHeight = srcScaleHeight;
			
			viewRectTop = Math.round((mViewHeight - viewRectHeight) / 2);
		}else{ 
			srcRectHeight = Math.round(mSrcHeight * (mViewHeight/srcScaleHeight));
			viewRectHeight = (int)mViewHeight;
			
			if(mIsInit) srcRectTop = 0;
			else{
				//Keep the scaled image centered to the viewport
				srcRectTop = Math.round(((srcRect.height() - srcRectHeight)/2) + srcRectTop);
			
				if(srcRectTop < 0) srcRectTop = 0;
				else if(srcRectTop > mSrcHeight - srcRectHeight) srcRectTop = (int)mSrcHeight-srcRectHeight;
			}//if
		}//if
		
		//.................................................
		//Save Updated Scale
		mSrcScale		= srcScale;
	
		//Update Source Rectangle
		srcRect.left	= srcRectLeft;
		srcRect.top		= srcRectTop;
		srcRect.right	= srcRectLeft+srcRectWidth;
		srcRect.bottom	= srcRectTop + srcRectHeight;
		
		//Update View Rectangle
		viewRect.left	= viewRectLeft;
		viewRect.top	= viewRectTop;
		viewRect.right	= viewRectLeft + viewRectWidth;
		viewRect.bottom	= viewRectTop + viewRectHeight;
		
		System.out.println("Scale Applied");
		System.out.println(viewRect.width());
		
		mIsInit = false;
		return true;
	}//func
	
	public boolean applyPan(float distanceX, float distanceY, boolean useScale){
		int[] outX = {0}, outY = {0}; //Since Java doesn't have out params, pass array by ref and use its updated values.
		if(calcPan(distanceX,distanceY,useScale,outX,outY)){
			srcRect.offset(outX[0],outY[0]);
			return true;
		}//if
		return false;
	}//func
	
	ValueAnimator mAnimPan = null;
	public boolean appyPanAnimate(final GestureImageView view,float distanceX, float distanceY, boolean useScale){
		//.........................................................
		//Disable any currently running Animations.
		if(mAnimPan != null && (mAnimPan.isRunning() || mAnimPan.isStarted())){
			mAnimPan.end(); mAnimPan = null;
		}//if
		
		//.........................................................
		int[] outX = {0}, outY = {0}; //Since Java doesn't have out params, pass array by ref and use its updated values.
		if(calcPan(distanceX,distanceY,useScale,outX,outY)){
			
			final int startX = srcRect.left,
				startY = srcRect.top,
				difX = outX[0],
				difY = outY[0];

			mAnimPan = ValueAnimator.ofFloat(0f,1f).setDuration(400);
			mAnimPan.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
				@Override public void onAnimationUpdate(ValueAnimator v){
					float fraction = v.getAnimatedFraction();

					int x = Math.round(startX + (difX * fraction));
					int y = Math.round(startY + (difY * fraction));
					
					srcRect.offsetTo(x,y);
					view.invalidate();
				}//func
			});
			
			mAnimPan.start();
			return true;
		}//if
		return false;
	}//func
	
}//cls
