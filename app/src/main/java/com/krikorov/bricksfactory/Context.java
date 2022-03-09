package com.krikorov.bricksfactory;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
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
    public volatile Vector<String> prg_name;
    public volatile Vector<Integer> prg_id;
    public volatile int prg_index = -1;

    final int COMMAND_AFTER_REBOOT = 0;
    final int COMMAND_CONNECT = 1;
    final int COMMAND_START = 2;
    final int COMMAND_PAUSE = 3;
    final int COMMAND_STOP = 4;
    final int COMMAND_CONTINUE = 5;
    final int COMMAND_GET_LIST_PRG = 6;
    final int COMMAND_UPDATE_STATUS = 7;
    final int COMMAND_DISCONNECT = 8;



    @Override
    public void onCreate() {
        super.onCreate();
        state = new Ready();
        prg_name = new Vector<>();
        prg_id = new Vector<>();
        //StrictMode.enableDefaults();
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
    public void updateState(){
        handlerConnection.sendEmptyMessage(COMMAND_UPDATE_STATUS);
    }
    public void disconnect(){
        state.disconnect();
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
                if(!jsonAnswer.getString("prg_id").equals(""))
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
                ((AutoModeActivity)activity).adapter.notifyDataSetChanged();
            }
        }
        catch(JSONException ignored){

        }
        //стоп
        if (status == 0){
            state = new Stopped();
            if(activity instanceof AutoModeActivity){
                activity.runOnUiThread(() -> {
                    ((AutoModeActivity)activity).changeVisible(true, false,
                            false, true, prg_index);
                });
            }

        }
        //пауза
        else if (status == 1){
            state = new Paused();
            if(activity instanceof AutoModeActivity){
                activity.runOnUiThread(() -> {
                    ((AutoModeActivity)activity).changeVisible(true, false,
                            true, false, prg_index);
                });
            }

        }
        //старт
        else if (status == 2){
            state = new Started();
            if(activity instanceof AutoModeActivity){
                activity.runOnUiThread(() -> {
                    ((AutoModeActivity)activity).changeVisible(false, true,
                            true, false, prg_index);
                });
            }
        }


    }

    public void onConnected() {
        Log.d("Socket", "connecting");
        state = new Stopped();
        if(handlerConnection.sendEmptyMessage(COMMAND_GET_LIST_PRG)){
            activity.runOnUiThread(() -> ((MainActivity)activity).changeActivity());
        }

    }

    public void onStateChanged(String s){
        Log.d("Socket", s);
        if (s.equals("CLOSED")){
            ((MainActivity)this.activity).progressBar.setVisibility(View.INVISIBLE);
        }
    }
    //public void
//**********************классы*****************************************************************

    public class Connection extends Thread{
        private WebSocket ws = null;
        private Context context;
        private final String HOST;
        private final int PORT;
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
                        case COMMAND_UPDATE_STATUS:
                            ws.sendText("status");
                            break;
                        case COMMAND_DISCONNECT:
                            disconnect();
                            break;
                    }
                }
            };
            context.handlerConnection = handler;
            handler.postDelayed(connectionError, 3000);
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
                public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
                    context.onConnected();
                }
                @Override
                public void onTextMessage(WebSocket websocket, String message_) {
                    //получили текст
                    context.onTextMessage(message_);
                }

                @Override
                public void onStateChanged (WebSocket websocket, WebSocketState newState){
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
            if (!ws.isOpen()){
                ws = null;
            }
            handler.post(new QuitLooper());
            context.connection = null;
        }

        final Runnable connectionError = () -> {
            //вызывается через 3 сек. после старта попытки подключения
            if(context.activity instanceof MainActivity) {
                ((MainActivity)context.activity).progressBar.setVisibility(View.INVISIBLE);
                context.activity.runOnUiThread(() -> {
                    Toast toast = Toast.makeText(context.activity,
                            R.string.connectionErr, Toast.LENGTH_LONG);
                    toast.show();
                });

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
        protected Context context = (Context) getApplicationContext();

        public void disconnect(){

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

        public void start(){

        }
        public void stop(){
            //((AutoModeActivity)activity).btnStop.setEnabled(false);
            handlerConnection.sendEmptyMessage(COMMAND_STOP);
            state = new Stopped();
        }
        public void pause() {
            //((AutoModeActivity)activity).btnPause.setEnabled(false);
            handlerConnection.sendEmptyMessage(COMMAND_PAUSE);
            state = new Paused();
        }
    }

    public class Stopped extends State {
        public void disconnect(){
            connection.disconnect();
            context.state = new Ready();
        }

        public void start(){
            //((AutoModeActivity)activity).btnStart.setEnabled(false);
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

        public void start(){
            //((AutoModeActivity)activity).btnStart.setEnabled(false);
            handlerConnection.sendEmptyMessage(COMMAND_CONTINUE);
            state = new Started();
        }
        public void stop(){
            //((AutoModeActivity)activity).btnStop.setEnabled(false);
            handlerConnection.sendEmptyMessage(COMMAND_STOP);
            state = new Stopped();
        }
        public void pause() {

        }
    }
}
