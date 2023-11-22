import node.NodeType;
import node.VNode;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        List<TokenType> tokenTypes = new ArrayList<>() {{
            add(TokenType.GRE);
            add(TokenType.GEQ);
            add(TokenType.LEQ);
            add(TokenType.RBRACE);
            add(TokenType.LBRACE);
        }};
        System.out.println(tokenTypes);
    }
}
