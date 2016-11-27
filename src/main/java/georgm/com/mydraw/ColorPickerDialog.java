package georgm.com.mydraw;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.widget.ImageView;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;

/**
 * Created by Georg on 21.11.2016.
 */
public class ColorPickerDialog extends Dialog {
    private ColorPicker colorPicker;
    private SVBar svBar;
    private Context ctx;
    private boolean brush;//Are we changing brush or background color?
    public ColorPickerDialog(Context context, boolean brush) {
        super(context);
        this.brush = brush;
        this.ctx = context;
        setContentView(R.layout.color_picker);
        colorPicker = (ColorPicker)findViewById(R.id.picker);
        svBar = (SVBar)findViewById(R.id.svbar);
        colorPicker.addSVBar(svBar);
        if(brush){
            int brushColor = ((MainActivity)ctx).getDrawingView().getPaint().getColor();
            colorPicker.setColor(brushColor);
        }
        else {
            int backgroundColor = ((MainActivity)ctx).getDrawingView().getBackgroundColor();
            if(backgroundColor != 0)
                colorPicker.setColor(backgroundColor);
        }
        colorPicker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                colorPicker.setOldCenterColor(colorPicker.getColor());
            }
        });
        setOnDismissListener(new DismissListener());
    }

    private void modifyBrushIcon(){
        ShapeDrawable innerCircle = new ShapeDrawable( new OvalShape());
        int width = ((MainActivity) ctx).findViewById(R.id.brush_color).getWidth() - 5;
        innerCircle.setIntrinsicHeight(width);
        innerCircle.setIntrinsicWidth(width);
        innerCircle.setBounds(new Rect(0, 0, width, width));
        innerCircle.getPaint().setColor(colorPicker.getColor());

        ShapeDrawable outerCircle = new ShapeDrawable(new OvalShape());
        int outerWidth = ((MainActivity) ctx).findViewById(R.id.brush_color).getWidth();
        outerCircle.setIntrinsicHeight(outerWidth);
        outerCircle.setIntrinsicWidth(outerWidth);
        outerCircle.setBounds(new Rect(0, 0, outerWidth, outerWidth));
        int outerColor = Color.parseColor("#810a0606");
        outerCircle.getPaint().setColor(outerColor);
        outerCircle.setPadding(5,5,5,5);
        Drawable[] drawables = {outerCircle,innerCircle};
        LayerDrawable drawable = new LayerDrawable(drawables);
        ((ImageView) ((MainActivity) ctx).findViewById(R.id.brush_color)).setImageDrawable(drawable);
    }

    private void modifyBackgroundIcon(){
        ShapeDrawable innerRectangle = new ShapeDrawable(new RectShape());
        int width = ((MainActivity) ctx).findViewById(R.id.background_color).getWidth() - 7;
        innerRectangle.setIntrinsicHeight(width);
        innerRectangle.setIntrinsicWidth(width);
        innerRectangle.setBounds(new Rect(0, 0, width, width));
        innerRectangle.getPaint().setColor(colorPicker.getColor());

        ShapeDrawable outerRectangle = new ShapeDrawable(new RectShape());
        int outerWidth = ((MainActivity) ctx).findViewById(R.id.background_color).getWidth();
        outerRectangle.setIntrinsicHeight(outerWidth);
        outerRectangle.setIntrinsicWidth(outerWidth);
        outerRectangle.setBounds(new Rect(0, 0, outerWidth, outerWidth));
        outerRectangle.setPadding(5,5,5,5);
        outerRectangle.getPaint().setColor(Color.BLACK);
        Drawable[] drawables = {outerRectangle,innerRectangle};
        LayerDrawable drawable = new LayerDrawable(drawables);
        ((ImageView) ((MainActivity) ctx).findViewById(R.id.background_color)).setImageDrawable(drawable);
        ((MainActivity)ctx).notifyBackgroundChanged();
    }

    private class DismissListener implements OnDismissListener {

        @Override
        public void onDismiss(DialogInterface dialog) {
            if(brush) {
                ((MainActivity) ctx).getDrawingView().getPaint().setColor(colorPicker.getColor());
                ColorPickerDialog.this.modifyBrushIcon();
            }
            else {
                DrawingView drawingArea = ((MainActivity)ctx).getDrawingView();
                drawingArea.setBackgroundColor(colorPicker.getColor());
                ColorPickerDialog.this.modifyBackgroundIcon();
                drawingArea.resetBitmap();
            }
        }
    }
}
