package com.example.treasurehunt.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MenuInflater;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

//respond to clicks
public class MainActivity extends Activity implements OnClickListener{

    //String to identify that it's our logging
    private static final String TAG = "TH";

    //String to store the converted time from Long
    public static String converted_time;

    private static boolean music;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get user's settings
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        music=sharedPrefs.getBoolean("music", true);

        //Play background music if music is enabled in settings
        /*if(music) {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.hunt);
            mediaPlayer.start(); // no need to call prepare(); create() does that for you
        }*/

        //Get screen resolution for images - easier to deal with artist this way
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        Log.d(TAG, "screenWidth: " + width);
        Log.d(TAG,   "screenHeight: " + height);

        //Function for each of the button in the main screen
        View newButton = findViewById(R.id.new_button);
        newButton.setOnClickListener((OnClickListener)this);

        View aboutButton = findViewById(R.id.about_button);
        aboutButton.setOnClickListener(this);

        View exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener((OnClickListener)this);

    }


     public void onClick(View v){
        switch (v.getId()){
        case R.id.about_button:
            Intent i = new Intent(this, About.class);
            startActivity(i);
            break;
        case R.id.exit_button:
            finish();
            break;
        case R.id.new_button:
            openNewGameDialog();
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.d(TAG,   "Comparing against options");

        switch (item.getItemId()){
            case R.id.settings:
                startActivity(new Intent(this, Prefs.class));
                // more cases go here...
        }
        return false;
    }

    private void openNewGameDialog(){

        //Debug:
        Log.d(TAG,   "Starting a new hunt");

        //Declare the Hunt intent
        Intent intent = new Intent(this, Hunt.class);

        //Start the activity via this intent
        startActivity(intent);
        // this is where the game (eventually) will be started
    }

    public void number_to_time(Long time) {
        //Convert it into hhmmss
        long s = (time / 1000) % 60;
        long m = (time / (1000 * 60)) % 60;
        long h = (time / (1000 * 60 * 60)) % 24;

        converted_time = "";
        //Make sure it's two digits
        if(h<10){
            converted_time = "0" + h;
        }else{
            converted_time = converted_time + h;
        }
        if(m<10){
            converted_time = converted_time + ":0" + m;
        }else{
            converted_time = converted_time + ":" + m;
        }
        if(s<10){
            converted_time = converted_time + ":0" + s;
        }else{
            converted_time = converted_time + ":" + s;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Play background music if music is enabled in settings
        if(music) {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.hunt);
            mediaPlayer.start(); // no need to call prepare(); create() does that for you
        }

        //TextView for our best times
        TextView scoreboard=(TextView)findViewById(R.id.scoreboard);

        //Check how many best times we have saved
        try {
            int count = 0;
            long c;
            String scoreboard_info = "";
            int content;
            String temp;

            //Debug to delete the file
            // deleteFile("scoreboard");

            //Filename
            String FILENAME = "scoreboard";

            //Open the file for reading
            FileInputStream fis = openFileInput(FILENAME);

            //read line by line
            InputStreamReader inputreader = new InputStreamReader(fis);
            BufferedReader buffreader = new BufferedReader(inputreader);

            for(int i=0;i<3;i++) {
                temp = buffreader.readLine();
                //If there is no new line then stop
                if(temp!=null) {
                    Log.d(TAG, "FROM FILE line:" + temp);
                    c = Long.parseLong(temp);
                    switch (count) {
                        case 0:
                            //Convert from Long to time
                            number_to_time(c);
                            //Add it to the display string
                            scoreboard_info = "\n\n Best times:\n\n 1. " + scoreboard_info + converted_time;
                            count++;
                            break;
                        case 1:
                            //Convert from Long to time
                            number_to_time(c);
                            //Add it to the display string
                            scoreboard_info = scoreboard_info + "\n 2. " + converted_time;
                            count++;
                            break;
                        case 2:
                            //Convert from Long to time
                            number_to_time(c);
                            //Add it to the display string
                            scoreboard_info = scoreboard_info + "\n 3. " + converted_time;
                            count++;
                            break;
                    }
                }
            }

            fis.close();

            scoreboard.setText(scoreboard_info);

        } catch (Exception e) {
            System.err.println("FileStreamsTest: " + e);
            scoreboard.setText("\n\nNo hunt\n has been  \n undertaken");

        }
    }
}
