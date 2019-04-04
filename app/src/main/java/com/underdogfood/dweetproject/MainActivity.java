package com.underdogfood.dweetproject;

import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.io.IOException;

/**
 * @author Mario Tommadich
 */

public class MainActivity extends AppCompatActivity {

    Handler h = new Handler();
    Runnable runnable;
    private EditText txtFrequency;
    private Switch lightSwitch;
    private Button btnDweet;
    private Button btnReset;
    private TextView txtCreated;
    private TextView txtTemperature;
    private TextView txtHumidity;
    private TextView txtBrightness;
    private TextView txtSound;
    private int LED;
    private boolean LEDinitial = true;
    private String thingReceiver = "underdogfood_android";
    private int updateInterval = 4;
    int delay = updateInterval * 1000; //creating an updatable delay

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
        lightSwitch = findViewById(R.id.lightSwitch);
        txtTemperature = findViewById(R.id.txtTemp);
        txtHumidity = findViewById(R.id.txtHumidity);
        txtBrightness = findViewById(R.id.txtBrightness);
        txtSound = findViewById(R.id.txtSound);
        btnDweet = findViewById(R.id.btnDweet);
        btnReset = findViewById(R.id.btnReset);
        txtCreated = findViewById(R.id.textCreated);

        lightSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lightSwitch.toggle();
            }
        });

        btnDweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String frequency = txtFrequency.getText().toString();

                //only send dweet if dweet text not empty
                if (!(frequency.equals(""))) {
                    //call the sendDweet method and pass in frequency and receiver name:

                    try {
                        sendDweet(thingReceiver, Integer.parseInt(txtFrequency.getText().toString()), false);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this,
                                "Dweet failed", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this,
                            "Frequency cannot be empty", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    sendDweet(thingReceiver, 4, true);
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this,
                            "Dweet failed", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            }
        });

    }

    @Override
    protected void onResume() {

        //start handler as activity becomes visible
        //This handler will run in the background and get the latest dweet
        //every time the delay has passed the delay is defaulted to 4 seconds
        //and changed to the requested frequency if the frequency gets changed.
        //This way I'm only polling for updates as often as needed.
        h.postDelayed(runnable = new Runnable() {
            public void run() {

                try {
                    getLatestDweet("underdogfood_rasp");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NumberFormatException ne){
                    txtTemperature.setText("Sensor fault");
                    txtHumidity.setText("Sensor fault");
                    txtBrightness.setText("Sensor fault");
                    txtSound.setText("Sensor fault");
                }

                h.postDelayed(runnable, delay);
            }
        }, delay);

        super.onResume();
    }

    @Override
    protected void onPause() {
        //The handler is stopped when the app is paused no need to keep polling data if
        //the user doesn't look at the app
        h.removeCallbacks(runnable);
        super.onPause();
    }

    public void sendDweet(String thing, int frequency, Boolean reset) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("interval", frequency);
        //we set the number of seconds at which we poll for updates
        // according to the desired frequency to reduce the strain on the API
        updateInterval = frequency;

        // We add the lightswitch settings to the dweet.
        // Similar to this we could remotely switch on or off any of the available sensors.
        // This value is being read by the RaspberryPi on the other end and used to switch
        // the led component on or off.
        if (lightSwitch.isChecked()) {
            json.addProperty("light", "1");
        } else {
            json.addProperty("light", "0");
        }
        //In case we are performing a reset
        if (reset){
            json.remove("light");
            json.addProperty("light", "0");
            lightSwitch.setChecked(false);
            txtFrequency.setText("4");
        }

        DweetIO.publish(thing, json);
        Toast.makeText(MainActivity.this,
                "Dweet sent successfully.", Toast.LENGTH_SHORT).show();
    }

    public void getLatestDweet(String thing) throws IOException, NumberFormatException {
        Dweet dweet = DweetIO.getLatestDweet(thing);

        if (!(dweet == null)) {
            //if we have a valid dweet, we read it's contents


                int temperature = dweet.getContent().get("Temperature").getAsInt();
                int humidity = dweet.getContent().get("Humidity").getAsInt();
                int brightness = dweet.getContent().get("Brightness").getAsInt();
                int sound = dweet.getContent().get("Ambient").getAsInt();
                String creationDate = dweet.getContent().get("PiTime").getAsString();
                LED = dweet.getContent().get("LED").getAsInt();

            //the value of LED corresponds to the light on the raspberryPi
            //we use it to set the display state of the lightSwitch toggle button
            //If the physical on off state of the LED doesn't correspond to the
            //display of the switch, we toggle the display of the switch such that
            //it corresponds to the actual state of the LED
            if (LEDinitial && ((lightSwitch.isChecked() && LED == 0) || (!lightSwitch.isChecked() && LED == 1))) {
                lightSwitch.toggle();
                LEDinitial = false;
            }

            txtCreated.setText("Last valid reading: " + creationDate.substring(0, creationDate.length() - 7));
            txtTemperature.setText(temperature + "ºC");
            txtHumidity.setText(humidity + "%");
            txtBrightness.setText(brightness + " Lumens");
            txtSound.setText(sound + "/1023");

        }
    }
}
