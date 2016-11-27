package georgm.com.mydraw;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.OutputStream;

/**
 * Created by Georg on 25.11.2016.
 */
public class ExportDialog extends Dialog {

    private EditText title,description;
    private Button export,cancel;
    private Context ctx;
    private DrawingView drawingView;

    public ExportDialog(Context context) {
        super(context);
        ctx = context;
    }

    public ExportDialog(Context context, DrawingView drawingView) {
        super(context);
        ctx = context;
        this.drawingView = drawingView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export_dialog);
        title = (EditText)findViewById(R.id.title_field);
        description = (EditText)findViewById(R.id.description_field);
        export = (Button)findViewById(R.id.export);
        export.setOnClickListener(new ExportOnClickListener());
        cancel = (Button)findViewById(R.id.cancel);
        cancel.setOnClickListener(new CancelOnClickListener());
    }

    private class ExportOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            ContentResolver cr = ctx.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE,title.getText().toString());
            values.put(MediaStore.Images.Media.DISPLAY_NAME, title.getText().toString());
            values.put(MediaStore.Images.Media.DESCRIPTION, description.getText().toString());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

            Uri url = null;

            Bitmap source = drawingView.getBitmap();

            Bitmap newBitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
            Canvas canvas = new Canvas(newBitmap);
            canvas.drawColor(drawingView.getBackgroundColor());
            canvas.drawBitmap(source, 0, 0, null);

            try {
                url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (source != null) {
                    OutputStream imageOut = cr.openOutputStream(url);
                    try {
                        newBitmap.compress(Bitmap.CompressFormat.JPEG, 50, imageOut);
                    } finally {
                        imageOut.close();
                        dismiss();
                    }

                } else {
                    cr.delete(url, null, null);
                    url = null;
                }
            } catch (Exception e) {
                if (url != null) {
                    cr.delete(url, null, null);
                }
                dismiss();
            }
        }
    }

    private class CancelOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            dismiss();
        }
    }
}
