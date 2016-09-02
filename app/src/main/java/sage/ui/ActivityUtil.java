package sage.ui;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

public class ActivityUtil {
	//---------------------------------------------------------------------------------------
	//Handle Screen Orientation
	public static final int ORIENTATION_DEVICE = 0;
	public static final int ORIENTATION_PORTRAIT = 1;
	public static final int ORIENTATION_LANDSCAPE = 2;
	private static int mOrientation = 0;
	
	public static int getScreenOrientation(){ return mOrientation; }
	public static void setScreenOrientation(Activity act,int mode){
		mOrientation = mode;
		switch(mOrientation){
			case ORIENTATION_DEVICE:	act.setRequestedOrientation(act.getRequestedOrientation()); break;
			case ORIENTATION_PORTRAIT:	act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT); break;
			case ORIENTATION_LANDSCAPE:	act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); break;
		}//switch
	}//func
	
	//---------------------------------------------------------------------------------------
	public static final int FEATURE_FULLSCREEN = 1;
	public static final int FEATURE_KEEPSCREENON = 2;
	public static void setFeatures(final Activity act,int features){
		int winFlags = 0;
		//.................................................................
    	if((features & FEATURE_FULLSCREEN) != 0){
    		final View rootView = act.getWindow().getDecorView();

    		//enable immersive mode if device build version is at 4.4 or higher
    		if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
    			
    			System.out.println("immersive mode");
    			rootView.setSystemUiVisibility(
    				View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
    					| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
    					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    		}else{ //else enable legacy fullscreen mode
        		winFlags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;  
                rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                
                //need to set event to hide navigation again.
                rootView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
					@Override public void onSystemUiVisibilityChange(int visibility){
						Thread thread = new Thread(){
						    @Override public void run(){
						        try{ Thread.sleep(7000); }catch(InterruptedException e){}
						        act.runOnUiThread(new Runnable() {
						            @Override public void run(){ rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);}
						        });
						    }//run
						};
						thread.start();
					}//func
				});
    		}//if
    	}//if
    	
    	//.................................................................
    	//Keep screen on
    	if((features & FEATURE_KEEPSCREENON) != 0) winFlags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		
    	//.................................................................
		//Apply Flags
    	if(winFlags != 0) act.getWindow().addFlags(winFlags);
	}//func

	public static void setImmersiveModeOn(final Activity act){
		//enable immersive mode if device build version is at 4.4 or higher
		if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
			final View rootView = act.getWindow().getDecorView();
			rootView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
					| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}//if
	}//func

}//cls
