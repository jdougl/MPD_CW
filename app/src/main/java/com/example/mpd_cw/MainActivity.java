package com.example.mpd_cw;

import android.app.Dialog;
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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

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
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private String result;
    private Button startButton;
    private ListView roadWorkList;
    LinearLayout layout;

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

    // used to store roadwork data which we will use to populate list
    ArrayList<String> roadworks = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        roadWorkList = findViewById(R.id.roadwork_list);

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


                mMapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(final GoogleMap googleMap) {
                        LatLng posisiabsen = new LatLng(100.2, 100.2); ////your lat lng
                        googleMap.addMarker(new MarkerOptions().position(posisiabsen).title("Yout title"));
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(posisiabsen));
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

                dialog.show();
            }
        });
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

    // Used to parse RAW XML data and populate a list with roadworks for that date, displaying the list to user
    public void parseRawTextByDate(InputStream rawRoadworks) {
        String tempTitle = null;
        String tempDesc = null;
        String tempLink = null;

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

                        // formatting - making the text more readable
                        tempTitle = "Title: " + tempTitle + "\n";
                    }

                    else if (xpp.getName().equalsIgnoreCase("description")) {
                            // Now just get the associated text
                            tempDesc = xpp.nextText();

                            // formatting and making the description more readable
                            tempDesc = tempDesc.replaceAll("<br />", "\n\n");
                    }

                    else if (xpp.getName().equalsIgnoreCase("link")) {
                                    // Now just get the associated text
                                    tempLink = xpp.nextText();
                    }

                    else if (xpp.getName().equalsIgnoreCase("pubDate")) {
                        // Now just get the associated text
                        String temp = xpp.nextText();

                        // System.out.println(temp);
                        // System.out.println(dateString);
                        if(temp.equals(dateString)) {
                            roadworks.add(tempTitle + "\n" + tempDesc + "\n" + tempLink);
                        }

                        //Log.e("MyTag", "pubDate is  " + temp);
                    }
                }

                // Get the next event
                eventType = xpp.next();
            }
            System.out.println(roadworks.size());

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


            Log.e("MyTag","in run");

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

            //
            // Now that you have the xml data I will call a function to parse it by date
            //

            // Now update the TextView to display raw XML data
            // Probably not the best way to update TextView
            // but we are just getting started !

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
