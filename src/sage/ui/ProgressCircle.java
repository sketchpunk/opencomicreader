package sage.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ProgressCircle extends View{
	private Paint mCircleFill;
	private Paint mCircleStroke;
	private Paint mArcFill;
	private RectF mArcRect;
	private float mArcPad = 4f;
	private float mPercent;
	
	public ProgressCircle(Context context){ super(context); init(context); }//func
	public ProgressCircle(Context context, AttributeSet attrs){ super(context, attrs); init(context); }
	public ProgressCircle(Context context, AttributeSet attrs, int defStyle){ super(context, attrs, defStyle); init(context); }
	
	private void init(Context context){
		mCircleFill = new Paint();
		mCircleFill.setColor(Color.WHITE);
		mCircleFill.setStyle(Paint.Style.FILL);
		
		mCircleStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		mCircleStroke.setColor(0xFF505050);
		mCircleStroke.setStyle(Paint.Style.STROKE);
		mCircleStroke.setStrokeWidth(2);

		mArcFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		mArcFill.setColor(0xFFB0B0B0);
		mArcFill.setStyle(Paint.Style.FILL);
		
		mPercent = 0.0f;
	}//func
	
	
	public void setProgress(float per){
		mPercent = per;
		this.invalidate();
	}//func
	
	@Override
	protected void onDraw(Canvas canvas){
		float w = (float)this.getWidth();
		float h = (float)this.getHeight();
		float x = w/2;
		float y = h/2;
		
		float deg = 360 * mPercent;
		if(mArcRect == null) mArcRect = new RectF(mArcPad,mArcPad,w-mArcPad,h-mArcPad);
		
		canvas.drawCircle(x,y,x-1,mCircleFill);
		canvas.drawCircle(x,y,x-1,mCircleStroke);
		
		if(deg > 0.0f) canvas.drawArc(mArcRect,270,deg,true,mArcFill);
	}//func	
}//cls
