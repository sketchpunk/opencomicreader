package sage.adapter;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

public class SqlCursorAdapter extends CursorAdapter {
	public static interface AdapterCallback {
		public View onCreateListItem(View v);

		public void onBindListItem(View v, Cursor c);
	}

	private LayoutInflater mInflater;
	private Context mContext;
	private AdapterCallback mCallback;
	private int mItemLayout;
	private Map<String, Integer> mCursorMap = new HashMap<String, Integer>();

	/*
	 * Main
	 */
	public SqlCursorAdapter(Context context) {
		super(context, null, 0);
		sharedInit(context, null);
	}

	public SqlCursorAdapter(Context context, Cursor cursor) {
		super(context, cursor, 0);
		sharedInit(context, cursor);
	}

	private void sharedInit(Context context, Cursor cursor) {
		// Going to be using this alot, just safe ref
		mInflater = LayoutInflater.from(context);
		mContext = context; // Reference back to activity

		// Certain bits should be handled outside the class
		if (context instanceof AdapterCallback)
			mCallback = (AdapterCallback) context;

		if (cursor != null)
			mapCursor(cursor);
	}

	/*
	 * Setters
	 */
	public void setCallback(AdapterCallback callback) {
		mCallback = callback;
	}

	public void setItemLayout(int i) {
		this.mItemLayout = i;
	}

	/*
	 * Data management
	 */
	@Override
	public void changeCursor(Cursor cursor) {
		try {
			super.changeCursor(cursor);
		} catch (Exception e) {
			System.out.println("problem changing cursor " + e.getMessage());
			return;
		}

		if (mCursorMap.isEmpty())
			mapCursor(cursor);
	}

	public void changeCursor(Cursor cursor, boolean refreshMap) {
		try {
			super.changeCursor(cursor);
		} catch (Exception e) {
			System.out.println("problem changing cursor " + e.getMessage());
			return;
		}

		if (cursor != null && refreshMap)
			mapCursor(cursor);
	}

	public int getColIndex(String key) {
		if (mCursorMap.containsKey(key))
			return mCursorMap.get(key).intValue();
		return 0;
	}

	// Save reference to the columns in the cursor
	private void mapCursor(Cursor c) {
		if (c == null)
			return;

		mCursorMap.clear();
		try {
			for (int i = 0; i < c.getColumnCount(); i++)
				mCursorMap.put(c.getColumnName(i), i);
		} catch (Exception e) {
			System.out.println("error mapping cursor :" + e.getMessage());
		}
	}

	/*
	 * Rendering
	 */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View oView = mInflater.inflate(mItemLayout, parent, false);

		if (mCallback != null)
			mCallback.onCreateListItem(oView);

		return oView;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		Cursor c = getCursor();
		if (!c.moveToPosition(position))
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);

		if (view == null)
			view = newView(mContext, c, parent);

		bindView(view, mContext, c);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		if (mCallback != null)
			mCallback.onBindListItem(view, cursor);
	}
}
