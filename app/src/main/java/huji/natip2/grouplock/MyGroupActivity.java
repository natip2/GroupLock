package huji.natip2.grouplock;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SendCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;


public class MyGroupActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, HistoryFragment.OnFragmentInteractionListener, UserFragment.OnFragmentInteractionListener {


    final static int PUSH_CODE_CONFIRM_NOTIFICATION = 10;
    final static int PUSH_CODE_UPDATE_LIST_FROM_PARSE = 11;
    final static int PUSH_CODE_QUIT = 12;

    static final String PUSH_CODE = "pushCode";
    final static int PUSH_CODE_ACCEPTED = 0;
    final static int PUSH_CODE_REJECTED = 1;
    final static int PUSH_CODE_NOT_SPECIFIED = 2;
    final static int PUSH_RESPONSE_CODE_ACCEPTED = 3;
    final static int PUSH_RESPONSE_CODE_REJECTED = 4;
    final static int PUSH_RESPONSE_CODE_NOT_SPECIFIED = 5;

    private static final int TO_RESPONSE_CONVERT_ADDITION = 3;
    public Group adminGroup;
    private int pushCode;


    private void setupSearchView() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) findViewById(R.id.searchView);
        SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
        searchView.setSearchableInfo(searchableInfo);
    }

    public String adminPhone = null;

    @Override
    protected void onNewIntent(Intent intent) {
        if (ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED.equals(intent.getAction())) {
            //handles suggestion clicked query
            addToListAdapter(intent);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) { // REMOVE: 14/09/2015
            // Other query
        } else if (intent.hasExtra(PUSH_CODE)) {
            pushCode = intent.getIntExtra(PUSH_CODE, -1);
            adminPhone = intent.getStringExtra("adminPhone");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
//        if (iOweViewFragmentWithTag == null) {
//            iOweViewFragmentWithTag = (ListViewFragment) getSupportFragmentManager().findFragmentByTag(Debt.I_OWE_TAG);
//        }
//        if (oweMeViewFragmentWithTag == null) {
//            oweMeViewFragmentWithTag = (ListViewFragmentOweMe) getSupportFragmentManager().findFragmentByTag(Debt.OWE_ME_TAG);
//        }
        // Check if we have a logged in user
        switch (pushCode) {
            case PUSH_CODE_ACCEPTED:
            case PUSH_CODE_REJECTED:
                sendPushResponseToAdmin(pushCode + TO_RESPONSE_CONVERT_ADDITION);
                break;
            case PUSH_CODE_NOT_SPECIFIED:
                showConfirmDialog();
                break;

        }
    }


    private void showConfirmDialog() {
        final AlertDialog.Builder b = new AlertDialog.Builder(MyGroupActivity.this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        String message = "Join to the group?\n\t" + getNameByPhone(adminPhone) + " invites you";
        b.setMessage(message);
        b.setPositiveButton("Join", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sendPushResponseToAdmin(PUSH_RESPONSE_CODE_ACCEPTED);
            }
        });
        b.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sendPushResponseToAdmin(PUSH_RESPONSE_CODE_REJECTED);
            }
        });
        b.show();
    }

    private void addToListAdapter(Intent intent) { //// TODO: 13/10/2015 send the person to UserFragment for adding the information
        Cursor phoneCursor = getContentResolver().query(intent.getData(), null, null, null, null);
        phoneCursor.moveToFirst();
        int idDisplayName = phoneCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        String name = phoneCursor.getString(idDisplayName);
        phoneCursor.close();
        String phoneNumber = getPhoneNumber(name);
        phoneNumber = arrangeNumberWithCountry(phoneNumber, getApplicationContext());
        if (phoneNumber == null) {
            Toast.makeText(MyGroupActivity.this, "Contact bad phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(MyGroupActivity.this, "NAME" + name + " num" + phoneNumber, Toast.LENGTH_SHORT).show();
        UserItem user = new UserItem(name, phoneNumber, doesUserHaveApp(phoneNumber));
        UserFragment.addperston(user);
        return;
    }

    private UserStatus doesUserHaveApp(String phoneNumber) {
        return userExist(phoneNumber) ? UserStatus.HAS_APP : UserStatus.DOES_NOT_HAVE_APP;
    }

    private boolean userExist(String phone) {
        ParseQuery<ParseUser> q = ParseUser.getQuery();
        q.whereEqualTo("username", phone);
        try {
            q.getFirst();
            return true;
        } catch (com.parse.ParseException e) {
            System.out.println("error finding user");
            return false;
        }
    }

    static String arrangeNumberWithCountry(String phoneNumber, Context context) {
        String countryLetters = getUserCountry(context);
        String ret = formatToE164(phoneNumber, countryLetters);
        return ret;
    }

    public static String formatToE164(String phone, String userCountry) {
        if (phone == null) {
            return null;
        }
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber numberProto = null;
        try {
            numberProto = phoneUtil.parse(phone, userCountry);
        } catch (NumberParseException e) {
            System.err.println("NumberParseException was thrown: " + e.toString());
        }
        String formatted = null;
        if (numberProto != null) {
            formatted = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
        }
        return (formatted != null ? formatted : phone.replaceAll("[^0-9+]+", ""));
    }

    /**
     * Get ISO 3166-1 alpha-2 country code for this device (or <code>null</code> if not available).
     *
     * @return country code or <code>null</code>.
     */
    static String getUserCountry(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String simCountry = tm.getSimCountryIso();
            if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
                return simCountry.toUpperCase(Locale.US);
            } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                    return networkCountry.toUpperCase(Locale.US);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }


    private String getPhoneNumber(String name) {
        String ret = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like'" + name + "'";
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);
        if (c.moveToFirst()) {
            ret = c.getString(0);
        }
        c.close();
        if (ret == null) {
            ret = "Unsaved";
        }
        return ret;
    }

    private NotificationManager mNotificationManager;

    /**
     * Synchronize the status of the other end
     *
     * @param pushCode to deliver to the other end
     */
    private void sendPushResponseToAdmin(final int pushCode) {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        if (adminPhone == null) {
            return;
        }
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject();

            jsonObject.put(PUSH_CODE, pushCode);
            jsonObject.put("adminPhone", adminPhone);
            jsonObject.put("senderPhone", myPhone);

            ParsePush push = new ParsePush();
            push.setChannel(LoginActivity.USER_CHANNEL_PREFIX + adminPhone.replaceAll("[^0-9]+", ""));
            push.setData(jsonObject);
            push.sendInBackground(new SendCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Push not sent: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();// REMOVE: 15/09/2015
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getNameByPhone(String phone) {
        return phone;
    }// TODO: 16/10/2015

    public static final int PICK_CONTACT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_group);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRequestToAll();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        onNavigationItemSelected(null);
        setupSearchView();
    }

    private void sendRequestToAll() {
        for (UserItem item : UserFragment.theList) {

            if (item.getStatus().equals(UserStatus.DOES_NOT_HAVE_APP)) {
                sendSmsRequest(item);
            } else if (item.getStatus().equals(UserStatus.HAS_APP)) {
                sendPushNotification(item);
            }


        }


    }


    private void sendPushNotification(UserItem item) {
        String myPhone = ParseUser.getCurrentUser().getUsername();

        if (adminPhone == null) {
            adminPhone = myPhone;
            createNewTable();
        }

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject();

            jsonObject.put(PUSH_CODE, PUSH_CODE_CONFIRM_NOTIFICATION);
            jsonObject.put("adminPhone", adminPhone);
            jsonObject.put("senderPhone", myPhone);

            ParsePush push = new ParsePush();
            push.setChannel(LoginActivity.USER_CHANNEL_PREFIX + item.getNumber().replaceAll("[^0-9]+", ""));
            push.setData(jsonObject);
            push.sendInBackground(new SendCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Push not sent: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();// REMOVE: 15/09/2015
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void createNewTable() {
        adminGroup = new Group();
        adminGroup.setAdmin(adminPhone);
        try {
            adminGroup.save();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    private void sendSmsRequest(UserItem item) {
        SmsManager smsManager = SmsManager.getDefault();
        String message = "Join GroupLock, " +
                "\nAdmin phone:" + adminPhone; // TODO: 15/10/2015 url
        smsManager.sendTextMessage(item.getNumber(), null, message, null, null);
        Toast.makeText(getApplicationContext(), "SMS Sent!",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.search_badge) { //// TODO: 13/10/2015 continue from here

        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_group, menu);
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;
        if (item == null) {
            fragment = UserFragment.newInstance("A", "B");
        } else {
            // Handle navigation view item clicks here.
            int id = item.getItemId();
//        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), item.getTitle() + " clicked", Snackbar.LENGTH_SHORT);
            if (id == R.id.nav_camara) {
                fragment = UserFragment.newInstance("A", "B");
            } else if (id == R.id.nav_gallery) {
                fragment = HistoryFragment.newInstance("C", "D");
            } else if (id == R.id.nav_slideshow) {

            } else if (id == R.id.nav_manage) {


            } else if (id == R.id.nav_share) {

            } else if (id == R.id.nav_send) {

            }
        }
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
//            transaction.addToBackStack(null);
            transaction.replace(R.id.content_main, fragment);
            transaction.commit();


            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

        }
        return true;
    }

    @Override
    public void onFragmentInteraction(String id) {

    }
}
