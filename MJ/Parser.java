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

	private static BitSet exprStart, statStart, statSeqFollow, declStart, declFollow, relopStart, statSync;
	private static Obj curMeth, curClass;

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
		check(program_);
		check(ident);
		Tab.insert(Obj.Prog, t.string, Tab.noType);
		Tab.openScope();
		for(;;){
			if (sym == final_){
				ConstDecl();
			}else if (sym == class_){
				ClassDecl();
			}else if (sym == ident){
				VarDecl();
			}else if (sym == lbrace || sym == eof) {
				break;
			} else {
				error("invalid declaration");
				do scan(); while (sym != final_ && sym != class_ && sym != lbrace && sym != eof);
				errDist = 0;
			}
		}
		check(lbrace);
		while (sym == void_ || sym == ident){
			MethodDecl();
		}
		check(rbrace);
		Tab.dumpScope(Tab.curScope.locals);
		Tab.closeScope();
	}

	//ConstDecl =  "final" Type ident "=" (number | charConst) ";".
	private static void ConstDecl(){
		check(final_);
		Struct type = Type();
		check(ident);
		String name = t.string;
		Tab.insert(Obj.Con, name, type);

		check(assign);
		if (sym == number) {
			scan();
			Tab.find(name).val = t.val;
		}
		else if(sym == charCon) {
			scan();
			Tab.find(name).val = t.val;
		}
		else error("Invalid ConstDecl");
		check(semicolon);
	}

	//VarDecl =  Type ident {"," ident } ";".
	private static void VarDecl(){
		Struct type = Type();
		check(ident);
		String name = t.string;
		Tab.insert(Obj.Var, name, type);
		while (sym == comma){
			scan();
			check(ident);
			name = t.string;
			Tab.insert(Obj.Var, name, type);
		}
		check(semicolon);
	}

	//ClassDecl =  "class" ident "{" {VarDecl} "}".
	private static void ClassDecl(){
		Struct type = Tab.nullType;
		String name;
		check(class_);
		check(ident);
		name = t.string;
		curClass = Tab.insert(Obj.Type, name, type);
		Tab.openScope();
		check(lbrace);
		while(sym == ident){
			VarDecl();
			Tab.find(name).type.fields = Tab.curScope.locals;
			Tab.find(name).type.nFields = Tab.curScope.nVars;
		}
		check(rbrace);
		Tab.closeScope();
	}

	//MethodDecl =  (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block.
	private static void MethodDecl(){
		Struct type = Tab.noType;
		if (sym == ident){
			type = Type();
		}else if (sym == void_){
			scan();
		}else {
			error("Invalid Method Declaration");
		}
		check(ident);
		String name = t.string;
		curMeth = Tab.insert(Obj.Meth, name, type);
		Tab.openScope();
		check(lpar);
		if (sym == ident) Formpars();
		curMeth.nPars = Tab.curScope.nVars;
		check(rpar);
			while (sym == ident) VarDecl();
			curMeth.locals = Tab.curScope.locals;
		Block();
		Tab.closeScope();
	}

	//FormPars =  Type ident  {"," Type ident}.
	private static void Formpars(){
		Struct type;
		String name;
		type = Type();
		check(ident);
		name = t.string;
		Tab.insert(Obj.Var, name, type);
			while (sym == comma){
				scan();
				type = Type();
				check(ident);
				name = t.string;
				Tab.insert(Obj.Var, name, type);
		}

	}

	//Type =  ident ["[" "]"].
	private static Struct Type(){
		check(ident);
		Obj o = Tab.find(t.string);
		Struct type = o.type;
		if (sym == lbrack){
			type = new Struct(Struct.Arr, type); //Passing array
			scan();
			check(rbrack);
		}
		return type;
	}

	//Block = "{" {Statement} "}".
	private static void Block(){
		check(lbrace);
	 	while (sym != rbrace && sym != eof){
			Statement();
		}
		check(rbrace);
	}

	/*Statement =  Designator ("=" Expr | ActPars) ";"
	|  "if" "(" Condition ")" Statement ["else" Statement]
	|  "while" "(" Condition ")" Statement
	|  "return" [Expr] ";"
	|  "read" "(" Designator ")" ";"
	|  "print" "(" Expr ["," number] ")" ";"
	|  Block
	|  ";". */
	private static void Statement(){
		if (!statStart.get(sym)){
			error("invalid start of statement");
			while(!statSync.get(sym)) scan();
			errDist = 0;
		}
		if (sym == ident){
			Designator();
			if (sym == assign){
				scan();
				Expr();
			}else if (sym == lpar){
				ActPars();
			} else error("Invalid Assignment or call");
			check(semicolon);
		}else if (sym == if_) {
			scan();
			check(lpar);
			Condition();
			check(rpar);
			Statement();
			if (sym == else_){
				scan();
				Statement();
			}
		}else if (sym == while_){
			scan();
			check(lpar);
			Condition();
			check(rpar);
			Statement();
		}else if (sym == return_){
			scan();
			if (sym == minus || sym == ident){
				Expr();
			}
			check(semicolon);
		}else if (sym == read_){
			scan();
			check(lpar);
			Designator();
			check(rpar);
			check(semicolon);
		}else if (sym == print_){
			scan();
			check(lpar);
			Expr();
			if (sym == comma){
				scan();
				check(number);
			}
			check(rpar);
			check(semicolon);
		}else if (sym == lbrace){
			Block();
		}else if (sym == semicolon){
			scan();
		}else error("Invalid start of statement");
	}

	//ActPars =  "(" [ Expr {"," Expr} ] ")".
	private static void ActPars(){
		check(lpar);
		if (exprStart.get(sym)){
			Expr();
			while (sym == comma){
					scan();
					Expr();
			}
		}
		check(rpar);
	}

	//Condition =  Expr Relop Expr.
	private static void Condition(){
		Expr();
		Relop();
		Expr();
	}

	//Relop =  "==" | "!=" | ">" | ">=" | "<" | "<=".
	private static void Relop(){
		if (relopStart.get(sym)) scan();
		else error("invalid Relop");
	}

	//Expr =  ["-"] Term {Addop Term}.
	private static void Expr(){
		if (sym == minus) scan();
		Term();
			while (sym == minus || sym == plus){
				Addop();
				Term();
		}
	}

	//Term =  Factor {Mulop Factor}.
	private static void Term(){
		Factor();
		while (sym == times || sym == slash || sym == rem){
			Mulop();
			Factor();
		}
	}

	//Factor =  Designator [ActPars] |  number |  charConst |  "new" ident ["[" Expr "]"] |  "(" Expr ")".
	private static void Factor(){
		if (sym == ident){
			Designator();
		}else if (sym == number){
			scan();
		} else if (sym == charCon){
			scan();
		} else if (sym == new_){
			scan();
			check(ident);
			Tab.find(t.string);
			if (sym == lbrack){
				scan();
				Expr();
				check(rbrack);
			}
		} else if (sym == lpar){
			scan();
			Expr();
			check(rpar);
		}else {
			error("Invalid Factor");
		}
	}

	//Designator =  ident {"." ident | "[" Expr "]"}.
	private static void Designator(){
		check(ident);
		Tab.find(t.string);
		for (;;){
			if (sym == period){
				scan();
				check(ident);
				Tab.find(t.string);
			} else if (sym == lbrack) {
				scan();
				Expr();
				check(rbrack);
			}else break;
		}
	}

	//Addop =  "+" | "-".
	private static void Addop(){
		if (sym == plus || sym == minus) scan();
		else error("Not valid addop");
	}

	//Mulop = "*" | "/" | "%".
	private static void Mulop(){
		if (sym == times || sym == slash || sym == rem) scan();
		else error("Invalid Mulop");
	}


	public static void parse() {
		// initialize symbol sets
		BitSet s;
		s = new BitSet(64); exprStart = s;
		s.set(ident); s.set(number); s.set(charCon); s.set(new_); s.set(lpar); s.set(minus);

		s = new BitSet(64); statStart = s;
		s.set(ident); s.set(if_); s.set(while_); s.set(read_);
		s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

		s = new BitSet(64); statSync = s;
		s.set(eof); s.set(if_); s.set(while_); s.set(read_);
		s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

		s = new BitSet(64); statSeqFollow = s;
		s.set(rbrace); s.set(eof);

		s = new BitSet(64); declStart = s;
		s.set(final_); s.set(ident); s.set(class_);

		s = new BitSet(64); declFollow = s;
		s.set(lbrace); s.set(void_); s.set(eof);

		s = new BitSet(64); relopStart = s;
		s.set(eql); s.set(neq);  s.set(gtr); s.set(geq); s.set(lss); s.set(leq);

		// start parsing
		Tab.init(); //Initialize symbol table
		errors = 0; errDist = 3;
		scan();
		Program();
		if (sym != eof) error("end of file found before end of program");
	}

}
