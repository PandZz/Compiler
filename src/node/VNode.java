package node;

import token.Token;
import utils.IOUtils;

import java.util.List;
import java.util.Set;

public class VNode {
    private VNode parentNode;
    private List<VNode> childrenNodes;
    private NodeType nodeType;
    private String value;

    private static final Set<NodeType> justifiedNodeTypes = Set.of(
            NodeType.MulExp, NodeType.AddExp, NodeType.RelExp, NodeType.EqExp, NodeType.LAndExp, NodeType.LOrExp
    );

    public VNode(VNode parentNode, List<VNode> childrenNodes, NodeType nodeType) {
        this.parentNode = parentNode;
        this.childrenNodes = childrenNodes;
        this.nodeType = nodeType;
        this.value = '<' + nodeType.toString() + '>';
    }

    public VNode(List<VNode> childrenNodes, NodeType nodeType) {
        this.childrenNodes = childrenNodes;
        this.nodeType = nodeType;
        this.value = '<' + nodeType.toString() + '>';
        // 以下节点类型不需要输出
        if (nodeType == NodeType.BlockItem || nodeType == NodeType.Decl || nodeType == NodeType.BType) {
            this.value = "";
        }
    }

    // 以Token创建的叶子节点都是终结符
    public VNode(Token token) {
        this.value = token.toString();
        this.nodeType = NodeType.EndNode;
    }

    public VNode getParentNode() {
        return parentNode;
    }

    public void setParentNode(VNode parentNode) {
        this.parentNode = parentNode;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public void addChildNode(VNode childNode) {
        childrenNodes.add(childNode);
    }

    public List<VNode> getChildrenNodes() {
        return childrenNodes;
    }

    // 形如: <Decl> 或 SEMICON ;
    public String getValue() {
        return value;
    }

    public void printToBuffer() {
        // 因改变了语法而导致语法树改变的, 需要特殊处理
        if (justifiedNodeTypes.contains(this.nodeType)) {
            childrenNodes.get(0).printToBuffer();
            IOUtils.appendBuffer(this.value);
            if (childrenNodes.size() > 1) {
                childrenNodes.get(1).printToBuffer();
                childrenNodes.get(2).printToBuffer();
            }
        } else {
            if (childrenNodes != null) {
                for (VNode childNode : childrenNodes) {
                    childNode.printToBuffer();
                }
            }
            if (!this.value.isEmpty()) {
                IOUtils.appendBuffer(this.value);
            }
        }
    }
}
