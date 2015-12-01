package me.smartlonghorn.smartlonghorn;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseAnalytics;

/**
 * Setup for Parse and possibly other services later on.
 */
public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.parse_appid), getString(R.string.parse_clientkey));
        ParseAnalytics.trackAppOpenedInBackground(null);
    }
}
