package com.krikorov.bricksfactory;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;


import androidx.appcompat.app.AppCompatActivity;

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
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View itemSelected, int selectedItemPosition, long selectedId) {
                context.prg_index = selectedItemPosition;
                Log.d("AutoModeActivity",context.prg_name.toString());
                Log.d("AutoModeActivity", String.valueOf(selectedItemPosition));

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }

        });
        btnBack.setOnClickListener(view -> {
            context.disconnect();
            Intent intent = new Intent(AutoModeActivity.this, MainActivity.class);
            startActivity(intent); finish();
        });

        btnStart.setOnClickListener(view -> context.start());

        btnPause.setOnClickListener(view -> context.pause());

        btnStop.setOnClickListener(view -> context.stop());
    }
    @Override
    public void onBackPressed(){
        context.state.disconnect();
        Intent intent = new Intent(AutoModeActivity.this, MainActivity.class);
        startActivity(intent); finish();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (context != null){
            context.updateState();
        }
        else{
            Intent intent = new Intent(AutoModeActivity.this, MainActivity.class);
            startActivity(intent); finish();
        }

    }

    public void changeVisible(boolean stateStart, boolean statePause, boolean stateStop,
                              boolean stateSpinner, int prg_index){
        btnStart.setEnabled(stateStart);
        btnPause.setEnabled(statePause);
        btnStop.setEnabled(stateStop);
        spinner.setEnabled(stateSpinner);
        spinner.setClickable(stateSpinner);
        if (prg_index != -1){
            spinner.setSelection(prg_index);
        }

        //adapter.notifyDataSetChanged();
    }

}