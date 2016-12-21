package georgm.com.mydraw;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class LayersListAdapter extends ArrayAdapter<Bitmap> {

    private List<Bitmap> layers;
    private Context context;
    private View currentlyDragged;
    private Bitmap currentlyDraggedViewBitmap;
    private int currentlyDraggedViewIndex = -1;
    private ListView container;
    private List<View> views;

    public int selectedLayerIndex = -1;

    public static final int MAX_LAYERS = 3;

    public LayersListAdapter(Context context, int resource) {
        super(context, resource);
        layers = new ArrayList<>();
        this.context = context;
        views = new ArrayList<>();
    }

    @Override
    public void add(Bitmap object) {
        super.add(object);
        if(getCount() == MAX_LAYERS){
            Toast.makeText(context, "No more layers available.(Max " + Integer.toString(MAX_LAYERS) + " layers.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        layers.add(object);
        selectLayer(layers.size() - 1);
        notifyDataSetChanged();
    }

    public void addAll(List<Bitmap> layers) {
        super.addAll(layers);
        this.layers.clear();
        this.views.clear();
        for(Bitmap bitmap : layers){
            this.layers.add(Bitmap.createScaledBitmap(bitmap,52,100,false));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return layers.size();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(container == null)container = (ListView)parent;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                100);
        ImageView v = new ImageView(context);
        v.setTag(position);
        v.setLayoutParams(params);
        v.setBackgroundColor(Color.WHITE);
        v.setImageBitmap(layers.get(position));
        v.setOnLongClickListener(new LongClickListener(position));
        v.setOnDragListener(new DragListener(position));
        v.setOnClickListener(new ClickListener(position));
        if(position == selectedLayerIndex)
            drawBorder(v);
        return v;
    }

    public void selectLayer(int position){
        selectedLayerIndex = position;
        notifyDataSetChanged();
    }

    public View getCurrentlyDraggedView(){return currentlyDragged;}

    private void moveBitmap(Bitmap movingBitmap, Bitmap destinationBitmap){
        int fromPosition = layers.indexOf(movingBitmap);
        int destinationPosition = layers.indexOf(destinationBitmap);
        if(fromPosition < destinationPosition){
            for(int i = fromPosition + 1; i <= destinationPosition; i++){
                swapBitmaps(movingBitmap,layers.get(i));
            }
        }
        else if (fromPosition > destinationPosition){
            for(int i = fromPosition - 1; i >= destinationPosition; i--){
                swapBitmaps(movingBitmap,layers.get(i));
            }
        }
        updateBitmaps();
    }

    private View drawBorder(View view){
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(5);
        gd.setColor(Color.WHITE);
        gd.setStroke(5,Color.BLUE);
        view.setBackground(gd);
        return view;
    }

    private void swapBitmaps(Bitmap bitmap1, Bitmap bitmap2){
        int location1 = layers.indexOf(bitmap1);
        int location2 = layers.indexOf(bitmap2);
        layers.remove(bitmap1);
        layers.remove(bitmap2);
        if(location1 < location2) {
            layers.add(location1, bitmap2);
            layers.add(location2, bitmap1);
        }
        else {
            layers.add(location2, bitmap1);
            layers.add(location1, bitmap2);
        }

    }

    private void updateBitmaps(){
        for(int i = 0; i < layers.size(); i++){
            ((ImageView)container.getChildAt(i)).setImageBitmap(layers.get(i));
        }
    }

    public void removeCurrentlyDraggedItem(){
        int position = (int)currentlyDragged.getTag();
        layers.remove(currentlyDraggedViewBitmap);
        ((OnLayersUpdated)context).removeLayer(position);
        notifyDataSetChanged();
        if(layers.size() == 0)((MainActivity)context).addLayer(null);
    }

    private class LongClickListener implements View.OnLongClickListener{

        int position;

        public LongClickListener(int position){
            this.position = position;
        }

        @Override
        public boolean onLongClick(View v) {
            ClipData.Item item = new ClipData.Item(Integer.toString(position));
            ClipData dragData = new ClipData(Integer.toString(position),new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},item);
            v.startDrag(dragData,new MyDragShadowBuilder(v),null,0);
            ((OnDragStateChanged)context).onDragStarted();
            currentlyDragged = v;
            currentlyDraggedViewBitmap = layers.get((int)v.getTag());
            currentlyDraggedViewIndex = position;
            v.setAlpha(0);
            return true;
        }
    }

    private static class MyDragShadowBuilder extends View.DragShadowBuilder {
        private View shadow;

        public MyDragShadowBuilder(View v) {
            super(v);
            shadow = v;
        }

        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            int width, height;
            width = getView().getWidth();
            height = getView().getHeight();
            size.set(width, height);
            touch.set(width, height);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            shadow.draw(canvas);
        }
    }

    private class DragListener implements View.OnDragListener{

        int viewPosition;

        public DragListener(int viewPosition){
            this.viewPosition = viewPosition;
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch(event.getAction()){
                case DragEvent.ACTION_DRAG_ENTERED:
                    ((ImageView)v).setImageBitmap(currentlyDraggedViewBitmap);
                    ((ImageView)currentlyDragged).setImageBitmap(layers.get((int)v.getTag()));
                    currentlyDragged.setAlpha(1);
                    currentlyDraggedViewIndex = (int)v.getTag();
                    container.getChildAt(currentlyDraggedViewIndex).setAlpha(0);
                    moveBitmap(currentlyDraggedViewBitmap,layers.get((int)v.getTag()));
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    currentlyDragged.setAlpha(1);
                    notifyDataSetChanged();
                    currentlyDraggedViewIndex = -1;
                    currentlyDraggedViewBitmap = null;
                    ((OnDragStateChanged)context).onDragFinished();
                    break;
                case DragEvent.ACTION_DROP:
                    currentlyDragged.setAlpha(1);
                    ((OnLayersUpdated)context).moveLayer((int)currentlyDragged.getTag(),(int)v.getTag());
                    currentlyDraggedViewIndex = -1;
                    currentlyDraggedViewBitmap = null;
                    ((OnDragStateChanged)context).onDragFinished();
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    if(v != currentlyDragged)
                        v.setAlpha(1);
                    break;
            }
            return true;
        }
    }

    private class ClickListener implements View.OnClickListener{

        int position;

        public ClickListener(int position){
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            drawBorder(v);
            ((MainActivity)context).getDrawingView().selectLayer(position);
        }
    }

    public interface OnLayersUpdated {
        void moveLayer(int from, int to);
        void removeLayer(int position);
    }

    public interface OnDragStateChanged{
        void onDragStarted();
        void onDragFinished();
    }

}
