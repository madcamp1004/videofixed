package com.example.viewpagerdemo;


import android.app.Activity;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;

import java.io.File;

public class ImageFromRemote extends Activity {

    Button backBtn;
    Button saveBtn;

    @Override
    public void onCreate(Bundle savedInstancestate){
        super.onCreate(savedInstancestate);
        setContentView(R.layout.remote_image);

        //get intent data
        Intent i = getIntent();

        ContentResolver cR = getApplicationContext().getContentResolver();

        ImageView imageView = (ImageView) findViewById(R.id.imgPreview);

        final String nameText = i.getExtras().getString("name");
        final String phoneText = i.getExtras().getString("phone");
        final String addrText = i.getExtras().getString("address");
        final String cardInt = i.getExtras().getString("card");

        backBtn = (Button)findViewById(R.id.reselectbtn);
        backBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });

        saveBtn = (Button)findViewById(R.id.savebtn);
        saveBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });

        imageView.setVisibility(View.VISIBLE);

        Glide.with(this).load(CardFragment.images[Integer.valueOf(cardInt)]).into(imageView);
    }
}
