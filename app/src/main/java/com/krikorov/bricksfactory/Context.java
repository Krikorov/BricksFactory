package com.krikorov.bricksfactory;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Context extends Application {
    public volatile Connection connection = null;
    public volatile State state = null;
    public volatile AppCompatActivity activity;
    public volatile Handler handlerConnection = null;
    private volatile int status = 0; //статус который приходит от машины
    public volatile Vector<String> prg_name = new Vector<String>();
    public volatile Vector<Integer> prg_id = new Vector<Integer>();
    public volatile int prg_index = 0;

    final int COMMAND_AFTER_REBOOT = 0;
    final int COMMAND_CONNECT = 1;
    final int COMMAND_START = 2;
    final int COMMAND_PAUSE = 3;
    final int COMMAND_STOP = 4;
    final int COMMAND_CONTINUE = 5;
    final int COMMAND_GET_LIST_PRG = 6;
    final int COMMAND_DISCONNECT = 7;



    @Override
    public void onCreate() {
        super.onCreate();
        state = new Ready();
        StrictMode.enableDefaults();
    }
    public void connect(String HOST_, int PORT_){
        connection = new Connection(HOST_, PORT_);
        connection.start();
    }

    public void start(){
        state.start();
    }
    public void pause(){
        state.pause();
    }
    public void stop(){
        state.stop();
    }

    public void disconnect(){
        state.disconnect();
    }
    public void setState (State state_){
        state = state_;
    }

    public State getState(){
        return this.state;
    }

    public void onTextMessage (String message_){
        Log.d("Socket", message_);
        //обрабатывем входящее сообщение
        try{
            JSONObject jsonAnswer = new JSONObject(message_);
            //пришел статус машины
            if(jsonAnswer.has("status")){
                status = jsonAnswer.getInt("status");
                //если поле программы не пустое, то запоминаем ее ID
                if(jsonAnswer.getString("prg_id") != "")
                {
                    prg_index = prg_id.indexOf(Integer.parseInt(jsonAnswer.getString("prg_id")));
                }
            }
            //пришел список программ
            if(jsonAnswer.has("program")){
                JSONArray jsonArray = jsonAnswer.getJSONArray("program");
                prg_id.clear();
                prg_name.clear();
                for(int i = 0; i < jsonArray.length(); i++){
                    JSONObject c = jsonArray.getJSONObject(i);
                    prg_name.add(c.getString("name"));
                    prg_id.add(c.getInt("ID"));
                }
            }
        }
        catch(JSONException e){

        }
        //машина остановлена
        if (status == 0){
            state = new Stopped();
            ((AutoModeActivity)this.activity).btnStart.setEnabled(true);
            ((AutoModeActivity)this.activity).btnPause.setEnabled(false);
            ((AutoModeActivity)this.activity).btnStop.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setEnabled(true);
            ((AutoModeActivity)activity).spinner.setClickable(true);

        }
        //пауза
        else if (status == 1){
            state = new Paused();
            ((AutoModeActivity)activity).btnStart.setEnabled(true);
            ((AutoModeActivity)activity).btnPause.setEnabled(false);
            ((AutoModeActivity)activity).btnStop.setEnabled(true);
            ((AutoModeActivity)activity).spinner.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setClickable(false);
            ((AutoModeActivity)activity).spinner.setSelection(prg_index);
            ((AutoModeActivity)activity).adapter.notifyDataSetChanged();
        }
        //старт
        else if (status == 2){
            state = new Started();
            ((AutoModeActivity)activity).btnStart.setEnabled(false);
            ((AutoModeActivity)activity).btnPause.setEnabled(true);
            ((AutoModeActivity)activity).btnStop.setEnabled(true);
            ((AutoModeActivity)activity).spinner.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setClickable(false);
            ((AutoModeActivity)activity).spinner.setSelection(prg_index);
            ((AutoModeActivity)activity).adapter.notifyDataSetChanged();
        }


    }

    public void onConnected() throws InterruptedException {
        Log.d("Socket", "connecting");
        state = new Stopped();
        handlerConnection.sendEmptyMessage(COMMAND_GET_LIST_PRG);
        AppCompatActivity oldActivity = activity;
        ((MainActivity)activity).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((MainActivity)activity).changeActivity();
            }
        });
    }

    public void onStateChanged(String s){
        Log.d("Socket", s);
        if (s == "CLOSED"){
            ((MainActivity)this.activity).progressBar.setVisibility(View.INVISIBLE);
        }
    }
    //public void
//**********************классы*****************************************************************

    public class Connection extends Thread{
        private WebSocket ws = null;
        private Context context = null;
        private String HOST = "";
        private int PORT;
        private boolean isRun = false;
        private Handler handler = null;

        Connection (String HOST_, int PORT_){
            HOST = HOST_;
            PORT = PORT_;
            context = (Context) getApplicationContext();
        }
        @Override
        public void run() {
            Log.d("Looper","start");
            Looper.prepare();
            connect();
            handler =new Handler(Looper.getMainLooper()) {
                public void handleMessage (android.os.Message msg){
                    Log.d("Looper", msg.toString());
                    switch (msg.what) {
                        case COMMAND_AFTER_REBOOT:
                            Log.d("Looper", "reboot");
                            break;
                        case COMMAND_CONNECT:
                            //connect();
                            break;
                        case COMMAND_CONTINUE:
                            ws.sendText("continue_prg");
                            break;
                        case COMMAND_GET_LIST_PRG:
                            ws.sendText("get_list_prg");
                            break;
                        case COMMAND_PAUSE:
                            ws.sendText("pause_prg");
                            break;
                        case COMMAND_STOP:
                            ws.sendText("stop_prg");
                            break;
                        case COMMAND_START:
                            ws.sendText("start_prg" + context.prg_id.get(context.prg_index));
                            break;
                        case COMMAND_DISCONNECT:
                            disconnect();
                            break;
                    }
                }
            };
            context.handlerConnection = handler;
            handler.postDelayed(connectionError, 1000);
            Looper.loop();
        }

        private void connect() {
            Log.d("Socket", "Start thread");
            try {
                ws = new WebSocketFactory()
                        .setConnectionTimeout(5000)
                        .createSocket(
                                "ws://" + HOST + ":" + PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                    context.onConnected();
                }
                @Override
                public void onTextMessage(WebSocket websocket, String message_) throws Exception {
                    //получили текст
                    context.onTextMessage(message_);
                }

                @Override
                public void onStateChanged (WebSocket websocket, WebSocketState newState) throws Exception{
                    context.onStateChanged( newState.toString());
                }
            });

            Log.d("Socket", "connecting to: ws://" + HOST + ":" + PORT);
            try {
                ws.connect();
            } catch (WebSocketException e) {
                e.printStackTrace();
            }

            Log.d("Socket", "connection OK");

        }
        private void disconnect(){
            Log.d("Socket", "disconnect");
            ws.disconnect();
            if (ws.isOpen() != true){
                ws = null;
            }
            handler.post(new QuitLooper());
            context.connection = null;
        }

        final Runnable connectionError = new Runnable() {
            public void run() {
                //вызывается через 3 сек. после старта попытки подключения
                if(context.activity instanceof MainActivity) {
                    ((MainActivity)context.activity).progressBar.setVisibility(View.INVISIBLE);
                    ((MainActivity)context.activity).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(((MainActivity)context.activity),
                                    R.string.connectionErr, Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });

                }
            }
        };

        class QuitLooper implements Runnable
        {
            @Override
            public void run()
            {
                Looper.myLooper().quit();
            }
        }
    }

    public abstract class State {
        protected Context context = (Context) getApplicationContext();;
        public void setContext(Context context_){
            context = context_;
        }

        public void disconnect(){

        }
        public void connect(){

        }
        public void start(){

        }
        public void stop(){

        }
        public void pause() {

        }
    }

    public class Ready extends State {

        public void disconnect(){

        }
        public void connect(){

        }
        public void start(){

        }
        public void stop(){

        }
        public void pause() {

        }
    }

    public class Started extends State {
        public void disconnect(){
            //переход из статуса Connection в статус Ready
            connection.disconnect();
            context.state = new Ready();
        }
        public void connect(){

        }
        public void start(){

        }
        public void stop(){
            ((AutoModeActivity)activity).btnStart.setEnabled(false);
            ((AutoModeActivity)activity).btnPause.setEnabled(false);
            ((AutoModeActivity)activity).btnStop.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setClickable(false);
            handlerConnection.sendEmptyMessage(COMMAND_STOP);
            state = new Stopped();
        }
        public void pause() {
            ((AutoModeActivity)activity).btnStart.setEnabled(false);
            ((AutoModeActivity)activity).btnPause.setEnabled(false);
            ((AutoModeActivity)activity).btnStop.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setClickable(false);
            handlerConnection.sendEmptyMessage(COMMAND_PAUSE);
            state = new Paused();
        }
    }

    public class Stopped extends State {
        public void disconnect(){
            connection.disconnect();
            context.state = new Ready();
        }
        public void connect(){

        }
        public void start(){
            ((AutoModeActivity)activity).btnStart.setEnabled(false);
            ((AutoModeActivity)activity).btnPause.setEnabled(false);
            ((AutoModeActivity)activity).btnStop.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setClickable(false);
            handlerConnection.sendEmptyMessage(COMMAND_START);
            state = new Started();
        }
        public void stop(){

        }
        public void pause() {

        }
    }

    public class Paused extends State {
        public void disconnect(){
            connection.disconnect();
            context.state = new Ready();
        }
        public void connect(){

        }
        public void start(){
            ((AutoModeActivity)activity).btnStart.setEnabled(false);
            ((AutoModeActivity)activity).btnPause.setEnabled(false);
            ((AutoModeActivity)activity).btnStop.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setClickable(false);
            handlerConnection.sendEmptyMessage(COMMAND_CONTINUE);
            state = new Started();
        }
        public void stop(){
            ((AutoModeActivity)activity).btnStart.setEnabled(false);
            ((AutoModeActivity)activity).btnPause.setEnabled(false);
            ((AutoModeActivity)activity).btnStop.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setEnabled(false);
            ((AutoModeActivity)activity).spinner.setClickable(false);
            handlerConnection.sendEmptyMessage(COMMAND_STOP);
            state = new Stopped();
        }
        public void pause() {

        }
    }
}
