/* MicroJava Scanner Tester
   ========================
   Place this file in a subdirectory MJ
   Compile with
     javac MJ\TestScanner.java
   Run with
     java MJ.TestScanner <inputFileName>
*/
package MJ;

import java.io.*;

public class TestScanner {
	private static final int  // token codes
		none      = 0,
		ident     = 1,
		number    = 2,
		charCon   = 3,
		plus      = 4,
		minus     = 5,
		times     = 6,
		slash     = 7,
		rem       = 8,
		eql       = 9,
		neq       = 10,
		lss       = 11,
		leq       = 12,
		gtr       = 13,
		geq       = 14,
		assign    = 15,
		semicolon = 16,
		comma     = 17,
		period    = 18,
		lpar      = 19,
		rpar      = 20,
		lbrack    = 21,
		rbrack    = 22,
		lbrace    = 23,
		rbrace    = 24,
		class_    = 25,
		else_     = 26,
		final_    = 27,
		if_       = 28,
		new_      = 29,
		print_    = 30,
		program_  = 31,
		read_     = 32,
		return_   = 33,
		void_     = 34,
		while_    = 35,
		eof       = 36;

	private static String[] tokenName = {
		"none",
		"ident  ",
		"number ",
		"char   ",
		"+",
		"-",
		"*",
		"/",
		"%",
		"==",
		"!=",
		"<",
		"<=",
		">",
		">=",
		"=",
		";",
		",",
		".",
		"(",
		")",
		"[",
		"]",
		"{",
		"}",
		"class",
		"else",
		"final",
		"if",
		"new",
		"print",
		"program",
		"read",
		"return",
		"void",
		"while",
		"eof"
	};

	// Main method of the scanner tester
	public static void main(String args[]) {
		Token t;
		if (args.length > 0) {
			String source = args[0];
			try {
				Scanner.init(new InputStreamReader(new FileInputStream(source)));
				do {
					t = Scanner.next();
					System.out.print("line " + t.line + ", col " + t.col + ": " + tokenName[t.kind]);
					switch (t.kind) {
						case ident:   System.out.println(t.string); break;
						case number:  System.out.println(t.val); break;
						case charCon: System.out.println(t.val); break;
						default: System.out.println(); break;
					}
				} while (t.kind != eof);
			} catch (IOException e) {
				System.out.println("-- cannot open input file " + source);
			}
		} else System.out.println("-- synopsis: java MJ.TestScanner <inputfileName>");
	}

}