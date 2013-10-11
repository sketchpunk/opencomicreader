package sage.ui;

import com.sketchpunk.ocomicreader.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.widget.NumberPicker;

public class Dialogs{
	public static void ConfirmBox(Context context,String title,String msg,DialogInterface.OnClickListener onOk){
		AlertDialog.Builder abBuilder = new AlertDialog.Builder(context);
		abBuilder.setTitle(title);
		abBuilder.setMessage(msg);
		abBuilder.setCancelable(false);
		abBuilder.setNegativeButton(R.string.dialog_confirmbot_button_no,null);
		abBuilder.setPositiveButton(R.string.dialog_confirmbot_button_yes,onOk);
		abBuilder.show();
	}//func
	
	public static void NumPicker(Context context,String title,int iMin,int iMax,int iVal,final DialogInterface.OnClickListener onOk){
		//Create main UI objects
		AlertDialog.Builder abBuilder = new AlertDialog.Builder(context);
		final NumberPicker np = new NumberPicker(context);
		
		//setup dialog
		abBuilder.setTitle(title);
		abBuilder.setNegativeButton(R.string.dialog_numpicker_button_cancel,null);
		abBuilder.setPositiveButton(R.string.dialog_numpicker_button_ok,new DialogInterface.OnClickListener(){  
            public void onClick(DialogInterface dialog, int whichButton){ onOk.onClick(null,np.getValue()); return; }  
		});
		
		//setup num picker
		np.setMinValue(iMin);
		np.setMaxValue(iMax);
		np.setValue(iVal);
		abBuilder.setView(np);
		
		//display dialog WITHOUT the keyboard
		final AlertDialog dialog = abBuilder.create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		dialog.show();
	}//funcs
}//cls

