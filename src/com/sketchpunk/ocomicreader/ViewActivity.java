package com.sketchpunk.ocomicreader;

import com.sketchpunk.ocomicreader.lib.ComicLoader;
import com.sketchpunk.ocomicreader.lib.ComicPageView;
import sage.listener.GestureListener;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class ViewActivity extends Activity implements 
	//View.OnTouchListener
	//,GestureListener.OnGestureListener
	ComicLoader.CallBack{

	private ComicPageView mImageView; //Main display of image
	private ComicLoader mComicLoad; //Object that will manage streaming and scaling images out of the archive file
	
    //private int mPageWidth,mPageHeight,mViewWidth,mViewHeight;
    //private float mBoundLeft = 0,mBoundBottom = 0,mLastDistance = 0;
    
    //tell the difference between touch input
    //static final int NOMODE=0,DRAGMODE=1,ZOOMMODE=2;
    //private int mMode = NOMODE;
    
    //private Matrix mMatrix = new Matrix();
    //private PointF mLastPos = new PointF();
   
    //private GestureListener mGestureListener;
    //private GestureDetector mGestureDetector;


	/*========================================================
	View Events*/
	@Override
	public void onDestroy(){
		//if(mComicLoad != null) mComicLoad.close();
		super.onDestroy();
	}//func
	
	@Override
	public void onResume(){
		super.onResume();
	}//func
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        this.getActionBar().hide();
        
        //.........................................
        Bundle b = this.getIntent().getExtras(); 
        String filePath = b.getString("path");
        
        System.out.println(filePath);
        
        /*
        //.........................................
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mViewWidth = size.x;
        mViewHeight = size.y;
		*/
        
        //.........................................        
        mImageView = (ComicPageView)this.findViewById(R.id.pageView);
        //mImageView.setOnClickListener(this);
        //mImageView.setOnTouchListener(this);
        //mImageView.setImageMatrix(mMatrix);
        
        //.........................................
        mComicLoad = new ComicLoader(this,mImageView);
        mComicLoad.loadArchive(filePath);
        mComicLoad.gotoPage(0);

        //.........................................
        //mGestureListener = new GestureListener(this);
        //mGestureDetector = new GestureDetector(mImageView.getContext(),mGestureListener);
        
        //.........................................
        //View root = mImageView.getRootView();
        //root.setBackgroundColor(0xFF000000);
    }//func


	/*========================================================
	page loading*/

	@Override
	public void onPageLoaded(boolean isSuccess,int pWidth,int pHeight){
		//if(!isSuccess) return;
		/*
		mMatrix.reset(); //new image loaded, lets reset the matrix.
		mPageWidth = pWidth;
		mPageHeight = pHeight;
		
		float ratio = (float)mViewHeight / mPageHeight; //Resize page to be the same height as view area.
		mMatrix.postScale(ratio,ratio);

		//float left = (((mPageWidth * ratio) - mViewWidth)/2) * -1f;
		//mMatrix.postTranslate(left,0);
		
		mImageView.setImageMatrix(mMatrix);
		*/
	}//func
    
    
	/*========================================================
	Zoom and Drag Events*/
	/*
    private void onDragStart(MotionEvent event){
    	//..................................
    	float[] m = new float[9];
		mMatrix.getValues(m);
        float scale = m[Matrix.MSCALE_X];

    	//..................................
    	float xmouse = event.getX();
		float ymouse = event.getY();
		
		mLastPos.set(xmouse,ymouse);
		
		mViewWidth = mImageView.getWidth();
		mViewHeight = mImageView.getHeight();
        mBoundLeft = ((mPageWidth*scale) - mViewWidth) * -1;
        mBoundBottom = ((mPageHeight*scale) - mViewHeight) * -1;
    }//func
    
    private void onDragMove(MotionEvent event){
        //..................................
    	float xmouse = event.getX();
		float ymouse = event.getY();
		float xDif = xmouse - mLastPos.x;
		float yDif = ymouse - mLastPos.y;
		
		//if(xDif < 0.5f && yDif < 0.5f) return;
    	
    	//..................................
		//Get current y,x,scale
    	float[] m = new float[9];
		mMatrix.getValues(m);
		float xmatrix = m[Matrix.MTRANS_X];
        float ymatrix = m[Matrix.MTRANS_Y];
        
		//..................................
		//Horizontal Bound
		if(xmatrix + xDif > 0) xDif = (xmatrix < 0)? xmatrix*-1 : 0;
		else if(xmatrix + xDif < mBoundLeft) xDif = (xmatrix < mBoundLeft)? xmatrix*-1 : 0;
			
		//..................................
		//Vertical Bound
		if(ymatrix + yDif > 0) yDif = (ymatrix < 0)? ymatrix*-1 : 0;
		else if(ymatrix + yDif < mBoundBottom) yDif = (ymatrix < mBoundBottom)? ymatrix*-1 : 0;
		
		mMatrix.postTranslate(xDif,yDif);
		mImageView.setImageMatrix(mMatrix);
		mLastPos.set(xmouse,ymouse);
    }//func
    
    private void onZoomStart(MotionEvent event){
    	float x = event.getX(1) - event.getX(0);
    	float y = event.getY(1) - event.getY(0);
    	mLastDistance = (float)Math.sqrt(x*x + y*y);    	
    }//func
    
    private void onZoom(MotionEvent event){        
    	float[] m = new float[9];
		mMatrix.getValues(m);
        float oldScale = m[Matrix.MSCALE_X];
    	
    	//..................................
    	//Get Distance
    	float x = event.getX(1) - event.getX(0);
    	float y = event.getY(1) - event.getY(0);
    	float distance = (float)Math.sqrt(x*x + y*y);
    	float ratio = distance / mLastDistance; //how much to add,remove from current scale.
    	float newScale = oldScale * ratio; //Calc new matrix scale to test what the height will become.
    	
    	//..................................
    	//Get Mid point
    	x = event.getX(1) + event.getX(0);
    	y = event.getY(1) + event.getY(0);
    	float xMid = x/2;
    	float yMid = y/2;
    	
    	//..................................
    	//Limit how small image can scale
    	float newHeight = mPageHeight * newScale;
    	
    	if(newHeight < mViewHeight){
    		ratio = (float)mViewHeight / (mPageHeight * oldScale);
    		newScale = oldScale * ratio;
    	}//if
    	
    	mMatrix.postScale(ratio,ratio,xMid,yMid);
	    	
    	//..................................
    	//when scaled small, need to realign x,y : keep at top and left
    	mMatrix.getValues(m);
        float ymatrix = m[Matrix.MTRANS_Y];
        float xmatrix = m[Matrix.MTRANS_X];
        
        ymatrix = (ymatrix > 0 || (mPageHeight * newScale) <= mViewHeight)? ymatrix*-1 : 0;        
        //xmatrix = (mPageWidth * newScale <= mViewWidth)? ((mViewWidth - (mPageWidth * newScale))/2) - xmatrix : 0; //this centers but acts up in portrait mode.
        xmatrix = (xmatrix > 0 || (mPageWidth * newScale) <= mViewWidth)? xmatrix*-1 : 0;
        
        if(ymatrix != 0 || xmatrix != 0) mMatrix.postTranslate(xmatrix,ymatrix);
        
    	//..................................    	
    	mImageView.setImageMatrix(mMatrix);
    	mLastDistance = distance;
    }//func
    
    
	@Override
	public boolean onTouch(View v, MotionEvent event){
		if(mMode != ZOOMMODE){
			if(mGestureDetector.onTouchEvent(event)) return true;
		}//if

		switch(event.getAction() & MotionEvent.ACTION_MASK){
			case MotionEvent.ACTION_POINTER_DOWN:
				mMode = ZOOMMODE;
				onZoomStart(event);
				System.out.println("Zooom Start");
				break;
			case MotionEvent.ACTION_DOWN:
				mMode = DRAGMODE;
				onDragStart(event);
				break;
			case MotionEvent.ACTION_MOVE:
				if(mMode == DRAGMODE) onDragMove(event);
				else if(mMode == ZOOMMODE) onZoom(event);
				break;
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_UP:
				mMode = NOMODE;
				break;
		}//if
		return true;
	}//func
*/
	
	/*========================================================
	Gesture Events*/
	/*
	@Override
	public boolean onDoubleTap(MotionEvent e) {
		System.out.println("DoubleTap");
		return true;
	}//func
	
	@Override
	public boolean onSingleTap(MotionEvent e){
		float x = e.getX();
		float area = mViewWidth/4;
		
		if(x <= area){
			if(!mComicLoad.prevPage()) Toast.makeText(this,"First Page",Toast.LENGTH_SHORT).show();
		}else if(x >= mViewWidth-area){
			if(!mComicLoad.nextPage()) Toast.makeText(this,"Last Page",Toast.LENGTH_SHORT).show();
		}else System.out.println("singleTap");

		return true;
	}//func

	@Override
	public void onFling(int direction){
		switch(direction){
			case GestureListener.LeftSwipe:
				System.out.println("Left Swipe");
				break;
			case GestureListener.RightSwipe:
				System.out.println("Right Swipe");
				break;
			case GestureListener.UpSwipe:
				System.out.println("Up Swipe");
				break;
			case GestureListener.DownSwipe:
				System.out.println("Down Swipe");
				break;
		}//switch
	}//func
	*/
}//cls
