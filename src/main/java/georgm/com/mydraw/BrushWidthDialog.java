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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class BrushWidthDialog extends Dialog {

    private Context ctx;
    private SeekBar seekBar;
    private TextView textView;

    public BrushWidthDialog(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        setContentView(R.layout.brush_width_dialog_layout);
        textView = (TextView)findViewById(R.id.text_view);
        int initialWidth = (int)((MainActivity)ctx).getDrawingView().getPaint().getStrokeWidth();
        textView.setText(Integer.toString(initialWidth));
        seekBar = (SeekBar)findViewById(R.id.seek_bar);
        seekBar.setMax(100);
        DrawingView drawingView = ((MainActivity)ctx).getDrawingView();
        seekBar.setProgress((int) drawingView.getPaint().getStrokeWidth());
        seekBar.setLeft(1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView.setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ((MainActivity) BrushWidthDialog.this.ctx).getDrawingView()
                        .getPaint().setStrokeWidth((float) seekBar.getProgress());
                dismiss();
            }
        });
        setOnDismissListener(new DismissListener());
    }

    private void modifyBrushSizeIcon(){
        ShapeDrawable outerCircle = new ShapeDrawable(new OvalShape());
        int outerWidth = ((MainActivity) ctx).findViewById(R.id.brush_width).getWidth();
        outerCircle.setIntrinsicHeight(outerWidth);
        outerCircle.setIntrinsicWidth(outerWidth);
        outerCircle.setBounds(new Rect(0, 0, outerWidth, outerWidth));
        int outerColor = Color.parseColor("#746e6e");
        outerCircle.getPaint().setColor(outerColor);
        int padding = (int)((100.0 - seekBar.getProgress())/100.0 * 0.5 * outerWidth);
        outerCircle.setPadding(padding, padding, padding, padding);

        ShapeDrawable innerCircle = new ShapeDrawable(new OvalShape());
        int innerWidth = (int)((seekBar.getProgress() / 100.0) * outerWidth);
        innerCircle.setIntrinsicHeight(innerWidth);
        innerCircle.setIntrinsicWidth(innerWidth);
        innerCircle.setBounds(new Rect(0, 0, innerWidth, innerWidth));
        innerCircle.getPaint().setColor(Color.BLACK);

        Drawable[] drawables = {outerCircle,innerCircle};
        LayerDrawable drawable = new LayerDrawable(drawables);
        ((ImageView) ((MainActivity) ctx).findViewById(R.id.brush_width)).setImageDrawable(drawable);
    }

    private class DismissListener implements OnDismissListener{
        @Override
        public void onDismiss(DialogInterface dialog) {
            modifyBrushSizeIcon();
        }
    }
}
