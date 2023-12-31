import config.Config;
import error.ErrorHandler;
import frontend.Lexer;
import frontend.Parser;
import ir.IRModule;
import ir.Vistor;
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
//            compUnitNode.printToBuffer();
//            IOUtils.writeBuffer2OutPut();

            boolean hasError = false;
            // 错误处理
            if (Config.ERROR) {
                ErrorHandler errorHandler = ErrorHandler.getInstance();
                errorHandler.CompUnitError(compUnitNode);
                errorHandler.printErrors2Buffer();
                hasError = IOUtils.getBufferLength() > 0;
                IOUtils.writeBuffer2Error();
                IOUtils.clearBuffer();
            }

            // 中间代码生成
            if (!hasError) {
                Vistor vistor = Vistor.getInstance();
                vistor.visitCompUnit(compUnitNode);
                IRModule.getInstance().print2Buffer();
                IOUtils.writeBuffer2IR();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
