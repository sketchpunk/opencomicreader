package sage.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.NumberPicker;

public class Dialogs{
	public static void ConfirmBox(Context context,String title,String msg,DialogInterface.OnClickListener onOk){
		AlertDialog.Builder abBuilder = new AlertDialog.Builder(context);
		abBuilder.setTitle(title);
		abBuilder.setMessage(msg);
		abBuilder.setCancelable(false);
		abBuilder.setNegativeButton("No",null);
		abBuilder.setPositiveButton("Yes",onOk);
		abBuilder.show();
	}//func
}//cls

