package com.example.nannan.camerv1;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhotoActivity extends AppCompatActivity {

    SurfaceView surfaceView;
    TextView textView;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;
    boolean pictureTaken = false;
    TextRecognizer textRecognizer;
    Rect cardBoundary;
    Rect textBoundary;
    public static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static Future<String> future;
//                    1024wx768h
//                    right = 768
//                    bottom = 1024

    public class saveToInternalStorageCallable implements Callable<String> {
        private final Bitmap bitmapImage;
        public saveToInternalStorageCallable(Bitmap bitmap) {
            bitmapImage = bitmap;
        }

        @Override
        public String call() {
            return saveToInternalStorage(bitmapImage);
        }
    }


    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"pan.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    public static Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }

                    return null;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }

        return null;
    }

    CameraSource.PictureCallback pictureCallback = new CameraSource.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes) {
            Intent intent = new Intent();
//            System.err.println(cameraSource.getPreviewSize().getWidth());
//            System.err.println(cameraSource.getPreviewSize().getHeight());
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            bitmap=Bitmap.createBitmap(bitmap, 0, bitmap.getHeight()/4, bitmap.getWidth(), bitmap.getHeight()/2);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<String> callable = new saveToInternalStorageCallable(bitmap);
            future = executor.submit(callable);
//            String path = saveToInternalStorage(bitmap);
//            intent.putExtra("path", path);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    CameraSource.ShutterCallback shutterCallback = new CameraSource.ShutterCallback() {
        @Override
        public void onShutter() {
            try {
//                Camera camera = getCamera(cameraSource);
//                Method _stopPreview = Camera.class.getDeclaredMethod("_stopPreview");
//                _stopPreview.setAccessible(true);
//                _stopPreview.invoke(camera);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    Detector.Processor textProcessor = new Detector.Processor<TextBlock>() {
        @Override
        public void release() {

        }

        @Override
        public void receiveDetections(Detector.Detections<TextBlock> detections) {

            final SparseArray<TextBlock> items = detections.getDetectedItems();
            if(items.size() != 0)
            {
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder stringBuilder = new StringBuilder();
                        for(int i =0;i<items.size();++i) {
                            TextBlock item = items.valueAt(i);
                            stringBuilder.append(item.getValue());
                            stringBuilder.append("\n");

                            List<Text> comps = new ArrayList<>();
                            comps.addAll(item.getComponents());
                            for(Text comp: comps) {
//                                if(comp.getValue().contains("INCOME")) {
//                                    textView.setText(comp.getBoundingBox().right +
//                                        ", " + comp.getBoundingBox().bottom);
//                                }

                            }
                        }
                        textView.setText(stringBuilder.toString());


                        if(clickPan(items) && !pictureTaken) {
                            pictureTaken = true;
                            cameraSource.takePicture(shutterCallback, pictureCallback);
                        }
                    }
                });
            }
        }
    };

    private boolean clickPan(SparseArray<TextBlock> items) {
        String[] sArr = {
//                "INCOME", "TAX", "DEPARTMENT", "Permanent", "Account", "Number",
//                "GOVT", "OF", "INDIA", "Signature"
                "Date", "of", "Birth", "DOB"
//                "Address"
        };
        int size = sArr.length;
        Boolean[] cArr = new Boolean[size];
        Arrays.fill(cArr, Boolean.FALSE);
        Pattern pattern = Pattern.compile("^[0-9]{12}|[0-9]{4}\\s[0-9]{8}|[0-9]{8}\\s[0-9]{4}|[0-9]{4}\\s[0-9]{4}\\s[0-9]{4}$");
        boolean regexMatch = false;
        Matcher matcher;
        List<Text> components = new ArrayList<>();
        for(int i=0; i<items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            components.addAll(item.getComponents());
        }
        for(int i =0;i<components.size();++i) {
            Text item = components.get(i);
            matcher = pattern.matcher(item.getValue());
            regexMatch |= matcher.matches();
            Rect bounds = item.getBoundingBox();
            if(textBoundary.contains(bounds)) {
                for(int j=0; j<size; j++) {
                    if(item.getValue().contains(sArr[j])) {
                        cArr[j] = true;
                    }
                }
            }
        }
        boolean result = true;
        for(boolean c: cArr) {
            result &= c;
        }

        return result & regexMatch;
    }

    SurfaceHolder.Callback surfaceHolder = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {

            try {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(PhotoActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        RequestCameraPermissionID);
                    return;
                }
                cameraSource.start(surfaceView.getHolder());
                int w = cameraSource.getPreviewSize().getWidth();   // 1024
                int h = cameraSource.getPreviewSize().getHeight();  // 768
                textBoundary = new Rect(0, w/4, h, w*3/4);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            cameraSource.stop();
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(surfaceView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("onCreate2");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        textView = (TextView) findViewById(R.id.text_view);

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("PhotoActivity", "Detector dependencies are not yet available");
        } else {

            GuideBox guideBox = new GuideBox(this);

            addContentView(guideBox, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
//                    .setRequestedPreviewSize(width, height)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            surfaceView.getHolder().addCallback(surfaceHolder);

            textRecognizer.setProcessor(textProcessor);
        }
    }

    public class GuideBox extends View {
        public GuideBox(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.GREEN);
            cardBoundary = new Rect(64, (canvas.getHeight()-64)/4, canvas.getWidth()-64, (canvas.getHeight()-64)*3/4);
            System.err.println("CanvasH: " + canvas.getHeight());
            System.err.println("CanvasW: " + canvas.getWidth());
            canvas.drawRect(cardBoundary, paint);
            super.onDraw(canvas);
        }
    }

}