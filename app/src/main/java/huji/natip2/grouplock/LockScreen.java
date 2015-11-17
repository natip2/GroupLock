package huji.natip2.grouplock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.view.Window;
import android.view.WindowManager;

public class LockScreen extends Activity {

    private static String TAG = SplashActivity.class.getName();
    private static long SLEEP_TIME = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);	// Removes title bar // FIXME: 17/11/2015
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);	// Removes notification bar // FIXME: 17/11/2015
        setContentView(R.layout.lock_screen);
    }

    @Override
    public void onBackPressed() {
    }
}
