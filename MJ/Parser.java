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
		Tab.insert(Obj.Prog, t.string, Tab.noType); //Insert program to symtable
		Tab.openScope(); //Open program scope
		for(;;){ //Endless loop
			if (sym == final_){
				ConstDecl();
			}else if (sym == class_){
				ClassDecl();
			}else if (sym == ident){
				VarDecl();
			}else if (sym == lbrace || sym == eof) {
				break;
			} else {
				error("Invalid declaration");
				do scan(); while (sym != final_ && sym != class_ && sym != lbrace && sym != eof); //Sync point
				errDist = 0;
			}
		}
		check(lbrace);
		while (sym == void_ || sym == ident){
			MethodDecl();
		}
		check(rbrace);
		Tab.dumpScope(Tab.curScope.locals);  //Dump scope variables
		Code.dataSize = Tab.curScope.nVars; //Set number of vars
		Tab.closeScope(); //Close program scope
	}

	//ConstDecl =  "final" Type ident "=" (number | charConst) ";".
	private static void ConstDecl(){
		check(final_);
		Struct type = Type();
		check(ident);
		String name = t.string;
		Obj obj = Tab.insert(Obj.Con, name, type); //Insert Const in symtable

		check(assign);
		if (sym == number) {
			if (type != Tab.intType){ //If the expected type is not int, a char is expected
				error("Char const expected");
			}
			scan();
			obj.val = t.val;
		}
		else if(sym == charCon) {
			if (type != Tab.charType){
				error("Int const expected"); //If the expected type is not char, an int is expected
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
		curMeth = Tab.insert(Obj.Type, name, type); //Track the current class being compiled
		Tab.openScope(); //Open class scope
		check(lbrace);
		while(sym == ident){
			VarDecl();
			type.fields = Tab.curScope.locals; //Set the fields of the class
			type.nFields = Tab.curScope.nVars; //Set the number of fields expected
		}
		check(rbrace);
		Tab.closeScope(); //Close class scope
	}

	//MethodDecl =  (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block.
	private static void MethodDecl(){
		Struct type = Tab.noType; //Set to void by default
		String name;
		int n = 0; //Set number of formpars to 0 by default
		if (sym == ident){
			type = Type(); //Change return type of the method
		}else if (sym == void_){
			scan();
		}else {
			error("Invalid Method Declaration"); //Must be a type or void
		}
		check(ident);
		name = t.string;
		curMeth = Tab.insert(Obj.Meth, name, type); //keep track of the current method
		Tab.openScope(); //Open the methods scope
		check(lpar);
		if (sym == ident) {
			n = Formpars(); //return the number of formpars expected
		}
		curMeth.nPars = n; //Set this methods number of formpars
		if (name.equals("main")){
			Code.mainPc = Code.pc; //Set current method as main
			if (curMeth.type != Tab.noType) error("Main method must be void");
			if (curMeth.nPars != 0) error("Main method must not have parameters");
		}
		curMeth.nPars = Tab.curScope.nVars;
		check(rpar);
			while (sym == ident) VarDecl();
		curMeth.locals = Tab.curScope.locals; //Set methods local variables
		curMeth.adr = Code.pc; //Set methods address for scope
		Code.put(Code.enter);
		Code.put(curMeth.nPars); //Put the number of parameters on the code buffer
		Code.put(Tab.curScope.nVars); //Put the number of variables on the code buffer
		Block();
		if (curMeth.type == Tab.noType) {
			Code.put(Code.exit);
			Code.put(Code.return_); //Return from this method
		} else {  // end of function reached without a return statement
			Code.put(Code.trap);
			Code.put(1);
		}
		Tab.closeScope(); //Close the method scope
	}

	//FormPars =  Type ident  {"," Type ident}.
	private static int Formpars(){
		int n = 0; //Keep track of the number of formpars to ensure the method recieves enough actpars
		Struct type;
		String name;
		type = Type(); //Receive type of parameter
		check(ident);
		name = t.string;
		Tab.insert(Obj.Var, name, type); //Insert parameter into symtable
		n++;
			while (sym == comma){
				scan();
				type = Type();
				check(ident);
				name = t.string;
				Tab.insert(Obj.Var, name, type); //Insert parameter into symtable
				n++;
		}
		return n; //Return number of formpars

	}

	//Type =  ident ["[" "]"].
	private static Struct Type(){
		check(ident);
		Obj o = Tab.find(t.string); //Check in symtable for the type
		if (o.kind == Obj.Var) error ("Type expected"); //The type cannot be a variable - must be a class or predefined type
		Struct type = o.type;
		if (sym == lbrack){
			type = new Struct(Struct.Arr, type); //Turn type into an array of type
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
		Operand x, y; //Hold the operands from Designator or Expr
		int op;
		if (!statStart.get(sym)){
			error("Invalid start of statement");
			while(!statSync.get(sym)) scan();
			errDist = 0;
		}
		if (sym == ident){
			x = Designator();
			if (sym == assign){
				scan();
				y = Expr();
				if (y.type.assignableTo(x.type)) Code.assign(x, y); //Check expr can be assigned to Designator
				else error("Invalid Types");
			}else if (sym == lpar){
				if (x.kind != Operand.Meth) error("Called object is not a method"); //Must be a method
				ActPars(x); //Pass method to actpars
			} else error("Invalid Assignment or call");
			check(semicolon);
		}else if (sym == if_) {
			scan();
			check(lpar);
			op = Condition();
			check(rpar);
			Code.putFalseJump(op, 0); //Placeholder for if statment to check for else
			int adr = Code.pc - 2; //Go back two spaces in instructions to check for else
			Statement();
			if (sym == else_){
				scan();
				Code.putJump(0);
				int adr2 = Code.pc - 2;
				Code.fixup(adr); //Replace if Placeholder instruction
				Statement();
				Code.fixup(adr2); //Replace else placeholder instruction
			}
			Code.fixup(adr); //Replace if placeholder jump
		}else if (sym == while_){
			scan();
			int top = Code.pc; //Set position of top of while instruction
			check(lpar);
			op = Condition();
			check(rpar);
			Code.putFalseJump(op, 0); //Placeholder for while statement
			int adr = Code.pc - 2;
			Statement();
			Code.putJump(top); //Put jump at top of while instruction
			Code.fixup(adr); //Replace placholder
		}else if (sym == return_){
			scan();
			if (curMeth.type == Tab.noType){ //Ensure method is not void
				error("Void method must not return a value");
			}
			if (sym == minus || sym == ident){
				x = Expr();
				Code.load(x); //Load expression to code buffer
				if (!x.type.assignableTo(curMeth.type)){ //Ensure return type matches method
					error("Type of return value must match method type");
				}
			}else { //No return value found
				if (curMeth.type != Tab.noType) error("return value expected");
			}
			Code.put(Code.exit);
			Code.put(Code.return_);
			check(semicolon);
		}else if (sym == read_){
			scan();
			check(lpar);
			x = Designator(); //Variable to read to
			if (x.type.kind == Struct.None) error("Read object must be a variable"); //Must be a variable
			if (x.type.kind != Struct.Int || x.type.kind != Struct.Int) error("Can only read int or char variables"); //Cannot be any other types
			check(rpar);
			check(semicolon);
		}else if (sym == print_){
			scan();
			check(lpar);
			x = Expr(); //Variable to print
			if (x.type.kind == Struct.None) error("Print object must be a variable");
			if (x.type.kind != Struct.Int || x.type.kind != Struct.Int) error("Can only print int or char variables");
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
	private static void ActPars(Operand x){ //Passed the operand which contains the formpars required
		check(lpar);
		if (x.kind != Operand.Meth) { //Check the operand is a method
			error("Not a method");
			x.obj = Tab.noObj;
		}
		Obj fp = x.obj.locals; //Collect the operands formpars
		int aPars = 0; //Set number of apars collected to 0
		int fPars = x.obj.nPars; //Set number of expected formpars
		if (exprStart.get(sym)){
			x = Expr();
			aPars++; //Increment apars
			if (fp != null){ //check there is a formpar to compare
				if (!x.type.assignableTo(fp.type)) error("Parameter type mismatch"); //Check for type matching
				fp = fp.next; //Select next formpar
			}
			while (sym == comma){
					scan();
					x = Expr();
					aPars++;
					if (fp != null){
						if (!x.type.assignableTo(fp.type)) error("Parameter type mismatch");
						fp = fp.next;
					}
			}
		}
		if (aPars > fPars) error("More acutal parameters than formal parameters"); //Too many parameters
		if (aPars < fPars) error("Fewer actual parameters than formal parameters"); //Too few parameters
		check(rpar);
	}

	//Condition =  Expr Relop Expr.
	private static int Condition(){
		int op; //Operator
		Operand x, y;
		x = Expr();
		Code.load(x); //Load expr to code buffer
		op = Relop();
		y = Expr();
		Code.load(y); //Load expr to code buffer
		if (!x.type.compatibleWith(y.type)) error ("Type Mismatch");
		if (x.type.isRefType() && op != Code.eq && op != Code.ne) error("Invalid Compare");
		return op; //Return operator to be loaded to code buffer
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
			error("Invalid Relop");
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
			if (x.type != Tab.intType) error("Operand must be of type int"); //If minus expression it must be an integer to be negative
			if (x.kind == Operand.Con) x.val = - x.val; //Set to negative
			else{
				Code.load(x);
				Code.put(Code.neg);
			}
		}
		else{
			x = Term();
		}
		while (true){ //endless loop for repetition in production
			if (sym == minus){
				op = Code.sub; //Subtraction
				scan();
			}else if (sym == plus){
				op = Code.add; //Addition
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
		while (true){ //Endless loop for repetition in production
			if (sym == times){
				op = Code.mul; //Multiplcation
				scan();
			}else if (sym == slash){
				op = Code.div; //Division
				scan();
			}else if (sym == rem){
				op = Code.rem; //modulus
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
			x.type = Tab.charType; //Charcon type

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
				Code.put(Code.newarray); //Put array on code buffer
				if (type == Tab.charType) Code.put(0);
				else Code.put(1);
				type = new Struct(Struct.Arr, type);
			}else {
				if(obj.kind != Obj.Type || type.kind != Struct.Class) error("Class type expected");
				Code.put(Code.new_);
				Code.put2(type.nFields);
			}
			x = new Operand(Operand.Stack, 0, type); //Create operand

		} else if (sym == lpar){
			scan();
			x = Expr();
			check(rpar);
		}else {
			error("Invalid Factor");
			x = new Operand(Operand.Stack, 0, Tab.noType); //create empty operand to ensure the compiler continues
		}
		return x;
	}

	//Designator =  ident {"." ident | "[" Expr "]"}.
	private static Operand Designator(){
		check(ident);
		Obj obj = Tab.find(t.string); //Retrieve ident from symtable
		Operand x = new Operand(obj); //Create a new operand from the returned object
		for (;;){
			if (sym == period){
				scan();
				check(ident);
				String fname = t.string;
				if (x.type.kind == Struct.Class){ //Check that it is a class otherwise error
					Code.load(x);
					Obj fld = Tab.findField(fname, x.type); //Retrieve the field from the class
					x.kind = Operand.Fld; //Set to kind field
					x.adr = fld.adr; //Set address to field address
					x.type = fld.type; //Set type to returned field type
				}else error(name + "is not an Object");

			} else if (sym == lbrack) {
				scan();				
				if (x.type.kind == Struct.Arr){ //Check is array otherwise error
					Code.load(x);
					Operand y = Expr();
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

		s = new BitSet(64); statSync = s; //Statement sync set
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
