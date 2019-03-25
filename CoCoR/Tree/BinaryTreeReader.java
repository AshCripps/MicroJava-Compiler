public class BinaryTreeReader{
  class Node {
    String name;
    Node left;
    Node right;
  }

  Node rootNode;

  public BinaryTreeReader(Node node){
    rootNode = node;
  }

  public PrintTree(Node node){
    System.out.println(name);
    PrintTree(left);
    PrintTree(right);
  }
}
