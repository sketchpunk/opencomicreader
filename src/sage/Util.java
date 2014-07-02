package sage;

import android.util.TypedValue;
import java.util.concurrent.atomic.AtomicInteger;

public class Util{
	
	private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

	/*Note: If Targeting API17+, can use View.generateViewId() else use this function
	Generate a value suitable for use in {@link #setId(int)}.
	This value will not collide with ID values generated at build time by aapt for R.id.*/
	public static int generateViewId() {
		for(;;){
			final int result = sNextGeneratedId.get(); // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
			int newValue = result + 1;

			if(newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
			if(sNextGeneratedId.compareAndSet(result,newValue)) return result;
		}//for
	}//func

	//...............................................................................
	//Conversion
	//public static int dp2px(float n){ return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,n,zApp.getContext().getResources().getDisplayMetrics()); }//func
	//public static int sp2px(float n){ return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,n,zApp.getContext().getResources().getDisplayMetrics()); }//func
}//cls
