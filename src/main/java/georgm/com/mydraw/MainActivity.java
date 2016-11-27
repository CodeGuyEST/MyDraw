package georgm.com.mydraw;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity{

    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        drawingView = new DrawingView(this);
        LinearLayout drawingArea = (LinearLayout)findViewById(R.id.drawing_area);
        drawingArea.addView(drawingView);
        findViewById(R.id.background_color).addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                       int oldTop, int oldRight, int oldBottom) {
                modifyBackgroundIcon();
                findViewById(R.id.background_color).removeOnLayoutChangeListener(this);//DrawingView takes care of modifying icons in the future.
            }
        });
        findViewById(R.id.brush_color).addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                       int oldTop, int oldRight, int oldBottom) {
                modifyBrushIcon();
                findViewById(R.id.brush_color).removeOnLayoutChangeListener(this);
            }
        });
        findViewById(R.id.brush_width).addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                       int oldTop, int oldRight, int oldBottom) {
                modifyBrushSizeIcon();
                findViewById(R.id.brush_width).removeOnLayoutChangeListener(this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            drawingView.setBitmap(BitmapFactory.decodeFile(picturePath));
            drawingView.setBackgroundColor(Color.WHITE);
            drawingView.hasCustomImage = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.new_drawing:
                newDrawing();
                return true;
            case R.id.export:
                export();
                return true;
            case R.id.import_file:
                getImageFromGallery();
                return true;
            case R.id.save:
                save();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getImageFromGallery();
                }
        }
    }

    public DrawingView getDrawingView(){
        return drawingView;
    }

    public void notifyBackgroundChanged(){
        modifyBackgroundIcon();
    }

    private void export(){
        ExportDialog dialog = new ExportDialog(this,drawingView);
        dialog.show();
    }

    private void modifyBrushSizeIcon(){
        ShapeDrawable outerCircle = new ShapeDrawable(new OvalShape());
        int outerWidth = findViewById(R.id.brush_width).getWidth();
        outerCircle.setIntrinsicHeight(outerWidth);
        outerCircle.setIntrinsicWidth(outerWidth);
        outerCircle.setBounds(new Rect(0, 0, outerWidth, outerWidth));
        int outerColor = Color.parseColor("#746e6e");
        outerCircle.getPaint().setColor(outerColor);
        int padding = (int)((100.0 - drawingView.getPaint().getStrokeWidth())/100.0 * 0.5 * outerWidth);
        outerCircle.setPadding(padding, padding, padding, padding);

        ShapeDrawable innerCircle = new ShapeDrawable(new OvalShape());
        int innerWidth = (int)((drawingView.getPaint().getStrokeWidth() / 100.0) * outerWidth);
        innerCircle.setIntrinsicHeight(innerWidth);
        innerCircle.setIntrinsicWidth(innerWidth);
        innerCircle.setBounds(new Rect(0, 0, innerWidth, innerWidth));
        innerCircle.getPaint().setColor(Color.BLACK);

        Drawable[] drawables = {outerCircle,innerCircle};
        LayerDrawable drawable = new LayerDrawable(drawables);
        ((ImageView)findViewById(R.id.brush_width)).setImageDrawable(drawable);
    }

    private void modifyBrushIcon(){
        ShapeDrawable innerCircle = new ShapeDrawable( new OvalShape());
        int width = findViewById(R.id.brush_color).getWidth() - 5;
        innerCircle.setIntrinsicHeight(width);
        innerCircle.setIntrinsicWidth(width);
        innerCircle.setBounds(new Rect(0, 0, width, width));
        innerCircle.getPaint().setColor(drawingView.getPaint().getColor());

        ShapeDrawable outerCircle = new ShapeDrawable(new OvalShape());
        int outerWidth = findViewById(R.id.brush_color).getWidth();
        outerCircle.setIntrinsicHeight(outerWidth);
        outerCircle.setIntrinsicWidth(outerWidth);
        outerCircle.setBounds(new Rect(0, 0, outerWidth, outerWidth));
        int outerColor = Color.parseColor("#810a0606");
        outerCircle.getPaint().setColor(outerColor);
        outerCircle.setPadding(5, 5, 5, 5);
        Drawable[] drawables = {outerCircle,innerCircle};
        LayerDrawable drawable = new LayerDrawable(drawables);
        ((ImageView)findViewById(R.id.brush_color)).setImageDrawable(drawable);
    }

    private void modifyBackgroundIcon(){
        ShapeDrawable innerRectangle = new ShapeDrawable(new RectShape());
        int width = findViewById(R.id.background_color).getWidth() - 7;
        innerRectangle.setIntrinsicHeight(width);
        innerRectangle.setIntrinsicWidth(width);
        innerRectangle.setBounds(new Rect(0, 0, width, width));
        innerRectangle.getPaint().setColor(drawingView.getBackgroundColor());

        ShapeDrawable outerRectangle = new ShapeDrawable(new RectShape());
        int outerWidth = findViewById(R.id.background_color).getWidth();
        outerRectangle.setIntrinsicHeight(outerWidth);
        outerRectangle.setIntrinsicWidth(outerWidth);
        outerRectangle.setBounds(new Rect(0, 0, outerWidth, outerWidth));
        outerRectangle.setPadding(5, 5, 5, 5);
        outerRectangle.getPaint().setColor(Color.BLACK);
        Drawable[] drawables = {outerRectangle,innerRectangle};
        LayerDrawable drawable = new LayerDrawable(drawables);
        ((ImageView)findViewById(R.id.background_color)).setImageDrawable(drawable);
    }

    private void save(){
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putInt(Utils.BRUSH_COLOR, drawingView.getPaint().getColor());
        editor.putInt(Utils.BACKGROUND_COLOR, drawingView.getBackgroundColor());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        drawingView.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.putString(Utils.BACKGROUND_BITMAP, Base64.encodeToString(byteArray, Base64.DEFAULT));
        editor.putBoolean(Utils.HAS_CUSTOM_IMAGE,drawingView.hasCustomImage);
        editor.commit();
    }

    public void newDrawing(){
        drawingView = new DrawingView(this);
        setContentView(drawingView);
    }

    private void resetDrawingModeIcons(){
        findViewById(R.id.draw_rectangle).setBackgroundColor(Color.TRANSPARENT);
        findViewById(R.id.draw_line).setBackgroundColor(Color.TRANSPARENT);
        ((ImageView)findViewById(R.id.draw_circle)).setImageResource(R.drawable.circle_not_selected);
    }

    //onClick() methods

    public void changeBrushColor(View v){
        ColorPickerDialog dialog = new ColorPickerDialog(this, true);
        dialog.show();
    }

    public void changeBackgroundColor(View v){
        ColorPickerDialog dialog = new ColorPickerDialog(this, false);
        dialog.show();
    }

    public void changeBrushWidth(View v){
        BrushWidthDialog dialog = new BrushWidthDialog(this);
        dialog.show();
    }

    public void setDrawingMode(View v){
        ((ImageView)findViewById(R.id.eraser)).setImageResource(R.drawable.eraser_not_selected);
        if(drawingView.isErasing())drawingView.toggleErasing();
        switch(v.getId()){
            case R.id.draw_circle:
                if(drawingView.getDrawingMode() == DrawingModes.CIRCLE) {
                    drawingView.setDrawingMode(DrawingModes.DEFAULT);
                    resetDrawingModeIcons();
                }
                else {
                    drawingView.setDrawingMode(DrawingModes.CIRCLE);
                    resetDrawingModeIcons();
                    ((ImageView)v).setImageResource(R.drawable.circle_selected);
                }
                break;
            case R.id.draw_rectangle:
                if(drawingView.getDrawingMode() == DrawingModes.RECTANGLE){
                    drawingView.setDrawingMode(DrawingModes.DEFAULT);
                    resetDrawingModeIcons();
                }
                else {
                    drawingView.setDrawingMode(DrawingModes.RECTANGLE);
                    resetDrawingModeIcons();
                    int color = Color.parseColor("#AA6F6B6B");
                    v.setBackgroundColor(color);
                }
                break;
            case R.id.draw_line:
                if(drawingView.getDrawingMode() == DrawingModes.LINE){
                    drawingView.setDrawingMode(DrawingModes.DEFAULT);
                    resetDrawingModeIcons();
                }
                else {
                    drawingView.setDrawingMode(DrawingModes.LINE);
                    resetDrawingModeIcons();
                    int color = Color.parseColor("#AA6F6B6B");
                    v.setBackgroundColor(color);
                }
                break;
        }
    }

    public void toggleErasingMode(View v){
        if(drawingView.toggleErasing()){
            ((ImageView)findViewById(R.id.eraser)).setImageResource(R.drawable.eraser_selected);
            resetDrawingModeIcons();
        }
        else ((ImageView)findViewById(R.id.eraser)).setImageResource(R.drawable.eraser_not_selected);
    }

    public void getImageFromGallery(){
        if(Build.VERSION.SDK_INT >= 23){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                return;
            }
        }
        Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, 1);
    }
}
