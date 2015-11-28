package huji.natip2.grouplock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.parse.ParseUser;

public class LockScreen extends Activity {

    private Intent broadcastIntent = new Intent("huji.natip2.grouplock.MyGroupActivity");
    private LocalBroadcastManager broadcaster;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);	// Removes title bar // FIXME: 17/11/2015
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);	// Removes notification bar // FIXME: 17/11/2015
        setContentView(R.layout.lock_screen);

        broadcaster = LocalBroadcastManager.getInstance(getApplicationContext());

        Button unlockButton = (Button) findViewById(R.id.unlock_button);
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                broadcastIntent.putExtra(MyGroupActivity.ACTION_CODE_EXTRA, MyGroupActivity.ACTION_REQUEST_UNLOCK);
                broadcaster.sendBroadcast(broadcastIntent);
            }
        });
    }

    @Override
    public void onBackPressed() {
    }
}
