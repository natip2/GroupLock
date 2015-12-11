package huji.natip2.grouplock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

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
public class HistoryFragment extends Fragment implements AbsListView.OnItemClickListener {

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
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private GroupAdapter mAdapter;
    private View mEmptyView;

    // TODO: Rename and change types of parameters
    public static HistoryFragment newInstance(String param1, String param2) {
        HistoryFragment fragment = new HistoryFragment();
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
    public HistoryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


        // TODO: Change Adapter to display your content
//        mAdapter = new ArrayAdapter<DummyContent.DummyItem>(getActivity(),
//                android.R.layout.simple_list_item_1, android.R.id.text1, DummyContent.ITEMS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_list, container, false);

        // Set the adapter
        mListView = (ListView) view.findViewById(R.id.list_view_groups);

        // Set up the Parse query to use in the adapter
        ParseQueryAdapter.QueryFactory<Group> factory = new ParseQueryAdapter.QueryFactory<Group>() {
            public ParseQuery<Group> create() {
                String myPhone = ParseUser.getCurrentUser().getUsername();

                ParseQuery<Group> participantsQuery = Group.getQuery();
                participantsQuery.whereEqualTo(Group.KEY_PARTICIPANTS_PHONE, myPhone);
                ParseQuery<Group> adminQuery = Group.getQuery();
                adminQuery.whereEqualTo(Group.KEY_ADMIN, myPhone);

                List<ParseQuery<Group>> queries = new ArrayList<>();
                queries.add(participantsQuery);
                queries.add(adminQuery);

                ParseQuery<Group> mainQuery = ParseQuery.or(queries);
                mainQuery.orderByDescending("createdAt");
                return mainQuery;
            }
        };

        mEmptyView = view.findViewById(R.id.empty_history_list);

        mAdapter = new GroupAdapter(getActivity(), factory);// TODO: 11/20/2015 loading... progress

        mListView.setEmptyView(mEmptyView);

        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Group group = mAdapter.getItem(position);
                viewGroup(group);
            }
        });


        return view;
    }

    private void viewGroup(final Group group) {
        // TODO: 19/11/2015 show group participants, images, locked times (from - to). and maybe connection history: joined, locked, left
        // TODO: 19/11/2015 chart that shows app usage over time

        ArrayList<UserItem> users = createUserList(group);
        UserAdapter participantAdapter = new UserAdapter(getActivity(), R.layout.user_list_item, users);

        (new AlertDialog.Builder(getActivity()))
                .setTitle("Admin: " + MyGroupActivity.getDisplayName(getActivity(), group.getAdmin()))
                .setAdapter(participantAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: 19/11/2015 call, sms. (social status - num of groups participated in)
                    }
                })
                .setPositiveButton("Add to current", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ((MyGroupActivity) getActivity()).chooseDrawerItem(R.id.nav_main, MyGroupActivity.MAIN_FRAGMENT_INDEX);
                        recreateGroup(group);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void recreateGroup(Group group) {
        String myPhone = ParseUser.getCurrentUser().getUsername();
        List<Object> participantsPhone = group.getParticipantsPhone();
        for (Object phoneObj : participantsPhone) {
            String phone = (String) phoneObj;
            String name = MyGroupActivity.getNameByPhone(getActivity(), phone);
            UserItem newItem = new UserItem(name, phone, MyGroupActivity.doesUserHaveApp(phone));
            if (!UserFragment.localList.contains(newItem)) {
                UserFragment.localList.add(newItem);
                if (phone.equals(myPhone)) {
                    UserFragment.myUserItem = newItem;
                }
            }
        }
        UserFragment.userAdapter.notifyDataSetChanged();
        ((MyGroupActivity)getActivity()).updateFab();
    }


    private ArrayList<UserItem> createUserList(Group group) {
        ArrayList<UserItem> users = new ArrayList<>();
        List<Object> participantsPhone = group.getParticipantsPhone();
        for (Object phoneObj : participantsPhone) {
            String phone = (String) phoneObj;
            String name = MyGroupActivity.getNameByPhone(getActivity(), phone);
            UserItem newItem = new UserItem(name, phone, null);
            users.add(newItem);
        }
        return users;
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
//            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
            // TODO: 07/10/2015  unovmment
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
