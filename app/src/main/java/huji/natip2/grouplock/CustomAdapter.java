package huji.natip2.grouplock;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.parse.ParseUser;

import java.util.ArrayList;

public class CustomAdapter extends ArrayAdapter<UserItem> {

    private final Context mContext;

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(getContext().LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.user_list_item, parent, false);
        String myPhone = ParseUser.getCurrentUser().getUsername();

        TextView phonePlace = (TextView) view.findViewById(R.id.phoneNumber);

//        phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_outline_black_24dp, 0, 0, 0);
        phonePlace.setTextSize(20);

        UserItem item = getItem(position);
        String name = item.getName();
        String phone = item.getNumber();
        String title = (name != null ? name : "") + "\n" + phone;
        if (phone.equals(myPhone)) {
            title = "Me";
            phonePlace.setTextColor(Color.GRAY);
        } else {
            phonePlace.setTextColor(Color.DKGRAY);
        }
        if (name != null && name.equals(mContext.getString(R.string.group_admin_name))) {
            phonePlace.setTypeface(null, Typeface.BOLD);
        }

        phonePlace.setText(title);
        phonePlace.setCompoundDrawablePadding(25);

        UserStatus stat = item.getStatus();
        switch (stat) {
            case DOES_NOT_HAVE_APP: {
//                phonePlace.setTextColor(Color.RED);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_phonelink_erase_black_24dp, 0, 0, 0);
                break;
            }
            case HAS_APP: {
//                phonePlace.setTextColor(Color.DKGRAY);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_phonelink_ring_black_24dp, 0, 0, 0);
                break;
            }
            case LOCKED: {
//                phonePlace.setTextColor(Color.BLUE);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_outline_black_24dp, 0, 0, 0);
                break;
            }
            case DENIED: {
//                phonePlace.setTextColor(Color.YELLOW);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_block_black_24dp, 0, 0, 0);
                break;
            }
            case VERIFIED: {
//                phonePlace.setTextColor(Color.GRAY);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
                break;
            }
            case WAIT_FOR_VERIFICATION: {
//                phonePlace.setTextColor(Color.CYAN);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hourglass_empty_black_24dp, 0, 0, 0);
                break;
            }
            default: {
                System.out.println("there is item with no status");
            }
        }
        return view;

    }


    public CustomAdapter(Context context, int resource, ArrayList<UserItem> arr) {
        super(context, resource, arr);
        mContext = context;
    }
}