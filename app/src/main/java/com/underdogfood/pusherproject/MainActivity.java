package com.underdogfood.pusherproject;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toast;

import com.pusher.client.PusherOptions;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.Pusher;
import com.pusher.client.channel.Channel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;



import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

/**
 * @author Mario Tommadich
 */

public class MainActivity extends AppCompatActivity {

    private EditText txtFrequency;
    private EditText txtToDo;
    private TextView txtTemperature;
    private TextView txtHumidity;
    private TextView txtBrightness;
    private TextView txtSound;

    //pusher data members
    private Pusher pusher;
    private Channel channelReadings;
    private PusherOptions options;
    private JsonParser jsonParser = new JsonParser();
    private JsonObject readings;

    //CloudMQTT data members
    String clientID;
    MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Creating a very lax thread policy to allow network access on the main thread
        //This is necessary for android versions >9
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        txtFrequency = findViewById(R.id.txtFrequency);
        txtToDo = findViewById(R.id.txtToDo);
        txtTemperature = findViewById(R.id.txtTemp);
        txtHumidity = findViewById(R.id.txtHumidity);
        txtBrightness = findViewById(R.id.txtBrightness);
        txtSound = findViewById(R.id.txtSound);

        //setting up pusher related options required to subscribe to
        //pusher's events
        options = new PusherOptions();
        options.setCluster("eu");
        pusher = new Pusher("bf91412880aadf37de06", options);
        channelReadings = pusher.subscribe("my-channel");
        //Setting up a listener for the readings-events which are the raspberrypi
        //sensor readings
        channelReadings.bind("readings-event", new SubscriptionEventListener() {
            @Override
            public void onEvent(String channelName, String eventName, final String data) {

                updateReadings(data);
            }
        });

        //Lastly start the connection to cloudMQTT, which I'm using to send instructons
        //to my raspberrypi
        clientConnect();
    }



    @Override
    protected void onResume() {
        super.onResume();
        pusher.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pusher.disconnect();
    }

    @Override
    protected  void onDestroy() {
        super.onDestroy();
        clientDisconnect();
    }

    public void updateReadings(String data){
        JsonObject readingsObject = jsonParser.parse(data).getAsJsonObject() ;
        JsonObject innerReadingsObject = readingsObject.get("message").getAsJsonObject();
        txtTemperature.setText(innerReadingsObject.get("Temperature").getAsString());
        txtHumidity.setText(innerReadingsObject.get("Humidity").getAsString());
        txtBrightness.setText(innerReadingsObject.get("Brightness").getAsString());
        txtSound.setText(innerReadingsObject.get("Ambient").getAsString());

    }

    public void clientConnect(){
        clientID = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://m24.cloudmqtt.com:15146", clientID);
        MqttConnectOptions options = new MqttConnectOptions();

        options.setUserName("cgucubwb");
        options.setPassword("JKjWyqVYzJ_2".toCharArray());

        try {
            final IMqttToken token = client.connect(options);

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Toast.makeText(MainActivity.this,"connected successfully",Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Toast.makeText(MainActivity.this,"failed to connect",Toast.LENGTH_LONG).show();

                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void clientDisconnect(){
        try {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this,"disconnected successfully",Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Toast.makeText(MainActivity.this,"failed to disconnect",Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topicName, String payload){

        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topicName, message);
            Toast.makeText(MainActivity.this,"message published successfully",Toast.LENGTH_LONG).show();

        } catch (UnsupportedEncodingException | MqttException e) {
            Toast.makeText(MainActivity.this,"message DIDN'T publish successfully",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    //defining my button actions
    public void disableTemperatureSensor(View view) {
        publish("Temperature", "toggle");
    }

    public void disableHumiditySensor(View view)  {
        publish("Humidity", "toggle");
    }

    public void disableLightSensor(View view)  {
        publish("Brightness", "toggle");
    }

    public void disableSoundSensor(View view)  {
        publish("Ambient", "toggle");
    }

    public void toDoListAdd(View view)  {
        String payload = txtToDo.getText().toString();
        if(payload.length()>0) {
            publish("ToDo", payload);
        }
    }

    public void changeFrequency(View view)  {
        String payload = txtFrequency.getText().toString();
        if(payload.length()>0) {
            publish("Frequency", payload);
        }
    }

}
