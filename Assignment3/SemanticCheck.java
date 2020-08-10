
public class SemanticCheck {
	private boolean m_result;
	private Id m_type;
	private Node m_node;
	
	public SemanticCheck(Node node) {
		m_result = true;
		m_type = Id.VOID;
		m_node = node;
	}
	
	public boolean getResult() {
		return m_result;
	}
	public void setResult(boolean result) {
		m_result = result;
	}
	public Id getType() {
		return m_type;
	}
	public void setType(Id type) {
		m_type = type;
	}
	
	public Node getNode() {
		return m_node;
	}

	public void setNode(Node node) {
		m_node = node;
	}

	public void combine(SemanticCheck chk){
		m_result = m_result && chk.getResult();	
	}	
}
