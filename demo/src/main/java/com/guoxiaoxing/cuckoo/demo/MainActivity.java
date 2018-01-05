package com.guoxiaoxing.cuckoo.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.guoxiaoxing.cuckoo.Cost;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Cost
    public void show(){
        for(int i = 0; i < 1000; i++){

        }
    }
}
