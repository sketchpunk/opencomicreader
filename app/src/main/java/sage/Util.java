package sage;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.TypedValue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

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
	public static int dp2px(float n,Context context){ return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,n,context.getResources().getDisplayMetrics()); }//func
	public static int sp2px(float n,Context context){ return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,n,context.getResources().getDisplayMetrics()); }//func
	
	//...............................................................................
	public static int getMaximumTextureSize(){
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        // Initialise
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        // Query total number of configurations
        int[] totalConfigurations = new int[1];
        egl.eglGetConfigs(display, null, 0, totalConfigurations);

        // Query actual list configurations
        EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

        int[] textureSize = new int[1];
        int maximumTextureSize = 0;

        // Iterate through all the configurations to located the maximum texture size
        for (int i = 0; i < totalConfigurations[0]; i++)
        {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0])
            {
                maximumTextureSize = textureSize[0];
            }
        }

        // Release
        egl.eglTerminate(display);
        return maximumTextureSize;
    }

	public static boolean isDebugging(){ return ( 0 != ( SageApp.getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) ); }
}//cls
