package georgm.com.mydraw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mPaint;
    Context context;
    private int backgroundColor = Color.WHITE;
    private boolean erasing;
    private DrawingModes drawingMode;
    private List<Bitmap> layers;
    private LayersListAdapter adapter;
    private int selectedLayerIndex;

    public DrawingView(Context c) {
        super(c);
        context=c;
        setDrawingCacheEnabled(true);
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
        mCanvas = new Canvas();
        drawingMode = DrawingModes.DEFAULT;
        setBackgroundColor(backgroundColor);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        setLayoutParams(params);
        layers = new ArrayList<>();
        zoom(0.5f,0.5f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(mBitmap == null && layers.size() > 0) {
            mBitmap = layers.get(layers.size() - 1);
        }
        if(layers.size() == 0){
            Bitmap layer = Bitmap.createBitmap(getWidth(),getHeight(), Bitmap.Config.ARGB_8888);
            insertLayer(0,layer);
            adapter.add(Bitmap.createScaledBitmap(layer,52,100,false));
        }
        center();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for(int i = 0; i < layers.size(); i++){
            canvas.drawBitmap(layers.get(i),0,0,mBitmapPaint);
        }
        if(!erasing) {
            canvas.drawPath(mPath, mPaint);
        }
    }

    private float oldX, oldY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x,y);
        oldX = x;
        oldY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - oldX);
        float dy = Math.abs(y - oldY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            switch(drawingMode) {
                case DEFAULT:
                    mPath.quadTo(oldX, oldY, x, y);
                    oldX = x;
                    oldY = y;
                    if (erasing)
                        mCanvas.drawPath(mPath, mPaint);
                    break;
                case CIRCLE:
                    mPath.reset();
                    float radius = (float)Math.sqrt(dx * dx + dy * dy);
                    mPath.addCircle(oldX, oldY, radius, Path.Direction.CW);
                    break;
                case RECTANGLE:
                    mPath.reset();
                    mPath.addRect(Math.min(oldX, x), Math.min(oldY, y), Math.max(oldX, x), Math.max(oldY, y), Path.Direction.CW);
                    break;
                case LINE:
                    mPath.reset();
                    mPath.moveTo(oldX, oldY);
                    mPath.lineTo(x, y);
                    break;
            }
        }
    }

    private void touch_up() {
        switch (drawingMode) {
            case DEFAULT:
                // commit the path to offscreen
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

    public void selectLayer(int position){
        mBitmap = layers.get(position);
        mCanvas.setBitmap(mBitmap);
        adapter.selectLayer(position);
        selectedLayerIndex = position;
    }

    public int getSelectedLayer(){return selectedLayerIndex;}

    public void populateLayers(List<Bitmap> newLayers){
        adapter.addAll(newLayers);
        layers = newLayers;
        invalidate();
        selectLayer(layers.size() - 1);
    }

    private float baseRotation = getRotation();

    private void touch_move_multi(float x1, float x2, float y1, float y2){
        float dX = x2 - x1;
        float dY = y2 - y1;
        float olddX = this.oldx2 - this.oldx1;
        float olddY = this.oldy2 - this.oldy1;
        Log.d("OLDDX",Float.toString(olddX));
        Log.d("OLDDY",Float.toString(olddY));
        Log.d("DX",Float.toString(dX));
        Log.d("DY",Float.toString(dY));
        double distance = Math.sqrt(dX * dX + dY * dY);
        double referenceDistance = Math.sqrt(olddX * olddX + olddY * olddY);
        double zoomArgument = distance/referenceDistance * getScaleX();
        double angle = getAngle(olddX,olddY,dX,dY);
        zoom(zoomArgument,zoomArgument);
        rotate(angle);
    }
    float oldx2 = 0;
    float oldy2 = 0;
    float oldx1 = 0;
    float oldy1 = 0;

    private boolean multiTouching = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction() & event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                this.oldx1 = event.getX(0);
                this.oldy1 = event.getY(0);
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if(event.getPointerCount() > 2)return false;
                baseRotation = getRotation();
                this.oldx2 = event.getX(1);
                this.oldy2 = event.getY(1);
                multiTouching = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                multiTouching = false;
                this.oldx2 = event.getX(1);
                this.oldy2 = event.getY(1);
                this.oldx1 = event.getX(0);
                this.oldy1 = event.getY(0);
                baseRotation = getRotation();
                oldX = event.getX(event.getActionIndex() == 0 ? 1 : 0);
                oldY = event.getY(event.getActionIndex() == 0 ? 1 : 0);
                mPath.moveTo(oldX,oldY);
                break;
            case MotionEvent.ACTION_MOVE:
                if(multiTouching)touch_move_multi(event.getX(0),event.getX(1),event.getY(0),event.getY(1));
                else {
                    touch_move(x, y);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                multiTouching = false;
                invalidate();
                break;
        }
        return true;
    }

    public Paint getPaint(){
        return mPaint;
    }

    public int getBackgroundColor(){return backgroundColor;}

    private void center(){
        LinearLayout parent = (LinearLayout)getParent();
        double centerX = parent.getWidth() / 2;
        double centerY = parent.getHeight() / 2;
        setX((float)centerX - getWidth()/2);
        setY((float)centerY - getHeight() / 2);
    }

    public void setLayersAdapter(LayersListAdapter adapter){
        this.adapter = adapter;
    }

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

    public boolean isErasing(){
        return erasing;
    }

    private void zoom(double width, double height){
        setScaleX((float)width);
        setScaleY((float)height);
        invalidate();
    }

    private double getAngle(float x1, float y1, float x2, float y2){
        float angle = (float)Math.toDegrees(Math.atan2(y1,x1) - Math.atan2(y2,x2));
        return -angle;
    }

    private float getAngle(float start1x,float start1y,float end1x,float end1y,float start2x,float start2y,float end2x,float end2y){
        float vector1x = end1x - start1x;
        float vector1y = end1y - start1y;
        float vector2x = end2x - start2x;
        float vector2y = end2y - start2y;

        float scalarProduct = vector1x * vector2x + vector1y * vector2y;
        float length1 = (float)Math.sqrt(vector1x * vector1x + vector1y * vector1y);
        float length2 = (float)Math.sqrt(vector2x * vector2x + vector2y * vector2y);

        return (float)Math.toDegrees(Math.acos(scalarProduct / (length1 * length2)));
    }

    private static float MAX_ROTATING_STEP = 5;

    private void rotate(double angle){
        Log.d("ANGLE",Double.toString(angle));
        float newRotation = baseRotation + (float)angle;
        if(Math.abs(getRotation() - newRotation) <= MAX_ROTATING_STEP)
            setRotation(baseRotation + (float)angle);
    }

    public List<Bitmap> getLayers(){return layers;}

    public void insertLayer(int position,Bitmap layer){
        layers.add(position,layer);
        selectLayer(position);
    }



    public void removeLayer(int position){
        layers.remove(position);
        if(layers.size() > 0) {
            selectLayer(position >= layers.size() ? position - 1 : position);
        }
        invalidate();
    }

    public void moveLayer(int layerPosition, int newPosition){
        if(layerPosition == newPosition)return;
        Bitmap layer = layers.remove(layerPosition);
        layers.add(newPosition,layer);
        selectLayer(layers.indexOf(layer));
        invalidate();
    }

    public int getLayerCount(){return layers.size();}
}
