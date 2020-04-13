package com.example.mpd_cw;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
    private TextView rawDataDisplay;
    private String result;
    private Button startButton;
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

        rawDataDisplay = (TextView)findViewById(R.id.rawDataDisplay);
        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

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

                dateString = tempDateString + ", " + dayOfMonth + " " + tempMonthString + " " + year + " 00:00:00 GMT";



                Log.d("Date", "Year=" + year + " Month=" + (month) + " day=" + dayOfMonth + dateString);
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

            xpp.setInput(new InputStreamReader(rawRoadworks));
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                // Found a start tag
                if (eventType == XmlPullParser.START_TAG) {

                    // temp variables to store values before we know if we need them or not

                    if (xpp.getName().equalsIgnoreCase("title")) {
                        // Now just get the associated text
                        tempTitle = xpp.nextText();
                        // Do something with text
                       // Log.e("MyTag", "Title is " + tempTitle);
                    }

                    else if (xpp.getName().equalsIgnoreCase("description")) {
                            // Now just get the associated text
                            tempDesc = xpp.nextText();
                            // Do something with text
                           // Log.e("MyTag", "Description is " + tempDesc);
                    }

                    else if (xpp.getName().equalsIgnoreCase("link")) {
                                    // Now just get the associated text
                                    tempLink = xpp.nextText();
                                    // Do something with text
                                    //Log.e("MyTag", "Washer is " + tempLink);
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
                    System.out.println(i);
                    System.out.println(roadworks.get(i));
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

                    // List we are going to populate with roadworks
                    ListView roadWorkList = findViewById(R.id.roadwork_list);
                    System.out.println("Adding roadworks to UI");
                    System.out.println(roadworks);
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
