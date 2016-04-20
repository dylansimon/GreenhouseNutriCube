package com.mbientlab.metawear.app;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.DeviceInformation;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.I2C;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.MultiChannelTemperature;
import com.mbientlab.metawear.module.SingleChannelTemperature;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

import javax.xml.transform.Source;



public class HomeFragment extends ModuleFragmentBase {
    Gpio gpioModule;
    I2C i2cModule;

    private View myFragmentView; // creates a view
    private TextView text;          // creates a textview
    public Thread graphThread = null;
    private LinearLayout graphLayout;
    private LineChart mChart;
    boolean keeprunning = true;
    Toast display;
    ScheduledExecutorService schedulerTempHumi = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService lightScheduler = Executors.newScheduledThreadPool(1);
     public int duration = 10;




    private float upperlimit = 25;
    private float lowerlimit = 17;
    public float realTemp;
    public float realHum;


    public HomeFragment() {
        super(R.string.navigation_fragment_home);
    }



    public static class MetaBootWarningFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.title_warning)
                    .setPositiveButton(R.string.label_ok, null)
                    .setCancelable(false)
                    .setMessage(R.string.message_metaboot)
                    .create();

        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        myFragmentView = inflater.inflate(R.layout.fragment_home, container, false); // access the fragment on home screen


        return myFragmentView; // returns the fragment view to be created
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        final EditText settime = (EditText) myFragmentView.findViewById(R.id.TimeInput);

        final Button sett = (Button) myFragmentView.findViewById(R.id.set);
        sett.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (settime.getText().toString().length() == 0) {
                    Log.i("HomeFragment1", String.format("ssettt"));
                    duration = 10;
                    settime.setText("10", TextView.BufferType.EDITABLE);


                } else {
                    duration = Integer.parseInt(settime.getText().toString());

                }

                if (duration > 24) {
                    duration = 10;
                    settime.setText("10", TextView.BufferType.EDITABLE);
                }


                lightSchedule(duration, TimeUnit.SECONDS);


                YAxis y1 = mChart.getAxisLeft();
                final EditText upper = (EditText) myFragmentView.findViewById(R.id.MaxTempValue);
                if (upper.getText().toString().length() == 0) {

                    upperlimit = 25;

                } else {
                    upperlimit = Float.valueOf(upper.getText().toString());
                }


                final EditText lower = (EditText) myFragmentView.findViewById(R.id.MinTempValue);

                if (lower.getText().toString().length() == 0) {

                    lowerlimit = 17;
                } else {
                    lowerlimit = Float.valueOf(lower.getText().toString());
                }


                LimitLine toplimit = new LimitLine(upperlimit, "Upper Limit");
                toplimit.setLineWidth(2f);
                toplimit.disableDashedLine();
                toplimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
                toplimit.setTextSize(10f);

                LimitLine bottomlimit = new LimitLine(lowerlimit, "Lower Limit");
                bottomlimit.setLineWidth(2f);
                bottomlimit.disableDashedLine();
                bottomlimit.setLineColor(Color.rgb(35, 66, 219));
                bottomlimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                bottomlimit.setTextSize(10f);

                y1.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
                y1.addLimitLine(toplimit);
                y1.addLimitLine(bottomlimit);
                y1.setDrawLimitLinesBehindData(true);


            }
        });


        graphLayout = (LinearLayout) myFragmentView.findViewById(R.id.mainLayout);
//This is making a new linechart called mChart//
        mChart = new LineChart(getActivity());

        //Add mChart to the layout
        graphLayout.addView(mChart, ActionBar.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT);

        /////////////ENABLES////////////
        //Highlight//
        mChart.setHighlightPerTapEnabled(true);
        mChart.setHighlightPerDragEnabled(true);
        //Touch
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(true);
        //Pinch//
        mChart.setPinchZoom(true);

        ////////Customize Chart////////////
        mChart.setBackgroundColor(Color.rgb(180, 255, 207));
        mChart.setDrawGridBackground(false);
        //Legend//
        Legend m = mChart.getLegend();
        m.setForm(Legend.LegendForm.SQUARE);
        m.setTextColor(Color.BLACK);//not this

        //Xaxis//
        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.BLACK);
        x1.setTextSize(12f);
        x1.setDrawGridLines(false);
        x1.setPosition(XAxis.XAxisPosition.BOTTOM);
        x1.setAvoidFirstLastClipping(true);

        //Yaxis//
        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.RED);
        //y1.setAxisMaxValue(35f);
        //y1.setAxisMinValue(20f);
        y1.setDrawGridLines(true);

        YAxis y3 = mChart.getAxisRight();
        y3.setEnabled(true);
        y3.setTextColor(ColorTemplate.getHoloBlue());
        //y3.setAxisMaxValue(35f);
        // y3.setAxisMinValue(20f);
        y3.setDrawGridLines(true);

        ////////Data///////
        LineData data = new LineData();
        LineData data2 = new LineData();


        data.setValueTextColor(Color.WHITE);// not this
        data2.setValueTextColor(Color.BLACK);// not this

        mChart.setDescription("");
        mChart.setNoDataText("No Data found");
        mChart.setData(data);
        mChart.setData(data2);
        mChart.invalidate();

        /////BUTTON///////






            graphThread = new Thread(new Runnable() {


                @Override
                public void run() {


                    for (int i = 0; i < Xmax; i++) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addEntry();

                            }
                        });

                        try {
                            Thread.sleep(600);

                        } catch (InterruptedException e) {

                        }

                    }

                }
            });

            graphThread.start();


        }




    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.temp_name, R.string.temp_desc));
        adapter.add(new HelpOption(R.string.daylight_name, R.string.daylight_desc));

    }

    @Override
    public void reconnected() {
        setupFragment(getView());
    } // when reconnected this will re-create the fragment view


    private void setupFragment(final View v) {
        final String METABOOT_WARNING_TAG = "metaboot_warning_tag", SWITCH_STREAM = "switch_stream";

        if (!mwBoard.isConnected()) {
            Log.i("HomeFragment", String.format("board not connected."));
            return;
        }

        if (mwBoard.inMetaBootMode()) {
            if (getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG) == null) {
                new MetaBootWarningFragment().show(getFragmentManager(), METABOOT_WARNING_TAG);
            }
        } else {
            DialogFragment metabootWarning = (DialogFragment) getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG);
            if (metabootWarning != null) {
                metabootWarning.dismiss();
            }
        }



    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        setupFragment(getView()); // setup the fragment

             i2cModule = mwBoard.getModule(I2C.class);
            gpioModule = mwBoard.getModule(Gpio.class);



        ReadTempHumidity(1, TimeUnit.SECONDS);
        gpioModule.clearDigitalOut((byte) 0x05);



    }

    ////ADDING DATA to a SET//////
    private void addEntry() {
        LineData data = mChart.getData();
        LineData data2 = mChart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            ILineDataSet set2 = data2.getDataSetByIndex(1);

            if (set == null) {
                set = createSet();
                set2 = createSet2();


                data.addDataSet(set);
                data2.addDataSet(set2);
            }


            //Random Value
            data.addXValue("");
            data.addEntry(new Entry(realTemp, set.getEntryCount()), 0);
            data.addEntry(new Entry(realHum, set.getEntryCount()), 1);

            mChart.notifyDataSetChanged();
            mChart.fitScreen();
            mChart.setVisibleXRange(0, 6);
            mChart.setBackgroundColor(Color.rgb(0, 0, 0));
            mChart.setMaxVisibleValueCount(20);
            mChart.setDrawGridBackground(false);
            //mChart.moveViewToX(data2.getXValCount() - 10);
            mChart.moveViewToX(data.getXValCount() - 7);
        }


    }


    //////method to create set/////

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Temperature");
        set.setDrawCubic(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.RED);
        set.setCircleColor(Color.RED);
        set.setLineWidth(2f);
        set.setCircleSize(4f); // on chart of the line
        set.setFillAlpha(65);
        set.setFillColor(Color.LTGRAY);
        set.setHighLightColor(Color.rgb(244, 117, 177));
        set.setDrawValues(false);
        //set.setValueTextColor(Color.rgb(154,31,31));
        //set.setValueTextSize(7f);


        return set;
    }


    private LineDataSet createSet2() {
        LineDataSet set2 = new LineDataSet(null, "Humidity");
        set2.setDrawCubic(true);
        set2.setCubicIntensity(0.2f);
        set2.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set2.setColor(ColorTemplate.getHoloBlue());
        set2.setCircleColor(Color.LTGRAY);
        set2.setLineWidth(2f);
        set2.setCircleSize(4f); // on chart of the line
        set2.setFillAlpha(65);
        set2.setFillColor(Color.WHITE);
        set2.setHighLightColor(Color.rgb(244, 117, 177));
        set2.setDrawValues(false);
        // set2.setValueTextColor(Color.rgb(154, 31, 31));
        //set2.setValueTextSize(7f);

        return set2;
    }


        void ReadTempHumidity(int value, TimeUnit timeunit) {


        schedulerTempHumi.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {


                        Log.i("HomeFragment", String.format("sending data"));
                        byte deviceAddress = (byte) (0x27);
                        byte registerAddress = (byte) 0x00; //don't have. KEEP as 0x00!
                        byte[] dummyArray = new byte[]{}; //don't care. KEEP as 0!
                        byte numBytesToRead = 4;


                        i2cModule.writeData(deviceAddress, registerAddress, dummyArray); // Send command to update data
                        try {
                            Thread.sleep(25);
                        } catch (Exception e) {
                        }

                        i2cModule.readData(deviceAddress, registerAddress, numBytesToRead).onComplete(new AsyncOperation.CompletionHandler<byte[]>() {


                            @Override
                            public void success(byte[] result) {
                                //Log.i("HomeFragment", String.format("RAW DATA = %x %x %x %x", result[0], result[1], result[2], result[3]));

                                int status = result[0] >> 6;
                                switch (status) {
                                    case 0:
                                        Log.i("HomeFragment", String.format("Good data."));
                                        break;

                                    case 1:
                                        Log.i("HomeFragment", String.format("Stale data."));
                                        break;

                                    case 2:
                                        Log.i("HomeFragment", String.format("Command mode."));
                                        break;

                                    default:
                                        Log.i("HomeFragment", String.format("Diagnostic condition encountered."));
                                        break;
                                }

                                ////////TEMP/////////

                                int t = (result[2] << 8) | (result[3]);
                                t = t / 4;
                                // Log.i("HomeFragment", String.format("CONCATENATED TEMP DATA = %d %x", t, t));
                                float temperature = (float) (t * 1.007e-2 - 40.0);
                                Log.i("HomeFragment", String.format("CALCULATED TEMP DATA = %.1f˚C", temperature));
                                if (temperature < 0) {


                                } else {

                                    text = (TextView) myFragmentView.findViewById(R.id.TempValue); // saves the textview title in an object
                                    text.setText(String.format("%.1f˚C", temperature));
                                    realTemp = temperature;

                                }
                                ////////HUMI/////////

                                int h = (((result[0]) << 8) | (result[1]));
                                h = h & 0x3FFF; // mask off 2msb status bits
                                double humidity = (h * 6.10e-3);
                                Log.i("HomeFragment", String.format("CALCULATED h = %.1f", humidity) + "%");
                                if (humidity > 95) {
                                } else {
                                    text = (TextView) myFragmentView.findViewById(R.id.humidity); // saves the textview title in an object
                                    text.setText((String.format("%.1f", humidity) + "%"));
                                    realHum = (float) humidity;
                                }


                                if ((realTemp < lowerlimit)) // if sensor temp is less than inputed min temp
                                {

                                    gpioModule.setDigitalOut((byte) 0x07); // fan off
                                    Log.i("HomeFragment", ("Fan off"));


                                }
                                if (realTemp > upperlimit) // sensor temperature is greater than inputed max temp
                                {



                                    //display.makeText(getActivity().getApplicationContext(), "Temperature is to high", display.LENGTH_SHORT).show();
                                    gpioModule.clearDigitalOut((byte) 0x07); // fan on
                                    Log.i("HomeFragment", ("Fan on"));
                                }

                            }
                        });


                    }
                }, 0, value, timeunit);


    }



    void lightSchedule (final int value, final TimeUnit timeunit)
    {

        final Runnable lighton = new Runnable() {
            public void run() {


                    Log.i("HomeFragment", "LIGHTS ON");
                    gpioModule.clearDigitalOut((byte) 0x06);



            }
        };
        final Runnable lightoff = new Runnable() {
            public void run() {


                Log.i("HomeFragment","LIGHTS OFF");
                gpioModule.setDigitalOut((byte) 0x06);

            }
        };


        lightScheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {

                ScheduledFuture future = lightScheduler.schedule(lighton,0,timeunit);
                ScheduledFuture future1 = lightScheduler.schedule(lightoff, value, timeunit);

            }


        }, 0, 24, timeunit);

    }




}






