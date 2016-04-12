package com.example.treasurehunt.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Random;


/**
 * Created by mukundagarwal on 09/03/2014.
 */

                    //you have to extend it or else it wont be able to cast it
public class Hunt extends FragmentActivity {

    //Google Map
    private static GoogleMap googleMap;
    //Logging string for this function
    private static final String TAG = "TH:Hunt:";

    //Will be used to store the server result
    public static ArrayList<String> result;
    public static double longitude;
    public static double latitude;
    public static String loc_name;
    public static String loc_tag;
    public static String loc_id;
    public static String loc_id_compare;
    public static String loc_hint;
    public static ArrayList<String> idarray = new ArrayList();
    public static int size_idx;
    public static int find_idx;

    //Media player variable
    private static MediaPlayer mediaPlayer;

    //To make note of the start time
    public static long time_start;

    //Scanner button
    public Button btn;

    //For dialog box
    final Context context = this;

    //Settings variable
    private static boolean difficulty;
    private static boolean music;
    private static boolean demo;
    private static int mapoption;

    //Upon creation call the layout
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //We will use the treasure hunt layout
        setContentView(R.layout.treasurehunt);

        //Declare the scanner button
        btn = (Button) findViewById(R.id.startScanner);
        //Listen for the button press
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Call the QR button click function
                startScannerOnClick(v);
            }
        });

        //Get user's settings
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        difficulty=sharedPrefs.getBoolean("difficulty", true);
        music=sharedPrefs.getBoolean("music", true);
        demo=sharedPrefs.getBoolean("demo", true);
        mapoption = Integer.parseInt(sharedPrefs.getString("googlemap", "0"));

        //Load the latest database
        //Declare the intent
        Intent serverdata = new Intent(this, AndroidHttpsClientJSONActivity.class);

        //Call the activity and get the result as well
        startActivityForResult(serverdata,10);

        //Parameters for map
        loc_name = "University of Surrey: Treasure Hunt";
        loc_tag = "Good luck!";
        latitude = 51.242722000000000000;
        longitude = -0.589514399999984600;

        try {
            //Loading map
                initilizeMap(loc_name, loc_tag, latitude, longitude);
        } catch (Exception e) {
            //Error catching
            e.printStackTrace();
        }

        //Rest is in onActivityResult

    }

    //AndroidHttpsClientJSONActivity and QR scanner will deliver the results here
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Randomly choose the next data location
        Random r = new Random();

        super.onActivityResult(requestCode, resultCode, data);

        //TextView for our hints display
        TextView hint_text=(TextView)findViewById(R.id.hintdisplay);

        //Set the color of hint text based on the map type
        switch(mapoption){
            case 0:
                hint_text.setTextColor(Color.parseColor("#000000"));
                break;
            case 1:
                //Hybrid
                hint_text.setTextColor(Color.parseColor("#FFFFFF"));
                break;
            case 2:
                //Satellite
                hint_text.setTextColor(Color.parseColor("#FFFFFF"));
                break;
            case 3:
                //Terrain
                hint_text.setTextColor(Color.parseColor("#000000"));
                break;
        }

        String item="NULL";

        /*//TextView for the timer
        //TextView timer_text=(TextView)findViewById(R.id.timer);
        Time time = new Time();
        time.setToNow();
        // textView is the TextView view that should display it
        timer_text.setText("time: " + time.hour+":"+time.minute+":"+time.second);*/

        switch(requestCode) {
            case 10:
                if (resultCode == RESULT_OK) {

                    //Assign the result to the global variable here
                    result = data.getStringArrayListExtra("result");

                    if(result!=null){
                        Toast.makeText(getApplicationContext(), "Latest campus tags downloaded!", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "" + result);

                        //Initialise search variables
                        find_idx = -1;
                        size_idx = result.size();

                        hint_text.setText("Scan a location to begin!");
                    }else{
                        //Something must be wrong with the internet connection
                        Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
                        hint_text.setText("Please restart the app after internet connection is established.");

                        //Disable the QR button
                        btn.setEnabled(false);
                    }
                }

                break;
            case 0x0000c0de:
                if (resultCode == RESULT_OK)
                    //Set the orientation to portrait as its left in landscape by barcode scanner
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    Toast.makeText(getApplicationContext(), "QRScan activity returned OK", Toast.LENGTH_LONG).show();

                    //retrieve scan result
                    IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    if (scanningResult.getContents() != null) {
                        //we have a result
                        String scanContent = scanningResult.getContents();
                        String scanFormat = scanningResult.getFormatName();
                        //can use scanFormat later for more informative error messages

                        //Visual debug
                        Toast.makeText(getApplicationContext(), "QR:"+scanContent, Toast.LENGTH_LONG).show();

                        //Debug:
                        Log.d(TAG, "Scanned tag:"+scanContent);

                        //Extract the tag itself
                        String tag="";
                        int found_idx=-1;
                        find_idx = scanContent.indexOf("=");
                        if(find_idx>-1) {
                            String[] scanContentarray = scanContent.split("=");
                            tag = scanContentarray[1];

                            //Try to find this tag
                            for (int i = 0; i < size_idx; i++){
                                item = result.get(i);
                                find_idx = item.indexOf(tag);
                                if(find_idx>-1){
                                    //Log the tag info
                                    Log.d(TAG, "i:" + i + " Check index:"+find_idx+"@"+item);

                                    found_idx = i;
                                    //Job done so get out of this loop
                                    break;
                                }
                            }

                        }
                        //Show a dialog as its more visual then the just the text box
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                context);


                        //If found
                        if (found_idx>-1) {

                            //Check if it's the first tag scanned?
                            if (idarray.size() == 0) {

                                //Make a note of the start time
                                time_start = new Date().getTime();

                                //Analyse the string
                                tag_string_analyse(item);

                                //Show the first tag location
                                setMap(loc_name, loc_tag, latitude, longitude, idarray.size());

                                //Play the first music is its enabled in settings
                                if(music) {
                                    mediaPlayer = MediaPlayer.create(this, R.raw.first);
                                    mediaPlayer.start(); // no need to call prepare(); create() does that for you
                                }

                                //Store the location index
                                idarray.add(loc_id);

                                //Randomly choose the next location
                                tag_random_safe_selection(r, item);

                                //Display hint for the next location
                                hint_text.setText("Hint: " + loc_hint);

                            }else{
                                //First check if it is the correct location and then show everything
                                //Analyse the new scanned tag
                                tag_string_analyse(item);

                                //Check if it's the correct location
                                if(loc_id.equals(loc_id_compare)){
                                    //Tell them its the correct string
                                    hint_text.setText("Nice work!");

                                    //Show the correctly scanned tag location
                                    setMap(loc_name, loc_tag, latitude, longitude, idarray.size());

                                    //Play music based on the treasure number and if music is enabled in settings
                                    if(music) {
                                        switch (idarray.size()) {
                                            case 2:
                                                mediaPlayer = MediaPlayer.create(this, R.raw.second);
                                                mediaPlayer.start(); // no need to call prepare(); create() does that for you
                                                break;
                                            case 3:
                                                mediaPlayer = MediaPlayer.create(this, R.raw.third);
                                                mediaPlayer.start(); // no need to call prepare(); create() does that for you
                                                break;
                                            case 4:
                                                mediaPlayer = MediaPlayer.create(this, R.raw.fourth);
                                                mediaPlayer.start(); // no need to call prepare(); create() does that for you
                                                break;
                                            case 5:
                                                mediaPlayer = MediaPlayer.create(this, R.raw.fifth);
                                                mediaPlayer.start(); // no need to call prepare(); create() does that for you
                                                break;
                                        }
                                    }

                                    //Check if all five locations have been scanned
                                    if(idarray.size()!=5) {
                                        //Show a dialog as its more visual then the just the text box
                                        //AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                          //      context);
/*
                                        // set title
                                        alertDialogBuilder.setTitle("Nice work!");

                                        // set dialog message
                                        alertDialogBuilder
                                                .setMessage("Correct tag scanned!")
                                                .setCancelable(false)
                                                .setNeutralButton("Continue", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                    }
                                                });
*/
                                        //Randomly choose the next location
                                        tag_random_safe_selection(r, item);

                                        //Display hint for the next location
                                        hint_text.setText("Hint: " + loc_hint);
                                    }else{
                                        //Disable the QR button
                                        btn.setEnabled(false);

                                        //Make a note of the finishing time stamp
                                        long time_end = new Date().getTime();

                                        //Calculate the time taken to reach the end
                                        long difference_ms = time_end - time_start;

                                        //Convert it into hhmmss
                                        long s = (difference_ms / 1000) % 60;
                                        long m = (difference_ms / (1000 * 60)) % 60;
                                        long h = (difference_ms / (1000 * 60 * 60)) % 24;

                                        String finishtext = "Congratulations! You have finished the game in " + h + "hr " + m + "min " + s + "seconds";
                                        //Tell them that they have finished the game
                                        hint_text.setText(finishtext);

                                        //Set the scoreboard
                                        set_scoreboard(difference_ms);

                                        // set title
                                        alertDialogBuilder.setTitle("Congratulations!");

                                        // set dialog message
                                        alertDialogBuilder
                                                .setMessage(finishtext)
                                                .setCancelable(false)
                                                .setPositiveButton("Exit",new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog,int id) {
                                                        //Stop any music being played
                                                        mediaPlayer.stop();
                                                        mediaPlayer.release();

                                                        //Clear all the saved locations and erase the googlemap
                                                        idarray.clear();
                                                        googleMap=null;
                                                        // if this button is clicked, close
                                                        // current activity
                                                        Hunt.this.finish();
                                                    }
                                                })
                                                .setNegativeButton("Admire your journey!", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        //Do nothing
                                                    }
                                                });

                                        // create alert dialog
                                        AlertDialog alertDialog = alertDialogBuilder.create();

                                        // show it
                                        alertDialog.show();

                                    }
                                }else {
                                    //Display error and ask them to search harder
                                    hint_text.setText("No it's not this one! Hint: " + loc_hint);
                                }
                            }
                        }
                        else{
                            //Display error and ask them to scan a valid tag
                            hint_text.setText("No this is not a valid uni QR code!");
                        }
                    }
                    else{
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Nothing scanned yet!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                break;
            default:
        }

    }

    //Function to randomly choose a new location but still making sure it's hasn't been chosen before
    public void tag_random_safe_selection(Random r, String item) {
        //Get number of stored locations which should be 1 here
        int idarray_length = idarray.size();

        //Randomly choose the next location
        boolean oldloc = true;

        //Keep looping until new location is randomly selected
        while(oldloc==true) {
            //-1 as array will length will be N but it starts from 0
            int rand_idx = r.nextInt(size_idx) - 1;

            //Demo mode:
            if(demo) {
                switch (idarray_length) {
                    case 1:
                        rand_idx = 4;
                        break;
                    case 2:
                        rand_idx = 3;
                        break;
                    case 3:
                        rand_idx = 13;
                        break;
                    case 4:
                        rand_idx = 1;
                        break;
                }
            }
            item = result.get(rand_idx);

            //Analyse the string
            tag_string_analyse(item);

            //Make sure it's not the same one as chosen before
            if (idarray.contains(loc_id)) {
                oldloc = true;
            } else {
                oldloc = false;
            }

            //Set the variable for comparison
            loc_id_compare = loc_id;
        }

        //Also have time textview - skip this for now

        //Check difficulty level
        if(difficulty) {
            //Generate an anagram
            String loc_name_anagram = "";
            int anagram_random = 0;
            int[] anagram_char_loc = new int[loc_name.length()];
            boolean alreadyused = true;

            for (int i = 0; i < loc_name.length(); i++) {
                //This anagram approach is way to hard for the user
                //Check if we already randomly selected character
                /*while(alreadyused) {
                    anagram_random = r.nextInt(loc_name.length() - 1);
                    alreadyused = anagram_char_loc.toString().contains(Integer.toString(anagram_random));
                }

                Log.d(TAG, "Random char choosen:" + anagram_random );

                //Add the new character
                loc_name_anagram = loc_name_anagram + loc_name.charAt(anagram_random);  // .indexOf(anagram_random)=loc_name.charAt(i);
                //Set the flag to true to choose the next value
                alreadyused = true;*/


                //For now lets just replace some of the characters with x
                if (i % 4 == 0) {
                    //Replace every 4th char with ?
                    loc_name_anagram = loc_name_anagram + "?";
                } else {
                    loc_name_anagram = loc_name_anagram + loc_name.charAt(i);
                }
            }
            loc_hint = loc_name_anagram;
        }
        else{
            loc_hint = loc_name;
        }
        //Store the location index
        idarray.add(loc_id);
    }

    //Sets the scoreboard
    public void set_scoreboard(long difference_ms) {
        long score_high = 0, score_medium = 0, score_low = 0;
        int count = 0;
        long c;

        //Check how many best times we have saved
        try {
            //Filename
            String FILENAME = "scoreboard";

            //Open the file for reading
            FileInputStream fis = openFileInput(FILENAME);

            //read line by line
            InputStreamReader inputreader = new InputStreamReader(fis);
            BufferedReader buffreader = new BufferedReader(inputreader);

            String temp = buffreader.readLine();
            while (temp != null) {
                c = Long.parseLong(temp);
                switch(count) {
                    case 0:
                        score_high = c;
                        count++;
                        break;
                    case 1:
                        score_medium = c;
                        count++;
                        break;
                    case 2:
                        score_low = c;
                        count++;
                        break;
                }
                temp = buffreader.readLine();
            }

            fis.close();

            //Start the comparison
            switch(count) {
                case 3:
                    //3 best times already scored
                    if(difference_ms<score_high){
                        //New best time
                        score_low = score_medium;
                        score_medium = score_high;
                        score_high = difference_ms;
                    } else if (difference_ms<score_medium){
                        score_low = score_medium;
                        score_medium = difference_ms;
                    } else if (difference_ms<score_low){
                        score_low = difference_ms;
                    }
                    break;
                case 2:
                    //only 2 best times scored
                    if(difference_ms<score_high){
                        //New best time
                        score_low = score_medium;
                        score_medium = score_high;
                        score_high = difference_ms;
                    } else if (difference_ms<score_medium){
                        score_low = score_medium;
                        score_medium = difference_ms;
                    } else{
                        score_low = difference_ms;
                    }
                    break;
                case 1:
                    //only 1 best time scored
                    if(difference_ms<score_high){
                        //New best time
                        score_medium = score_high;
                        score_high = difference_ms;
                    } else {
                        score_high = difference_ms;
                    }
                    break;
                case 0:
                    //no best time scored
                        //New best time
                        score_high = difference_ms;
                    break;
            }

        } catch (Exception e) {
            System.err.println("FileStreamsTest: " + e);
            //no best time scored
            //New best time
            score_high = difference_ms;
        }

        try {
            //Update the file
            String FILENAME = "scoreboard";
            //Temporary string variable
            String temp;
            //Delete the old file
            deleteFile(FILENAME);

            //Lets make the updated one
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);

            //Only write the scores which are available
            if(score_high!=0){
                //Convert the long to string using temp
                temp = ""+score_high+"\n";
                //Write in the file
                fos.write(temp.getBytes());
                if(score_medium!=0){
                    temp = ""+score_medium+"\n";
                    fos.write(temp.getBytes());
                    if(score_low!=0){
                        temp = ""+score_low;
                        fos.write(temp.getBytes());
                    }
                }
            }
            //Close the file
            fos.close();
        } catch (Exception e) {
            System.err.println("FileStreamsTest: " + e);
        }


    }

    //Function to analyse the string and update the public variables
    public void tag_string_analyse(String item){
        //Get the corresponding data into a string array
        String[] items = item.split(",");

        //Loop through all the items
        for (int i = 0; i < items.length; i++){
            String item_str = items[i];
            String[] items_sub;

            System.out.println("item = " + item_str);

            //Look for id
            if(item_str.startsWith("id:")){
                items_sub = item_str.split(":");
                loc_id = items_sub[1];
                //Add it for future reference
                //idarray.add(Integer.parseInt(loc_tag));
            }
            //Look for location tag
            if(item_str.contains("area_id:")){
                items_sub = item_str.split(":");
                loc_tag = items_sub[1];
            }
            //Look for location name
            if(item_str.contains("location_name:")){
                items_sub = item_str.split(":");
                loc_name = items_sub[1];
            }
            //Look for latitude
            if(item_str.contains("latitude:")){
                items_sub = item_str.split(":");
                latitude =  Double.valueOf(items_sub[1]);
            }
            //Look for longitude
            if(item_str.contains("longitude:")){
                items_sub = item_str.split(":");
                longitude =  Double.valueOf(items_sub[1]);
            }
        }

        //Debug
        Log.d(TAG, "Location info:"+loc_name+", Lat: "+latitude+", Long: "+longitude);

    }

    //Function to load map. If map is not created it will create it for you
    private void initilizeMap(String Mloc_name, String Mloc_tag, Double Mlatitude, Double Mlongitude) {
        //Carry out the initialisation steps
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            //University coordinates
            LatLng uni = new LatLng(Mlatitude,Mlongitude);

            //Show user's current location
            googleMap.setMyLocationEnabled(true);

            //Enable the button for current location
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            //Enable map rotate gestures
            googleMap.getUiSettings().setRotateGesturesEnabled(true);

            //Enable compass
            googleMap.getUiSettings().setCompassEnabled(true);

            //Zoom into those coordinates
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uni, 17));

            //Place a marker at that location
            /*googleMap.addMarker(new MarkerOptions()
                    .title(Mloc_name)
                    .snippet("Starting point...")
                    .position(uni)).showInfoWindow();*/

            //Map type - link to a settings window
            switch(mapoption){
                case 0:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    break;
                case 1:
                    //Hybrid
                    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    break;
                case 2:
                    //Satellite
                    googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    break;
                case 3:
                    //Terrain
                    googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    break;
            }

            // check if map is created successfully or not
            if (googleMap == null) {
                Log.d(TAG, "Sorry! unable to create maps");
                return;
            }
        }

    }


    //Function to place and zoom into a marker on the map
    private void setMap(String Mloc_name, String Mloc_tag, Double Mlatitude, Double Mlongitude, int Mnumber) {

            //University coordinates
            LatLng coordinates = new LatLng(Mlatitude,Mlongitude);

        //Choose the treasure marker image based on the treasure number
            switch(Mnumber){
                case 0:
                    //Place a marker at that location
                    googleMap.addMarker(new MarkerOptions()
                                    .title(Mloc_name)
                                    .snippet(Mloc_tag)
                                    .position(coordinates)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.spiderman))
                    ).showInfoWindow();
                    break;
                case 2:
                    //Place a marker at that location
                    googleMap.addMarker(new MarkerOptions()
                                    .title(Mloc_name)
                                    .snippet(Mloc_tag)
                                    .position(coordinates)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ironman))
                    ).showInfoWindow();
                    break;
                case 3:
                    //Place a marker at that location
                    googleMap.addMarker(new MarkerOptions()
                                    .title(Mloc_name)
                                    .snippet(Mloc_tag)
                                    .position(coordinates)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.mrt))
                    ).showInfoWindow();
                    break;
                case 4:
                    //Place a marker at that location
                    googleMap.addMarker(new MarkerOptions()
                                    .title(Mloc_name)
                                    .snippet(Mloc_tag)
                                    .position(coordinates)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.avatar))
                    ).showInfoWindow();
                    break;
                case 5:
                    //Place a marker at that location
                    googleMap.addMarker(new MarkerOptions()
                                    .title(Mloc_name)
                                    .snippet(Mloc_tag)
                                    .position(coordinates)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.r2d2))
                    ).showInfoWindow();
                    break;
            }
        //Zoom into those coordinates
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 17));

    }

    //Scanner button
    public void startScannerOnClick(View v) {
        //Debug
        Log.d(TAG, "Start the scanner!");

        //Call the scan intent
        IntentIntegrator scanIntegrator = new IntentIntegrator(this);
        scanIntegrator.initiateScan();
    }

    //Overwrite the activity overwrite behaviour as we dont want to create the class again
    /*public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

    }*/

    @Override
    protected void onResume() {
        super.onResume();
        //On resume initialise the map again
        //initilizeMap(loc_name, loc_tag, latitude, longitude);
    }

    @Override
    public void onBackPressed()
    {
        //Show a dialog as its more visual then the just the text box
        /*AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        //Show a dialog as its more visual then the just the text box
        // set title
        alertDialogBuilder.setTitle("Leave hunt!");

        // set dialog message
        alertDialogBuilder
                .setMessage("This will cancel the current hunt !")
                .setCancelable(false);

        AlertDialog dialog = alertDialogBuilder.create();*/

        //Stop any music being played
        mediaPlayer.stop();
        mediaPlayer.release();

        //Clear all the saved locations and erase the googlemap
        idarray.clear();
        googleMap=null;

        finish();
    }
}

