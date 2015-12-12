package huji.natip2.grouplock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SendCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

//import huji.natip2.grouplock.dummy.DummyContent;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class UserFragment extends Fragment implements ListView.OnItemClickListener {

    private String currentUserId;
    private ListView usersListView;
    private BroadcastReceiver receiver = null;
    public static ArrayAdapter<UserItem> userAdapter;
    public static ArrayList<UserItem> localList = new ArrayList<UserItem>();
    public static UserItem myUserItem;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;
    private View mEmptyView;
    private boolean isInSession = false;
    protected String myNumber;

    public static void addPerson(UserItem user) {
        localList.add(user);
        userAdapter.notifyDataSetChanged();
    }

    private void setAdapter() {
        //set the adapter
        userAdapter = new UserAdapter(getActivity(), R.layout.user_list_item, localList);
        mListView.setEmptyView(mEmptyView);
        mListView.setAdapter(userAdapter);
    }


    private boolean isVerified(UserItem item) {
        return item.getStatus().equals(UserStatus.VERIFIED);
    }


    private void sendPushNotification(UserItem item) {
        String myPhone = ParseUser.getCurrentUser().getUsername();

        if (((MyGroupActivity) getActivity()).adminPhone == null) {
            ((MyGroupActivity) getActivity()).adminPhone = myPhone;
            ((MyGroupActivity) getActivity()).createNewTable();
        }

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject();

            jsonObject.put(MyGroupActivity.PUSH_CODE_EXTRA, MyGroupActivity.PUSH_CODE_CONFIRM_NOTIFICATION);
            jsonObject.put("adminPhone", ((MyGroupActivity) getActivity()).adminPhone);
            jsonObject.put("groupId", ((MyGroupActivity) getActivity()).groupId);
            jsonObject.put("senderPhone", myPhone);


            ParsePush push = new ParsePush();
            push.setChannel(LoginActivity.USER_CHANNEL_PREFIX + item.getPhone().replaceAll("[^0-9]+", ""));
            push.setData(jsonObject);
            push.sendInBackground(new SendCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                    } else {
                        Toast.makeText(getActivity(),
                                "Push not sent: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();// REMOVE: 15/09/2015
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendSmsRequest(UserItem item) {
        SmsManager smsManager = SmsManager.getDefault();
        String message = "Join GroupLock, " +
                "\nAdmin phone:" + ((MyGroupActivity) getActivity()).adminPhone; // TODO: 15/10/2015 url
        smsManager.sendTextMessage(item.getPhone(), null, message, null, null);
        Toast.makeText(getActivity(), "SMS Sent!",
                Toast.LENGTH_LONG).show();
    }

    private void removeFromParse(final String number) {
        ParseQuery<Group> query = Group.getQuery();
        query.whereEqualTo("objectId", ((MyGroupActivity) getActivity()).groupId);
        query.getFirstInBackground(new GetCallback<Group>() {
            @Override
            public void done(Group group, ParseException e) {
                if (e == null) {
                    group.removeParticipant(number);
                    group.saveInBackground();
                }
            }
        });
    }


    // TODO: Rename and change types of parameters
    public static UserFragment newInstance(String param1, String param2) {
        UserFragment fragment = new UserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public UserFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


//        mListView = (ListView) getView().findViewById(R.id.list_view_users);
//        mListView.setAdapter(mAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        // Set the adapter

//        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView = (ListView) view.findViewById(R.id.list_view_users);
        mEmptyView = view.findViewById(R.id.empty_list);
        setAdapter();

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        if (null != mListener) {
            final UserItem item = localList.get(position);
            final AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            b.setIcon(android.R.drawable.ic_dialog_alert);
            final String number = item.getPhone();
            b.setMessage(item.getDisplayName());

            if (MyGroupActivity.isAdmin) {

            }

            switch (item.getStatus()) {
                case DOES_NOT_HAVE_APP:
                    b.setPositiveButton("SMS-invite", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            sendSmsRequest(item);
                            Toast.makeText(getActivity(), "SMS invite sent", Toast.LENGTH_SHORT).show();
                        }
                    });
                    b.setNegativeButton("Remove", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //todo open the lock for this number
                            localList.remove(position);
                            userAdapter.notifyDataSetChanged();
                            if (isVerified(item)) {
                                removeFromParse(number);
                                MyGroupActivity.broadcastChange(((MyGroupActivity) getActivity()).adminGroup, ((MyGroupActivity) getActivity()).adminPhone, ((MyGroupActivity) getActivity()).groupId);
                            }
                        }
                    });
                    break;
                case HAS_APP:
                    b.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String myPhone = ParseUser.getCurrentUser().getUsername();
                            if (MyGroupActivity.adminPhone == null) {
                                MyGroupActivity.adminPhone = myPhone;
                                MyGroupActivity.createNewTable();
                            }
                            MyGroupActivity.sendPush(item.getPhone(), myPhone, MyGroupActivity.PUSH_CODE_CONFIRM_UNLOCK);
                            Toast.makeText(getActivity(), "Request sent", Toast.LENGTH_SHORT).show();
                        }
                    });
                    b.setNegativeButton("Remove", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //todo open the lock for this number
                            localList.remove(position);
                            userAdapter.notifyDataSetChanged();
                            if (isVerified(item)) {
                                removeFromParse(number);
                                MyGroupActivity.broadcastChange(((MyGroupActivity) getActivity()).adminGroup, ((MyGroupActivity) getActivity()).adminPhone, ((MyGroupActivity) getActivity()).groupId);
                            }
                        }
                    });
                    break;
                case LOCKED:
                    if (MyGroupActivity.isAdmin) {
                        b.setPositiveButton("Unlock", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                item.setStatus(UserStatus.VERIFIED);
                                // Sends a push for a user to unlock himself
                                MyGroupActivity.sendPush(item.getPhone(), MyGroupActivity.adminPhone, MyGroupActivity.PUSH_ADMIN_UNLOCK);
                                MyGroupActivity.updateSingleUserInParse(item);
                                Toast.makeText(getActivity(), "User unlocked", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
                case VERIFIED:
                    if (MyGroupActivity.isAdmin) {
                        b.setPositiveButton("Lock", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                item.setStatus(UserStatus.LOCKED);
                                // Sends a push for a user to unlock himself
                                MyGroupActivity.sendPush(item.getPhone(), MyGroupActivity.adminPhone, MyGroupActivity.PUSH_ADMIN_LOCK);
                                MyGroupActivity.updateSingleUserInParse(item);
                                Toast.makeText(getActivity(), "User locked", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
            }
            b.show();
        }

    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

}
