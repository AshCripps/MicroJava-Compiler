/*  MicroJava Parser (HM 06-12-28)
    ================
*/
package MJ;

import java.util.*;
import MJ.SymTab.*;
import MJ.CodeGen.*;

public class Parser {
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
	private static final String[] name = { // token names for error messages
		"none", "identifier", "number", "char constant", "+", "-", "*", "/", "%",
		"==", "!=", "<", "<=", ">", ">=", "=", ";", ",", ".", "(", ")",
		"[", "]", "{", "}", "class", "else", "final", "if", "new", "print",
		"program", "read", "return", "void", "while", "eof"
		};

	private static Token t;			// current token (recently recognized)
	private static Token la;		// lookahead token
	private static int sym;			// always contains la.kind
	public  static int errors;  // error counter
	private static int errDist;	// no. of correctly recognized tokens since last error

	private static BitSet exprStart, statStart, statSeqFollow, declStart, declFollow;

	//------------------- auxiliary methods ----------------------
	private static void scan() {
		t = la;
		la = Scanner.next();
		sym = la.kind;
		errDist++;
		/*
		System.out.print("line " + la.line + ", col " + la.col + ": " + name[sym]);
		if (sym == ident) System.out.print(" (" + la.string + ")");
		if (sym == number || sym == charCon) System.out.print(" (" + la.val + ")");
		System.out.println();*/
	}

	private static void check(int expected) {
		if (sym == expected) scan();
		else error(name[expected] + " expected");
	}

	public static void error(String msg) { // syntactic error at token la
		if (errDist >= 3) {
			System.out.println("-- line " + la.line + " col " + la.col + ": " + msg);
			errors++;
		}
		errDist = 0;
	}

	//-------------- parsing methods (in alphabetical order) -----------------

	// Program = "program" ident {ConstDecl | ClassDecl | VarDecl} '{' {MethodDecl} '}'.
	private static void Program() {
		TODO  // add parsing actions
	}

	TODO  // add parsing methods for all productions

	public static void parse() {
		// initialize symbol sets
		BitSet s;
		s = new BitSet(64); exprStart = s;
		s.set(ident); s.set(number); s.set(charCon); s.set(new_); s.set(lpar); s.set(minus);

		s = new BitSet(64); statStart = s;
		s.set(ident); s.set(if_); s.set(while_); s.set(read_);
		s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

		s = new BitSet(64); statSeqFollow = s;
		s.set(rbrace); s.set(eof);

		s = new BitSet(64); declStart = s;
		s.set(final_); s.set(ident); s.set(class_);

		s = new BitSet(64); declFollow = s;
		s.set(lbrace); s.set(void_); s.set(eof);

		// start parsing
		errors = 0; errDist = 3;
		scan();
		Program();
		if (sym != eof) error("end of file found before end of program");
	}

}








