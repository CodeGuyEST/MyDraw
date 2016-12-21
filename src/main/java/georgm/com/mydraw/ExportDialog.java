package georgm.com.mydraw;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SearchEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import java.io.OutputStream;

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
        if(!checkPermissions()){
            dismiss();
            return;
        }
    }

    private boolean checkPermissions(){
        if(Build.VERSION.SDK_INT >= 23) {
            if(ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions((Activity)ctx,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MainActivity.PERMISSION_WRITE_EXTERNAL_STORAGE);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onSearchRequested(SearchEvent searchEvent) {
        return super.onSearchRequested(searchEvent);
    }

    private class ExportOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            ContentResolver cr = ctx.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, title.getText().toString());
            values.put(MediaStore.Images.Media.DISPLAY_NAME, title.getText().toString());
            values.put(MediaStore.Images.Media.DESCRIPTION, description.getText().toString());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

            Uri url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Canvas canvas = new Canvas();
            canvas.drawColor(drawingView.getBackgroundColor());
            canvas.drawBitmap(drawingView.getDrawingCache(),0,0,null);
            try {
                OutputStream imageOut = cr.openOutputStream(url);
                drawingView.getDrawingCache().compress(Bitmap.CompressFormat.PNG,100,imageOut);
                imageOut.close();
                dismiss();
            }
            catch (Exception e) {
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
