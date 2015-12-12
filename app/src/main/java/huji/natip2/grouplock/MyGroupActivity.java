/**
 * todo:
 * 1. 2 people
 * + 2. update locked phone after push lock
 * 3. maps, parse geo location
 * + 4. history
 * 5. images
 * ! 6. group merge
 * <p/>
 * <p/>
 * <p/>
 * <p/>
 * dont:
 * 1. splash screen (+ lock)
 * 2. phone- no contact phone
 * 3. group picture
 */


package huji.natip2.grouplock;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
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
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MyGroupActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, HistoryFragment.OnFragmentInteractionListener, UserFragment.OnFragmentInteractionListener {


    final static int MAIN_FRAGMENT_INDEX = 0;

    final static int PUSH_CODE_CONFIRM_NOTIFICATION = 100;
    final static int PUSH_CODE_UPDATE_LIST_FROM_PARSE = 101;
    final static int PUSH_CODE_QUIT = 102;
    final static int PUSH_CODE_CONFIRM_UNLOCK = 103;

    static final String PUSH_CODE_EXTRA = "pushCode";

    final static int PUSH_CODE_NO_CODE = -10;
    final static int PUSH_CODE_ACCEPTED = 0;
    final static int PUSH_CODE_REJECTED = 1;
    final static int PUSH_CODE_NOT_SPECIFIED = 2;
    final static int PUSH_RESPONSE_CODE_ACCEPTED = 3;
    final static int PUSH_RESPONSE_CODE_REJECTED = 4;
    static final int PUSH_ADMIN_LOCK = 6;
    static final int PUSH_ADMIN_UNLOCK = 7;
    static final int PUSH_CODE_UNLOCK_ACCEPTED = 8;
    static final int PUSH_CODE_UNLOCK_REJECTED = 9;
    static final int PUSH_CODE_UNLOCK_NOT_SPECIFIED = 10;
    static final int PUSH_RESPONSE_CODE_UNLOCK_ACCEPTED = 11;
    static final int PUSH_CODE_CLEAR_REQUEST = 12;
    static final int PUSH_ADMIN_CLEAR = 13;
    static final int PUSH_CODE_CLEAR_NOT_SPECIFIED = 14;
    static final int PUSH_CODE_CLEAR_ACCEPTED = 15;
    static final int PUSH_CODE_CLEAR_REJECTED = 16;


    private static final int TO_RESPONSE_CONVERT_ADDITION = 3;

    // broadcast receiver actions
    static final int NO_ACTION = -1;
    static final int ACTION_LOCK = 1;
    static final int ACTION_UNLOCK = 2;
    static final int ACTION_UPDATE = 3;
    static final int ACTION_REQUEST_UNLOCK = 4;
    static final int ACTION_INCREMENT_UNLOCK_ACCEPTED_COUNT = 5;
    static final int ACTION_UNLOCK_ALL = 6;
    static final int ACTION_CLEAR = 7;
    static final String ACTION_CODE_EXTRA = "actionCode";


    static boolean isAdmin;


    private BroadcastReceiver receiver = null;

    static Group adminGroup;

    static String adminPhone = null;
    static String groupId = null;

    private SearchView searchView;
    private ImageView closeBtn;
    private String countryCodeChosen = null;
    private FloatingActionButton fab;
    private TextView actionBarTitle;
    private boolean isLocked = false;
    private int unlockAcceptedCount = 0; // TODO: 19/11/2015 make sure the variable is stable, i.e. not 0 after new intent (e.g. notification open)
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ProgressDialog progressDialog;
    private boolean isShowProgress = false;
    private MenuItem destroyMenuItem;
    private MenuItem exitMenuItem;

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (intent.hasExtra("countryCodeChosen")) {
            countryCodeChosen = intent.getStringExtra("countryCodeChosen");
        }
        if (ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED.equals(intent.getAction())) {
            //handles suggestion clicked query
            Cursor phoneCursor = getContentResolver().query(intent.getData(), null, null, null, null);
            phoneCursor.moveToFirst();
            int idDisplayName = phoneCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            String name = phoneCursor.getString(idDisplayName);
            phoneCursor.close();
            String phoneNumber = getPhoneNumber(name);
            addToListAdapter(name, phoneNumber);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) { // REMOVE: 14/09/2015
            // Other search query - next pressed
            // TODO: 18/10/2015 only phone
            String phoneNumber = searchView.getQuery().toString();
            addToListAdapter(null, phoneNumber);
        }
    }

    private void addToListAdapter(String name, String phoneNumber) {
        if (countryCodeChosen == null) {
            countryCodeChosen = ParseUser.getCurrentUser().getString("countryCodeChosen");
        }
        phoneNumber = arrangeNumberWithCountry(phoneNumber, countryCodeChosen);
        if (phoneNumber == null) {
            Toast.makeText(MyGroupActivity.this, "Contact bad phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        UserItem user = new UserItem(name, phoneNumber, doesUserHaveApp(phoneNumber));
        UserFragment.addPerson(user);
        closeSearchView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update view and status
        isLocked = AppLockService.isServiceRunning();
        updateView();

        Intent intent = getIntent();

        if (intent.hasExtra(PUSH_CODE_EXTRA)) {
            int pushCode = intent.getIntExtra(PUSH_CODE_EXTRA, PUSH_CODE_NO_CODE);
            adminPhone = intent.getStringExtra("adminPhone");
            // make sure the options are updated
            supportInvalidateOptionsMenu();

            String senderPhone = intent.getStringExtra("senderPhone");
            groupId = intent.getStringExtra("groupId");
            switch (pushCode) {
                case PUSH_CODE_ACCEPTED:
                    isShowProgress = true;
                case PUSH_CODE_REJECTED:
                case PUSH_CODE_UNLOCK_ACCEPTED:
                    sendUnlockResponse(senderPhone, pushCode + TO_RESPONSE_CONVERT_ADDITION);
                    break;
                case PUSH_CODE_CLEAR_ACCEPTED:
                    removeUserFromList(senderPhone);
                    break;
                case PUSH_CODE_UNLOCK_REJECTED:
                    // TODO: 11/12/2015 ?
                    break;
                case PUSH_CODE_NOT_SPECIFIED:
                    showConfirmDialog();
                    break;
                case PUSH_CODE_UNLOCK_NOT_SPECIFIED:
                    showUnlockConfirmDialog(senderPhone);
                    break;

            }
            intent.removeExtra(PUSH_CODE_EXTRA);
        }
        if (intent.hasExtra(MyPushReceiver.INTENT_EXTRA_NOTIFICATION_TAG)) {
            String tag = intent.getStringExtra(MyPushReceiver.INTENT_EXTRA_NOTIFICATION_TAG);
            int id = intent.getIntExtra(MyPushReceiver.INTENT_EXTRA_NOTIFICATION_ID, MyPushReceiver.NOTIFICATION_ID);
            if (MyPushReceiver.NOTIFICATION_ID == id && MyPushReceiver.NOTIFICATION_TAG.equals(tag)) {
                // dismiss notification
                NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
                manager.cancel(tag, id);
            }
            intent.removeExtra(MyPushReceiver.INTENT_EXTRA_NOTIFICATION_TAG);
        }
    }

    private void removeUserFromList(String phone) {
        int index = -1;
        UserItem item = null;
        for (int i = 0; i < UserFragment.localList.size(); i++) {
            item = UserFragment.localList.get(i);
            if (item.getPhone().equals(phone)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            UserFragment.removeUser(item, index);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (isShowProgress) {
            isShowProgress = false;
            progressDialog = ProgressDialog.show(MyGroupActivity.this, null,
                    "Loading group", true, false);
        }
    }

    private void incrementUnlockAcceptedCount(boolean isFromAdmin) {
        unlockAcceptedCount++;
        if (isFromAdmin || unlockAcceptedCount > 2) {
            unlock();
            updateSingleUserInParse(UserFragment.myUserItem);
            unlockAcceptedCount = 0;
        }
    }


    private void lockAll() {
        if (!isAdmin()) {
            return;
        }
        List<UserItem> modifiedUsers = new ArrayList<>();
        //String myPhone = ParseUser.getCurrentUser().getUsername(); //netanel changed

        // Lock myself (group admin)
        lock();
        modifiedUsers.add(UserFragment.myUserItem);

        int numLocked = 0;
        int numNotiSent = 0;
        int numSMSSent = 0;

        // Lock all verified
        for (UserItem item : UserFragment.localList) {
            if (item.getStatus().equals(UserStatus.VERIFIED)) {
                item.setStatus(UserStatus.LOCKED);
                // Sends a push for a user to lock himself
                sendPush(item.getPhone(), adminPhone, MyGroupActivity.PUSH_ADMIN_LOCK);
                numLocked++;
                modifiedUsers.add(item);
            } else if (item.getStatus().equals(UserStatus.DOES_NOT_HAVE_APP)) {
                sendSmsRequest(item);
                numSMSSent++;
            } else if (item.getStatus().equals(UserStatus.HAS_APP)) {
                sendPushNotification(item);
                numNotiSent++;
            }
        }
        if (numLocked > 0) {
            Toast.makeText(MyGroupActivity.this, "Me and " + numLocked + " others are locked", Toast.LENGTH_SHORT).show();
        }
        if (numNotiSent > 0) {
            Toast.makeText(MyGroupActivity.this, numNotiSent + " request(s) sent", Toast.LENGTH_SHORT).show();
        }
        if (numSMSSent > 0) {
            Toast.makeText(MyGroupActivity.this, numSMSSent + " SMS invite(s) sent", Toast.LENGTH_SHORT).show();
        }
        updateUsersInParse(modifiedUsers);
    }

    // TODO: 28/11/2015 remove if no admin privileges
    private void unlockAll() {
        if (!isAdmin()) {
            return;
        }
        List<UserItem> modifiedUsers = new ArrayList<>();
        // Unlock myself (group admin)
        unlock();
        modifiedUsers.add(UserFragment.myUserItem);

        int numUnlocked = 0;
        // Unlock all locked
        for (UserItem item : UserFragment.localList) {
            if (item.getStatus().equals(UserStatus.LOCKED)) {
                item.setStatus(UserStatus.VERIFIED);
                // Sends a push for a user to unlock himself
                sendPush(item.getPhone(), adminPhone, MyGroupActivity.PUSH_ADMIN_UNLOCK);
                numUnlocked++;
                modifiedUsers.add(item);
            }
        }
        if (numUnlocked > 0) {
            Toast.makeText(MyGroupActivity.this, "Me and " + numUnlocked + " others are unlocked", Toast.LENGTH_SHORT).show();
        }
        updateUsersInParse(modifiedUsers);
    }

    private void clearAll() {
        if (!isAdmin()) {
            return;
        }
        adminGroup.setActive(false);
        String myPhone = ParseUser.getCurrentUser().getUsername();
        int numRemoved = 0;
        for (UserItem item : UserFragment.localList) {
            // Sends a push for a user to clear his local list and unlock
            if (!item.getPhone().equals(myPhone)) {
                sendPush(item.getPhone(), adminPhone, MyGroupActivity.PUSH_ADMIN_CLEAR);
                numRemoved++;
            }
        }
        if (numRemoved > 0) {
            Toast.makeText(MyGroupActivity.this, numRemoved + " users removed", Toast.LENGTH_SHORT).show();
        }
    }

    void updateView() {
        updateLockIcon();
        updateFab();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    void updateLockIcon() {
        if (isLocked) {
            actionBarTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_lock_white_36dp, 0);
            toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUnlockPushDialog();
                }
            });
        } else {
            actionBarTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_lock_open_white_36dp_light, 0);
            toolbar.setOnClickListener(null);
        }
    }

    private void lock() {
        isLocked = true;
        if (UserFragment.myUserItem != null) {
            UserFragment.myUserItem.setStatus(UserStatus.LOCKED);
        }
        startLockService();
        updateView();
    }

    private void unlock() {
        isLocked = false;
        if (UserFragment.myUserItem != null) {
            UserFragment.myUserItem.setStatus(UserStatus.VERIFIED);
        }
        stopLockService();
        updateView();
    }

    private void startLockService() {
        Intent lockServiceIntent = new Intent(MyGroupActivity.this, AppLockService.class);
        startService(lockServiceIntent);
    }

    private void stopLockService() {
        Intent lockServiceIntent = new Intent(MyGroupActivity.this, AppLockService.class);
        stopService(lockServiceIntent);
    }


    protected void updateFab() {
        if (adminPhone == null || isAdmin()) {
            // TODO: 20/10/2015 change to send :
            fab.setVisibility(View.VISIBLE);
        } else {
            fab.setVisibility(View.GONE);
            return;
        }

        int numVerified = 0;
        int numNotSent = 0;
        for (UserItem item : UserFragment.localList) {
            UserStatus status = item.getStatus();
            if (status.equals(UserStatus.VERIFIED)) {
                numVerified++;
            } else if (!status.equals(UserStatus.WAIT_FOR_VERIFICATION)) {
                numNotSent++;
            }
        }

        if (isLocked) {
            // -> unlock all button
            fab.setImageResource(R.drawable.ic_lock_open_white_24dp);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    unlockAll();
                }
            });
        } else if (numVerified > 0) {
            // -> lock all button
            fab.setImageResource(R.drawable.ic_lock_white_24dp);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    lockAll();
                }
            });
        } else if (numNotSent > 0) {
            // -> send all button
            fab.setImageResource(R.drawable.ic_send_white_24dp);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRequestToAll();
                }
            });
        } else {
            // -> add person button
            fab.setImageResource(R.drawable.ic_person_add_white_24dp);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchView.setIconified(false);
                }
            });
        }
    }


    private void showConfirmDialog() {
        final AlertDialog.Builder b = new AlertDialog.Builder(MyGroupActivity.this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        String message = "Join the group?\n\t" + getNameByPhone(getApplicationContext(), adminPhone) + " invites you";
        b.setMessage(message);
        b.setPositiveButton("Join", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                progressDialog = ProgressDialog.show(MyGroupActivity.this, null,
                        "Loading group", true, false);
                sendPushToAdmin(PUSH_RESPONSE_CODE_ACCEPTED);
            }
        });
        b.show();
    }

    private void showUnlockConfirmDialog(final String senderPhone) {
        final AlertDialog.Builder b = new AlertDialog.Builder(MyGroupActivity.this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        String message = getNameByPhone(getApplicationContext(), senderPhone) + " wants to unlock himself?";
        b.setMessage(message);
        b.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sendUnlockResponse(senderPhone, PUSH_RESPONSE_CODE_UNLOCK_ACCEPTED);
            }
        });
        b.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
//                sendUnlockResponse(senderPhone,PUSH_RESPONSE_CODE_UNLOCK_REJECTED); // TODO: 11/12/2015 remove
            }
        });
        b.show();
    }


    static UserStatus doesUserHaveApp(String phoneNumber) {
        return userExist(phoneNumber) ? UserStatus.HAS_APP : UserStatus.DOES_NOT_HAVE_APP;
    }

    static boolean userExist(String phone) {
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
        return formatToE164(phoneNumber, countryLetters);
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
        if (c != null) {
            if (c.moveToFirst()) {
                ret = c.getString(0);
            }
            c.close();
        }
        if (ret == null) {
            ret = "Unsaved";
        }
        return ret;
    }

    static String getNameByPhone(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_group);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBarTitle = (TextView) findViewById(R.id.toolbar_title);
        actionBarTitle.setCompoundDrawablePadding(25);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int actionCode = intent.getIntExtra(ACTION_CODE_EXTRA, NO_ACTION);
                switch (actionCode) {
                    case ACTION_LOCK:
                        lock();
//                        MyGroupActivity.updateSingleUserInParse(adminPhone, groupId, UserFragment.myUserItem);
                        break;

                    case ACTION_UNLOCK:
                        unlock();
//                        updateSingleUserInParse(adminPhone, groupId, UserFragment.myUserItem);
                        break;

                    case ACTION_CLEAR:
                        clearGroup();
//                        updateSingleUserInParse(adminPhone, groupId, UserFragment.myUserItem);
                        break;

                    case ACTION_UNLOCK_ALL:
                        unlockAll();
//                        updateSingleUserInParse(adminPhone, groupId, UserFragment.myUserItem);
                        break;

                    case ACTION_UPDATE:
                        updateView();
                        break;

                    case ACTION_REQUEST_UNLOCK:
                        sendUnlockRequest();
                        break;

                    case ACTION_INCREMENT_UNLOCK_ACCEPTED_COUNT:
                        String senderPhone = intent.getStringExtra("senderPhone");
                        incrementUnlockAcceptedCount(senderPhone.equals(adminPhone));
                        break;

                    default:
                        break;

                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("huji.natip2.grouplock.MyGroupActivity"));

        fab = (FloatingActionButton) findViewById(R.id.fab);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        onNavigationItemSelected(null);
        setupSearchView();
    }

    private void showUnlockPushDialog() {
        final AlertDialog.Builder b = new AlertDialog.Builder(MyGroupActivity.this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        String message = "Ask group for unlock?";
        b.setMessage(message);
        b.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sendUnlockRequest();
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    boolean isAdmin() {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        isAdmin = myPhone.equals(adminPhone);
        return isAdmin;
    }

    private void sendRequestToAll() {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        if (adminPhone == null) {
            adminPhone = myPhone;
            createNewTable();
            supportInvalidateOptionsMenu();
        }
        int numNotiSent = 0;
        int numSMSSent = 0;
        for (UserItem item : UserFragment.localList) {
            if (!item.getPhone().equals(myPhone)) {
                if (item.getStatus().equals(UserStatus.DOES_NOT_HAVE_APP)) {
                    sendSmsRequest(item);
                    numSMSSent++;
                } else if (item.getStatus().equals(UserStatus.HAS_APP)) {
                    sendPushNotification(item);
                    numNotiSent++;
                }
            }
        }
        if (numNotiSent > 0) {
            Toast.makeText(MyGroupActivity.this, numNotiSent + " request(s) sent", Toast.LENGTH_SHORT).show();
        }
        if (numSMSSent > 0) {
            Toast.makeText(MyGroupActivity.this, numSMSSent + " SMS invite(s) sent", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendUnlockRequest() {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        for (UserItem item : UserFragment.localList) {
            if (item.getStatus().equals(UserStatus.LOCKED) && !item.getPhone().equals(myPhone)) {
                sendPush(item.getPhone(), myPhone, PUSH_CODE_CONFIRM_UNLOCK);
            }
        }
    }


    static void updateSingleUserInParse(UserItem modifiedUser) {
        List<UserItem> modifiedUsers = new ArrayList<>();
        modifiedUsers.add(modifiedUser);
        updateUsersInParse(modifiedUsers);
    }

    static void updateUsersInParse(final List<UserItem> modifiedUsers) {
        // add current person to parse
        ParseQuery<Group> query = Group.getQuery();
        query.whereEqualTo("objectId", groupId);
        query.getFirstInBackground(new GetCallback<Group>() {
            @Override
            public void done(final Group group, ParseException e) {
                for (UserItem user : modifiedUsers) {
                    if (user == null) {
                        continue;
                    }
                    group.putParticipant(user.getPhone(), user.getStatus());
                }
                group.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        broadcastChange(group);
                    }
                });
            }
        });
    }


    /**
     * Tell everyone (and myself) to update local list from parse
     *
     * @param group current group
     */
    static void broadcastChange(Group group) {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        List<Object> participants = group.getParticipantsPhone();
        List<Object> statuses = group.getParticipantsStatus();
        for (int i = 0; i < participants.size(); i++) {

            String phone = (String) participants.get(i);
            UserStatus status = UserStatus.valueOf((String) statuses.get(i));
            if (!status.equals(UserStatus.REMOVED)) {// REMOVE: 28/11/2015
                sendPush(phone, myPhone, PUSH_CODE_UPDATE_LIST_FROM_PARSE);
            }

        }
    }

    private void sendPushNotification(UserItem item) {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        sendPush(item.getPhone(), myPhone, PUSH_CODE_CONFIRM_NOTIFICATION);
    }

    /**
     * Sends a Push Notification.
     *
     * @param receiverPhone channel TO address
     * @param senderPhone   extra
     * @param pushCode      extra
     */
    static void sendPush(String receiverPhone, String senderPhone, int pushCode) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject();
            jsonObject.put(MyGroupActivity.PUSH_CODE_EXTRA, pushCode);
            jsonObject.put("adminPhone", adminPhone);
            jsonObject.put("groupId", groupId);
            jsonObject.put("senderPhone", senderPhone);

            ParsePush push = new ParsePush();
            push.setChannel(LoginActivity.USER_CHANNEL_PREFIX + receiverPhone.replaceAll("[^0-9]+", ""));
            push.setData(jsonObject);
            push.sendInBackground();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Synchronize the status of the other end
     *
     * @param pushCode to deliver to the other end
     */
    private void sendPushToAdmin(final int pushCode) {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        if (adminPhone == null) {
            return;
        }
        sendPush(adminPhone, myPhone, pushCode);
    }

    private void sendUnlockResponse(String receiverPhone, final int pushCode) {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        if (adminPhone == null) {
            return;
        }
        unlockAcceptedCount = 0;
        sendPush(receiverPhone, myPhone, pushCode);
    }

    static void createNewTable() {
        isAdmin = true;
        adminGroup = new Group();
        adminGroup.setAdmin(adminPhone);
        adminGroup.setActive(true);
        addAdminToList();
        try {
            adminGroup.save();
            groupId = adminGroup.getObjectId();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    static void addAdminToList() {
        adminGroup.putParticipant(adminPhone, UserStatus.VERIFIED);
        adminGroup.saveInBackground();
        UserFragment.myUserItem = new UserItem("Group Admin", adminPhone, UserStatus.VERIFIED);
        UserFragment.localList.add(UserFragment.myUserItem);
        UserFragment.userAdapter.notifyDataSetChanged();
    }


    private void sendSmsRequest(UserItem item) {
        SmsManager smsManager = SmsManager.getDefault();
        String message = "Join GroupLock, " +
                "\nAdmin phone:" + adminPhone; // TODO: 15/10/2015 url
        smsManager.sendTextMessage(item.getPhone(), null, message, null, null);
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
/*        if (id == R.id.action_settings) {
            return true;
        }*/
        if (id == R.id.send_to_all) {
            sendRequestToAll();
            return true;
        }
        if (id == R.id.admin_destroy_group) {
            clearAll();
            clearGroup();
            return true;
        }
        if (id == R.id.clear_and_exit) {
            exitFromGroup();

            return true;
        }
        if (id == R.id.about_app) {
            TextView content = (TextView) getLayoutInflater().inflate(R.layout.about_view, null);
            content.setMovementMethod(LinkMovementMethod.getInstance());
            content.setText(Html.fromHtml(getString(R.string.about_body)));
            new AlertDialog.Builder(this)
                    .setTitle(R.string.action_about_app)
                    .setView(content)
                    .setInverseBackgroundForced(true)// FIXME: 06/09/2015
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private void exitFromGroup() {
        final AlertDialog.Builder b = new AlertDialog.Builder(MyGroupActivity.this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        String message = "Ask admin to exit? (unlock if necessary)";
        b.setMessage(message);
        b.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sendPushToAdmin(PUSH_CODE_CLEAR_REQUEST);
            }
        });
        b.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
//                sendUnlockResponse(senderPhone,PUSH_RESPONSE_CODE_UNLOCK_REJECTED); // TODO: 11/12/2015 remove
            }
        });
        b.show();
    }

    private void clearGroup() {
        unlock();
        adminPhone = null;
        groupId = null;
        UserFragment.localList.clear();
        recreate();
    }

    private void updateMenuItemsVisibility() {
        destroyMenuItem.setVisible(isAdmin || adminPhone == null);
        exitMenuItem.setVisible(!isAdmin || adminPhone == null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_group, menu);
        destroyMenuItem = menu.findItem(R.id.admin_destroy_group);
        exitMenuItem = menu.findItem(R.id.clear_and_exit);

        updateMenuItemsVisibility();

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment;
        if (item == null) {
            fragment = UserFragment.newInstance("A", "B");
        } else {
            // Handle navigation view item clicks here.
            fragment = chooseFragmentById(item.getItemId());
//        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), item.getTitle() + " clicked", Snackbar.LENGTH_SHORT);

        }
        openFragment(fragment);
        return true;
    }

    Fragment chooseFragmentById(int id) {
        if (id == R.id.nav_main) {
            return UserFragment.newInstance("A", "B");
        } else if (id == R.id.nav_history) {
            return HistoryFragment.newInstance("C", "D");
        }/* else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {


        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/
        return null;
    }

    void chooseDrawerItem(int id, int index) {
        navigationView.getMenu().getItem(index).setChecked(true);
        openFragment(chooseFragmentById(id));
    }

    void openFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
//            transaction.addToBackStack(null);
            transaction.replace(R.id.content_main, fragment);
            transaction.commit();


            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

        }
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
        searchAutoComplete.setDropDownHorizontalOffset(-100);
        searchAutoComplete.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                CharSequence query = searchView.getQuery();
                if (query == null || query.toString().equals("")) {
                    closeSearchView();
                    return true;
                } else {
                    return false;
                }
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
            searchButton.setImageResource(R.drawable.ic_person_add_white_24dp);

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

    static String getDisplayName(Context context, String phone) {
        String name = getNameByPhone(context, phone);
        if (name == null) {
            name = phone;
        } else {
            name += " (" + phone + ")";
        }
        return name;
    }
}
