package MJ;
import java.io.*;
import MJ.CodeGen.Decoder;

// This class can be used to decode MicroJava object files.
// Synopsis: java MJ.Decode <filename>.obj
public class Decode {

	public static void main(String[] arg) {
		if (arg.length == 0)
			System.out.println("-- no filename specified");
		else {
			try {
				InputStream s = new FileInputStream(arg[0]);
				byte[] code = new byte[3000];
				int len = s.read(code);
				Decoder.decode(code, 14, len);
			} catch (IOException e) {
				System.out.println("-- could not open file " + arg[0]);
			}
		}
	}
}