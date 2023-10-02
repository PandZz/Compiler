import frontend.Lexer;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.StringJoiner;

public class Compiler {
    public static void main(String[] args) {
        try {
            Lexer lexer = Lexer.getInstance();
            lexer.setSource(IOUtils.readFile("testfile.txt"));
            Token token;
            StringJoiner lexerOutput = new StringJoiner("\n");
            do {
                token = lexer.next();
                if (token != null) {
                    lexerOutput.add(token.toString());
                }
            } while (token != null);
            IOUtils.writeOutput(lexerOutput.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
