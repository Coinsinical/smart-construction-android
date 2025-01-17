// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.hhu.smartdetection;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.xuexiang.xui.XUI;
import com.xuexiang.xui.widget.actionbar.TitleBar;
import com.xuexiang.xui.widget.button.switchbutton.SwitchButton;
import com.xuexiang.xui.widget.imageview.preview.view.SmoothImageView;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DetectionActivity extends AppCompatActivity
{
    private static final int SELECT_MEDIA = 1;
    public static final int TAKE_PHOTO = 2;

    private int current_model = 0;
    private int current_cpugpu = 0;

    private int threadStop = 0;

    private String mediaType = null;

    private int option = 0;

    private Uri mediaUri;

    private ImageView imageView;
    private ImageView videoFrameView;
    private
     VideoView videoView;
    private Bitmap bitmap = null;
    private List<Bitmap> detectedBitmaps = new ArrayList<>();
    private Bitmap yourSelectedImage = null;

    private YoloV5Ncnn yolov5ncnn = new YoloV5Ncnn();

    /** Called when the activity is first created. */
    @Override
    //初始化按钮
    public void onCreate(Bundle savedInstanceState) {
        XUI.initTheme(this);//加入这行代码
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        option = getIntent().getIntExtra("OPTION",0);
        TextView textContent = (TextView) findViewById(R.id.textContent);
        TitleBar titleBar = (TitleBar) findViewById(R.id.detectionTitleBar);
        titleBar.setLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threadStop = 0;
                finish();
            }
        });

        String[] titles = getResources().getStringArray(R.array.class_array);
        if (option == 0 || option == 1 || option == 2 || option == 3 || option == 4 || option == 7 || option == 8) {
            current_model = 0;
            textContent.setText(R.string.construction_introduction);
        } else if (option == 5) {
            current_model = 1;
            textContent.setText(R.string.construction_introduction);
        } else if (option == 6) {
            current_model = 1;
            textContent.setText(R.string.pipe_introduction);
        }
        titleBar.setTitle(titles[option]);
        reload();

        imageView = (ImageView) findViewById(R.id.imageView);
        videoView = (VideoView) findViewById(R.id.videoView);

        Button buttonMedia = (Button) findViewById(R.id.buttonImage);
        buttonMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                threadStop = 0;
                // 创建选择图片的Intent
                Intent photoIntent = new Intent(Intent.ACTION_PICK);
                photoIntent.setType("image/*");

                // 创建选择视频的Intent
                Intent videoIntent = new Intent(Intent.ACTION_PICK);
                videoIntent.setType("video/*");

                // 创建选择器Intent，用于同时选择图片和视频
                Intent chooserIntent = Intent.createChooser(photoIntent, "Select Media");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { videoIntent });

                // 启动选择器并传递请求码
                startActivityForResult(chooserIntent, SELECT_MEDIA);
            }
        });

        // 拍照按钮实现
        Button buttonShot = (Button) findViewById(R.id.buttonShot);
        buttonShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                threadStop = 0;
                //创建File对象，用来存储拍照后的照片
                //getExternalCacheDir()获取此应用缓存数据的位置，在这个位置保存图片
                File outputImage=new File(getExternalCacheDir(),"output_image.jpg");
                System.out.println("shot button is pressed");
                try{
                    if (outputImage.exists()){//如果图片已经存在就删除再重新创建
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT>=24){
                    mediaUri= FileProvider.getUriForFile(DetectionActivity.this,
                            "com.hhu.smartdetection.fileprovider",outputImage);
                }else{
                    mediaUri=Uri.fromFile(outputImage);
                }
                //启动相机
                Intent intent=new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,mediaUri);
                startActivityForResult(intent,TAKE_PHOTO);
            }
        });

        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            public void detect(){
                YoloV5Ncnn.Obj[] objects = null;
                if (current_cpugpu == 0) {
                    objects = yolov5ncnn.Detect(yourSelectedImage, current_model, false);
                } else if (current_cpugpu == 1) {
                    // System.out.println("start gpu");
                    objects = yolov5ncnn.Detect(yourSelectedImage, current_model, true);
                }
                showObjects(objects);
            }

            private void videoDetect() {
                // 创建一个新的线程来执行耗时操作
                Thread videoDetectThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 创建MediaMetadataRetriever对象
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        try {
                            // 设置数据源为视频路径
                            retriever.setDataSource(getApplicationContext(), mediaUri);

                            // 获取视频时长（单位：毫秒）
                            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                            // 获取视频帧率
                            String frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);

                            // 将时长,帧率转换为整数
                            int videoDuration = Integer.parseInt(duration);
                            int videoFrameRate = (int) (Float.parseFloat(frameRate));

                            // 定义每一帧的时间间隔（单位：毫秒）
                            int frameInterval = 1000 / videoFrameRate; // 每秒获取一帧
                            //int frameInterval = 40; // 每秒获取一帧

                            // 遍历视频的每一帧
                            for (int time = 0; time < videoDuration; time += frameInterval) {
                                if (threadStop == 0)
                                    break;
                                // 获取当前时间的帧
                                Bitmap frameBitmap = retriever.getFrameAtTime(time * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                                bitmap = processBitmap(frameBitmap);
                                if (bitmap != null) {
                                    yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        detect();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            // 释放MediaMetadataRetriever对象
                            retriever.release();
                        }
                    }
                });

                // 启动线程
                videoDetectThread.start();
            }
            @Override
            public void onClick(View arg0) {
                threadStop = (threadStop == 1) ? 0 : 1;

                if (mediaType == null)
                        return;
                if (mediaType.startsWith("video/")) {
                    if (mediaUri == null)
                        return;
                    videoView.stopPlayback();
                    videoView.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    videoDetect();
                } else if (mediaType.startsWith("image/")) {
                    if (yourSelectedImage == null)
                        return;
                    videoView.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    detect();
                }
            }
        });

        SwitchButton buttonMode = (SwitchButton) findViewById(R.id.buttonMode);
        buttonMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (buttonMode.isChecked()) {
                    current_cpugpu = 1;
                }
                else
                    current_cpugpu = 0;

                String inform = "模式切换成功";
                Toast toast=Toast.makeText(getApplicationContext(), inform, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private void reload()
    {
        boolean ret_init = yolov5ncnn.Init(getAssets(), current_model);
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov5ncnn loadModel failed");
        }
    }

    private void showCustomDialog(List<String> messages) {
        String text;
        CustomDialog customDialog = new CustomDialog(DetectionActivity.this);
        // 连环定义设置

        customDialog.setsTitle("提   示");
        customDialog.setsMessage(messages.get(0));
        if (messages.get(0).equals("未检测到异常"))
            text = "确定";
        else
            text = "实施依据";


        customDialog.setsRight("关闭", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                customDialog.dismiss();
            }
        });
        customDialog.setsLeft(text, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (customDialog.getsLeft()) {
                    case "实施依据":
                        Log.e("CUSTOM", "CHANGE");
                        customDialog.setsMessage(messages.get(1));
                        customDialog.setsLeft("返回");
                        break;
                    case "返回":
                        Log.e("CUSTOM", "CHANGE back");
                        customDialog.setsMessage(messages.get(0));
                        Log.e("CUSTOM", messages.get(0));
                        customDialog.setsLeft("实施依据");
                        break;
                    case "确定":
                        customDialog.dismiss();
                        break;
                }
            }
        });
        customDialog.show();
    }

    private void showObjects(YoloV5Ncnn.Obj[] objects)
    {
        if (objects == null)
        {
            System.out.println("none");
            imageView.setImageBitmap(bitmap);
            return;
        }
        System.out.println("objects exist");

        // draw objects on bitmap
        Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        final int[] colors = new int[] {
                Color.rgb( 54,  67, 244),
                Color.rgb( 99,  30, 233),
                Color.rgb(176,  39, 156),
                Color.rgb(183,  58, 103),
                Color.rgb(181,  81,  63),
                Color.rgb(243, 150,  33),
                Color.rgb(244, 169,   3),
                Color.rgb(212, 188,   0),
                Color.rgb(136, 150,   0),
                Color.rgb( 80, 175,  76),
                Color.rgb( 74, 195, 139),
                Color.rgb( 57, 220, 205),
                Color.rgb( 59, 235, 255),
                Color.rgb(  7, 193, 255),
                Color.rgb(  0, 152, 255),
                Color.rgb( 34,  87, 255),
                Color.rgb( 72,  85, 121),
                Color.rgb(158, 158, 158),
                Color.rgb(139, 125,  96)
        };

        Canvas canvas = new Canvas(rgba);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        Paint textbgpaint = new Paint();
        textbgpaint.setColor(Color.WHITE);
        textbgpaint.setStyle(Paint.Style.FILL);

        Paint textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(26);
        textpaint.setTextAlign(Paint.Align.LEFT);

        // 定义标签与次数 映射
        Map<String, Integer> map=new HashMap<>();

        // 定义label与中文映射
        Map<String,String> labelMap = new HashMap<>();
        labelMap.put("tou","未佩戴安全帽");
        labelMap.put("noc","未穿戴反光衣");
        labelMap.put("dao","有人跌倒");
        labelMap.put("yan","烟雾");
        labelMap.put("huo","火焰");
        labelMap.put("zhui","反光锥");
        labelMap.put("keng","水坑");
        labelMap.put("dang","围挡");
        labelMap.put("shi","未回填石块");

        labelMap.put("PL","破裂");
        labelMap.put("BX","变形");
        labelMap.put("FS","腐蚀");
        labelMap.put("CK","错口");
        labelMap.put("QF","起伏");
        labelMap.put("TJ","脱节");
        labelMap.put("JG","结垢");
        labelMap.put("FZ","浮渣");
        labelMap.put("ZW","障碍物");
        labelMap.put("BT","坝头");
        labelMap.put("CJ","沉积");
        labelMap.put("CR","异物穿入");
        labelMap.put("SG","树根");

        // 定义各个选项列表
        List<List<String>> option_List = new ArrayList<>();
        option_List.add(0,Arrays.asList("tou","noc"));
        option_List.add(1,Arrays.asList("keng","dang","zhui"));
        option_List.add(2,Arrays.asList("yan","huo"));
        option_List.add(3,Collections.singletonList("keng"));
        option_List.add(4,Collections.singletonList("dao"));
        option_List.add(5,Collections.singletonList("shi"));
        option_List.add(6,Arrays.asList("PL","BX","FS","CK","QF","TJ","JG","FZ","ZW","BT","CJ","CR","SG"));
        option_List.add(7,Arrays.asList("dang","tou","noc"));
        option_List.add(8,Arrays.asList("tou","noc"));

        Integer time = 1; // 定义次数


        for (int i = 0; i < objects.length; i++)
        {
            // 根据选择排除输出
            if (!option_List.get(option).contains(objects[i].label))
                continue;

            paint.setColor(colors[i % 19]);

            canvas.drawRect(objects[i].x, objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, paint);

            // draw filled text inside image
            {
                String text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%";
                // System.out.println(text);
                if (map.containsKey(objects[i].label)){
                    map.put(objects[i].label,map.get(objects[i].label) + time);
                }
                else {
                    map.put(objects[i].label,time);
                }

                float text_width = textpaint.measureText(text);
                float text_height = - textpaint.ascent() + textpaint.descent();

                float x = objects[i].x;
                float y = objects[i].y - text_height;
                if (y < 0)
                    y = 0;
                if (x + text_width > rgba.getWidth())
                    x = rgba.getWidth() - text_width;

                canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);

                canvas.drawText(text, x, y - textpaint.ascent(), textpaint);
            }
        }

        StringBuilder inform = new StringBuilder();
        StringBuilder rationale = new StringBuilder();
        if (map.size() == 0){
            inform = new StringBuilder("未检测到异常");
        }
        else {
            Set<String> keySet = map.keySet();
            String[] informs = getResources().getStringArray(R.array.inform_array);
            String[] rationales = getResources().getStringArray(R.array.rationale_array);
            for (String key : keySet) {
                //由键找值
                Integer value = map.get(key);
                //输出键和值
                System.out.print(key + " " + value + ", ");
                key = labelMap.get(key);
                // inform.append("检测到").append(key).append(value).append("处，应按例处罚\n");
                if (option == 0) {
                    if (key == "未佩戴安全帽" || key == "未穿戴反光衣")
                        inform.append("检测到").append(key).append(value).append("处\n").append(informs[option]);
                } else if (option == 1) {
                    inform = new StringBuilder("未检测到异常");
                    if (key == "水坑") {
                        if (!keySet.contains("dang") && !keySet.contains("zhui")) {
                            inform = new StringBuilder("");
                            inform.append("检测到").append("围挡不规范\n").append(informs[option]);
                        }
                    }
                } else if (option == 2) {
                    if (Objects.equals(key, "烟雾") || Objects.equals(key, "火焰"))
                        inform.append("检测到").append(key).append(value).append("处\n").append(informs[option]);
                } else if (option == 3) {
                    if (key == "水坑")
                        inform.append("检测到").append(key).append(value).append("处\n").append(informs[option]);
                } else if (option == 4) {
                    if (key == "有人跌倒")
                        inform.append("检测到").append(key).append(value).append("处\n").append(informs[option]);
                } else if (option == 5) {
                    if (key == "未回填石块")
                        inform.append("检测到").append(key).append(value).append("处\n").append(informs[option]);
                } else if (option == 6) {
                    inform.append("检测到").append(key).append(value).append("处\n").append(informs[option]);
                } else if (option == 7){
                    if (key == "围挡")
                        if (keySet.contains("tou") || keySet.contains("noc"))
                            inform.append("检测到").append("围挡区域闯入").append(informs[option]);
                        else
                            inform = new StringBuilder("未检测到异常");
                }else if (option == 8)
                    if (key == "办公人员脱岗") {
                        inform.append("检测到").append(key).append(value).append("处\n").append(informs[option]);
                    }
            }
            if (!inform.toString().equals("未检测到异常")){
                rationale.append(rationales[option]);
            }
        }


        if (mediaType.startsWith("image/")) {
            imageView.setImageBitmap(rgba);
            List<String> messages = new ArrayList<>();
            messages.add(inform.toString());
            messages.add(rationale.toString());
            showCustomDialog(messages);
            // toast.setGravity(Gravity.CENTER,0,0);
        }
        else if (mediaType.startsWith("video/"))
            detectedBitmaps.add(rgba);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("resultCode:" + resultCode);
        try {
            if (resultCode == RESULT_OK) {
                LinearLayout layout = findViewById(R.id.imageLayout);
                layout.setBackground(null);
                switch (requestCode) {
                    case TAKE_PHOTO:
                        bitmap = decodeUri(mediaUri);
                        // System.out.println("show photo");
                        yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        imageView.setImageBitmap(bitmap);
                        break;
                    case SELECT_MEDIA:
                        // System.out.println("show images");
                       if (null != data) {
                           mediaUri = data.getData();
                           if (mediaUri != null) {
                               mediaType = getContentResolver().getType(mediaUri);
                           }
                           if (mediaType != null && mediaType.startsWith("image/")) {
                               imageView.setVisibility(View.VISIBLE);
                               videoView.setVisibility(View.GONE);
                               bitmap = decodeUri(mediaUri);
                               yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                               imageView.setImageBitmap(bitmap);
                           } else if (mediaType != null && mediaType.startsWith("video/")) {
                               imageView.setVisibility(View.GONE);
                               videoView.setVisibility(View.VISIBLE);
                               videoView.setVideoURI(mediaUri);
                               // videoView.start();
                           }
                       }
                        break;
                    default:
                        break;
                }
            }
        }catch (FileNotFoundException e) {
            Log.e("MainActivity", "FileNotFoundException");
            return;
        }
    }

    private Bitmap processBitmap(Bitmap bitmap) {
        try {
            // Rotate according to EXIF
            int rotate = 0;
            try {
                // Assuming the bitmap is obtained from a camera or saved with EXIF orientation information
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream inputStream = new ByteArrayInputStream(stream.toByteArray());
                ExifInterface exif = new ExifInterface(inputStream);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotate = 270;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotate = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotate = 90;
                        break;
                }
            } catch (IOException e) {
                Log.e("MainActivity", "ExifInterface IOException");
            }

            // Scale bitmap
            // The new size we want to scale to
            final int REQUIRED_SIZE = 640;

            // Find the correct scale value. It should be the power of 2.
            int width_tmp = bitmap.getWidth(), height_tmp = bitmap.getHeight();
            int scale = 1;
            while (width_tmp / 2 >= REQUIRED_SIZE
                    && height_tmp / 2 >= REQUIRED_SIZE) {
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            // Apply rotation
            matrix.postRotate(rotate);

            // Apply matrix
            Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Reduce image quality
            ByteArrayOutputStream qualityStream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 10, qualityStream);
            byte[] qualityBytes = qualityStream.toByteArray();
            Bitmap reducedQualityBitmap = BitmapFactory.decodeByteArray(qualityBytes, 0, qualityBytes.length);

            return reducedQualityBitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException
    {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 640;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (width_tmp / 2 >= REQUIRED_SIZE
                && height_tmp / 2 >= REQUIRED_SIZE) {
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

        // Rotate according to EXIF
        int rotate = 0;
        try
        {
            ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(selectedImage));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        }
        catch (IOException e)
        {
            Log.e("MainActivity", "ExifInterface IOException");
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}

