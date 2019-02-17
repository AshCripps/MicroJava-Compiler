/* MicroJava Main Class  (HM 06-12-28)
   ====================
*/
package MJ;

import java.io.*;
import MJ.CodeGen.*;

public class Compiler {

	private static String objFileName(String s) {
		int i = s.lastIndexOf('.');
		if (i < 0) return s + ".obj"; else return s.substring(0, i) + ".obj";
	}

	// Main procedure of MicroJava compiler
	public static void main(String args[]) {
		if (args.length > 0) {
			String source = args[0];
			String output = objFileName(source);
			try {
				Scanner.init(new InputStreamReader(new FileInputStream(source)));
				Parser.parse();
				if (Parser.errors == 0) {
					try {
						Code.write(new FileOutputStream(output));
					} catch (IOException e) {
						System.out.println("-- cannot open output file "+output);
					}
				}
			} catch (IOException e) {
				System.out.println("-- cannot open input file " + source);
			}
		} else System.out.println("-- synopsis: java MJ.Compiler <inputfileName>");
	}

}