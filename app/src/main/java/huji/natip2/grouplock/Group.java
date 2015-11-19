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

    /**
     * Adds new or updates existing
     *
     * @param phone  used as key
     * @param status new status
     */
    void putParticipant(String phone, UserStatus status) {
        List<Object> phoneList = getList(KEY_PARTICIPANTS_PHONE);
        if (phoneList == null) {
            phoneList = new ArrayList<>();
            put(KEY_PARTICIPANTS_PHONE, phoneList);
        }
        int index = phoneList.indexOf(phone);
        if (index != -1) {
            updateParticipantStatus(index, status.name());
        } else if (phone != null) {
            add(KEY_PARTICIPANTS_PHONE, phone);
            addParticipantStatus(status.name());
        }
    }

    /**
     * Adds new status to the end of the list.
     * Should be called after adding a new phone to the corresponding phone list.
     *
     * @param status new status
     */
    private void addParticipantStatus(String status) {
        List<Object> statusList = getList(KEY_PARTICIPANTS_STATUS);
        if (statusList == null) {
            statusList = new ArrayList<>();
            put(KEY_PARTICIPANTS_STATUS, statusList);
        }
        if (status != null) {
            add(KEY_PARTICIPANTS_STATUS, status);
        }

    }

    /**
     * @param index  location corresponding to the location in the phone list
     * @param status new status
     */
    private void updateParticipantStatus(int index, String status) {
        List<Object> statusList = getList(KEY_PARTICIPANTS_STATUS);
        if (statusList == null) {
            return;
        }
        statusList.set(index, status);
        setParticipantsStatus(statusList);
    }

    List<Object> getParticipantsPhone() {
        return getList(KEY_PARTICIPANTS_PHONE);
    }

    List<Object> getParticipantsStatus() {
        return getList(KEY_PARTICIPANTS_STATUS);
    }

    private void setParticipantsStatus(List<Object> statusList) {
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

    static ParseQuery<Group> getQuery() {
        return ParseQuery.getQuery(Group.class);
    }

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

    int countParticipants() {
        List<Object> phoneList = getParticipantsPhone();
        return phoneList == null ? -1 : phoneList.size();
    }
}
