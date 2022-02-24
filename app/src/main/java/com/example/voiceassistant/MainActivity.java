package com.example.voiceassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    //UI Component
    ImageView SettingButton;
    ImageView AddPreference;
    FloatingActionButton button;
    ListView listView;
    TextView spokenText;

    //Recognizer Component
    SpeechRecognizer speechRecognizer;
    TextToSpeech tts;

    //Python Object
    PyObject pyObj;

    //Intents
    Intent speechRecognizerIntent;

    //Constants
    private static final String LOGCAT_TAG = "AAP";
    final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather";
    final String APP_ID = "0c30e69f8971e94f6af2e0d8dfda60d9";
    final int REQUEST_CODE = 123;
    final int NEW_CITY_CODE = 456;
    final long MIN_TIME = 5000;
    final float MIN_DISTANCE = 1000;
    final String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;

    //Array Adapter
    private chatArrayAdapter chatArrayAdapter;

    //Variable
    String messageHolder;

    //Weather Data
    LocationManager mLocationManager;
    LocationListener mLocationListener;

    //SMS Reading Constant
    public static final String INBOX = "content://sms/inbox";
    public static final String SENT = "content://sms/sent";
    public static final String DRAFT = "content://sms/draft";

    //BackgroundTask Variable
    SeprateQuery queryCode = new SeprateQuery();;


    //Dictonary for number
    Dictonaryforname numbersDict = new Dictonaryforname();

    //News API
    final static String NEWS_API = "2194a166c4fe415f83891a8c0f018635";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Activity for this Class
        setContentView(R.layout.activity_main);
        //Permission Asked
        PermissionAsked();
        //Start Python
        PythonStarter();

        //Reference Variable
        ReferenceVariable();

        //Setup Speech Recognizer
        Init_SpeechRecognizer();

        //Setup TextToSpeech
        Init_TTS();

        //TelePhoneManager
        Init_Telephone();

        //LocationManager
        Init_LocationManager();

        //Setup ArrayAdapter
        Init_ArrayAdapter();

        //Mic Button Listner
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSpeechToText(speechRecognizerIntent,true);
                //Greeting();
            }
        });
        //Add Button Listner
        AddPreference.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent AddPreferenceActivity = new Intent(MainActivity.this,NewsSection.class);
                startActivity(AddPreferenceActivity);
            }
        });
        //Setting Button Listner
        SettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent AddPreferenceActivity = new Intent(MainActivity.this,SettingsClass.class);
                startActivity(AddPreferenceActivity);
            }
        });

    }

    private void Greeting(int code) {
        switch (code){
            case 0:
                messageHolder = "Hello! How can i help you,";
                    break;
            case 1:
                messageHolder = "I'm I, A virtual Assistant!";
                break;

        }
        sendChatMessage(false);
        SleepMainThread(3);
    }

    void SleepMainThread(int sec){
        try {
            Thread.sleep(sec*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void SendSms(){

    }

    private void ReadSms(int code, String name){
         Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        cursor.moveToNext();
        if(code==0) {
            if (cursor != null) {

                // 2 for subject person number
                //3 for person number
                //4 for date
                //5 for date sent
                //7 for read status 0 for unread
                //8 for status
                //12 for body
                messageHolder = cursor.getString(2);
                sendChatMessage(false);
                messageHolder = cursor.getString(2);
                sendChatMessage(false);
                messageHolder = cursor.getString(2);
                sendChatMessage(false);
            }
        }else{
            if(numbersDict.returnNumber(name).equals("0")){
                Toast.makeText(this,"No number from "+name,Toast.LENGTH_SHORT).show();
                return;
            }
            if (cursor.moveToFirst()) { // must check the result to prevent exception
                do {
                    String msgData = "";
                    msgData = cursor.getString(2);
                    //Log.e("App",msgData+numbersDict.returnNumber(name));

                    if(msgData.contains(numbersDict.returnNumber(name))){
                        messageHolder = cursor.getString(12);
                        sendChatMessage(false);
                        //Log.e("App",messageHolder);
                        break;
                    }
                    // use msgData
                } while (cursor.moveToNext());
            }
        }
}

    private void ForwardSms(){
        //forward the last read sms to name defined in adding preference

    }



    private void Init_ArrayAdapter() {
        chatArrayAdapter = new chatArrayAdapter(getApplicationContext(),R.layout.right_msg);
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);
        //list code
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount()-1);
            }
        });
    }

    private void Init_LocationManager() {

        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //
                String longitude = String.valueOf(location.getLongitude());
                String latitude = String.valueOf(location.getLatitude());

                //
                // Providing 'lat' and 'lon' (spelling: Not 'long') parameter values
                RequestParams params = new RequestParams();
                params.put("lat", latitude);
                params.put("lon", longitude);
                params.put("appid", APP_ID);
                letsDoSomeNetworking(params);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Log statements to help you debug your app.
            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

    private void Init_Telephone() {
        //Telephone Number
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET,
                    Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.CALL_PHONE},PackageManager.PERMISSION_GRANTED);
        }else{
            String number = tMgr.getLine1Number();
        }
    }

    private void Init_TTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("APP", "Lang not supported");
                    }
                }
            }
        });
    }

    private void Init_SpeechRecognizer() {
        //button and text to speech code here
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

    }

    private void ReferenceVariable() {
        // Buttons Declaration
        button = findViewById(R.id.mic_button);
        listView = findViewById(R.id.msg_list);
        spokenText = findViewById(R.id.spokenText);
        AddPreference = findViewById(R.id.imageView);
        SettingButton = findViewById(R.id.setting);
    }

    private void PythonStarter() {
        //Python Connector
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        ///Python Code
        Python py = Python.getInstance();
        pyObj = py.getModule("main");
    }

    private void PermissionAsked() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_SMS,Manifest.permission.INTERNET,
                            Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.CALL_PHONE,Manifest.permission.ACCESS_COARSE_LOCATION}
                    , PackageManager.PERMISSION_GRANTED);
        }else{

        }
    }

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
    */

    private class BackStuff extends AsyncTask<String,String,String>{


        @Override
        protected String doInBackground(String... strings) {
            Log.d("APP","String in background is "+ strings[0]);
            String query = strings[0];
            query = searchQuery(query,false);
            return query;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            messageHolder = s;
            tts.speak(s,TextToSpeech.QUEUE_ADD,null);
            sendChatMessage(false);
        }
    }

    class query{
        int code;
        String querys;

        public query(int code, String querys) {
            this.code = code;
            this.querys = querys;
        }
    }

    private class SeprateQuery extends AsyncTask<String,String,query>{
        @Override
        protected query doInBackground(String... strings) {
            String v_query = strings[0].toString().toLowerCase(Locale.ROOT);
            Log.e("APP","String recieved inn seprateQuery and is"+v_query);
            if(v_query.indexOf("hello")!=-1||v_query.indexOf("hai")!=-1||v_query.indexOf("hello I")!=-1||v_query.indexOf("hi")!=-1||v_query.indexOf("I")!=-1){
                //Greeting(0);
                return new query(0,"greet");
            }
            if(v_query.indexOf("who are you")!=-1||v_query.indexOf("Tell me About you")!=-1||v_query.indexOf("About you")!=-1){
                //Greeting(1);
                return new query(0,"about");
            }
            if(v_query.indexOf("read sms")!=-1 ){
                if(v_query.contains("from")){

                    v_query = v_query.replace("read sms from ","");
                    Log.e("App","Inside from returning " + v_query);
                    return new query(7,v_query);
                }
                return new query(7,"sms");
            }
            if(v_query.indexOf("wikipedia") != -1){
                Log.e("App","Query Contains wikipedia");
                v_query = v_query.replace("wikipedia ","");
                Log.e("App","Query Contains wikipedia and replaced with space new : "+v_query);
                return new query(1,v_query);
            }else if(v_query.indexOf("translate")!=-1){
                Log.e("App","Query Contains translate");
                return new query(2,v_query);
            }else if(v_query.indexOf("Call")!=-1){
                Log.e("APP","Calling Triggered");
                return new query(3,v_query);
            }else if(v_query.indexOf("create contact")!=-1){
                v_query = v_query.replace("create contact ","");
                return new query(4,v_query);
            }else if(v_query.indexOf("how")!=-1){
                return new query(5,"Weather");
            }
            return new query(-1,"null");
        }

        @Override
        protected void onPostExecute(query code) {
            super.onPostExecute(code);
            if(code.code==0){
                if(code.querys.contains("about")){
                    Greeting(1);
                }else{
                    Greeting(0);
                }
                startSpeechToText(speechRecognizerIntent,true);
            }
            if(code.code==7){
                if(code.querys.contains("sms")){
                    ReadSms(0, code.querys);
                }else{
                    ReadSms(1,code.querys);
                }

            }
            if( code.code == 1){
                Log.e("App","now seraching for code and fetching wikipedia");

                BackStuff back = new BackStuff();
                Log.e("App","Back Started and query is " + code.querys);
                back.execute(code.querys);
            }else if(code.code==2){
                String q = code.querys;
                Log.e("App","Seperated string is"+q);

                //Log.e("App","First Lang is" +firstLang +"Second is" + secondLang);
                messageHolder = "Enter the text to Translate";
                sendChatMessage(false);

                startSpeechToText(speechRecognizerIntent,false);
                PyObject py = pyObj.callAttr("translatorl",messageHolder,q);
                sendChatMessage(false);
                //speechRecognizer.startListening(speechRecognizerIntent,false);
            }else if(code.code==4){
                messageHolder = "Enter the name";
                sendChatMessage(false);
                startSpeechToText(speechRecognizerIntent,false);
                String name = messageHolder;
                messageHolder = "Enter the number";
                sendChatMessage(false);
                startSpeechToText(speechRecognizerIntent,false);
                String contact = messageHolder;
                addContact(name,contact);
            }else if(code.code==5){
                //Weather Data
                Log.e("APP","Weather data");
                getWeatherForCurrentLocation();
            }

        }
    }

    private void letsDoSomeNetworking(RequestParams params) {

        // AsyncHttpClient belongs to the loopj dependency.
        AsyncHttpClient client = new AsyncHttpClient();

        // Making an HTTP GET request by providing a URL and the parameters.
        client.get(WEATHER_URL, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                WeatherDataModel weatherData = WeatherDataModel.fromJson(response);
                updateUI(weatherData);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {


                Toast.makeText(MainActivity.this, "Request Failed", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void getWeatherForCurrentLocation() {


        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {


                String longitude = String.valueOf(location.getLongitude());
                String latitude = String.valueOf(location.getLatitude());

                // Providing 'lat' and 'lon' (spelling: Not 'long') parameter values
                RequestParams params = new RequestParams();
                params.put("lat", latitude);
                params.put("lon", longitude);
                params.put("appid", APP_ID);
                letsDoSomeNetworking(params);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        // This is the permission check to access (fine) location.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }



//         Speed up update on screen by using last known location.
        Location lastLocation = mLocationManager.getLastKnownLocation(LOCATION_PROVIDER);
//        String longitude = String.valueOf(lastLocation.getLongitude());
//        String latitude = String.valueOf(lastLocation.getLatitude());
//        RequestParams params = new RequestParams();
//        params.put("lat", latitude);
//        params.put("lon", longitude);
//        params.put("appid", APP_ID);
//        letsDoSomeNetworking(params);

        // Some additional log statements to help you debug
        Log.d(LOGCAT_TAG, "Location Provider used: "
                + mLocationManager.getProvider(LOCATION_PROVIDER).getName());
        Log.d(LOGCAT_TAG, "Location Provider is enabled: "
                + mLocationManager.isProviderEnabled(LOCATION_PROVIDER));
        Log.d(LOGCAT_TAG, "Last known location (if any): "
                + mLocationManager.getLastKnownLocation(LOCATION_PROVIDER));
        Log.d(LOGCAT_TAG, "Requesting location updates");

        mLocationManager.requestLocationUpdates(LOCATION_PROVIDER, MIN_TIME, MIN_DISTANCE, mLocationListener);

    }

        // Updates the information shown on screen.
    private void updateUI(WeatherDataModel weather) {

        messageHolder = weather.getTemperature() + " " + weather.getCity();
        Log.e("APP",weather.getTemperature()+weather.getCity());
        sendChatMessage(false);

    }
    void speak(){
        tts.speak(messageHolder,TextToSpeech.QUEUE_FLUSH,null);
}
    private void sendChatMessage(boolean side){

        chatArrayAdapter.add(new ChatMessage(side,messageHolder));
        chatArrayAdapter.notifyDataSetChanged();

        if(side != true){
            tts.speak(messageHolder,TextToSpeech.QUEUE_ADD,null);
            SleepMainThread(3);
        }

    }

    private void startSpeechToText(Intent speechRecognizerIntent,boolean forbtn){

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                button.setVisibility(View.INVISIBLE);
                spokenText.setVisibility(View.VISIBLE);
                tts.stop();
                queryCode.cancel(true);

            }
            @Override
            public void onBeginningOfSpeech() {            }
            @Override
            public void onRmsChanged(float v) {            }
            @Override
            public void onBufferReceived(byte[] bytes) {           }
            @Override
            public void onEndOfSpeech() {
                //button.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.mic_disabled_color));
                button.setVisibility(View.VISIBLE);
                spokenText.setVisibility(View.INVISIBLE);
                spokenText.setText("Listening...");
            }

            @Override
            public void onError(int i) {            }
            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> result = bundle.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                if(result!= null){
                    String message = result.get(0);
                    messageHolder = message;
                    sendChatMessage(true);
                    if(forbtn == true) {
                        queryCode = new SeprateQuery();
                        queryCode.execute(message);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                String[] resultss = bundle.getStringArray(speechRecognizer.RESULTS_RECOGNITION);
                ArrayList<String> results = bundle.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                if(results!=null){
                    spokenText.setText(results.get(0));
                }

            }
            @Override
            public void onEvent(int i, Bundle bundle) {            }
        });

        speechRecognizer.startListening(speechRecognizerIntent);
        //searchQuery();

    }

    private String searchQuery(String strings, boolean b){
        try {
            Log.e("APP","Searching query");
            PyObject py = pyObj.callAttr("wikipediaSearch", strings);
            Log.e("APP","Getting the Result String"+py.toString());
            return py.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "Null";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tts.stop();
        tts.shutdown();
    }


    private void addContact(String given_name, String mobile) {
        ArrayList<ContentProviderOperation> contact = new ArrayList<ContentProviderOperation>();
        contact.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        // first and last names
        contact.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, given_name)
                .build());

        // Contact No Mobile
        contact.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, mobile)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());



        try {
            ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, contact);
            messageHolder = "Contact Saved";
            sendChatMessage(false);
        } catch (Exception e) {
            messageHolder = "Error saving Contact";
            sendChatMessage(false);
            e.printStackTrace();
        }
    }

}
//TODO: Add volume for speech recognizer and speed
//TODO: Add Calling for speech recognizer and speed
//TODO: Add Email for speech recognizer and speed
//TODO: Add notes Making for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed

