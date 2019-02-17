/* MicroJava Scanner Token  (HM 06-12-28)
   =======================
*/
package MJ;

public class Token {
	public int kind;		// token kind
	public int line;		// token line
	public int col;			// token column
	public int val;			// token value (for number and charConst)
	public String string;	// token string
}