import ir.type.IntType;
import ir.type.Type;
import node.NodeType;
import node.VNode;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Test {
    private int x;
    Test(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public static void main(String[] args) {
        List<Test> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new Test(i));
        }
        List<Test> list1 = new ArrayList<>(list);
        list.clear();
        System.out.println(list1.get(0).getX());
    }
}
