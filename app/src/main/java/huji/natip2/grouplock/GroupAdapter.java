package huji.natip2.grouplock;


import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.ParseQueryAdapter;

import java.util.List;

public class GroupAdapter extends ParseQueryAdapter<Group> {


    private Context mContext;
    private ProgressDialog progressDialog;

    public GroupAdapter(final Context context,
                           ParseQueryAdapter.QueryFactory<Group> queryFactory) {
        super(context, queryFactory);
        mContext = context;

        addOnQueryLoadListener(new OnQueryLoadListener<Group>() {
            @Override
            public void onLoading() {
                progressDialog = ProgressDialog.show(context, null,
                        "Loading group list", true, false);
            }

            @Override
            public void onLoaded(List<Group> groupList, Exception e) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            }
        });
    }

    @Override
    public View getItemView(Group group, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.group_list_item, parent, false);
            holder = new ViewHolder();
            holder.groupTitle = (TextView) view
                    .findViewById(R.id.group_title);
            holder.groupSubtitle = (TextView) view
                    .findViewById(R.id.group_subtitle);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        TextView groupTitle = holder.groupTitle;
        groupTitle.setText("Admin:\n"+group.getAdmin());


        TextView groupSubtitle = holder.groupSubtitle;
        groupSubtitle.setText(group.countParticipants()+" participants");

        return view;
    }

    private static class ViewHolder {
        TextView groupTitle;
        TextView groupSubtitle;
    }

}