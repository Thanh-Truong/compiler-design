import java.util.*;

public class Type {
	String m_identName;

	int m_scopeNumber;

	private Id m_idType;

	// getters and setters
	public String getIdentName() {
		return m_identName;
	}

	public void setIdentName(String identName) {
		m_identName = identName;
	}

	public int getScopeNumber() {
		return m_scopeNumber;
	}

	public void setScopeNumber(int scopeNumber) {
		m_scopeNumber = scopeNumber;
	}

	public Id getIdType() {
		return m_idType;
	}

	public void setIdType(Id idType) {
		m_idType = idType;
	}

	// compare two objects	
	public boolean Equals(Type type) {
		return m_identName == type.getIdentName()
				&& m_scopeNumber == type.getScopeNumber()
				&& m_idType == type.getIdType();
	}
}
