import frontend.Lexer;
import frontend.Parser;
import node.VNode;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class Compiler {
    public static void main(String[] args) {
        try {
            // 读取源代码
            String source = IOUtils.readInput();

            // 词法分析
            Lexer lexer = Lexer.getInstance();
            List<Token> tokens = lexer.transStr2Tokens(source);

            // 语法分析
            Parser parser = Parser.getInstance();
            VNode compUnitNode = parser.transTokens2VNode(tokens);

            // 打印语法树
            compUnitNode.printToBuffer();
            IOUtils.appendBuffer("");
            IOUtils.writeBuffer2OutPut();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
