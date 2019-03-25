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
	private static Obj curMeth;

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
		Code.dataSize = Tab.curScope.nVars;
		Tab.closeScope();
	}

	//ConstDecl =  "final" Type ident "=" (number | charConst) ";".
	private static void ConstDecl(){
		check(final_);
		Struct type = Type();
		check(ident);
		String name = t.string;
		Obj obj = Tab.insert(Obj.Con, name, type);

		check(assign);
		if (sym == number) {
			if (type != Tab.intType){
				error("Int const expected");
			}
			scan();
			obj.val = t.val;
		}
		else if(sym == charCon) {
			if (type != Tab.charType){
				error("Cha const expected");
			}
			scan();
			obj.val = t.val;
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
		Struct type = new Struct(Struct.Class);
		String name;
		check(class_);
		check(ident);
		name = t.string;
		curMeth = Tab.insert(Obj.Type, name, type);
		Tab.openScope();
		check(lbrace);
		while(sym == ident){
			VarDecl();
			type.fields = Tab.curScope.locals;
			type.nFields = Tab.curScope.nVars;
		}
		check(rbrace);
		Tab.closeScope();
	}

	//MethodDecl =  (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block.
	private static void MethodDecl(){
		Struct type = Tab.noType;
		String name;
		int n = 0;
		if (sym == ident){
			type = Type();
		}else if (sym == void_){
			scan();
		}else {
			error("Invalid Method Declaration");
		}
		check(ident);
		name = t.string;
		curMeth = Tab.insert(Obj.Meth, name, type);
		Tab.openScope();
		check(lpar);
		if (sym == ident) {
			n = Formpars();
		}
		curMeth.nPars = n;
		if (name.equals("main")){
			Code.mainPc = Code.pc;
			if (curMeth.type != Tab.noType) error("Main method must be void");
			if (curMeth.nPars != 0) error("Main method must not have parameters");
		}
		curMeth.nPars = Tab.curScope.nVars;
		check(rpar);
			while (sym == ident) VarDecl();
		curMeth.locals = Tab.curScope.locals;
		curMeth.adr = Code.pc;
		Code.put(Code.enter);
		Code.put(curMeth.nPars);
		Code.put(Tab.curScope.nVars);
		Block();
		if (curMeth.type == Tab.noType) {
			Code.put(Code.exit);
			Code.put(Code.return_);
		} else {  // end of function reached without a return statement
			Code.put(Code.trap);
			Code.put(1);
		}
		Tab.closeScope();
	}

	//FormPars =  Type ident  {"," Type ident}.
	private static int Formpars(){
		int n = 0;
		Struct type;
		String name;
		type = Type();
		check(ident);
		name = t.string;
		Tab.insert(Obj.Var, name, type);
		n++;
			while (sym == comma){
				scan();
				type = Type();
				check(ident);
				name = t.string;
				Tab.insert(Obj.Var, name, type);
				n++;
		}
		return n;

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
		Operand x, y;
		int op;
		if (!statStart.get(sym)){
			error("invalid start of statement");
			while(!statSync.get(sym)) scan();
			errDist = 0;
		}
		if (sym == ident){
			x = Designator();
			if (sym == assign){
				scan();
				y = Expr();
				if (y.type.assignableTo(x.type)) Code.assign(x, y);
				else error("Invalid Types");
			}else if (sym == lpar){
				ActPars();
			} else error("Invalid Assignment or call");
			check(semicolon);
		}else if (sym == if_) {
			scan();
			check(lpar);
			op = Condition();
			check(rpar);
			Code.putFalseJump(op, 0);
			int adr = Code.pc - 2;
			Statement();
			if (sym == else_){
				scan();
				Code.putJump(0);
				int adr2 = Code.pc - 2;
				Code.fixup(adr);
				Statement();
				Code.fixup(adr2);
			}
			Code.fixup(adr);
		}else if (sym == while_){
			scan();
			int top = Code.pc;
			check(lpar);
			op = Condition();
			check(rpar);
			Code.putFalseJump(op, 0);
			int adr = Code.pc - 2;
			Statement();
			Code.putJump(top);
			Code.fixup(adr);
		}else if (sym == return_){
			scan();
			if (sym == minus || sym == ident){
				x = Expr();
				Code.load(x);
				if (curMeth.type == Tab.noType){
					error("Void method must not return a value");
				}else if (!x.type.assignableTo(curMeth.type)){
					error("Type of return value must match method type");
				}
			}else {
				if (curMeth.type != Tab.noType) error("return value expected");
			}
			Code.put(Code.exit);
			Code.put(Code.return_);
			check(semicolon);
		}else if (sym == read_){
			scan();
			check(lpar);
			x = Designator();
			check(rpar);
			check(semicolon);
		}else if (sym == print_){
			scan();
			check(lpar);
			x = Expr();
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
	private static int Condition(){
		int op;
		Operand x, y;
		x = Expr();
		Code.load(x);
		op = Relop();
		y = Expr();
		Code.load(y);
		if (!x.type.compatibleWith(y.type)) error ("Type Mismatch");
		if (x.type.isRefType() && op != Code.eq && op != Code.ne) error("Invalid Compare");
		return op;
	}

	//Relop =  "==" | "!=" | ">" | ">=" | "<" | "<=".
	private static int Relop(){
		if (sym == eql) {
			scan();
			return Code.eq;
		}else if (sym == neq) {
			scan();
			return Code.ne;
		}else if (sym == gtr) {
			scan();
			return Code.gt;
		}else if (sym == geq) {
			scan();
			return Code.ge;
		}else if (sym == lss) {
			scan();
			return Code.lt;
		}else if (sym == leq) {
			scan();
			return Code.le;
		}else {
			error("invalid Relop");
			return Code.eq;
		}
	}

	//Expr =  ["-"] Term {Addop Term}.
	private static Operand Expr(){
		Operand x;
		int op;
		if (sym == minus) {
			scan();
			x = Term();
			if (x.type != Tab.intType) error("operand must be of type int");
			if (x.kind == Operand.Con) x.val = - x.val;
			else{
				Code.load(x);
				Code.put(Code.neg);
			}
		}
		else{
			x = Term();
		}
		while (true){
			if (sym == minus){
				op = Code.sub;
				scan();
			}else if (sym == plus){
				op = Code.add;
				scan();
			} else break;
			Code.load(x);
			Operand y = Term();
			Code.load(y);
			if (x.type != Tab.intType || y.type != Tab.intType){
				error("operands must be of type int");
			}
			Code.put(op);
		}
		return x;
	}

	//Term =  Factor {Mulop Factor}.
	private static Operand Term(){
		Operand x, y;
		int op;
		x = Factor();
		while (true){
			if (sym == times){
				op = Code.mul;
				scan();
			}else if (sym == slash){
				op = Code.div;
				scan();
			}else if (sym == rem){
				op = Code.rem;
				scan();
			}else break;
			Code.load(x);
			y = Factor();
			Code.load(y);
			if (x.type != Tab.intType || y.type != Tab.intType){
				error("operands must be of type int");
			}
			Code.put(op);
		}
		return x;
	}

	//Factor =  Designator [ActPars] |  number |  charConst |  "new" ident ["[" Expr "]"] |  "(" Expr ")".
	private static Operand Factor(){
		Operand x;
		int val;
		String name;
		if (sym == ident){
			x = Designator();
		}else if (sym == number){
			scan();
			x = new Operand(t.val);

		} else if (sym == charCon){
			scan();
			x = new Operand(t.val);
			x.type = Tab.charType;

		} else if (sym == new_){
			scan();
			check(ident);
			name = t.string;
			Obj obj = Tab.find(t.string);
			Struct type = obj.type;
			if (sym == lbrack){
				scan();
				if (obj.kind != Obj.Type) error("Type expected");
				x = Expr();
				check(rbrack);
				if (x.type != Tab.intType) error("Array size must be of type int");
				Code.load(x);
				Code.put(Code.newarray);
				if (type == Tab.charType) Code.put(0);
				else Code.put(1);
				type = new Struct(Struct.Arr, type);
			}else {
				if(obj.kind != Obj.Type || type.kind != Struct.Class) error("Class type expected");
				Code.put(Code.new_);
				Code.put2(type.nFields);
			}
			x = new Operand(Operand.Stack, 0, type);

		} else if (sym == lpar){
			scan();
			x = Expr();
			check(rpar);
		}else {
			error("Invalid Factor");
			x = new Operand(Operand.Stack, 0, Tab.noType);
		}
		return x;
	}

	//Designator =  ident {"." ident | "[" Expr "]"}.
	private static Operand Designator(){
		check(ident);
		Obj obj = Tab.find(t.string);
		Operand x = new Operand(obj);
		for (;;){
			if (sym == period){
				scan();
				check(ident);
				String fname = t.string;
				if (x.type.kind == Struct.Class){
					Code.load(x);
					Obj fld = Tab.findField(fname, x.type);
					x.kind = Operand.Fld;
					x.adr = fld.adr;
					x.type = fld.type;
				}else error(name + "is not an Object");

			} else if (sym == lbrack) {
				Code.load(x);
				scan();
				Operand y = Expr();
				if (x.type.kind == Struct.Arr){
					if (y.type.kind != Struct.Int) error("Index must be of type int");
					Code.load(y);
					x.kind = Operand.Elem;
					x.type = x.type.elemType;
				}else error(name + " is not an array");
				check(rbrack);
			}else break;
		}
		return x;
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
		Code.init(); //Initialize code buffer
		errors = 0; errDist = 3;
		scan();
		Program();
		if (sym != eof) error("end of file found before end of program");
	}

}
