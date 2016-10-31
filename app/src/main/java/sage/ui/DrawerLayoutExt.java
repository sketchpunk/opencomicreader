package sage.ui;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

/* --NOTES--
    DrawerLayout's onTouchEvent does not allow bubbling up the events, so when trying to use GestureDetector will not work.
    This Extended version allows touch events to bubble up while allowing the drawer functionality to continue to function as intended.
*/

public class DrawerLayoutExt extends DrawerLayout{
	//region Vars,Constructors
	private View mDrawer = null;
	private float mEdge = 30;

	public DrawerLayoutExt(Context context){ super(context); }
	public DrawerLayoutExt(Context context, AttributeSet attrs){ super(context, attrs); }
	//endregion

	//region Layout Events
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		//Find out which child is the drawer, they have to have a gravity set to START or END.
		LayoutParams lp;
		for(int i=0; i < this.getChildCount(); i++){
			lp = (LayoutParams) getChildAt(i).getLayoutParams();
			if(lp.gravity == Gravity.START || lp.gravity == Gravity.END){
				mDrawer = getChildAt(i);
				break;
			}
		}
	}
	//endregion

	//region Touch events
	@Override
	public boolean onTouchEvent(MotionEvent evt){
		super.onTouchEvent(evt);

		//System.out.println("DrawerLayoutext ");
		//System.out.println(isDrawerOpen(mDrawer));
		//System.out.println(isDrawerVisible(mDrawer));

		if(isDrawerOpen(mDrawer) || isDrawerVisible(mDrawer)) return true; //Event is being handled by DrawerLayout
		else if(evt.getAction() == MotionEvent.ACTION_DOWN
				&& evt.getX() > mEdge
				&& evt.getX() < this.getWidth()-mEdge) return false; //Else if not next to the edge, allow event to bubble up.

		return true;
	}
	//endregion
}
