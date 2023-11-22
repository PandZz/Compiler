package ir.value;

import ir.Use;
import ir.type.Type;

import java.util.ArrayList;
import java.util.List;

public class Value {
    private String name;
    private Type type;
    private List<Use> useList;
    // 全局的valNUmber, 用于给Value编号
    public static int valNumber = -1;

    public Value() {}

    public Value(String name, Type type) {
        this.name = name;
        this.type = type;
        this.useList = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<Use> getUseList() {
        return useList;
    }

    public void addUse(Use use) {
        useList.add(use);
    }

    public List<User> getUserList() {
        List<User> userList = new ArrayList<>();
        for (Use use : useList) {
            userList.add(use.getUser());
        }
        return userList;
    }
}
