package huji.natip2.grouplock;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class AppLockService extends Service {


    private static final String APP_TO_LOCK = "com.whatsapp";

    private static boolean isServiceRunning = false;
    private Handler handler;



/*    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();

        System.out.println("Starting lock service -->");

        // Do work here, based on the contents of dataString

    }*/

    static boolean isServiceRunning() {
        return isServiceRunning;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(AppLockService.this, "Starting lock service --> " + startId + " " + isServiceRunning, Toast.LENGTH_SHORT).show();
        if (!isServiceRunning) {
            isServiceRunning = true;
            Toast.makeText(AppLockService.this, "First start", Toast.LENGTH_SHORT).show();
            handler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    // TODO Auto-generated method stub
                    super.handleMessage(msg);
                    lockTopForegroundApp();
                }

            };

            new Thread(new Runnable() {
                public void run() {
                    // TODO Auto-generated method stub
                    while (isServiceRunning) {
                        try {
                            Thread.sleep(4000);
                            handler.sendEmptyMessage(0);

                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }

                }
            }).start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        System.out.println("<-- Destroying lock service");
        isServiceRunning = false;
        Toast.makeText(AppLockService.this, "<-- Destroying lock service", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void lockTopForegroundApp() {
        /*ArrayList<String> apps = new ArrayList<>();
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
        List<ActivityManager.RunningAppProcessInfo> tasks = mActivityManager.getRunningAppProcesses();
        ActivityManager.RunningAppProcessInfo ar = tasks.get(0);

        System.out.println();*/// REMOVE: 18/11/2015
        String activityOnTop = getForegroundTask();
//        Toast.makeText(AppLockService.this, "top: "+activityOnTop, Toast.LENGTH_SHORT).show();
        if (activityOnTop != null && activityOnTop.equals(APP_TO_LOCK)) {
            Intent lockIntent = new Intent(this, LockScreen.class); // TODO: 13/11/2015 context
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(lockIntent);
        }
    }

    private String getForegroundTask() {
/*        String currentApp = "NULL";
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000*1000, time);
            if (appList != null && appList.size() > 0) {
                 SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            ActivityManager am = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
        }*/// REMOVE: 18/11/2015
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        List<String> foreground = new ArrayList<>();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                foreground.add(appProcess.processName);
            }
        }
        return foreground.isEmpty() ? null : foreground.get(0);
    }
}