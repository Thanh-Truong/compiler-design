import java.util.*;

// NOTE There are two scope levels in uC called as Global scope(env) and Local scope(env) respectively
public class MsgHelper {
	static void semanticMsg(String msg) {
		//System.out.println(msg);
	}

	static void semanticError(String msg, Position p) {
		msg = "-->ERROR " + msg;
		if (p != null) {
			msg += " at position " + p.toString();
		}
		System.out.println(msg);
	}
	
}
