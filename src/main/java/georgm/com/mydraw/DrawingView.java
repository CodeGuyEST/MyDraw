package georgm.com.mydraw;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Georg on 20.11.2016.
 */
public class DrawingView extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path    mPath;
    private Paint   mBitmapPaint;
    private Paint mPaint;
    Context context;
    private int backgroundColor;
    private boolean erasing;
    private DrawingModes drawingMode;
    public boolean hasCustomImage;

    public DrawingView(Context c) {
        super(c);
        context=c;
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
        erasing = false;
        drawingMode = DrawingModes.DEFAULT;
        setBackgroundColor(Color.WHITE);
        hasCustomImage = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        SharedPreferences preferences = ((Activity)context).getPreferences(Context.MODE_PRIVATE);
        if(preferences.contains(Utils.BACKGROUND_BITMAP)){
            byte[] bytes = Base64.decode(preferences.getString(Utils.BACKGROUND_BITMAP,null),Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            mBitmap = Bitmap.createScaledBitmap(bitmap,w,h,true);
            mBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888,true);
        }
        else
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        hasCustomImage = preferences.getBoolean(Utils.HAS_CUSTOM_IMAGE,false);
        setBackgroundColor(preferences.getInt(Utils.BACKGROUND_COLOR,0));
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        if(!erasing)
            canvas.drawPath( mPath,  mPaint);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x,y);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            switch(drawingMode) {
                case DEFAULT:
                    mPath.quadTo(mX, mY, x, y);
                    mX = x;
                    mY = y;
                    if (erasing)
                        mCanvas.drawPath(mPath, mPaint);
                    break;
                case CIRCLE:
                    mPath.reset();
                    float radius = (float)Math.sqrt(dx * dx + dy * dy);
                    mPath.addCircle(mX, mY, radius, Path.Direction.CW);
                    break;
                case RECTANGLE:
                    mPath.reset();
                    mPath.addRect(Math.min(mX, x), Math.min(mY, y), Math.max(mX, x), Math.max(mY, y), Path.Direction.CW);
                    break;
                case LINE:
                    mPath.reset();
                    mPath.moveTo(mX, mY);
                    mPath.lineTo(x, y);
                    break;
            }
        }
    }

    private void touch_up() {
        switch (drawingMode) {
            case DEFAULT:
                mPath.lineTo(mX, mY);
                // commit the path to our offscreen
                mCanvas.drawPath(mPath, mPaint);
                // kill this so we don't double draw
                mPath.reset();
                break;
            case CIRCLE:
                mCanvas.drawPath(mPath, mPaint);
                mPath.reset();
                break;
            case RECTANGLE:
                mCanvas.drawPath(mPath, mPaint);
                mPath.reset();
                break;
            case LINE:
                mCanvas.drawPath(mPath, mPaint);
                mPath.reset();
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    public Paint getPaint(){
        return mPaint;
    }

    public int getBackgroundColor(){return backgroundColor;}

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        backgroundColor = color;
    }

    public boolean toggleErasing(){
        erasing = !erasing;
        if(erasing){
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            drawingMode = DrawingModes.DEFAULT;
        }
        else mPaint.setXfermode(null);
        return erasing;
    }

    public void setDrawingMode(DrawingModes mode){
        drawingMode = mode;
    }

    public DrawingModes getDrawingMode(){return drawingMode;}

    public Bitmap getBitmap(){return mBitmap;}

    public void setBitmap(Bitmap bitmap){
        mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        mCanvas.setBitmap(mBitmap);
        backgroundColor = 0;
    }

    public boolean isErasing(){
        return erasing;
    }

    public void resetBitmap(){
            mBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mCanvas.setBitmap(mBitmap);
    }
}
