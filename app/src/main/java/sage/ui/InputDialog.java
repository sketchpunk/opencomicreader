package sage.ui;

import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.EditText;

public abstract class InputDialog extends AlertDialog.Builder implements OnClickListener{
	private final EditText mEditText;
	
	public InputDialog(Context context, String title, String msg, String defaultValue){
		super(context);
		if(title != null) this.setTitle(title);
		if(msg != null) this.setMessage(msg);
		
		mEditText = new EditText(context);
		if(defaultValue != null) mEditText.setText(defaultValue);
		this.setView(mEditText);
		
		this.setPositiveButton("OK",this);
		this.setNegativeButton("Cancel",this);
	}//func
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(which == DialogInterface.BUTTON_POSITIVE){
			if(onOk(mEditText.getText().toString())) dialog.dismiss();
		}else dialog.dismiss();
	}//func
	
	abstract public boolean onOk(String txt);
}//cls
