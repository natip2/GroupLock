package huji.natip2.grouplock;

/**
 * Created by natip2 on 08/09/2015.
 */
public class UserItem {

    private String name;
    private String number;
    private UserStatus status;

    public UserItem(String name, String number, UserStatus status) {
        this.name = name;
        this.status = status;
        this.number = number;
    }

    public String getName() {
        return this.name;
    }

    public String getNumber() {
        return this.number;
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

        return  number.equals(o.number);
    }
}


