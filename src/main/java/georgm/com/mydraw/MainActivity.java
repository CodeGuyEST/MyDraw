package georgm.com.mydraw;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LayersListAdapter.OnLayersUpdated,
        LayersListAdapter.OnDragStateChanged{

    private DrawingView drawingView;
    private ListView layersListView;
    private ExportDialog exportDialog;

    public static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;

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
                findViewById(R.id.background_color).removeOnLayoutChangeListener(this);//DrawingView takes care of modifying icons from now on.
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
        layersListView = (ListView)findViewById(R.id.layers_list);
        layersListView.setAdapter(new LayersListAdapter(this,0));
        drawingView.setLayersAdapter((LayersListAdapter) layersListView.getAdapter());
        restoreDrawingView(savedInstanceState);
        restoreLayers(savedInstanceState);
        setDragListener();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                InputStream input = this.getContentResolver().openInputStream(selectedImage);
                BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
                boundsOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input,null,boundsOptions);
                input.close();
                int originalSize = (boundsOptions.outHeight > boundsOptions.outWidth)
                        ? boundsOptions.outHeight : boundsOptions.outWidth;

                double ratio = (originalSize > Math.max(drawingView.getWidth(),drawingView.getHeight())) ? (originalSize /
                        Math.max(drawingView.getWidth(), drawingView.getHeight())) : 1.0;

                BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
                input = getContentResolver().openInputStream(selectedImage);
                Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
                input.close();
                drawingView.setBackground(new BitmapDrawable(getResources(),bitmap));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        saveDrawingViewLayout(savedInstanceState);
        saveLayers(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(exportDialog == null || !exportDialog.isShowing())
                        getImageFromGallery();
                }
        }
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    public DrawingView getDrawingView(){
        return drawingView;
    }

    public void notifyBackgroundChanged(){
        modifyBackgroundIcon();
    }

    private void export(){
        exportDialog = new ExportDialog(this,drawingView);
        exportDialog.show();
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
        saveBrush(editor);
        saveLayers(editor);
        saveBackground(editor);
        saveLayout(editor);
        editor.commit();
    }

    private void saveBrush(SharedPreferences.Editor editor){
        editor.putInt(Utils.BRUSH_COLOR, drawingView.getPaint().getColor());
        editor.putFloat(Utils.BRUSH_WIDTH, drawingView.getPaint().getStrokeWidth());
        editor.commit();
    }

    private void saveLayout(SharedPreferences.Editor editor){
        editor.putInt(Utils.DRAWING_VIEW_WIDTH,drawingView.getWidth());
        editor.putInt(Utils.DRAWING_VIEW_HEIGHT,drawingView.getHeight());
        editor.putFloat(Utils.DRAWING_VIEW_ROTATION,drawingView.getRotation());
        editor.putFloat(Utils.DRAWING_VIEW_SCALE,drawingView.getScaleX());
        editor.putInt(Utils.BACKGROUND_COLOR,drawingView.getBackgroundColor());
        editor.commit();
    }

    private void saveLayers(SharedPreferences.Editor editor){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        List<Bitmap> layers = drawingView.getLayers();
        for(int i = 0; i < layers.size(); i++){
            try {
                stream.flush();
                stream.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
            layers.get(i).compress(Bitmap.CompressFormat.PNG,100,stream);
            byte[] layerBytes = stream.toByteArray();
            editor.putString(Utils.LAYER + Integer.toString(i), Base64.encodeToString(layerBytes, Base64.DEFAULT));
        }
        editor.putInt(Utils.LAYER_COUNT,layers.size());
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.commit();
    }

    private void saveBackground(SharedPreferences.Editor editor){
        byte[] background;
        if(drawingView.getBackground() instanceof BitmapDrawable) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ((BitmapDrawable) drawingView.getBackground()).getBitmap().compress(Bitmap.CompressFormat.PNG,100,stream);
            background = stream.toByteArray();
            editor.putString(Utils.BACKGROUND,Base64.encodeToString(background,0,background.length,0));
        }
        editor.putInt(Utils.BACKGROUND_COLOR,drawingView.getBackgroundColor());
        editor.commit();
    }

    public void newDrawing(){
        drawingView.setBackgroundColor(Color.WHITE);
        this.modifyBackgroundIcon();
        Bitmap layer = Bitmap.createBitmap(drawingView.getWidth(),drawingView.getHeight(), Bitmap.Config.ARGB_8888);
        ArrayList<Bitmap> layers = new ArrayList<>();
        layers.add(layer);
        drawingView.populateLayers(layers);
    }

    private void resetDrawingModeIcons(){
        findViewById(R.id.draw_rectangle).setBackgroundColor(Color.TRANSPARENT);
        findViewById(R.id.draw_line).setBackgroundColor(Color.TRANSPARENT);
        ((ImageView)findViewById(R.id.draw_circle)).setImageResource(R.drawable.circle_not_selected);
    }

    public void getImageFromGallery(){
        if(Build.VERSION.SDK_INT >= 23){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_WRITE_EXTERNAL_STORAGE);
                return;
            }
        }
        Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, PERMISSION_WRITE_EXTERNAL_STORAGE);
    }

    private Bundle saveDrawingViewLayout(Bundle in){
        in.putInt(Utils.DRAWING_VIEW_WIDTH,drawingView.getWidth());
        in.putInt(Utils.DRAWING_VIEW_HEIGHT,drawingView.getHeight());
        in.putFloat(Utils.DRAWING_VIEW_ROTATION,drawingView.getRotation());
        in.putFloat(Utils.DRAWING_VIEW_SCALE,drawingView.getScaleX());
        in.putInt(Utils.BACKGROUND_COLOR,drawingView.getBackgroundColor());
        if(drawingView.getBackground() instanceof BitmapDrawable) {
            Bitmap background = ((BitmapDrawable) drawingView.getBackground()).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            background.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            in.putByteArray(Utils.BACKGROUND, byteArray);
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return in;
    }

    private Bundle saveLayers(Bundle in){
        for(int i = 0; i < drawingView.getLayerCount(); i++) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            drawingView.getLayers().get(i).compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            in.putByteArray(Utils.LAYER + Integer.toString(i),byteArray);
        }
        in.putInt(Utils.LAYER_COUNT,drawingView.getLayerCount());
        in.putInt(Utils.SELECTED_LAYER,drawingView.getSelectedLayer());
        return in;
    }

    private void restoreDrawingView(Bundle savedInstance){
        if(savedInstance == null){
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            if(preferences.contains(Utils.DRAWING_VIEW_HEIGHT)){
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)drawingView.getLayoutParams();
                params.height = preferences.getInt(Utils.DRAWING_VIEW_HEIGHT,0);
                params.width = preferences.getInt(Utils.DRAWING_VIEW_WIDTH,0);
                drawingView.setLayoutParams(params);
                drawingView.setRotation(preferences.getFloat(Utils.DRAWING_VIEW_ROTATION,0));
                drawingView.setScaleX(preferences.getFloat(Utils.DRAWING_VIEW_SCALE,0));
                drawingView.setScaleY(preferences.getFloat(Utils.DRAWING_VIEW_SCALE,0));
                drawingView.setBackgroundColor(preferences.getInt(Utils.BACKGROUND_COLOR,Color.WHITE));
                if(preferences.contains(Utils.BACKGROUND)) {
                    byte[] backgroundBytes = Base64.decode(preferences.getString(Utils.BACKGROUND, ""), 0);
                    if (backgroundBytes != null) {
                        Bitmap background = BitmapFactory.decodeByteArray(backgroundBytes, 0, backgroundBytes.length);
                        drawingView.setBackground(new BitmapDrawable(getResources(), background));
                    }
                }
            }
            return;
        }
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)drawingView.getLayoutParams();
        params.height = savedInstance.getInt(Utils.DRAWING_VIEW_HEIGHT);
        params.width = savedInstance.getInt(Utils.DRAWING_VIEW_WIDTH);
        drawingView.setLayoutParams(params);
        drawingView.setRotation(savedInstance.getFloat(Utils.DRAWING_VIEW_ROTATION));
        drawingView.setScaleX(savedInstance.getFloat(Utils.DRAWING_VIEW_SCALE));
        drawingView.setScaleY(savedInstance.getFloat(Utils.DRAWING_VIEW_SCALE));
        drawingView.setBackgroundColor(savedInstance.getInt(Utils.BACKGROUND_COLOR));
        byte[] backgroundBytes = savedInstance.getByteArray(Utils.BACKGROUND);
        if(backgroundBytes != null) {
            Bitmap background = BitmapFactory.decodeByteArray(backgroundBytes, 0, backgroundBytes.length);
            drawingView.setBackground(new BitmapDrawable(getResources(), background));
        }
    }

    private void restoreBackground(SharedPreferences preferences){
        if(preferences.contains(Utils.BACKGROUND)){
            byte[] backgroundArr = Base64.decode(preferences.getString(Utils.BACKGROUND ,""),0);
            if(backgroundArr != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(backgroundArr, 0, backgroundArr.length);
                drawingView.setBackground(new BitmapDrawable(getResources(), bitmap));
            }
        }
    }

    private void restoreLayers(Bundle instanceState){
        if(instanceState == null){
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            if(preferences.contains(Utils.LAYER_COUNT)){
                List<Bitmap> layers = new ArrayList<>();
                for(int i = 0; i < preferences.getInt(Utils.LAYER_COUNT,-1); i++){
                    byte[] bitmapArr = Base64.decode(preferences.getString(Utils.LAYER + Integer.toString(i),""),0);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapArr,0,bitmapArr.length);
                    layers.add(i,bitmap.copy(Bitmap.Config.ARGB_8888, true));
                }
                drawingView.populateLayers(layers);
            }
            restoreBackground(preferences);
        }
        else {
            int layerCount = instanceState.getInt(Utils.LAYER_COUNT);
            for (int i = 0; i < layerCount; i++) {
                if (instanceState.containsKey(Utils.LAYER + Integer.toString(i))) {
                    byte[] bytes = instanceState.getByteArray(Utils.LAYER + Integer.toString(i));
                    Bitmap layer = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    layer = layer.copy(Bitmap.Config.ARGB_8888, true);
                    drawingView.insertLayer(i, layer);
                } else return;
            }
            drawingView.selectLayer(instanceState.getInt(Utils.SELECTED_LAYER));
        }
    }

    private void notifyAboutMaxLayers(){
        Toast.makeText(this,"No more layers left.(Max " + Integer.toString(LayersListAdapter.MAX_LAYERS) + ")",Toast.LENGTH_SHORT)
                .show();
    }

    private void setDragListener(){
        findViewById(R.id.drawing_area).setOnDragListener(new DragListener());
    }

    @Override
    public void moveLayer(int from, int to) {
        drawingView.moveLayer(from,to);
    }

    @Override
    public void removeLayer(int position){
        drawingView.removeLayer(position);
    }

    @Override
    public void onDragStarted() {
        ImageView deletionView = ((ImageView)findViewById(R.id.drag_icon));
        deletionView.setImageResource(R.drawable.ic_delete_black_24dp);
        deletionView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                if(event.getAction() == DragEvent.ACTION_DROP){
                    ((LayersListAdapter)layersListView.getAdapter()).removeCurrentlyDraggedItem();
                }
                return true;
            }
        });
    }

    @Override
    public void onDragFinished() {
        ((ImageView)findViewById(R.id.drag_icon)).setImageResource(R.drawable.ic_add_black_24dp);
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

    public void toggleLayerListVisibility(View v){
        LinearLayout layersLayout = (LinearLayout)findViewById(R.id.layers_layout);
        if(layersLayout.getVisibility() == View.VISIBLE)
            layersLayout.setVisibility(View.INVISIBLE);
        else {
            ((LayersListAdapter)layersListView.getAdapter()).addAll(drawingView.getLayers());
            layersLayout.setVisibility(View.VISIBLE);
        }
    }

    public void addLayer(View v){
        if(drawingView.getLayers().size() >= LayersListAdapter.MAX_LAYERS){
            notifyAboutMaxLayers();
            return;
        }
        Bitmap newLayer = Bitmap.createBitmap(drawingView.getWidth(),drawingView.getHeight(), Bitmap.Config.ARGB_8888);
        drawingView.insertLayer(drawingView.getLayerCount(),newLayer);
        ((LayersListAdapter)layersListView.getAdapter()).add(Bitmap.createScaledBitmap(newLayer,
                LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT,false));
    }

    private class DragListener implements View.OnDragListener{

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch(event.getAction()){
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setVisibility(View.VISIBLE);
                    break;
                case DragEvent.ACTION_DROP:
                    ((LayersListAdapter)layersListView.getAdapter()).getCurrentlyDraggedView().setVisibility(View.VISIBLE);
                    break;
            }
            return true;
        }
    }
}
