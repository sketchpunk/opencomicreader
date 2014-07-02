package sage.listener;

import java.util.Calendar;

import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class MultiFingerGestureDetector{
	private int mPointerCnt = 0;
	private long mStartTime;
	private int TIMEOUT_TAP = ViewConfiguration.getTapTimeout();
	private int TIMEOUT_DTAP = ViewConfiguration.getDoubleTapTimeout();
	private int TIMEOUT_LONG = ViewConfiguration.getLongPressTimeout();
	private boolean mLongActive = false;
	private OnGestureListener mListener = null;
	private float mStartX, mStartY;
	private boolean mIgnore = false;
	
	public static interface OnGestureListener{
		public void onMFingGesture(int fingerCnt,boolean isLongPress);
	}//interface
	
	public MultiFingerGestureDetector(OnGestureListener listener){
		mListener = listener;
	}//func
	
	
	public boolean onTouchEvent(MotionEvent e){
		Long now;
		int pCnt;
		float x,y;
		
		switch(e.getActionMasked()){
			//.........................................................
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_DOWN:
				pCnt = e.getPointerCount();
				mLongActive = false;
				mIgnore = false;
				
				if(pCnt >= 2 && pCnt <= 3){
					mStartX = e.getRawX();
					mStartY = e.getRawY();
					mPointerCnt = pCnt;
					mStartTime = Calendar.getInstance().getTimeInMillis();;
				}//if
			break;
			
			//.........................................................
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				if(mIgnore || mLongActive || mPointerCnt == 0 || e.getPointerCount() != mPointerCnt) return false;
				
				x = e.getRawX();
				y = e.getRawY();
				now = Calendar.getInstance().getTimeInMillis();

				//System.out.format("MultiTabe %d %d %d %d %n",(int)Math.abs(x-mStartX),(int)Math.abs(y-mStartY),(long)now - mStartTime,TIMEOUT_TAP);
				if(Math.abs(x-mStartX) <= 5 && Math.abs(y-mStartY) <= 5 && now - mStartTime <= TIMEOUT_TAP){
					if(mListener != null) mListener.onMFingGesture(e.getPointerCount(),false);
					return true;
				}//id
			break;
			
			//.........................................................
			case MotionEvent.ACTION_CANCEL:
			break;
			
			//.........................................................
			default:
				if(mIgnore || mLongActive) return false;

				x = e.getRawX();
				y = e.getRawY();
				
				//If there is any movement, dont bother trying to determine tap
				if(Math.abs(x-mStartX) > 2 && Math.abs(y-mStartY) > 2){
					mIgnore = true;
					return false;
				}//if
				
				pCnt = e.getPointerCount();
				now = Calendar.getInstance().getTimeInMillis();
				
				if(pCnt == mPointerCnt && now-mStartTime >= TIMEOUT_LONG){
					if(mListener != null) mListener.onMFingGesture(pCnt,true);
					mLongActive = true;
					return true;
				}//if
			break;
		}//func
		
		return false;
	}//func
}//cls
