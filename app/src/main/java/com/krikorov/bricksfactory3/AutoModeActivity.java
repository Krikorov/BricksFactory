package com.krikorov.bricksfactory3;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import java.util.Vector;

public class AutoModeActivity extends AppCompatActivity{
    public Spinner spinner;
    public Button btnBack;
    public ImageButton btnStart;
    public ImageButton btnPause;
    public ImageButton btnStop;
    private Context context = null;
    public ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actyvity_auto_mode);
        spinner = findViewById(R.id.spinner);
        btnBack = findViewById(R.id.btnBack);
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        context = (Context) getApplicationContext();
        context.activity = this;

        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if(context.prg_name.size() == 0){
            context.prg_name.add("Выберите программу");
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter(this,
                        R.layout.spinner_item,
                        context.prg_name
                );

        // Определяем разметку для использования при выборе элемента
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_layout);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,
                                       View itemSelected, int selectedItemPosition, long selectedId) {
                context.prg_index = selectedItemPosition;
                Log.d("AutoModeActivity",context.prg_name.toString());
                Log.d("AutoModeActivity", String.valueOf(selectedItemPosition));

            }
            public void onNothingSelected(AdapterView<?> parent) {
            }

        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.disconnect();
                Intent intent = new Intent(AutoModeActivity.this, MainActivity.class);
                startActivity(intent); finish();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.start();
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.pause();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.stop();
            }
        });
    }
    @Override
    public void onBackPressed(){
        context.state.disconnect();
        Intent intent = new Intent(AutoModeActivity.this, MainActivity.class);
        startActivity(intent); finish();
    }

}