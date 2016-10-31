package sage.ui;


import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerSpacingItemDecoration extends RecyclerView.ItemDecoration{
	private int mCol = 1;
	private int mSpacing = 0;
	private int mSpacingH = 0;

	public RecyclerSpacingItemDecoration(int cols, int spacing){
		mCol = cols;
		mSpacing = spacing;
		mSpacingH = spacing/2;
	}

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state){
		int pos = parent.getChildPosition(view);

		outRect.left = ((pos % mCol) == 0)? mSpacing : mSpacingH;
		outRect.right = ((pos % mCol) == mCol-1)? mSpacing : mSpacingH;
		outRect.top = (pos <= mCol)? mSpacing : mSpacingH;
		outRect.bottom = mSpacingH;
	}

}
