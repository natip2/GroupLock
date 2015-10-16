package huji.natip2.grouplock;



import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends ArrayAdapter<UserItem> {

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(getContext().LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.user_list_item, parent, false);

        TextView phonePlace = (TextView) view.findViewById(R.id.phoneNumber);

        phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_outline_white_24dp,0,0,0);
        phonePlace.setTextSize(20);

        UserItem item = getItem(position);
        phonePlace.setText(item.getName());
        UserStatus stat = item.getStatus();
        switch (stat) {
            case DOES_NOT_HAVE_APP: {
                phonePlace.setTextColor(Color.RED);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sms_white_24dp, 0, 0, 0);
                break;
            }
            case HAS_APP: {
                phonePlace.setTextColor(Color.DKGRAY);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_send_white_24dp, 0, 0, 0);
                break;
            }
            case LOCKED: {
                phonePlace.setTextColor(Color.BLUE);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_outline_white_24dp, 0, 0, 0);
                break;
            }
            case USER_CANCEL: {
                phonePlace.setTextColor(Color.YELLOW);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_block_white_24dp, 0, 0, 0);
                break;
            }
            case VERIFIED_LOCK: {
                phonePlace.setTextColor(Color.GRAY);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_verified_user_white_24dp, 0, 0, 0);
                break;
            }
            case WAIT_FOR_VERIFICATION: {
                phonePlace.setTextColor(Color.CYAN);
                phonePlace.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hourglass_empty_white_24dp, 0, 0, 0);
                break;
            }
            default:{
                System.out.println("there is item with no status");
            }
        }
        return view;

    }



    public CustomAdapter(Context context, int resource,ArrayList<UserItem> arr) {

        super(context, resource,arr);

    }
}