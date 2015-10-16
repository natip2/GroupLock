package huji.natip2.grouplock;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

@ParseClassName("Group")
public class Group extends ParseObject {

    static final String KEY_ADMIN = "admin";
    static final String KEY_PARTICIPANTS_PHONE = "participantsPhone";
    static final String KEY_PARTICIPANTS_STATUS = "participantsStatus";

    void addParticipant(String phone, UserStatus status) {
        List<Object> phoneList = getList(KEY_PARTICIPANTS_PHONE);
        if (phoneList == null) {
            phoneList = new ArrayList<>();
            put(KEY_PARTICIPANTS_PHONE, phoneList);
        } else {
            if (phone != null) {
                add(KEY_PARTICIPANTS_PHONE, phone);
            }
        }
        addParticipantStatus(status.name());
    }

    private void addParticipantStatus(String status) {
        List<Object> statusList = getList(KEY_PARTICIPANTS_STATUS);
        if (statusList == null) {
            statusList = new ArrayList<>();
            put(KEY_PARTICIPANTS_STATUS, statusList);
        } else {
            if (status != null) {
                add(KEY_PARTICIPANTS_STATUS, status);
            }
        }
    }

    List<Object> getParticipantsPhone() {
        return getList(KEY_PARTICIPANTS_PHONE);
    }

    List<Object> getParticipantsStatus() {
        return getList(KEY_PARTICIPANTS_STATUS);
    }

    void setParticipantsStatus(List<Object> statusList) {
        if (statusList != null) {
            addAll(KEY_PARTICIPANTS_STATUS, statusList);
        } else {
            remove(KEY_PARTICIPANTS_STATUS);
        }
    }

    void setParticipantsPhone(List<Object> phoneList) {
        if (phoneList != null) {
            addAll(KEY_PARTICIPANTS_PHONE, phoneList);
        } else {
            remove(KEY_PARTICIPANTS_PHONE);
        }
    }

    String getAdmin() {
        return getString(KEY_ADMIN);
    }

    void setAdmin(String admin) {
        if (admin != null) {
            put(KEY_ADMIN, admin);
        } else {
            remove(KEY_ADMIN);
        }
    }

    static ParseQuery<Group> getQuery(){return ParseQuery.getQuery(Group.class);}

    void removeParticipant(String phone) {
        if (phone != null) {
            List<Object> toRemovePhone = new ArrayList<>();
            toRemovePhone.add(phone);
            List<Object> toRemoveStatus = new ArrayList<>();
            toRemoveStatus.add(phone);
            removeAll(KEY_PARTICIPANTS_PHONE, toRemovePhone);
            removeAll(KEY_PARTICIPANTS_STATUS, toRemoveStatus);
        }

    }
}
