package sage.data;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class ViewBindHolder extends RecyclerView.ViewHolder{
	public ViewBindHolder(View itemView){ super(itemView); }

	public void bindData(int pos){}					//For normal adapter
	public void bindData(int pos, Cursor cursor){}	//For Cursor adapter
}//func
