package ir;

import ir.value.User;
import ir.value.Value;

public class Use {
    private Value value;
    private User user;
    // pos表示该value是user的第几个操作数
    private int pos;

    public Use(Value value, User user, int pos) {
        this.value = value;
        this.user = user;
        this.pos = pos;
    }

    public Value getValue() {
        return value;
    }

    public User getUser() {
        return user;
    }

    public int getPos() {
        return pos;
    }
}
