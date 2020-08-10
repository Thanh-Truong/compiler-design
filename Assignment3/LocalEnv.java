import java.util.*;
//NOTE There are two scope levels in uC called as Global scope(env) and Local scope(env) respectively
class LocalEnv implements Env {
	
	Map<String, Type> m_symbolTable =  new HashMap<String, Type>();
	
	Env m_globalEnv; 
	
	Type m_result;
	
	LocalEnv(Env _globalEnv) {
		m_globalEnv = _globalEnv;
	}
	
	public void insert(String s, Type t, Position p) {
		System.out.println("Locally inserting "+s);
		if (m_symbolTable.containsKey(s)) {            
			MsgHelper.semanticError("Identifier "+s+" doubly defined", p);
		} else {       
			// calculate the scope number
			t.setScopeNumber(1);
			m_symbolTable.put(s,t);
		}
	}
	
	public Type lookup(String s) {
		System.out.println("Local lookup: "+s);
		Type r = m_symbolTable.get(s);
		if (r!=null) return r;
		else return m_globalEnv.lookup(s);
	}
	
	public void setResult(Type t) {
	}
	
	public Type getResult() {
		return m_result;
	}
	
	public Env enter() {
		throw new IllegalStateException("Already in local environment");
	}
	public int scopeNumber(){
		return 1;
	}
}
