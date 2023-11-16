import node.NodeType;
import node.VNode;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        Token token = new Token(TokenType.IDENFR, "a", 1);
        VNode node = new VNode(token);
        System.out.println(node.getValue());
    }
}
