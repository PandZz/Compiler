package error;

import node.NodeType;
import static node.NodeType.*;
import node.VNode;
import token.TokenType;
import utils.IOUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ErrorHandler {
    private static ErrorHandler instance = null;
    private final Set<Error> errorSet = new HashSet<>();

    private ErrorHandler() {
    }

    public static ErrorHandler getInstance() {
        if (instance == null) {
            instance = new ErrorHandler();
        }
        return instance;
    }

    public void addError(Error error) {
        errorSet.add(error);
    }

    public void printErrors2Buffer() {
        List<Error> toSort = new ArrayList<>(errorSet);
        toSort.sort(null);
        for (Error error : toSort) {
            IOUtils.appendBuffer(error.toString());
        }
    }

    private boolean isVNodeMatchTokenType(VNode endNode, TokenType tokenType) {
        return endNode.getNodeType() == EndNode && endNode.getValue().split(" ")[0].equals(tokenType.toString());
    }

    /*private void CompUnitError(VNode compUnitNode) {
        for (VNode child : compUnitNode.getChildrenNodes()) {
            switch (child.getNodeType()) {
                case Decl -> DeclError(child);
                case FuncDef -> FuncDefError(child);
                case MainFuncDef -> MainFuncDefError(child);
            }
        }
    }

    private void DeclError(VNode declNode) {
        VNode child = declNode.getChildrenNodes().get(0);
        switch (child.getNodeType()) {
            case ConstDecl -> ConstDeclError(child);
            case VarDecl -> VarDeclError(child);
        }
    }*/
}
