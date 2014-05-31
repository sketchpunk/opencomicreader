package sage.ui;

import com.sketchpunk.ocomicreader.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.WindowManager;
import android.widget.NumberPicker;
import android.widget.TextView;

public class Dialogs {
	public static void About(Context context, CharSequence sMsg) {
		final TextView msg = new TextView(context);
		final SpannableString str = new SpannableString(sMsg);
		Linkify.addLinks(str, Linkify.WEB_URLS);
		msg.setText(str);
		msg.setMovementMethod(LinkMovementMethod.getInstance());
		msg.setPadding(10, 2, 10, 2);

		// Create Dialog
		AlertDialog.Builder abBuilder = new AlertDialog.Builder(context)
				.setIcon(R.drawable.ic_launcher).setTitle(R.string.app_name)
				.setView(msg);

		abBuilder.create().show();
	}

	public static void ConfirmBox(Context context, String title, String msg,
			DialogInterface.OnClickListener onOk) {
		AlertDialog.Builder abBuilder = new AlertDialog.Builder(context)
				.setTitle(title).setMessage(msg).setNegativeButton("No", null)
				.setPositiveButton("Yes", onOk).setCancelable(false);
		abBuilder.show();
	}

	public static void NumPicker(Context context, String title, int iMin,
			int iMax, int iVal, final DialogInterface.OnClickListener onOk) {
		// Create main UI objects
		AlertDialog.Builder abBuilder = new AlertDialog.Builder(context);
		final NumberPicker np = new NumberPicker(context);

		// setup dialog
		abBuilder.setTitle(title);
		abBuilder.setNegativeButton("Cancel", null);
		abBuilder.setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						onOk.onClick(null, np.getValue());
						return;
					}
				});

		// setup num picker
		np.setMinValue(iMin);
		np.setMaxValue(iMax);
		np.setValue(iVal);
		abBuilder.setView(np);

		// display dialog WITHOUT the keyboard
		final AlertDialog dialog = abBuilder.create();
		dialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		dialog.show();
	}
}