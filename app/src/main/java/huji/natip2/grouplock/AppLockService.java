package huji.natip2.grouplock;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

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
        if (!isServiceRunning) {
            Toast.makeText(AppLockService.this, "startId: " + startId, Toast.LENGTH_SHORT).show();
            isServiceRunning = true;
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
        } else {
            Toast.makeText(AppLockService.this, "Already running", Toast.LENGTH_SHORT).show();// REMOVE: 19/11/2015
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(AppLockService.this, "onDestroy()", Toast.LENGTH_SHORT).show();// REMOVE: 19/11/2015
        isServiceRunning = false;
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
        String activityOnTop = getForegroundApp();
        Toast.makeText(AppLockService.this, "top: " + activityOnTop, Toast.LENGTH_SHORT).show();// REMOVE: 19/11/2015
        if (activityOnTop != null && activityOnTop.equals(APP_TO_LOCK)) {
            Intent lockIntent = new Intent(this, LockScreen.class); // TODO: 13/11/2015 context
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(lockIntent);
        }
    }

    private String getForegroundApp() {
        // TODO: 11/21/2015 on Sony M4 (android 5.0): topApp: null


        // Method 4: track app changes 
        // (exception)
/*        AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        appOpsManager.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, APP_TO_LOCK, new AppOpsManager.OnOpChangedListener() {

            @Override
            public void onOpChanged(String op, String packageName) {
                Toast.makeText(AppLockService.this, APP_TO_LOCK + " changed state", Toast.LENGTH_SHORT).show();
            }
        });*/

        String topApp = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            List<String> foreground = new ArrayList<>();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    foreground.add(appProcess.processName);
                }
            }
            if (!foreground.isEmpty()) {
                topApp = foreground.get(0);
            }
            if (topApp == null) { // FIXME: 11/21/2015 android 5.0: sony m4
                /*ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                // The first in the list of RunningTasks is always the foreground task.
                ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
                topApp = foregroundTaskInfo.topActivity.getPackageName();*/

                ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                // The first in the list of RunningTasks is always the foreground task.
                ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
                topApp = foregroundTaskInfo.topActivity.getPackageName();

                                                    // Method 3:
//                 Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
/*                UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                long currentTime = System.currentTimeMillis();
                // get usage stats for the last 10 seconds
                List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 10, currentTime);
                // search for app with most recent last used time
                if (stats != null) {
                    long lastUsedAppTime = 0;
                    for (UsageStats usageStats : stats) {
                        if (usageStats.getLastTimeUsed() > lastUsedAppTime) {
                            topApp = usageStats.getPackageName();
                            lastUsedAppTime = usageStats.getLastTimeUsed();
                        }
                    }
                }*/
            }

                    /*                // Method 3:
                // with: Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                long currentTime = System.currentTimeMillis();
                // get usage stats for the last 10 seconds
                List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 10, currentTime);
                // search for app with most recent last used time
                if (stats != null) {
                    long lastUsedAppTime = 0;
                    for (UsageStats usageStats : stats) {
                        if (usageStats.getLastTimeUsed() > lastUsedAppTime) {
                            topApp = usageStats.getPackageName();
                            lastUsedAppTime = usageStats.getLastTimeUsed();
                        }
                    }
                }*/
        } else {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            // The first in the list of RunningTasks is always the foreground task.
            ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
            topApp = foregroundTaskInfo.topActivity.getPackageName();
            /*
            // get package info
            PackageManager pm = getPackageManager();
            PackageInfo foregroundAppPackageInfo;
            try {
                foregroundAppPackageInfo = pm.getPackageInfo(foregroundTaskPackageName, 0);
                topApp = foregroundAppPackageInfo.applicationInfo.loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }*/
        }

        return topApp;
    }

/*    public static boolean needPermissionForBlocking(Context context){
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            return  (mode != AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }*/
}