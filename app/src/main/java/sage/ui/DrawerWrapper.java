package sage.ui;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;

public class DrawerWrapper{
	//region vars, properties, constructor
	public ActionBarDrawerToggle mToggle = null;
	private DrawerLayout mLayout = null;
	private ActionBarActivity mActivity = null;

	public DrawerWrapper(ActionBarActivity act, int layout, int rShadow,int dir){
		mActivity = act;
		mLayout = (DrawerLayout) act.findViewById(layout);
		if(rShadow != 0) mLayout.setDrawerShadow(rShadow,(dir == 0)? GravityCompat.START:GravityCompat.END);
	}
	//endregion

	//region getter/setters
	public boolean isOpen(){ return mLayout.isDrawerOpen(GravityCompat.START); }

	public void setListener(DrawerLayout.DrawerListener listener){ mLayout.setDrawerListener(listener); }
	//endregion

	//region control state of drawer
	public void openLeft(){ mLayout.openDrawer(GravityCompat.START); }
	public void closeLeft(){ mLayout.closeDrawer(GravityCompat.START); }
	public void toggleLeft(){
		if(mLayout.isDrawerOpen(GravityCompat.START)) mLayout.closeDrawer(GravityCompat.START);
		else mLayout.openDrawer(GravityCompat.START);
	}

	public void openRight(){ mLayout.openDrawer(GravityCompat.END); }
	public void toggleRight(){
		if(mLayout.isDrawerOpen(GravityCompat.END)) mLayout.closeDrawer(GravityCompat.END);
		else mLayout.openDrawer(GravityCompat.END);
	}
	//endregion

	//region configure the toolbar home button to toggle drawer
	public void configToggle(Toolbar tbar){
		mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mActivity.getSupportActionBar().setHomeButtonEnabled(true);

		mToggle = new ActionBarDrawerToggle(mActivity,mLayout,tbar,0,0);//
		mToggle.setDrawerIndicatorEnabled(true);
		mLayout.setDrawerListener(mToggle);
		mToggle.syncState();
	}
	//endregion
}