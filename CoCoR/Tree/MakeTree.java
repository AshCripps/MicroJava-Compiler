import java.io.*;

class MakeTree{

  public static void main(String[] args){
    String inFileName = args[0];
    Scanner scanner = new Scanner(inFileName);
    Parser parser = new Parser(scanner);
    parser.Parse();
    System.out.println(parser.errors.count + " errors detected");


  }
}
