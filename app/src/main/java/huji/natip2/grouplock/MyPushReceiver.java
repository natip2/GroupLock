package huji.natip2.grouplock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


/**
 * Creates notification for the received debt, which allows the user to add/ignore it
 */
public class MyPushReceiver extends ParsePushBroadcastReceiver {
    static final String NOTIFICATION_TAG = "pushNotification";
    static final String INTENT_EXTRA_NOTIFICATION_TAG = "notificationTag";
    static final String INTENT_EXTRA_NOTIFICATION_ID = "notificationId";
    static final int NOTIFICATION_ID = 10;
    private String adminPhone;
    private String senderPhone;
    private int pushCode;
    private String groupId;

    private Intent broadcastIntent = new Intent("huji.natip2.grouplock.MyGroupActivity");
    private LocalBroadcastManager broadcaster;

    private Context mContext;


    @Override
    public void onPushReceive(final Context context, Intent intent) {
        mContext = context;
        broadcaster = LocalBroadcastManager.getInstance(context);

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(intent.getStringExtra(MyPushReceiver.KEY_PUSH_DATA));
            adminPhone = jsonObject.getString("adminPhone");
            groupId = jsonObject.getString("groupId");
            senderPhone = jsonObject.getString("senderPhone");
            pushCode = jsonObject.getInt(MyGroupActivity.PUSH_CODE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (jsonObject == null) {
            return;
        }
        switch (pushCode) {
            //ask to add to the group
            case MyGroupActivity.PUSH_CODE_CONFIRM_NOTIFICATION:
                showJoinNotification(context);
                break;
            //ask to unlock
            case MyGroupActivity.PUSH_CODE_CONFIRM_UNLOCK:
                showUnlockNotification(context);
                break;
            //the admin updated the list-now is your turn to update your list
            case MyGroupActivity.PUSH_CODE_UPDATE_LIST_FROM_PARSE:
                updateLocalListFromParse();
                break;
            //the admin get push for removing the user from parse
            case MyGroupActivity.PUSH_CODE_QUIT:
                // TODO: 15/10/2015
                break;
            //the admin get this code from the user
            case MyGroupActivity.PUSH_RESPONSE_CODE_ACCEPTED:
            case MyGroupActivity.PUSH_RESPONSE_CODE_REJECTED:
                addSenderToLocalListAndUpdateParseAndBroadcast();
                break;
            case MyGroupActivity.PUSH_RESPONSE_CODE_UNLOCK_ACCEPTED:
                broadcastIntent.putExtra(MyGroupActivity.ACTION_CODE_EXTRA, MyGroupActivity.ACTION_INCREMENT_UNLOCK_ACCEPTED_COUNT);
                broadcaster.sendBroadcast(broadcastIntent);
                break;
            case MyGroupActivity.PUSH_ADMIN_LOCK:
                broadcastIntent.putExtra(MyGroupActivity.ACTION_CODE_EXTRA, MyGroupActivity.ACTION_LOCK);
                broadcaster.sendBroadcast(broadcastIntent);
                break;
            case MyGroupActivity.PUSH_ADMIN_UNLOCK:
                broadcastIntent.putExtra(MyGroupActivity.ACTION_CODE_EXTRA, MyGroupActivity.ACTION_UNLOCK);
                broadcaster.sendBroadcast(broadcastIntent);
                break;
        }
    }


    private void updateLocalListFromParse() {
        ParseQuery<Group> query = Group.getQuery();
        query.whereEqualTo("objectId", groupId);
        query.getFirstInBackground(new GetCallback<Group>() {
            @Override
            public void done(Group group, ParseException e) {
                removeVerifiedUsers();
                List<Object> participantsPhone = group.getParticipantsPhone();
                List<Object> participantsStatus = group.getParticipantsStatus();
                int i = 0;
                for (Object phoneObj : participantsPhone) {
                    String phone = (String) phoneObj;
                    String name = getNameByPhone(phone);
                    UserStatus status = UserStatus.valueOf((String) participantsStatus.get(i));
                    UserItem newItem = new UserItem(name, phone, status);
                    UserFragment.theList.add(newItem);
                    i++;
                }
                UserFragment.userAdapter.notifyDataSetChanged();
                broadcastIntent.putExtra(MyGroupActivity.ACTION_CODE_EXTRA, MyGroupActivity.ACTION_UPDATE);
                broadcaster.sendBroadcast(broadcastIntent);
            }
        });
    }

    private void removeVerifiedUsers() {
        for (UserItem item : UserFragment.theList) {
            if (item.getStatus().equals(UserStatus.VERIFIED) && !item.getNumber().equals(adminPhone)) {
                UserFragment.theList.remove(item);
            }
        }
    }


    private void addSenderToLocalListAndUpdateParseAndBroadcast() {
        final UserStatus status;
        if (pushCode == MyGroupActivity.PUSH_RESPONSE_CODE_ACCEPTED) {
            status = UserStatus.VERIFIED;
        } else {
            status = UserStatus.DENIED;
        }
        UserItem senderItem = new UserItem(getNameByPhone(senderPhone), senderPhone, status);
        boolean isFound = false;
        for (UserItem item : UserFragment.theList) {
            if (item.getNumber().equals(senderItem.getNumber())) {
                isFound = true;
                item.setStatus(status);
                break;
            }
        }
        if (!isFound) {
            UserFragment.theList.add(senderItem);
        }
        UserFragment.userAdapter.notifyDataSetChanged();
        broadcastIntent.putExtra(MyGroupActivity.ACTION_CODE_EXTRA, MyGroupActivity.ACTION_UPDATE);
        broadcaster.sendBroadcast(broadcastIntent);

        // add current person to parse
        ParseQuery<Group> query = Group.getQuery();
        query.whereEqualTo("objectId", groupId);
        query.getFirstInBackground(new GetCallback<Group>() {
            @Override
            public void done(final Group group, ParseException e) {
                group.putParticipant(senderPhone, status);
                group.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        MyGroupActivity.broadcastChange(group, adminPhone, groupId);
                    }
                });
            }
        });

    }


    public void showJoinNotification(Context context) {
        String title = "Join the group?";
        String text = getNameByPhone(adminPhone) + " invites you";

        // prepare intent which is triggered if the
        // notification is selected

        Intent intent = new Intent(context, MyGroupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        intent.putExtra(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_NOT_SPECIFIED);
        intent.putExtra("adminPhone", adminPhone);
        intent.putExtra("groupId", groupId);
        intent.putExtra(INTENT_EXTRA_NOTIFICATION_TAG, NOTIFICATION_TAG);
        intent.putExtra(INTENT_EXTRA_NOTIFICATION_ID, NOTIFICATION_ID);
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);


        Intent intent2 = new Intent(context, MyGroupActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent2.putExtra(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_ACCEPTED);
        intent2.putExtra("adminPhone", adminPhone);
        intent2.putExtra("groupId", groupId);
        intent2.putExtra(INTENT_EXTRA_NOTIFICATION_TAG, NOTIFICATION_TAG);
        intent2.putExtra(INTENT_EXTRA_NOTIFICATION_ID, NOTIFICATION_ID);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent2 = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent2, 0);

        Intent intent3 = new Intent(context, MyGroupActivity.class);
        intent3.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent3.putExtra(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_REJECTED);
        intent3.putExtra("adminPhone", adminPhone);
        intent3.putExtra("groupId", groupId);
        intent3.putExtra(INTENT_EXTRA_NOTIFICATION_TAG, NOTIFICATION_TAG);
        intent3.putExtra(INTENT_EXTRA_NOTIFICATION_ID, NOTIFICATION_ID);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent3 = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent3, 0);

        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification n = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_splash_launcher)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_sms_white_24dp, "Join", pIntent2)
                .addAction(R.drawable.ic_block_white_24dp, "Deny", pIntent3).build();


        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, n);

    }

    public void showUnlockNotification(Context context) {
        String title = getNameByPhone(senderPhone) + " wants to unlock himself?";
        String text = "Your agreement is necessary";

        // prepare intent which is triggered if the
        // notification is selected

        Intent intent = new Intent(context, MyGroupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        intent.putExtra(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_UNLOCK_NOT_SPECIFIED);
        intent.putExtra("adminPhone", adminPhone);
        intent.putExtra("senderPhone", senderPhone);
        intent.putExtra("groupId", groupId);
        intent.putExtra(INTENT_EXTRA_NOTIFICATION_TAG, NOTIFICATION_TAG);
        intent.putExtra(INTENT_EXTRA_NOTIFICATION_ID, NOTIFICATION_ID);
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);


        Intent intent2 = new Intent(context, MyGroupActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent2.putExtra(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_UNLOCK_ACCEPTED);
        intent2.putExtra("adminPhone", adminPhone);
        intent2.putExtra("groupId", groupId);
        intent2.putExtra(INTENT_EXTRA_NOTIFICATION_TAG, NOTIFICATION_TAG);
        intent2.putExtra(INTENT_EXTRA_NOTIFICATION_ID, NOTIFICATION_ID);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent2 = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent2, 0);

        Intent intent3 = new Intent(context, MyGroupActivity.class);
        intent3.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent3.putExtra(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_UNLOCK_REJECTED);
        intent3.putExtra("adminPhone", adminPhone);
        intent3.putExtra("groupId", groupId);
        intent3.putExtra(INTENT_EXTRA_NOTIFICATION_TAG, NOTIFICATION_TAG);
        intent3.putExtra(INTENT_EXTRA_NOTIFICATION_ID, NOTIFICATION_ID);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent3 = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent3, 0);

        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification n = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_splash_launcher)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_sms_white_24dp, "Allow", pIntent2)
                .addAction(R.drawable.ic_block_white_24dp, "Deny", pIntent3).build();


        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, n);

    }

    private String getNameByPhone(String phoneNumber) {
        ContentResolver cr = mContext.getContentResolver();
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
}
