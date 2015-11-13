package huji.natip2.grouplock;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RSSPullService extends IntentService {


    private static final String APP_TO_LOCK = "com.";

    /**

     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public RSSPullService(String name) {
        super(name);
    }
    public RSSPullService() {
        super("RSSPullService");
    }

    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();

        // Do work here, based on the contents of dataString
        Thread thread = new Thread()
        {
            @Override
            public void run() {
                try {
                    while(true) {
                        Thread.sleep(5000); // for the 5 seconds requirement
                        lockForeground();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    private void lockForeground() {
        ArrayList<String> apps = new ArrayList<>();

        PackageManager packageManager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> appList = packageManager.queryIntentActivities(mainIntent, 0);
        Collections.sort(appList, new ResolveInfo.DisplayNameComparator(packageManager));
        List<PackageInfo> packs = packageManager.getInstalledPackages(0);
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            ApplicationInfo a = p.applicationInfo;
            // skip system apps if they shall not be included
            if ((a.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                continue;
            }
            apps.add(p.packageName);
        }

        ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> RunningTask = mActivityManager.getRunningTasks(1);
        ActivityManager.RunningTaskInfo ar = RunningTask.get(0);

        String activityOnTop=ar.topActivity.getClassName();

        if (activityOnTop != null && activityOnTop.equals(APP_TO_LOCK)) {
            Intent lockIntent = new Intent(this, LockScreen.class); // TODO: 13/11/2015 context
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(lockIntent);
        }
    }
}