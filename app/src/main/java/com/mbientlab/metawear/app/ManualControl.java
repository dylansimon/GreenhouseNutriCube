package com.mbientlab.metawear.app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Gpio;


public class ManualControl extends ModuleFragmentBase {

    private EditText upper;
    private View myFragmentView; // creates a view

    Gpio gpioModule;



    public ManualControl() {
        super(R.string.navigation_fragment_gpio);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        myFragmentView = inflater.inflate(R.layout.fragment_manual_control, container, false); // access the fragment on home screen


        return myFragmentView; // returns the fragment view to be created

    }

    @Override
    public void reconnected() {
        setupFragment(getView());


    } // when reconnected this will re-create the fragment view


    private void setupFragment(final View v) {
        final String METABOOT_WARNING_TAG = "metaboot_warning_tag", SWITCH_STREAM = "switch_stream";

        if (!mwBoard.isConnected()) {
            return;
        }

        if (mwBoard.inMetaBootMode()) {
            if (getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG) == null) {
                new HomeFragment.MetaBootWarningFragment().show(getFragmentManager(), METABOOT_WARNING_TAG);
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

        gpioModule = mwBoard.getModule(Gpio.class);


        final Button fanon = (Button) getActivity().findViewById(R.id.fanon); // access the fan on button from xml


        fanon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioModule.clearDigitalOut((byte) 0x07); // turn on the fan when clicked


            }
        });

        final Button fanoff = (Button) myFragmentView.findViewById(R.id.fanoff); // access the fan off button from xml
        fanoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioModule.setDigitalOut((byte) 0x07); // turn off the fan when clicked
            }
        });

        final Button lightson = (Button) myFragmentView.findViewById(R.id.lightson); // access the lights on button from xml
        lightson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                gpioModule.clearDigitalOut((byte) 0x06); // turn on the lights when clicked

            }


        });

        final Button lightsoff = (Button) myFragmentView.findViewById(R.id.lightsoff); // access the lights off button from xml
        lightsoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                gpioModule.setDigitalOut((byte) 0x06); // turn off the lights when clicked

            }


        });


        final Button pumpon = (Button) myFragmentView.findViewById(R.id.pumpon); // access the pump on button from xml
        pumpon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                gpioModule.clearDigitalOut((byte) 0x05);  // turn on the pump when clicked
            }


        });


        final Button pumpoff = (Button) myFragmentView.findViewById(R.id.pumpoff); //  access the pump off button from xml
        pumpoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                gpioModule.setDigitalOut((byte) 0x05); // turn off the pump when clicked


            }


        });



    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {

        adapter.add(new HelpOption(R.string.manual_name,R.string.manual_desc));



    }



}
