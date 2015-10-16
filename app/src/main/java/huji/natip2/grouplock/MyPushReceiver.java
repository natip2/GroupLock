package huji.natip2.grouplock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseQuery;
import com.parse.SendCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


/**
 * Creates notification for the received debt, which allows the user to add/ignore it
 */
public class MyPushReceiver extends ParsePushBroadcastReceiver {
    private String adminPhone;
    private String senderPhone;
    private int pushCode;
    private Context mContext;

    @Override
    public void onPushReceive(final Context context, Intent intent) {
        mContext = context;
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(intent.getStringExtra(MyPushReceiver.KEY_PUSH_DATA));
            adminPhone = jsonObject.getString("adminPhone");
            senderPhone = jsonObject.getString("senderPhone");
            pushCode = jsonObject.getInt("pushCode");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (jsonObject == null) {
            return;
        }
        switch (pushCode) {
            //ask to add to the group
            case MyGroupActivity.PUSH_CODE_CONFIRM_NOTIFICATION:
                showNotification(context);
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
        }
    }


    //send push to phone, need to update theList from parse
    private void sendUpdatePush(String phone) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject();

            jsonObject.put(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_UPDATE_LIST_FROM_PARSE);
            jsonObject.put("adminPhone", adminPhone);
            jsonObject.put("senderPhone", adminPhone);

            ParsePush push = new ParsePush();
            // TODO: 16/10/2015  start from here

            push.setChannel(LoginActivity.USER_CHANNEL_PREFIX + phone.replaceAll("[^0-9]+", ""));
            push.setData(jsonObject);
            push.sendInBackground(new SendCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                    } else {
                        Toast.makeText(mContext,
                                "Push not sent: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();// REMOVE: 15/09/2015
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateLocalListFromParse() {
        ParseQuery<Group> query = Group.getQuery();
        query.whereEqualTo(Group.KEY_ADMIN, adminPhone);
        query.getFirstInBackground(new GetCallback<Group>() {
            @Override
            public void done(Group group, ParseException e) {
                List<Object> participantsPhone = group.getParticipantsPhone();
                List<Object> participantsStatus = group.getParticipantsStatus();
                int i = 0;
                for (Object phoneObj : participantsPhone) {
                    String phone = (String) phoneObj;
                    String name = getNameByPhone(phone);
                    UserStatus status = UserStatus.valueOf((String) participantsStatus.get(i));
                    UserItem newItem = new UserItem(name, phone, status);
                    int currIndex = UserFragment.theList.indexOf(newItem);
                    if (currIndex != -1) {
                        UserFragment.theList.get(currIndex).setStatus(status);
                    } else {
                        UserFragment.theList.add(newItem);
                    }
                    i++;
                }
                UserFragment.adapterTodo.notifyDataSetChanged();
                //// TODO: 15/10/2015  notifiy data change
            }
        });
    }


    private void addSenderToLocalListAndUpdateParseAndBroadcast() {
        final UserStatus status;
        if (pushCode == MyGroupActivity.PUSH_RESPONSE_CODE_ACCEPTED) {
            status = UserStatus.LOCKED;
        } else {
            status = UserStatus.USER_CANCEL;
        }
        UserItem senderItem = new UserItem(getNameByPhone(senderPhone), senderPhone, status);
        boolean isFound = false;
        for (UserItem item : UserFragment.theList) {
            if (item.getNumber().equals(senderItem.getNumber())) {
                isFound = true;
                item.setStatus(status);
            }
        }
        if (!isFound) {
            UserFragment.theList.add(senderItem);
        }

        // update parse from local
        ParseQuery<Group> query = Group.getQuery();
        query.whereEqualTo(Group.KEY_ADMIN, adminPhone);
        query.getFirstInBackground(new GetCallback<Group>() {
            @Override
            public void done(Group group, ParseException e) {
                group.addParticipant(senderPhone, status);
                broadcastChange(group);
            }
        });

    }

    private void broadcastChange(Group group) {
        List<Object> participants = group.getParticipantsPhone();
        for (Object phoneObj : participants) {
            String phone = (String) phoneObj;
            sendUpdatePush(phone);
        }
    }


    private void updateParse() {

    }

    public void showNotification(Context context) {
        String title = "Join to the group?";
        String text = getNameByPhone(adminPhone) + " invites you";

        // prepare intent which is triggered if the
        // notification is selected

        Intent intent = new Intent(context, MyGroupActivity.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        intent.putExtra(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_NOT_SPECIFIED);
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

        Intent intent2 = new Intent(context, MyGroupActivity.class);
        intent2.putExtra(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_ACCEPTED);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent2 = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent2, 0);

        Intent intent3 = new Intent(context, MyGroupActivity.class);
        intent3.putExtra(MyGroupActivity.PUSH_CODE, MyGroupActivity.PUSH_CODE_REJECTED);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent3 = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent3, 0);


        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification n = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_sms_white_24dp, "Join", pIntent2)
                .addAction(R.drawable.ic_block_white_24dp, "Deny", pIntent3).build();


        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);

    }

    private String getNameByPhone(String s) {
        return "Your friend";// TODO: 15/10/2015
    }
}
