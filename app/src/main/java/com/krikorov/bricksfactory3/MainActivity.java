package com.krikorov.bricksfactory3;

import static androidx.core.content.ContextCompat.startActivity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.krikorov.bricksfactory3.AutoModeActivity;
import com.krikorov.bricksfactory3.Context;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private EditText machine_ip;
    private Button btn_connect;
    private Context context = null;
    private int PORT = 81;
    private String HOST;
    private PrintWriter bufferSender;
    public ProgressBar progressBar;
    private final String fileSettings= "settings";

    final Pattern sPattern = Pattern.compile("^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.)" +
            "{3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])$");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        machine_ip = findViewById(R.id.machine_ip);
        btn_connect = findViewById(R.id.btn_connect);
        progressBar = findViewById(R.id.progressBar);
        context = (Context) getApplicationContext();
        context.activity = this;
        progressBar.setVisibility(View.INVISIBLE);

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    openFileInput(fileSettings)));
            String str = "";
            while ((str = br.readLine()) != null) {
                machine_ip.setText(str);
            }
            br.close();
        } catch (FileNotFoundException e) {
            machine_ip.setText(R.string.start_ip);
            File file = new File(getFilesDir(), fileSettings);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write(getResources().getString(R.string.start_ip));
                bw.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                if (sPattern.matcher(machine_ip.getText()).matches()){
                    context.connect(machine_ip.getText().toString(), PORT);
                    File file = new File(getFilesDir(), fileSettings);
                    try {
                        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                        bw.write(machine_ip.getText().toString());
                        bw.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    progressBar.setVisibility(View.VISIBLE);
                    /*Intent intent = new Intent(MainActivity.this, AutoModeActivity.class);
                    startActivity(intent);*/
                    //activity.finish();
                }
                else{
                    Toast toast = Toast.makeText(getApplicationContext(),
                            getString(R.string.error_ip), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

    public void changeActivity(){
        Intent intent = new Intent(MainActivity.this,
                AutoModeActivity.class);
        startActivity(intent);
        this.finish();
    }

}
