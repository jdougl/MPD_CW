// Jamie Douglas | S1625371
// 15/04/2020

package com.example.mpd_cw;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.icu.text.SymbolTable;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private String result;
    private Button startButton;
    private ListView roadWorkList;
    public Toolbar toolbar;

    // Traffic Scotland URLs
    private String urlSource = "https://trafficscotland.org/rss/feeds/roadworks.aspx";
    //private String urlSource = "https://trafficscotland.org/rss/feeds/plannedroadworks.aspx";
    //private String urlSource = "https://trafficscotland.org/rss/feeds/currentincidents.aspx";
    private DatePicker datePicker;

    // Datepicker variables, storing locally to avoid recalling the API, wasting resources
    private int year;
    int month;
    int dayOfMonth;
    String dateString;

    // used to avoid repopulating data when the application doesn't need to, using up resources
    private boolean firstParse;

    // used to store roadwork data which we will use to populate list
    ArrayList<String> roadworks = new ArrayList<String>();

    //  variable used to store ONLY roadworks for searching functionality - could be refactored to inner class to stick to OOP
    ArrayList<String> roadworkTitles = new ArrayList<String>();
    ArrayList<String> roadworkDescs = new ArrayList<String>();
    ArrayList<String> roadworkLinks = new ArrayList<String>();
    ArrayList<String> roadworkLatLongs = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // on create pull ALL information from roadworks FEED - to ensure search functionality works
        startProgress();

        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        Button searchSubmitButton = (Button)findViewById(R.id.searchButton);

        roadWorkList = findViewById(R.id.roadwork_list);

        // initialize the custom textview with autocomplete
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, roadworkTitles);
        final AutoCompleteTextView textView = (AutoCompleteTextView)
                findViewById(R.id.editText1);
        textView.setAdapter(adapter);

        // Initialize datepicker and add a listener
        DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker datePicker, int newYear, int newMonth, int newDayOfMonth) {
                year = newYear;
                month = newMonth;
                dayOfMonth = newDayOfMonth;

                calendar.set(year, month, dayOfMonth);

                String tempDateString = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
                tempDateString = tempDateString.substring(0,3);

                String tempMonthString = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
                tempMonthString = tempMonthString.substring(0,3);

                // ensures year is 2 digits wide to make sure that single digits have a leading 0 e.g. 5 must be 05
                String formattedDay = String.format("%02d", dayOfMonth);

                dateString = tempDateString + ", " + formattedDay + " " + tempMonthString + " " + year + " 00:00:00 GMT";

                Log.d("Date", "Year=" + year + " Month=" + (month) + " day=" + formattedDay + dateString);
          }
        });

        searchSubmitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // get search value
                String searchValue = textView.getText().toString();

               // System.out.println(searchValue);

               // System.out.println(roadworkTitles);

                // check if titles contains the value
                if(roadworkTitles.contains(searchValue)) {
                    roadworks.clear();

                    // get index of location of search value
                    int indexOfTitle = roadworkTitles.indexOf(searchValue);

                    System.out.println(indexOfTitle);

                    // formatting - making the text more readable
                    String tempTitle = "Title: " + roadworkTitles.get(indexOfTitle) + "\n";
                    String tempDesc = roadworkDescs.get(indexOfTitle);
                    String tempLink = roadworkLinks.get(indexOfTitle);
                    String tempLatLngString = roadworkLatLongs.get(indexOfTitle);

                    roadworks.add(tempTitle + "\n" + tempDesc + "\n" + tempLink + "\n" + tempLatLngString);

                    System.out.println("Adding roadworks to UI");
                    // System.out.println(roadworks);
                    ArrayAdapter<String> roadworkArrayAdapter = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_list_item_1,
                            roadworks);

                    roadWorkList.setAdapter(roadworkArrayAdapter);

                }
            }
        });

        roadWorkList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                final Dialog dialog = new Dialog(MainActivity.this);

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                /////make map clear
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                dialog.setContentView(R.layout.map_popup_view);////your custom content

                MapView mMapView = (MapView) dialog.findViewById(R.id.map_view);
                MapsInitializer.initialize(MainActivity.this);

                mMapView.onCreate(dialog.onSaveInstanceState());
                mMapView.onResume();

                // extract info from roadwork String by splitting into lines and use method to build a latlng from the String
                String[] lines = roadworks.get(position).split("\\r?\\n");
                String tempPointLatLngString = lines[8];
                String tempDescription = lines[2] + lines[4];

                System.out.println(tempPointLatLngString);
                final LatLng pointLatLng = buildLatLng(tempPointLatLngString);

                mMapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(final GoogleMap googleMap) {
                        googleMap.addMarker(new MarkerOptions().position(pointLatLng).title("Work Location"));
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(pointLatLng));
                        googleMap.getUiSettings().setZoomControlsEnabled(true);
                        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
                    }
                });


                Button dialogButton = (Button) dialog.findViewById(R.id.btn_cancel);
                // if button is clicked, close the custom dialog
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                // roadwork length image (red/yellow/green) image used to demonstrate how long the roadworks will last
                 Drawable colourImg = roadworkLengthColour(tempDescription);
                 System.out.println(colourImg);
                 ImageView imgView = dialog.findViewById(R.id.colour_image);
                 imgView.setImageDrawable(colourImg);

                 dialog.show();
            }
        });
    }

    // method to build a LatLng from given string Latitude and Longitude seperated by spaces
    public LatLng buildLatLng(String latLngStr) {

        // parse out lat and lng from string
        String tempLat;
        String tempLong;

        System.out.println(latLngStr);

        double latInt;
        double longInt;

        tempLat = latLngStr.substring(0, latLngStr.indexOf(" "));
        tempLong = latLngStr.substring(latLngStr.indexOf(" "));
        System.out.println(latLngStr);

        System.out.println(tempLat);
        System.out.println(tempLong);

        // make latlng object
       LatLng tempLatLng = new LatLng(Double.parseDouble(tempLat), Double.parseDouble(tempLong));

       return tempLatLng;
    }

    public void onClick(View aview)
    {
        startProgress();
    }

    public void startProgress()
    {
        // Run network access on a separate thread;
        new Thread(new Task(urlSource)).start();
    } //

    // adds a red, yellow or green image dependent on how long the roadwork is going to last
    public Drawable roadworkLengthColour(String roadworkDesc) {

        // parse all numbers out of the description
        roadworkDesc = roadworkDesc.replaceAll("[^0-9]+", " ");
        List numberArray = Arrays.asList(roadworkDesc.trim().split(" "));

        System.out.println(roadworkDesc);

        // casting values to start and end date, being careful to deal with leading 0's appropriately
        String startDate = (String) numberArray.get(0);
        String endDate = (String) numberArray.get(4);

        int startInt = Integer.parseInt(startDate);
        int endInt = Integer.parseInt(endDate);
        int roadworkLength = endInt - startInt;

        // System.out.println(startInt);
        // System.out.println(endInt);

        // determine green/yellow/red image and return it
        if(roadworkLength <= 7) {
            Drawable roadworkColour  = ContextCompat.getDrawable(this,R.drawable.green);
            return roadworkColour;
        }
        // yellow
        else if(roadworkLength <= 14) {
            Drawable roadworkColour  = ContextCompat.getDrawable(this,R.drawable.yellow);
            return roadworkColour;
        }
        // red
        else {
            Drawable roadworkColour  = ContextCompat.getDrawable(this,R.drawable.red);
            return roadworkColour;
        }

    }

    // Used to parse RAW XML data and populate a list with roadworks for that date, displaying the list to user
    public void parseRawTextByDate(InputStream rawRoadworks) {
        String tempTitle = null;
        String tempDesc = null;
        String tempLink = null;
        String tempLatLngString = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            roadworks.clear();

            xpp.setInput(new InputStreamReader(rawRoadworks));
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                // Found a start tag
                if (eventType == XmlPullParser.START_TAG) {

                    // temp variables to store values before we know if we need them or not
                    if (xpp.getName().equalsIgnoreCase("title")) {
                        //  get the associated text
                        tempTitle = xpp.nextText();

                        // adding all titles to roadworks titles for search functionality - only on first parse of data
                        if(!firstParse) {
                            roadworkTitles.add(tempTitle);
                        }

                        // formatting - making the text more readable
                        tempTitle = "Title: " + tempTitle + "\n";

                    }

                    // parse out description, remove tags
                    else if (xpp.getName().equalsIgnoreCase("description")) {
                        // Now just get the associated text
                        tempDesc = xpp.nextText();

                        // formatting and making the description more readable
                        tempDesc = tempDesc.replaceAll("<br />", "\n\n");

                        // adding all descs to roadworks titles for search functionality - only on first parse of data
                        if(!firstParse) {
                            roadworkDescs.add(tempDesc);
                        }
                    }

                    // parse out link
                    else if (xpp.getName().equalsIgnoreCase("link")) {
                        // Now just get the associated text
                        tempLink = xpp.nextText();

                        // formatting
                        tempLink = "Link: " + tempLink;

                        // adding all links to roadworks titles for search functionality - only on first parse of data
                        if(!firstParse) {
                            roadworkLinks.add(tempLink);
                        }
                    }

                    // get Longitude and Latitude from RSS feed
                    else if (xpp.getName().equalsIgnoreCase("point")) {
                        // Now just get the associated text
                        tempLatLngString = xpp.nextText();

                        // adding all LatLngStrings to roadworks titles for search functionality - only on first parse of data
                        if(!firstParse) {
                            roadworkLatLongs.add(tempLatLngString);
                        }
                    }

                    // parse out pub date, if pubdate is the same as the one entered in datepicker, add it to the list
                    else if (xpp.getName().equalsIgnoreCase("pubDate")) {
                        // Now just get the associated text
                        String temp = xpp.nextText();

                        // System.out.println(temp);
                        // System.out.println(dateString);

                        if(temp.equals(dateString)) {
                            roadworks.add(tempTitle + "\n" + tempDesc + "\n" + tempLink + "\n" + tempLatLngString);
                        }

                        // Log.e("MyTag", "pubDate is  " + temp);
                    }
                }

                // Get the next event
                eventType = xpp.next();
            }

            firstParse = true;

            // modifies view - displaying roadworks or telling the user that no roadworks exist on the specified date
            if(roadworks.size() > 0) {
                int tempRoadworksSize = roadworks.size();

                // return list of roadworks if there are some
                for (int i = 0; i <= tempRoadworksSize; i++) {

                    // add roadworks for given date to global array
                    roadworks.add(roadworks.get(i));

                    // System.out.println(i);
                   // System.out.println(roadworks.get(i));
                }
            }

            // otherwise return a message to user informing them no roadworks exist on given date
            else {
                System.out.println("No roadworks found on given date!");
                roadworks.add("No roadworks found on given date!");
            }

        } catch (XmlPullParserException ae1) {
            Log.e("MyTag", "Parsing error" + ae1.toString());
        } catch (IOException ae1) {
            Log.e("MyTag", "IO error during parsing");
        }
        Log.e("MyTag", "End document");
    }

    // Need separate thread to access the internet resource over network
    // Other neater solutions should be adopted in later iterations.
    private class Task implements Runnable
    {
        private String url;

        public Task(String aurl)
        {
            url = aurl;
        }

        @Override
        public void run()
        {

            URL aurl;
            URLConnection yc;
            BufferedReader in = null;
            String inputLine = "";

            try
            {
                Log.e("MyTag","in try");
                aurl = new URL(url);
                yc = aurl.openConnection();
                in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
                InputStream rawInput = yc.getInputStream();

                //
                // Throw away the first 2 header lines before parsing
                //
                //
                //

                parseRawTextByDate(rawInput);
                System.out.println(roadworkTitles);
                while ((inputLine = in.readLine()) != null)
                {
                    result = result + inputLine;

                }
                in.close();
        }
            catch (IOException ae)
            {
                Log.e("MyTag", "ioexception");
            }

            // UI thread which runs concurrently
            MainActivity.this.runOnUiThread(new Runnable()
            {
                public void run() {
                    System.out.println("Adding roadworks to UI");
                   // System.out.println(roadworks);
                    ArrayAdapter<String> roadworkArrayAdapter = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_list_item_1,
                            roadworks);

                    roadWorkList.setAdapter(roadworkArrayAdapter);
                }
            });
        }

    }

} // End of MainActivity
