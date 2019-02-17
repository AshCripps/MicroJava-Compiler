/* MicroJava Instruction Decoder  (HM 06-12-28)
   =============================
*/
package MJ.CodeGen;

public class Decoder {

	private static final int  // instruction codes
		load        =  1,
		load0       =  2,
		load1       =  3,
		load2       =  4,
		load3       =  5,
		store       =  6,
		store0      =  7,
		store1      =  8,
		store2      =  9,
		store3      = 10,
		getstatic   = 11,
		putstatic   = 12,
		getfield    = 13,
		putfield    = 14,
		const0      = 15,
		const1      = 16,
		const2      = 17,
		const3      = 18,
		const4      = 19,
		const5      = 20,
		const_m1    = 21,
		const_      = 22,
		add         = 23,
		sub         = 24,
		mul         = 25,
		div         = 26,
		rem         = 27,
		neg         = 28,
		shl         = 29,
		shr         = 30,
		new_        = 31,
		newarray    = 32,
		aload       = 33,
		astore      = 34,
		baload      = 35,
		bastore     = 36,
		arraylength = 37,
		pop         = 38,
		jmp         = 39,
		jeq         = 40,
		jne         = 41,
		jlt         = 42,
		jle         = 43,
		jgt         = 44,
		jge         = 45,
		call        = 46,
		return_     = 47,
		enter       = 48,
		exit        = 49,
		read        = 50,
		print       = 51,
		bread       = 52,
		bprint      = 53,
		trap		    = 54;

	private static byte[] code;		// code buffer
	private static int cur;			// address of next byte to decode
	private static int adr;			// address of currently decoded instruction

	private static int get() {
		return ((int)code[cur++])<<24>>>24;
	}

	private static int get2() {
		return (get()*256 + get())<<16>>16;
	}

	private static int get4() {
		return (get2()<<16) + (get2()<<16>>>16);
	}

	private static void P(String s) {
		System.out.println(adr+": "+s);
		adr = cur;
	}

	public static void decode(byte[] c, int off, int len) {
		int op;
		code = c;
		cur = off;
		adr = cur;
		while (cur < len) {
			switch(get()) {
				case load:      P("load "+get()); break;
				case load0:     P("load0"); break;
				case load1:     P("load1"); break;
				case load2:     P("load2"); break;
				case load3:     P("load3"); break;
				case store:     P("store "+get()); break;
				case store0:    P("store0"); break;
				case store1:    P("store1"); break;
				case store2:    P("store2"); break;
				case store3:    P("store3"); break;
				case getstatic: P("getstatic "+get2()); break;
				case putstatic: P("putstatic "+get2()); break;
				case getfield:  P("getfield "+get2()); break;
				case putfield:  P("putfield "+get2()); break;
				case const0:    P("const0"); break;
				case const1:    P("const1"); break;
				case const2:    P("const2"); break;
				case const3:    P("const3"); break;
				case const4:    P("const4"); break;
				case const5:    P("const5"); break;
				case const_m1:  P("const_m1"); break;
				case const_:    P("const "+get4()); break;
				case add:       P("add"); break;
				case sub:       P("sub"); break;
				case mul:       P("mul"); break;
				case div:       P("div"); break;
				case rem:       P("rem"); break;
				case neg:       P("neg"); break;
				case shl:       P("shl"); break;
				case shr:       P("shr"); break;
				case new_:      P("new "+get2()); break;
				case newarray:  P("newarray "+get()); break;
				case aload:     P("aload"); break;
				case astore:    P("astore"); break;
				case baload:    P("baload"); break;
				case bastore:   P("bastore"); break;
				case arraylength: P("arraylength"); break;
				case pop:       P("pop"); break;
				case jmp:       P("jmp "+get2()); break;
				case jeq:       P("jeq "+get2()); break;
				case jne:       P("jne "+get2()); break;
				case jlt:       P("jlt "+get2()); break;
				case jle:       P("jle "+get2()); break;
				case jgt:       P("jgt "+get2()); break;
				case jge:       P("jge "+get2()); break;
				case call:      P("call "+get2()); break;
				case return_:   P("return"); break;
				case enter:     P("enter "+get()+" "+get()); break;
				case exit:      P("exit"); break;
				case read:      P("read"); break;
				case print:     P("print"); break;
				case bread:     P("bread"); break;
				case bprint:    P("bprint"); break;
				case trap:      P("trap "+get()); break;
				default:        P("-- error--"); break;
			}
		}
	}
}