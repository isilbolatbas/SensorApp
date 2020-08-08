package com.example.sensorapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.List;

import com.ubidots.*;

public class MainActivity extends AppCompatActivity implements LocationListener {

    Button butonSensor;
    TextView saglik;
    TextView konum;
    LocationManager locationManager;
    List<String> loglist = new ArrayList<String>();



    private static final String brokerUrl = "tcp://industrial.api.ubidots.com:1883";
    private final String topic     = "/v1.6/devices/demo";
    private final String username = "";
    private final String password = "";
    private final String deviceLabel = "demo";
    private final String variableLabel = "new-variable";
    String testValue     = "11";
    String payload       = "{\"" + variableLabel + "\": " + testValue + "}";

    int qos=0;

    double normalBolge = 40.002300;
    double tehlikeliBolge = 40.002200;
    double ikazBolge = 40.002100;


    MqttAndroidClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        butonSensor = findViewById(R.id.buton1);
        konum = findViewById(R.id.textView1);
        saglik = findViewById(R.id.textView2);

        konum.setTextColor(Color.parseColor("#164897"));





        butonSensor.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String clientId = MqttClient.generateClientId();
                client = new MqttAndroidClient(MainActivity.this, brokerUrl, clientId);
                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Toast.makeText(MainActivity.this, "connection lost", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                      //  String incomingData = new String(message.getPayload());

                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                      //  Toast.makeText(MainActivity.this, "send data", Toast.LENGTH_SHORT).show();
                    }
                });

                connectmqtt();
                getLocation();

            }
        });

    }




    void connectmqtt(){


        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        try {

            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(getApplicationContext(),"connected",Toast.LENGTH_SHORT).show();
                    try {

                        client.subscribe("/v1.6/devices/demo/new-variable", 0);
                        System.out.println("veri gitti");

                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(getApplicationContext(),"unconnected",Toast.LENGTH_SHORT).show();
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }



    void getLocation() {
     try {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 1, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3600, 3600, this);


        } catch (SecurityException e) {

            e.printStackTrace();
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        String lng = new DecimalFormat("##.######").format(location.getLongitude());
        String lat = new DecimalFormat("##.######").format(location.getLatitude());

        konum.setText("Mevcut Konumunuz: " + lat + " , " + lng);
        getList(location);
        publish(location);

    }
   public void publish(Location location){
        //Ubidots için topic: /v1.6/devices/{cihaz-ismi}
       String topic = "/v1.6/devices/demo";

       Date date = new Date();
       SimpleDateFormat sdf = new SimpleDateFormat("kk:mm");
       String time = sdf.format(date);
       JSONObject obj = new JSONObject();
       String lng = new DecimalFormat("##.######").format(location.getLongitude());
       String lat = new DecimalFormat("##.######").format(location.getLatitude());

       try {

           obj.put("Latitude", lat);
           obj.put("Longitude", lng);


           int sensorValue = (int) (Math.random()*49+1);

           String variable       = "{\"" + variableLabel + "\": ";
           String context = " {\"value\": "+ sensorValue + ", \"context\":";
           String x = variable + context + obj +"}}";

           System.out.print(x);


           Double lati = Double.parseDouble(lat);

           if(lati <= tehlikeliBolge && lati >= ikazBolge ){
               Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
               // Vibrate for 500 milliseconds
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                   v.vibrate(VibrationEffect.createOneShot(3500, VibrationEffect.DEFAULT_AMPLITUDE));

                   AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle).create();

                   alertDialog.setTitle("TEHLİKE");
                   alertDialog.setMessage("TEHLİKELİ BÖLGEDESİNİZ, UZAKLAŞIN");
                   alertDialog.setIcon(R.drawable.radioactive);
                   alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.RED));

                   alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "ÇIKIŞ",
                           new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int which) {
                                   dialog.dismiss();
                               }
                           });

                   alertDialog.show();
               } else {
                   //deprecated in API 26
                 //  v.vibrate(500);
               }}
               else if(lati >= tehlikeliBolge && lati <= normalBolge){
               AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle).create();

               alertDialog.setTitle("DİKKAT");
               alertDialog.setMessage("DİKKATLİ OLMALISINIZ");
               alertDialog.setIcon(R.drawable.radioactive);
               alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.YELLOW));
               alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "ÇIKIŞ",
                       new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int which) {
                               dialog.dismiss();
                           }
                       });

               alertDialog.show();
               }
               else if(lati >= normalBolge) {
               AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle).create();

               alertDialog.setTitle("NORMAL");
               alertDialog.setMessage("GÜVENLİ BÖLGEDESİNİZ");
               alertDialog.setIcon(R.drawable.radioactive);
               alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GREEN));
               alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "ÇIKIŞ",
                       new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int which) {
                               dialog.dismiss();
                           }
                       });

               alertDialog.show();
           }









           MqttMessage mqttMessage = new MqttMessage();
           mqttMessage.setPayload(x.getBytes());

           client.publish(topic, x.getBytes() , 1 ,false);
       } catch (MqttException | JSONException e) {
           e.printStackTrace();
       }




    }



    void getList(Location location) {

        DecimalFormat dFormat = new DecimalFormat("#.####");
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("kk:mm");
        String time = sdf.format(date);

   //     Toast.makeText(MainActivity.this, loglist.toString(), Toast.LENGTH_LONG).show();
        if (loglist.size() == 6) {// you reach 6 values => send the SMS
            StringBuilder log = new StringBuilder();
            for (int j = 0; j < loglist.size(); j++) {
                log.append(loglist.get(j).toString());
            }


        } else {
            loglist.add(dFormat.format(location.getLatitude()) + ",+" + dFormat.format(location.getLongitude()) + "," + time);
            try {
                JSONArray jArry = new JSONArray();
                for (int i = 0; i < loglist.size(); i++) {
                    JSONObject jObjd = new JSONObject();
                    jObjd.put("Latitude", location.getLatitude());
                    jObjd.put("Longitude", location.getLongitude());
                    jObjd.put("Time", time);
                    jArry.put(jObjd);
                }

                System.out.println(jArry);
            } catch (JSONException ex) {
                ex.getMessage();
            }


        }


    }

    public JSONObject getJson(String line) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("kk:mm");
        String time = sdf.format(date);
        Location location = null;



        String [] toJson = line.split(" ");

        JSONObject obj = new JSONObject();
        try {
            obj.put("Latitude", location.getLatitude());
            obj.put("Longitude", location.getLongitude());
            obj.put("Time", time);
        } catch (JSONException e) {
            e.getMessage();
        }
        return obj;
    }


    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(MainActivity.this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }



}