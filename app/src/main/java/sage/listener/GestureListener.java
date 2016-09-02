package sage.listener;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class GestureListener extends GestureDetector.SimpleOnGestureListener{
	public static interface OnGestureListener{
		boolean onDoubleTap(MotionEvent e);
		boolean onSingleTap(MotionEvent e);
		void onFling(int direction);
	}//interface
	
	
	/*========================================================
	*/
	private OnGestureListener mCallback;
	public static final int LeftSwipe = 1;
	public static final int RightSwipe = 2;
	public static final int UpSwipe = 3;
	public static final int DownSwipe = 4;
	
    private float mMinDistance;
    private float mMaxOffPath;
	
    
	/*========================================================
	*/
	public GestureListener(Context context){
		mCallback = (OnGestureListener)context;
		
        final ViewConfiguration vc = ViewConfiguration.get(context);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        //.........................................
        mMinDistance = vc.getScaledPagingTouchSlop() * dm.density; //min distance(x) to determine a swipe or a click
        //VELOCITY = vc.getScaledMinimumFlingVelocity(); //Min of Pixels per seconds to determine fling
        mMaxOffPath = mMinDistance * 2;
	}//func
	

	/*========================================================
	*/
	@Override
	public boolean onDoubleTap(MotionEvent e){ return mCallback.onDoubleTap(e); }//func

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e){ return mCallback.onSingleTap(e); }//func
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){    			
		boolean rtn = true;
		float xDif = e1.getX() - e2.getX();
		float yDif = e1.getY() - e2.getY();
		//....................................
		if(Math.abs(yDif) <= mMaxOffPath){
			if(xDif > 0){
				mCallback.onFling(GestureListener.LeftSwipe);
			}else if(xDif < 0){
				mCallback.onFling(GestureListener.RightSwipe);
			}else rtn = false;

		//....................................					
		}else if(Math.abs(xDif) <= mMaxOffPath){
			if(yDif > 0){
				mCallback.onFling(GestureListener.UpSwipe);
			}else if(yDif < 0){
				mCallback.onFling(GestureListener.DownSwipe);
			}else rtn = false;

		//....................................
		}else rtn = false;
		return rtn;
	}//func
}//cls