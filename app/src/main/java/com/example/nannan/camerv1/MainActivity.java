package com.example.nannan.camerv1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static LruCache<String, Bitmap> memoryCache;

    ImageView imageView;
    TextView textView;
    String[] bio = new String[4];

    private Bitmap loadImageFromStorage(String path)
    {

        try {
            File f=new File(path, "pan.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            imageView.setImageBitmap(b);
            return b;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCamera = findViewById(R.id.btnCamera);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.text_view);

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PhotoActivity.class);
                startActivityForResult(intent,0);
            }
        });

        memoryCache = new LruCache<String, Bitmap>(30*1024*1024) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    @Override
    protected void onStart() {
        System.out.println("onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        System.out.println("onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        System.out.println("onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        System.out.println("onStop");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        System.out.println("onRestart");
        super.onRestart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == 0) {
            String path = null;
            System.out.println("TIME5: " + PhotoActivity.dateFormat.format(new Date()));
            try {
                path = PhotoActivity.future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("TIME6: " + PhotoActivity.dateFormat.format(new Date()));
            Bitmap bitmap = loadImageFromStorage(path);
            System.out.println("TIME7: " + PhotoActivity.dateFormat.format(new Date()));
            imageView.setImageBitmap(bitmap);
            System.out.println("TIME8: " + PhotoActivity.dateFormat.format(new Date()));

            TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
            if (!textRecognizer.isOperational()) {
                Log.w("PhotoActivity", "Detector dependencies are not yet available");
            } else {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);
                List<Text> components = new ArrayList<Text>();
                for (int i = 0; i < textBlocks.size(); i++) {
                    TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                    components.addAll(textBlock.getComponents());
                }

                Comparator<Text> comparator = new Comparator<Text>() {
                    @Override
                    public int compare(Text t1, Text t2) {
                        return t1.getBoundingBox().top - t2.getBoundingBox().top;
                    }
                };

                Collections.sort(components, comparator);

                StringBuilder sb = new StringBuilder();
                for(Text component: components) {
                    sb.append(component.getValue());
                    sb.append("\n");
                }
                textView.setText(sb.toString());

                getPanAndDob(components);
                getName(components);
                getFather(components);
                String pan = bio[3];
                String name = bio[0];
                String father = bio[1];
                String dob = bio[2];
//                textView.setText("\nPan: " + pan);
            }
            System.out.println("TIME9: " + PhotoActivity.dateFormat.format(new Date()));
        }
    }

    private void getFather(List<Text> components) {
        for(int i=0; i<components.size()-2; i++) {
            String n = components.get(i).getValue();
            String d = components.get(i+2).getValue();
            if(n.equals(bio[0]) && d.equals(bio[2])) {
                bio[1] = components.get(i+1).getValue();
                return;
            }
        }
        bio[1] = "unable to get father's name";
    }

    private void getName(List<Text> components) {
        for(int i=0; i<components.size()-2; i++) {
            String string = components.get(i).getValue() + components.get(i+1).getValue();
            if(string.contains("INCOME") &&
                    string.contains("TAX") &&
                    (string.contains("DEPARTMENT") ||
                    string.contains("DEPARTIMENT")) &&
                    string.contains("GOVT") &&
                    string.contains("OF") &&
                    string.contains("INDIA")) {
                bio[0] = components.get(i+2).getValue().toUpperCase();
                return;
            }
        }
        bio[0] = "Unable to retrieve name";
    }

    private void getPanAndDob(List<Text> components) {
        for(int i=1; i<components.size()-1; i++) {
            String string = components.get(i).getValue();
            if(string.contains("Number")) {
                bio[3] = components.get(i+1).getValue().toUpperCase();
                bio[2] = components.get(i-1).getValue().toUpperCase();
                return;
            }
        }
        bio[3] = "Unable to retrieve pan";
        bio[2] = "Unable to retrieve dob";
    }
}
