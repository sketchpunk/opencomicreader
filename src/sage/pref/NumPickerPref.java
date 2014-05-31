package sage.pref;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

public class NumPickerPref extends DialogPreference {
	private NumberPicker mNumPicker = null;
	private int mNum = 0;
	private int mMax = 0;
	private int mMin = 0;
	private int mDefault = 0;

	public NumPickerPref(Context context, AttributeSet attrs) {
		super(context, attrs);

		for (int i = 0; i < attrs.getAttributeCount(); i++) {
			String attr = attrs.getAttributeName(i), val = attrs
					.getAttributeValue(i);

			if (attr.equalsIgnoreCase("maxnum"))
				mMax = Integer.parseInt(val);
			else if (attr.equalsIgnoreCase("minnum"))
				mMin = Integer.parseInt(val);
			else if (attr.equalsIgnoreCase("defaultvalue"))
				mDefault = Integer.parseInt(val);
		}
	}

	@Override
	protected View onCreateDialogView() {
		mNumPicker = new NumberPicker(getContext());
		return (mNumPicker);
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);

		mNumPicker.setMinValue(mMin);
		mNumPicker.setMaxValue(mMax);
		mNumPicker.setValue(mNum);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			// When using the keyboard to change the size, need to unfocus for
			// the data to be saved into the control.
			mNumPicker.clearFocus();
			this.mNum = mNumPicker.getValue();
			this.persistInt(this.mNum);
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return (a.getInt(index, mDefault));
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		try {
			if (restoreValue)
				this.mNum = this.getPersistedInt(mDefault);
			else
				this.mNum = (defaultValue != null) ? Integer
						.parseInt(defaultValue.toString()) : 1;
		} catch (Exception e) {
			this.mNum = this.mDefault;
			System.err
					.println("Error on SetInitialValue for NumberPickerPref : "
							+ e.getMessage());
		}
	}
}
