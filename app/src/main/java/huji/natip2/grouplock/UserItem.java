package huji.natip2.grouplock;

/**
 * Created by natip2 on 08/09/2015.
 */
public class UserItem {

    private String name;
    private String phone;
    private UserStatus status;

    public UserItem(String name, String phone, UserStatus status) {
        this.name = name;
        this.status = status;
        this.phone = phone;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.name != null ? this.name : this.phone;
    }


    public String getPhone() {
        return this.phone;
    }

    public UserStatus getStatus() {
        return this.status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof UserItem)) {
            return false;
        }
        UserItem o = (UserItem) obj;

        return phone.equals(o.phone);
    }
}


