/**
 * todo:
 * 1. 2 people
 * 2. update locked phone after push lock
 * 3. maps, parse geo location
 * 4. history

 *
 * dont:
 * 1. splash screen (+ lock)
 * 2. phone- no contact phone
 * 3. group picture
 */


package huji.natip2.grouplock;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;


public class MyGroupActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, HistoryFragment.OnFragmentInteractionListener, UserFragment.OnFragmentInteractionListener {


    final static int PUSH_CODE_CONFIRM_NOTIFICATION = 10;
    final static int PUSH_CODE_UPDATE_LIST_FROM_PARSE = 11;
    final static int PUSH_CODE_QUIT = 12;

    static final String PUSH_CODE = "pushCode";

    final static int PUSH_CODE_NO_CODE = -10;
    final static int PUSH_CODE_ACCEPTED = 0;
    final static int PUSH_CODE_REJECTED = 1;
    final static int PUSH_CODE_NOT_SPECIFIED = 2;
    final static int PUSH_RESPONSE_CODE_ACCEPTED = 3;
    final static int PUSH_RESPONSE_CODE_REJECTED = 4;
    static final int PUSH_ADMIN_LOCK = 6;

    final static int PUSH_RESPONSE_CODE_NOT_SPECIFIED = 5;
    private static final int TO_RESPONSE_CONVERT_ADDITION = 3;
    public Group adminGroup;
    private int pushCode = PUSH_CODE_NO_CODE;


    public String adminPhone = null;
    public String groupId = null;

    private SearchView searchView;
    private ImageView closeBtn;
    private String countryCodeChosen = null;
    private FloatingActionButton fab;

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (intent.hasExtra("countryCodeChosen")) {
            countryCodeChosen = intent.getStringExtra("countryCodeChosen");
        }
        if (intent.hasExtra(MyPushReceiver.INTENT_EXTRA_NOTIFICATION_TAG)) {
            String tag = intent.getStringExtra(MyPushReceiver.INTENT_EXTRA_NOTIFICATION_TAG);
            int id = intent.getIntExtra(MyPushReceiver.INTENT_EXTRA_NOTIFICATION_ID, MyPushReceiver.NOTIFICATION_ID);
            if (MyPushReceiver.NOTIFICATION_ID == id && MyPushReceiver.NOTIFICATION_TAG.equals(tag)) {
                // dismiss notification
                NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
                manager.cancel(tag, id);
            }
        }
        if (ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED.equals(intent.getAction())) {
            //handles suggestion clicked query
            addToListAdapter(intent);
            closeSearchView();
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) { // REMOVE: 14/09/2015
            // Other search query - next pressed
            // TODO: 18/10/2015 only phone
            String phoneManualy = "lal";
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


//            fab.setImageDrawable(getDrawable(R.drawable.ic_send_white_24dp));
        changeFabIcon();
        Intent intent = getIntent();
        if (intent.hasExtra(PUSH_CODE)) {
            pushCode = intent.getIntExtra(PUSH_CODE, PUSH_CODE_NO_CODE);
            adminPhone = intent.getStringExtra("adminPhone");
            groupId = intent.getStringExtra("groupId");
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
        if (adminPhone == null || isAdmin()) {
             // TODO: 20/10/2015 change to send :

            fab.setVisibility(View.VISIBLE);
        }else{
            fab.setVisibility(View.GONE);
        }
    }
    private void sendPushLockToAll(){
        for (UserItem item : UserFragment.theList){
            if (item.getStatus().equals(UserStatus.VERIFIED) && !item.getNumber().equals(adminPhone)) {
                sendPush(item.getNumber(),adminPhone,adminPhone,groupId,MyGroupActivity.PUSH_ADMIN_LOCK);
            }
        }
    }

    protected void changeFabIcon() {
        if(hasMoreThenVerify()) {
            int id = getResources().getIdentifier("huji.natip2.grouplock:drawable/" + "ic_send_white_24dp", null, null);

//            fab.setImageDrawable(getDrawable(R.drawable.ic_send_white_24dp));
            fab.setImageResource(id);
        }
    }

    private boolean hasMoreThenVerify() {
        for (UserItem item :  UserFragment.theList){
            if (item.getStatus().equals(UserStatus.VERIFIED)){
                return true;
            }
        }
        return false;
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
        if (countryCodeChosen == null) {
            countryCodeChosen = ParseUser.getCurrentUser().getString("countryCodeChosen");
        }
        phoneNumber = arrangeNumberWithCountry(phoneNumber, countryCodeChosen);
        if (phoneNumber == null) {
            Toast.makeText(MyGroupActivity.this, "Contact bad phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(MyGroupActivity.this, "NAME" + name + " num" + phoneNumber, Toast.LENGTH_SHORT).show();
        UserItem user = new UserItem(name, phoneNumber, doesUserHaveApp(phoneNumber));
        UserFragment.addperson(user);
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

    static String arrangeNumberWithCountry(String phoneNumber, String countryLetters) {
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
            jsonObject.put("groupId", groupId);
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

        fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasMoreThenVerify()) {
                    sendPushLockToAll();
                } else {
                    sendRequestToAll(); // FIXME: 20/10/2015
                }
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

    boolean isAdmin() {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        return myPhone.equals(adminPhone);
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

    static void broadcastChange(Group group, String adminPhone, String groupId) {
        List<Object> participants = group.getParticipantsPhone();
        for (Object phoneObj : participants) {
            String phone = (String) phoneObj;
            if(!phone.equals(adminPhone)) {
                sendPush(phone, adminPhone, adminPhone, groupId, PUSH_CODE_UPDATE_LIST_FROM_PARSE);
            }
        }
    }

    private void sendPushNotification(UserItem item) {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        if (adminPhone == null) {
            adminPhone = myPhone;
            createNewTable();
        }
        sendPush(item.getNumber(), myPhone, adminPhone, groupId, PUSH_CODE_CONFIRM_NOTIFICATION);
    }

    /**
     * Sends a Push Notification.
     *
     * @param receiverPhone channel TO address
     * @param senderPhone   extra
     * @param adminPhone    extra
     * @param groupId       extra
     * @param pushCode      extra
     */
    static void sendPush(String receiverPhone, String senderPhone, String adminPhone, String groupId, int pushCode) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject();
            jsonObject.put(MyGroupActivity.PUSH_CODE, pushCode);
            jsonObject.put("adminPhone", adminPhone);
            jsonObject.put("groupId", groupId);
            jsonObject.put("senderPhone", senderPhone);

            ParsePush push = new ParsePush();
            push.setChannel(LoginActivity.USER_CHANNEL_PREFIX + receiverPhone.replaceAll("[^0-9]+", ""));
            push.setData(jsonObject);
            push.sendInBackground(new SendCallback() {
                @Override
                public void done(ParseException e) {
                    // TODO: 18/10/2015
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void createNewTable() {
        adminGroup = new Group();
        adminGroup.setAdmin(adminPhone);
        addAdminToList();
        try {
            adminGroup.save();
            groupId = adminGroup.getObjectId();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void addAdminToList() {
        adminGroup.putParticipant(adminPhone, UserStatus.VERIFIED);
        adminGroup.saveInBackground();
        UserFragment.theList.add(new UserItem(getString(R.string.group_admin_name), adminPhone, UserStatus.VERIFIED));
        UserFragment.adapterTodo.notifyDataSetChanged();
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
        if (id == R.id.search_badge) {
            sendRequestToAll();
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


    private void setupSearchView() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) findViewById(R.id.searchView);
        SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
        searchView.setSearchableInfo(searchableInfo);
        setSearchIcons();
        setSearchTextColors();
    }

    /**
     * Changes the hint and text colors of the <code>SearchView</code>.
     */
    private void setSearchTextColors() {
        LinearLayout linearLayout1 = (LinearLayout) searchView.getChildAt(0);
        LinearLayout linearLayout2 = (LinearLayout) linearLayout1.getChildAt(2);
        LinearLayout linearLayout3 = (LinearLayout) linearLayout2.getChildAt(1);
        AutoCompleteTextView searchAutoComplete = (AutoCompleteTextView) linearLayout3.getChildAt(0);
        //Set the input text color
        searchAutoComplete.setTextColor(Color.WHITE);
        // set the hint text color
        searchAutoComplete.setHintTextColor(Color.WHITE);
        //Some drawable (e.g. from xml)
        searchAutoComplete.setDropDownBackgroundResource(R.drawable.search_autocomplete_dropdown);
        searchAutoComplete.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                closeSearchView();
                return true;

            }
        });
    }

    private void closeSearchView() {
        closeBtn.performClick();
        closeBtn.performClick();
    }

    /**
     * Changes the icons of the <code>SearchView</code>, using Java's reflection.
     */
    private void setSearchIcons() {
        try {
            Field searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            closeBtn = (ImageView) searchField.get(searchView);
            closeBtn.setImageResource(R.drawable.ic_close_white_24dp);

            searchField = SearchView.class.getDeclaredField("mVoiceButton");
            searchField.setAccessible(true);
            ImageView voiceBtn = (ImageView) searchField.get(searchView);
            voiceBtn.setImageResource(R.drawable.ic_keyboard_voice_white_24dp);

            searchField = SearchView.class.getDeclaredField("mSearchButton");
            searchField.setAccessible(true);
            ImageView searchButton = (ImageView) searchField.get(searchView);
            searchButton.setImageResource(R.drawable.ic_search_white_24dp);

            // Accessing the SearchAutoComplete
            int queryTextViewId = getResources().getIdentifier("android:id/search_src_text", null, null);
            View autoComplete = searchView.findViewById(queryTextViewId);

            Class<?> clazz = Class.forName("android.widget.SearchView$SearchAutoComplete");

            SpannableStringBuilder stopHint = new SpannableStringBuilder("   ");
            stopHint.append(getString(R.string.findContact));

            // Add the icon as an spannable
            Drawable searchIcon = getResources().getDrawable(R.drawable.ic_search_white_24dp);
            Method textSizeMethod = clazz.getMethod("getTextSize");
            Float rawTextSize = (Float) textSizeMethod.invoke(autoComplete);
            int textSize = (int) (rawTextSize * 1.25);
            if (searchIcon != null) {
                searchIcon.setBounds(0, 0, textSize, textSize);
            }
            stopHint.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set the new hint text
            Method setHintMethod = clazz.getMethod("setHint", CharSequence.class);
            setHintMethod.invoke(autoComplete, stopHint);

        } catch (NoSuchFieldException e) {
            Log.e("SearchView", e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e("SearchView", e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
