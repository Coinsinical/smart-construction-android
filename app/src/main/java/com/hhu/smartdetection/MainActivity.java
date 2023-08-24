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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.collection.CircularArray;
import androidx.core.content.FileProvider;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends Activity
{
    private static final int SELECT_IMAGE = 1;
    public static final int TAKE_PHOTO = 2;

    private int current_model = 0;
    private int current_cpugpu = 0;

    private int option = 0;
    private int secondOption = 0;

    ArrayAdapter<String>  modelAdapter;
    ArrayAdapter<String>  secondModelAdapter;

    private Uri imageUri;

    private ImageView imageView;
    private Bitmap bitmap = null;
    private Bitmap yourSelectedImage = null;

    private TextView textContent;

    private YoloV5Ncnn yolov5ncnn = new YoloV5Ncnn();

    /** Called when the activity is first created. */
    @Override
    //初始化按钮
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        reload();
        textContent = (TextView) findViewById(R.id.textContent);
        textContent.setText(R.string.construction_introduction);

        Spinner spinnerModel = (Spinner) findViewById(R.id.spinnerModel);
        Spinner spinnerSecondModel = (Spinner) findViewById(R.id.spinnerSecondModel);
        String[] modellist = new String[] {"质量监测","管道监测"};
        String[] qualityList = new String[] {"安全隐患","石块识别"};
        String[] pipeList = new String[] {"管道缺陷"};

        modelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,  modellist);
        spinnerModel.setAdapter(modelAdapter);
        spinnerModel.setSelection(0,true);//设置初始默认值
        //绑定适配器和值
        secondModelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, qualityList);
        spinnerSecondModel.setAdapter(secondModelAdapter);
        spinnerSecondModel.setSelection(0,true);//设置初始默认值


        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != option)
                {
                    System.out.println("model changed");
                    // 两个选项调用相同的模型
                    if (position == 0){
                        current_model = 0;
                    } else if (position == 1) {
                        current_model = 1;
                    }
                    option = position;
                    /*
                     一个选项对应一个模型
                     current_model = position;
                    */
                    if(position == 0){
                        secondModelAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,qualityList);
                        spinnerSecondModel.setAdapter(secondModelAdapter);
                    }else if (position == 1){
                        secondModelAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item, pipeList);
                        spinnerSecondModel.setAdapter(secondModelAdapter);
                    }
                    reload();

                    if (position == 0){
                        textContent.setText(R.string.construction_introduction);
                    } else if (position == 1 ) {
                        textContent.setText(R.string.pipe_introduction);
                    }
                    String inform = new String("模型切换成功");
                    Toast toast=Toast.makeText(getApplicationContext(), inform.toString(),
                            Toast.LENGTH_SHORT);
                    // toast.setGravity(Gravity.CENTER,0,0);
                    LinearLayout linearLayout = (LinearLayout) toast.getView();
                    TextView messageTextView = (TextView) linearLayout.getChildAt(0);
                    messageTextView.setTextSize(15);
                    toast.show();
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        spinnerSecondModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                //获取列表项的值
                String category = adapterView.getItemAtPosition(position).toString();
                System.out.println(category);
                switch (category) {
                    case "安全隐患":
                        secondOption = 0;
                        break;
                    case "石块识别":
                        secondOption = 1;
                        break;
                    case "管道缺陷":
                        secondOption = 2;
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        Spinner spinnerCPUGPU = (Spinner) findViewById(R.id.spinnerCPUGPU);
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_cpugpu)
                {
                    current_cpugpu = position;
                    // reload();
                    String inform = new String("模式切换成功");
                    Toast toast=Toast.makeText(getApplicationContext(), inform.toString(),
                            Toast.LENGTH_SHORT);
                    // toast.setGravity(Gravity.CENTER,0,0);
                    LinearLayout linearLayout = (LinearLayout) toast.getView();
                    TextView messageTextView = (TextView) linearLayout.getChildAt(0);
                    messageTextView.setTextSize(15);
                    toast.show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        imageView = (ImageView) findViewById(R.id.imageView);

        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                System.out.println("select button is pressed");
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE);
            }
        });

        // 拍照按钮实现
        Button buttonShot = (Button) findViewById(R.id.buttonShot);
        buttonShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                    imageUri= FileProvider.getUriForFile(MainActivity.this,
                            "com.hhu.smartdetection.fileprovider",outputImage);
                }else{
                    imageUri=Uri.fromFile(outputImage);
                }
                //启动相机
                Intent intent=new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent,TAKE_PHOTO);
            }
        });

        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;

                YoloV5Ncnn.Obj[] objects = null;
                // reload();

                if (current_cpugpu == 0){
                    System.out.println("start cpu");
                    objects = yolov5ncnn.Detect(yourSelectedImage, current_model,false);
                    System.out.println("end cpu");
                } else if (current_cpugpu == 1) {
                    // System.out.println("start gpu");
                    objects = yolov5ncnn.Detect(yourSelectedImage, current_model,true);
                }
                showObjects(objects);
            }
        });

//        Button buttonDetectGPU = (Button) findViewById(R.id.buttonDetectGPU);
//        buttonDetectGPU.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                if (yourSelectedImage == null)
//                    return;
//                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(yourSelectedImage, true);
//                showObjects(objects);
//            }
//        });

    }

    private void reload()
    {
        boolean ret_init = yolov5ncnn.Init(getAssets(), current_model);
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov5ncnn loadModel failed");
        }
    }

    private void showCustomDialog(String message) {
        CustomDialog customDialog = new CustomDialog(MainActivity.this);
        // 连环定义设置
        customDialog.setsTitle("提   示");
        customDialog.setsMessage(message);
        customDialog.setsRight("确定", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                customDialog.dismiss();
            }
        });
        customDialog.setsLeft("关闭", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                customDialog.dismiss();
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
        List<String> option0_list = Arrays.asList("tou","noc","dang","zhui","yan","huo","dao","keng");
        List<String> option1_list = Arrays.asList("shi");
        List<String> option2_list = Arrays.asList("PL","BX","FS","CK","QF","TJ","JG","FZ","ZW","BT","CJ","CR","SG");

        Integer time = 1; // 定义次数


        for (int i = 0; i < objects.length; i++)
        {
            // 根据选择排除输出
            if (secondOption == 0 && !option0_list.contains(objects[i].label)
                    || (secondOption == 1 && !option1_list.contains(objects[i].label))
                    || (secondOption == 2 && !option2_list.contains(objects[i].label)))
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
        if (map.size() == 0){
            inform = new StringBuilder("未检测到异常");
        }
        else {
            Set<String> keySet = map.keySet();
            for (String key : keySet) {
                //由键找值
                Integer value = map.get(key);
                //输出键和值
                System.out.print(key + " " + value + ", ");
                key = labelMap.get(key);
                // inform.append("检测到").append(key).append(value).append("处，应按例处罚\n");
                if (secondOption == 0) {
                    if (key == "有人跌倒") {
                        inform.append("检测到").append(key).append(value).append("处\n序号\t类别\t实施对象\t实施依据\n" +
                                "2\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "3\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "4\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "5\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "6\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "7\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "8\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "9\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "10\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "11\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "12\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\n" +
                                "13\t基本要求\t施工单位\n" +
                                "14\t基本要求\t施工单位\n" +
                                "15\t基本要求\t施工单位\n" +
                                "16\t基本要求\t施工单位\n" +
                                "17\t基本要求\t施工单位\n" +
                                "18\t基本要求\t施工单位\n" +
                                "19\t基本要求\t施工单位\n" +
                                "20\t基本要求\t施工单位\n" +
                                "21\t基本要求\t施工单位\n" +
                                "22\t基本要求\t施工单位\n" +
                                "23\t基本要求\t施工单位\n" +
                                "24\t基本要求\t施工单位\n" +
                                "25\t基本要求\t施工单位\n" +
                                "26\t基本要求\t施工单位\n" +
                                "27\t基本要求\t施工单位\n" +
                                "28\t基本要求\t建设、勘察、设计、施工、监测、监理单位\n" +
                                "29\t基本要求\t建设、勘察、设计、施工、监测、监理单位\n" +
                                "30\t基本要求\t建设、勘察、设计、施工、监理单位\n" +
                                "31\t基本要求\t建设、勘察、设计、施工、监理单位\n" +
                                "32\t基本要求\t建设、勘察、设计、施工、监理单位\n\n");
                    } else if (key == "围挡区域闯入") {
                        inform.append("检测到").append(key).append(value).append("处\n序号\t类别\t实施对象\t实施依据\n" +
                                "946\t起重机械——一般规定\t施工单位、监理单位\t《建筑机械使用安全技术规程》（JGJ33-2012）\n" +
                                "1744\t模板支撑体系\t施工单位、监理单位\t《建筑施工碗扣式钢管脚手架安全技术规范》 (JGJ166-2016)\n" +
                                "2141\t其他\t施工单位、监理单位\t《装配式混凝土建筑技术标准》(GB／T 51231-2016)\n" +
                                "2153\t其他\t施工单位、监理单位\t《建筑拆除工程安全技术规范》(JGJ147-2016)\n" +
                                "2427\t模板支撑体系资料\t施工单位、监理单位\t《建筑施工碗扣式钢管脚手架安全技术规范》(JGJ166-2016) \n" +
                                "2490\t临时用电资料\t施工单位、监理单位\t《建筑施工安全检查标准》（JGJ 59-2011） \n"
                        );
                    } else if (key == "办公人员脱岗") {
                        inform.append("检测到").append(key).append(value).append("处\n序号\t类别\t实施对象\t实施依据\n" +
                                "12\t基本要求\t建设、勘察、设计、施工、监理、检测、监测单位\t《危险性较大的分部分项工程安全管理规定》（住房城乡建设部令第37号）\n" +
                                "57\t安全行为要求\t施工单位\t《建筑施工企业主要负责人、项目负责人和专职安全生产管理人员安全生产管理规定》（住房城乡建设部令第17号）\n" +
                                "90\t安全行为要求\t施工单位\t《建筑施工企业负责人及项目负责人施工现场带班暂行办法》（建质〔2011〕111号）\n" +
                                "91\t安全行为要求\t施工单位\t《建筑施工企业负责人及项目负责人施工现场带班暂行办法》（建质〔2011〕111号）\n" +
                                "103\t安全行为要求\t施工单位\t《江苏省房屋建筑和市政基础设施工程危险性较大的分部分项工程实施细则》（苏建质安（2019）379号文）\n");
                    } else if (key == "未佩戴安全帽" || key == "未穿戴反光衣") {
                        inform.append("检测到").append(key).append(value).append("处\n序号\t类别\t实施对象\t实施依据\n" +
                                "1341\t起重吊装\t施工单位、监理单位\t《建筑施工起重吊装安全技术规范》(JGJ276-2012) \n" +
                                "1629\t模板支撑体系\t施工单位、监理单位\t《建筑施工碗扣式钢管脚手架安全技术规范》 (JGJ166-2016)\n" +
                                "1941\t安全防护\t施工单位、监理单位\t《城市轨道交通工程质量安全检查指南》（建质〔2016〕173 号）\n" +
                                "1957\t其他\t施工单位、监理单位\t《金属与石材幕墙工程技术规范》 (JGJ133-2001) \n" +
                                "2492\t其他\t施工单位、监理单位\t《人造板材幕墙工程技术规范》(JGJ336-2016) \n");
                    } else if (key == "围挡" || key == "反光锥") {
                        inform.append("检测到").append(key).append(value).append("处\n序号\t类别\t实施对象\t实施依据\n" +
                                "84\t安全行为要求\t施工单位\t《建设工程安全生产管理条例》 \n" +
                                "194\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "195\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "196\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "197\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "198\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "199\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "200\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "201\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "202\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "203\t基坑工程\t施工单位、监理单位\t《建筑施工土石方工程安全技术规范》（JGJ180-2009）\n" +
                                "204\t基坑工程\t施工单位、监理单位\t《关于印发起重机械、基坑工程等五项危险性较大的分部分项工程施工安全要点的通知》（建安办函[2017]12号）\n" +
                                "1245\t施工升降机\t施工单位、监理单位\t《施工现场机械设备检查技术规范》（JGJ160-2016） \n");
                    } else if (Objects.equals(key, "烟雾") || Objects.equals(key, "火焰")) {
                        inform.append("检测到").append(key).append(value).append("处\n序号\t类别\t实施对象\t实施依据\n" +
                                "1356\t起重吊装\t施工单位、监理单位\t《建筑施工起重吊装安全技术规范》(JGJ276-2012) \n" +
                                "1840\t临时消防\t施工单位、监理单位\t《建设工程施工现场消防安全技术规范》 (GB50720-2011)\n" +
                                "1843\t临时消防\t施工单位、监理单位\t《建设工程施工现场消防安全技术规范》 (GB50720-2011)\n" +
                                "1845\t临时消防\t施工单位、监理单位\t《建设工程施工现场消防安全技术规范》 (GB50720-2011)\n" +
                                "1876\t安全防护\t施工单位、监理单位\t《建筑施工高处作业安全技术规范》 (JGJ 80-2016)\n" +
                                "1899\t安全防护\t施工单位、监理单位\t《缺氧危险作业安全规程 》(GB8958-2006)\n" +
                                "1954\t其他\t施工单位、监理单位\t《玻璃幕墙工程技术规范》 (JGJ102-2003) \n" +
                                "1959\t其他\t施工单位、监理单位\t《金属与石材幕墙工程技术规范》 (JGJ133-2001) \n" +
                                "1982\t其他\t施工单位、监理单位\t《人造板材幕墙工程技术规范》(JGJ336-2016) \n" +
                                "2030\t其他\t施工单位、监理单位\t《钢结构工程施工规范》(GB50755-2012)\n" +
                                "2031\t其他\t施工单位、监理单位\t《钢结构工程施工规范》(GB50755-2012)\n" +
                                "2032\t其他\t施工单位、监理单位\t《钢结构工程施工规范》(GB50755-2012)\n" +
                                "2033\t其他\t施工单位、监理单位\t《钢结构工程施工规范》(GB50755-2012)\n" +
                                "2161\t其他\t施工单位、监理单位\t《建筑拆除工程安全技术规范》(JGJ147-2016)\n" +
                                "2511\t安全防护资料\t施工单位、监理单位\t《缺氧危险作业安全 规 程 》（GB8958-2006）\n");
                    } else if (key == "水坑") {
                        inform.append("检测到").append(key).append(value).append("处\n序号\t类别\t实施对象\t实施依据\n" +
                                "158\t安全行为要求\t监测单位\t《危险性较大的分部分项工程安全管理规定》（住房城乡建设部令第37号） \n" +
                                "166\t基坑工程\t施工单位、监理单位\t《关于印发起重机械、基坑工程等五项危险性较大的分部分项工程施工安全要点的通知》（建安办函[2017]12号）\n" +
                                "167\t基坑工程\t施工单位、监理单位\t《关于印发起重机械、基坑工程等五项危险性较大的分部分项工程施工安全要点的通知》（建安办函[2017]12号）\n" +
                                "175\t基坑工程\t施工单位、监理单位\t《建筑深基坑工程施工安全技术规范》（JGJ311-2013）\n" +
                                "354\t脚手架工程——一般规定\t施工单位、监理单位\t《建筑施工门式钢管脚手架安全技术标准》 (JGJ128-2019)\n" +
                                "1544\t模板支撑体系\t施工单位、监理单位\t《建筑施工碗扣式钢管脚手架安全技术规范》（JGJ166-2016）\n" +
                                "2304\t脚手架工程资料\t施工单位、监理单位\t《建筑施工扣件式钢管脚手架安全技术规范》（JGJ130-2011） \n" +
                                "2453\t模板支撑体系资料\t施工单位、监理单位\t《建筑施工脚手架安 全技术统一标准》 (GB51210-2016）\n" +
                                "2455\t模板支撑体系资料\t施工单位、监理单位\t《建筑施工扣件式钢管脚手架安全技术规范》（JGJ130-2011） \n" +
                                "2458\t模板支撑体系资料\t施工单位、监理单位\t《建筑施工门式钢管脚手架安全技术规范》（JGJ128-2010） \n");
                    }
                } else if (secondOption == 1) {
                    if (key == "未回填石块") {
                        inform.append("检测到").append(key).append(value).append("处\n序号\t类别\t实施对象\t实施依据\n" +
                                "160\t安全行为要求\t监测单位\t《危险性较大的分部分项工程安全管理规定》（住房城乡建设部令第37号） \n" +
                                "161\t安全行为要求\t监测单位\t《危险性较大的分部分项工程安全管理规定》（住房城乡建设部令第37号） \n" +
                                "213\t基坑工程\t施工单位、监理单位\t《关于印发起重机械、基坑工程等五项危险性较大的分部分项工程施工安全要点的通知》（建安办函[2017]12号）\n" +
                                "354\t脚手架工程——一般规定\t施工单位、监理单位\t《建筑施工门式钢管脚手架安全技术标准》 (JGJ128-2019)\n" +
                                "1431\t起重吊装\t施工单位、监理单位\t《建筑施工起重吊装安全技术规范》(JGJ276-2012) \n" +
                                "1442\t起重吊装\t施工单位、监理单位\t《建筑施工起重吊装安全技术规范》(JGJ276-2012) \n" +
                                "2250\t基坑工程资料\t施工单位、监理单位\t《建筑深基坑工程施 工安全技术规范》（JGJ311-2013） \n");
                    }
                } else if (secondOption == 2) {
                        inform.append("检测到").append(key).append(value).append("处\n序号\t类别\t实施对象\t实施依据\n" +
                                "1730\t施工、监理单位\t管道安装符合设计和规范要求。\t3.3.15 管道接口应符合下列规定：\n" +
                                "1731\t施工、监理单位\t管道安装符合设计和规范要求。\t(1)管道采用粘接接口，管端插入承口的深度不得小于规定要求；\n" +
                                "1732\t施工、监理单位\t管道安装符合设计和规范要求。\t(2)熔接连接管道的结合面应有均匀的熔接接口，不得出现局部熔瘤或熔接圈凸凹不匀现象；\n" +
                                "1733\t施工、监理单位\t管道安装符合设计和规范要求。\t(3)采用橡胶圈接口的管道，允许沿曲线敷设，每个接口的最大偏转角不得超过2°；\n" +
                                "1734\t施工、监理单位\t管道安装符合设计和规范要求。\t(4)法兰连接时衬垫不得凸人管内，其外边缘接近螺栓孔为宜。不得安放双垫或偏垫；\n" +
                                "1735\t施工、监理单位\t管道安装符合设计和规范要求。\t(5)连接法兰的螺栓，直径和长度应符合标准，拧紧后，突出螺母的长度不应大于螺杆直径的1/2；\n" +
                                "1736\t施工、监理单位\t管道安装符合设计和规范要求。\t(6)螺纹连接管道安装后的管螺纹根部应有2～3扣的外露螺纹，多余的麻丝应清理干净并做防腐处理；\n" +
                                "1737\t施工、监理单位\t管道安装符合设计和规范要求。\t(7)承插口采用水泥捻口时，油麻必须清洁、填塞密实,水泥捻入并密实饱满,其接口面凹入承口边缘的深度不得大于2mm\n" +
                                "1738\t施工、监理单位\t管道安装符合设计和规范要求。\t(8)卡箍(套)式连接两管口端应平整\\无缝隙,沟槽应均匀，卡紧螺栓后管道应平直，卡箍(套)安装方向应一致。\n" +
                                "1739\t施工、监理单位\t管道安装符合设计和规范要求。\t4.1.2 给水管道必须采用与管材相适应的管件。生活给水系统所涉及的材料必须达到饮用水卫生标准、\n" +
                                "1740\t施工、监理单位\t管道安装符合设计和规范要求。\t4.1.3 管径小于或等于100mm 的镀锌钢管应采用螺纹连接，套丝扣时破坏的镀锌层表面及外露螺纹部分应做防腐处理；管径大于100mm 的镀锌钢管应采用法兰或卡套式专用管件连接，镀锌钢管与法兰的焊接处应二次镀锌。\n" +
                                "1741\t施工、监理单位\t管道安装符合设计和规范要求。\t4.2.10 安装螺翼式水表，表前与阀门应有不小于8倍水表接口直径的直线管段。表外壳距墙表面净距为10~30mm。\n" +
                                "1742\t施工、监理单位\t管道安装符合设计和规范要求。\t4.9.14 屋面排水系统应设置雨水斗。不同设计排水流态、排水特征的屋面雨水排水系统应选用相应的雨水斗。\n" +
                                "1743\t施工、监理单位\t管道安装符合设计和规范要求。\t5.2.6 第2款：在连接2个及2个以上大便器或3个及3个以上卫生器具的污水横管上应设置清扫口。当污水管在楼板下悬吊敷设时，可将清扫口设在上一层楼地面上，污水管起点的清扫口与管道相垂直的墙面距离不得小于200mm；若污水管起点设置堵头代替清扫口时，与墙面距离不得小于400mm。\n" +
                                "1744\t施工、监理单位\t管道安装符合设计和规范要求。\t5.2.13 通向室外的排水管，穿过墙壁或基础必须下返时，应采用45°三通和45°弯头连接，并应在垂直管段顶部设置清扫口。\n" +
                                "1745\t施工、监理单位\t管道安装符合设计和规范要求。\t5.2.15 用于室内排水的水平管道与水平管道、水平管道与立管的连接，应采用45°三通和45°四通和90°斜三通或90°斜四通。立管与排出管端部的连接，应采用两个45°弯头或曲率半径不小于4倍管径的90°弯头。\n");
                    }
                }
            }

        imageView.setImageBitmap(rgba);
        showCustomDialog(inform.toString());
        // toast.setGravity(Gravity.CENTER,0,0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("resultCode:" + resultCode);
        try {
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case TAKE_PHOTO:
                            bitmap = decodeUri(imageUri);
                            // System.out.println("show photo");
                            yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                            imageView.setImageBitmap(bitmap);
                        break;
                    case SELECT_IMAGE:
                        // System.out.println("show images");
                        if (null != data) {
                            Uri selectedImage = data.getData();
                            bitmap = decodeUri(selectedImage);
                            yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                            imageView.setImageBitmap(bitmap);
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
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
               || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
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
