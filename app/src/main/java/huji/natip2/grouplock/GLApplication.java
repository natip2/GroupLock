package huji.natip2.grouplock;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;

public class GLApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Required - Initialize the Parse SDK
        Parse.initialize(this, "qKmz3Nbj1A3efQj8wKOnFRKafxenQJZrFuP0AOdk", "1MCwpWJH4edQMi85wcSw7LR13MhDDbzRrpaJgCBc");
        ParseInstallation.getCurrentInstallation().saveInBackground();

        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        ParseObject.registerSubclass(Group.class);
    }
}
