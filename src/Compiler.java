import frontend.Lexer;
import frontend.Parser;
import node.VNode;
import token.Token;
import utils.IOUtils;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) {
        try {
            Lexer lexer = Lexer.getInstance();
            lexer.setSource(IOUtils.readInput());

            Parser parser = Parser.getInstance();
            parser.setTokens(lexer.getTokens());
            VNode root = parser.parse();

            root.printToBuffer();
            IOUtils.appendBuffer("");
            IOUtils.writeBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
