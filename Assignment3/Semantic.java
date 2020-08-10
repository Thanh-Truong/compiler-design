import java.util.*;


/**
 * @author Thanh, Truong
 * This class aims to check Semantic for syntax tree
 *
 */
class Semantic {
	
	
	static boolean checkProgram(Node program) {

		Env env = new GlobalEnv();
		SemanticCheck programFlag = new SemanticCheck(new Node(Id.VOID));

		for (Node stmnt : program.getChildren()) {
			SemanticCheck stmntFlag = checkNode(stmnt, env);
			programFlag.combine(stmntFlag);
		}
		return programFlag.getResult();
	}

	static SemanticCheck checkNode(Node node, Env env) {		
		SemanticCheck semanticResult = new SemanticCheck(node);
		boolean recursive = true;
		switch (node.getId()) {
		case VOID:
		case INT:
		case CHAR:		
		case EMPTY_STMNT:
			semanticResult.setType(node.getId());		
			recursive = false;
			break;
		case INTEGER_LITERAL:
			semanticResult.setType(Id.INT);		
			recursive = false;
			break;
		case ARR_TYPE:
			semanticResult = checkArrTypeNode(node, env);
			semanticResult.setType(node.getId());
			recursive = false;
			break;
		case EXTERN:
			semanticResult = checkExtern(node, env);
			break;
		case FUNC:
			semanticResult = checkFunctionNode(node, env);
			recursive = false;
			break;
		case SCALAR_TYPE:
			semanticResult = checkScalarTypeNode(node, env);
			recursive = false;
			break;
		case ASSIGN:
			semanticResult = checkAssignNode(node, env);
			recursive = false;
			break;
		case IDENT:
			semanticResult = checkIndentifierNode(node, env);
			recursive = false;
			break;
		case BINARY:
			semanticResult = checkBinaryNode(node, env);
			recursive = false;
			break;
		case VARDEC:
			// it will check its children
			break;
		case UNARY:
			break;

		case ARRAY: // Expr = Ident [Expr]
			semanticResult = checkArrayNode(node, env);
			break;
		case RETURN:
			semanticResult = checkReturnNode(node, env);
			recursive = false;
			break;
		case FCALL:
			semanticResult = checkFCallNode(node, env);
			recursive = false;
			break;
		case FUNCBODY:
			semanticResult = checkFuncBodyNode(node, env);
			recursive = false;
			break;
		default:
			MsgHelper.semanticMsg("Not yet implemented: " + node.getId());
		}
		if (node.numberOfChildren() > 0 && recursive) {
			for (Node child : node.getChildren()) {
				SemanticCheck stmntFlag = checkNode(child, env);
				semanticResult.combine(stmntFlag);
			}
		}
		node.setChecked(true);
		return semanticResult;
	}

	static SemanticCheck checkAssignNode(Node node, Env env) {
		SemanticCheck smticResult = new SemanticCheck(node);
		Node var = node.getChild(0, 2);
		String name = "";
		
		Id dataTypeLeftHandSide = Id.VOID;
		Type identType = null;
		
		if (var.getId() == Id.IDENT) {
			name = ((IdentifierNode)var).getName();
			identType = env.lookup(name);
			if (identType instanceof FunctionDeclarationType) {
				smticResult.setResult(false);
				MsgHelper.semanticError("Can not assign value to function " , node.getPosition());
				return smticResult;
			}
			dataTypeLeftHandSide = identType.getIdType();
			
		} else if(var.getId() == Id.ARRAY){
			
			Node identNode = var.getChild(0);
			name = ((IdentifierNode)identNode).getName();
			identType = env.lookup(name);
			
			if ((identType instanceof ArrayType)) {
				dataTypeLeftHandSide = ((ArrayType)identType).getTypeOfArray();
			} else {
				smticResult.setResult(false);
				MsgHelper.semanticError("Left-hand-side of assignment is wrong " , node.getPosition());
				return smticResult;
			}
		} else {
			smticResult.setResult(false);
			MsgHelper.semanticError("Left-hand-side of assignment is wrong " , node.getPosition());
			return smticResult;
		}

		if (identType != null) {
			// continue check the Right hand side of ASSIGN
			Node exp = node.getChild(1, 2);
			SemanticCheck smticRightNode = checkNode(exp, env);

			if ((dataTypeLeftHandSide == Id.CHAR && smticRightNode.getType() == Id.INT)
				|| (dataTypeLeftHandSide == Id.INT && smticRightNode.getType() == Id.CHAR)){
				
			}else if (dataTypeLeftHandSide != smticRightNode.getType()) {
				MsgHelper.semanticError(
						"Left-hand-side's datatype and Right-hand-side's datatype are not the same  "
								+ name, var.getPosition());
				
			}

		} else {
			smticResult.setResult(false);
			MsgHelper.semanticError("Variable " + name
					+ " not defined.", node.getPosition());
		}
		return smticResult;
	}

	// declare int x;
	static SemanticCheck checkScalarTypeNode(Node node, Env env) {
		SemanticCheck result = new SemanticCheck(node);
		MsgHelper.semanticMsg("Checking SCALAR_TYPE");
		// get the data type
		Node leftNode = node.getChild(0, 2);
		result.setType(leftNode.getId());
		
		// get the identifier
		IdentifierNode rightNode = (IdentifierNode) node.getChild(1, 2);

		String nodeName = rightNode.getName();
		Type t = env.lookup(nodeName);

		if (t != null && t.getScopeNumber() == env.scopeNumber()) {
			MsgHelper.semanticError("Duplicated " + nodeName, rightNode
					.getPosition());
			result.setResult(false);

		} else {
			// insert new declaration
			t = new Type();
			t.setIdentName(nodeName);
			t.setIdType(leftNode.getId());

			env.insert(nodeName, t, leftNode.getPosition());
		}
		MsgHelper.semanticMsg("Ending SCALAR_TYPE");
		return result;
	}

	static SemanticCheck checkIndentifierNode(Node node, Env env) {
		SemanticCheck result = new SemanticCheck(node);
		MsgHelper.semanticMsg("Checking IDENTIFIER");
		String name = ((IdentifierNode) node).getName();
		Type t = env.lookup(name);
		if (t == null) {
			result.setResult(false);
			MsgHelper.semanticError("Variable " + name + " not defined.", node
					.getPosition());
		}else {
			result.setType(t.getIdType());
		}
		MsgHelper.semanticMsg("Ending IDENTIFIER");
		return result;
	}

	/**
	 * In this method, we are gonna deal with scope handling
	 * 
	 * @param fNode
	 * @param env
	 * @return
	 */
	static SemanticCheck checkFunctionNode(Node fNode, Env env) {
		SemanticCheck result = new SemanticCheck(fNode);
		MsgHelper.semanticMsg("Checking FUNC");
		// get the function type
		Node fTypeNode = fNode.getChild(0);
		result.setType(fTypeNode.getId());

		// get the identifier (function Name)
		IdentifierNode fNameNode = (IdentifierNode) fNode.getChild(1);
		String name = fNameNode.getName();
		if (env.lookup(name) != null) {
			result.setResult(false);
					
			MsgHelper.semanticError("Duplicated name " + name, fNameNode
					.getPosition());
		} else {
			FunctionDeclarationType t = new FunctionDeclarationType();
			t.setIdentName(name);
			t.setIdType(fTypeNode.getId());
			Node argsNode = fNode.getChild(2);
			if (argsNode.getId() == Id.FORMAL) {
				t.setNumberOfParameters(argsNode.numberOfChildren());
			}
			env.insert(name, t, fNameNode.getPosition());
		}

		LocalEnv localEnv = new LocalEnv(env);
		// get the formal node( list of arguements)
		/*
		 * c1 : VOID c 2: FORMAL SCALAR_TYPE INT @ [7,8-10] IDENT @ [7,12] i
		 * 
		 */
		Node argsNode = fNode.getChild(2);
		if (argsNode.getId() == Id.FORMAL) {
			// check arguements from L to R
			for (Node arg : argsNode.getChildren()) {
				SemanticCheck stmntFlag = checkNode(arg, localEnv);
				result.combine(stmntFlag);
			}
		}

		// Check function body
		Node funcBody = fNode.getChild(3);
		SemanticCheck bodyCheck = checkNode(funcBody, localEnv);
		result.combine(bodyCheck);
		// check Function type and RETURN statement
		checkFunctionTypeAndReturn(name, result, bodyCheck);

		MsgHelper.semanticMsg("Ending FUNC");
		return result;
	}

	private static void checkFunctionTypeAndReturn(String fname, SemanticCheck functionCheck,
			SemanticCheck bodyCheck) {
		
		// exception for main method
		if(fname.equals("main") && bodyCheck.getType() == Id.VOID) {
			return;
		}
		if ((functionCheck.getType() == Id.CHAR && bodyCheck.getType() == Id.INT)
				|| (functionCheck.getType() == Id.INT && bodyCheck.getType() == Id.CHAR)) {
			return;

		}
		
		if (functionCheck.getType() != bodyCheck.getType()) {
			functionCheck.combine(bodyCheck);
			String msg = "Imcompatible type between FUNC ";
			// Function
			IdentifierNode fNameNode = (IdentifierNode) functionCheck.getNode()
					.getChild(1);
			String name = fNameNode.getName();
			msg += name;
			if (functionCheck.getNode().getPosition() != null) {
				msg += fNameNode.getPosition().toString();
			}

			// Return type
			msg += " and its RETURN statement OR RETURN is missing ";
			if (bodyCheck.getNode().getPosition() != null) {
				msg += bodyCheck.getNode().getId()
						+ bodyCheck.getNode().getPosition().toString();
			}
			functionCheck.setResult(false);
			MsgHelper.semanticError(msg, null);
		}
	}

	/*
	 * ARR_TYPE INT @ [3,1-3] IDENT @ [3,5] a INTEGER_LITERAL @ [3,7] 4
	 */
	static SemanticCheck checkArrTypeNode(Node node, Env env) {
		SemanticCheck smanticResult = new SemanticCheck(node);
		MsgHelper.semanticMsg("Checking ARR_TYPE");

		// get the identifier
		IdentifierNode fNameNode = (IdentifierNode) node.getChild(1);
		String name = fNameNode.getName();
		Type t = env.lookup(name);
		if (t != null && t.getScopeNumber() == env.scopeNumber()) {
			smanticResult.setResult(false);
			MsgHelper.semanticError("Duplicated name " + name, fNameNode
					.getPosition());
		} else {
			ArrayType art = new ArrayType();
			art.setIdentName(name);
			art.setIdType(Id.ARRAY);
			Node primitiveTypeNode = node.getChild(0);
			art.setTypeOfArray(primitiveTypeNode.getId());
			env.insert(name, art, fNameNode.getPosition());
		}
		MsgHelper.semanticMsg("Ending ARR_TYPE");
		return smanticResult;
	}

	/*
	 * EXTERN VOID IDENT @ [25,6-13] printInt FORMAL SCALAR_TYPE INT @
	 * [25,15-17] IDENT @ [25,19] x
	 */
	static SemanticCheck checkExtern(Node fNode, Env env) {
		SemanticCheck smanticResult = new SemanticCheck(fNode);
		MsgHelper.semanticMsg("Checking EXTERN");
		// get the identifier (function Name)
		IdentifierNode fNameNode = (IdentifierNode) fNode.getChild(1);
		String name = fNameNode.getName();
		if (env.lookup(name) != null) {
			smanticResult.setResult(false);
			MsgHelper.semanticError("Duplicated name " + name, fNameNode
					.getPosition());
		} else {
			FunctionDeclarationType t = new FunctionDeclarationType();
			t.setIdentName(name);
			t.setIdType(fNameNode.getId());
			
			Node argsNode = fNode.getChild(2);
			if (argsNode.getId() == Id.FORMAL) {
				t.setNumberOfParameters(argsNode.numberOfChildren());
			}
			env.insert(name, t, fNameNode.getPosition());
		}

		/*LocalEnv localEnv = new LocalEnv(env);
		// get the formal node( list of arguements)
		
		 * c1 : VOID c 2: FORMAL SCALAR_TYPE INT @ [7,8-10] IDENT @ [7,12] i
		 * 
		 
		Node argsNode = fNode.getChild(2);
		if (argsNode.getId() == Id.FORMAL) {
			// check arguements from L to R
			for (Node arg : argsNode.getChildren()) {
				SemanticCheck stmntFlag = checkNode(arg, localEnv);
				smanticResult.combine(stmntFlag);
			}
		}*/
		MsgHelper.semanticMsg("Ending EXTERN");
		return smanticResult;
	}

	/*
	 * FUNCBODY VARDEC ARR_TYPE INT @ [22,3-5] IDENT @ [22,7] k INTEGER_LITERAL @
	 * [22,9] 3 STMNT ASSIGN IDENT @ [23,3] y IDENT @ [23,7] z RETURN
	 * INTEGER_LITERAL @ [24,10] 1
	 */
	static SemanticCheck checkFuncBodyNode(Node fNode, Env env) {
		SemanticCheck smanticResult = new SemanticCheck(fNode);
		MsgHelper.semanticMsg("Checking FUNCBODY");
		// get the declaration | Empty statement Node
		Node varDecNode = fNode.getChild(0);
		SemanticCheck smticVarDec = checkNode(varDecNode, env);

		smanticResult.combine(smticVarDec);

		// get the Statement Node ( tree of Statement) or Empty statment Node
		Node statementsNode = fNode.getChild(1);

		if (statementsNode.getId() == Id.STMNT) {
			// check each statement from L to R
			for (Node smt : statementsNode.getChildren()) {
				SemanticCheck stmntFlag = checkNode(smt, env);
				if (smt.getId() == Id.RETURN) {
					smanticResult.setType(stmntFlag.getType());
				}
				smanticResult.combine(stmntFlag);
			}
		}

		MsgHelper.semanticMsg("Ending FUNCBODY");
		return smanticResult;
	}

	/*
	 * BINARY @ [27,9-13] PLUS INTEGER_LITERAL @ [27,9] 2 INTEGER_LITERAL @
	 * [27,13] 3
	 */
	static SemanticCheck checkBinaryNode(Node node, Env env) {
		SemanticCheck smanticResult = new SemanticCheck(node);
		BinaryNode bNode = (BinaryNode) node;
		MsgHelper.semanticMsg("Checking BINARY");
		// check data type of two operators

		SemanticCheck operator1 = checkNode(bNode.getChild(0), env);
		SemanticCheck operator2 = checkNode(bNode.getChild(1), env);

		smanticResult.combine(operator1);
		smanticResult.combine(operator2);
		if (bNode.getOp() == Binop.PLUS || bNode.getOp() == Binop.MINUS
				|| bNode.getOp() == Binop.MUL || bNode.getOp() == Binop.DIV) {

			if ((operator1.getType() == Id.INT || operator1.getType() == Id.INTEGER_LITERAL)
				&& (operator2.getType() == Id.INT || operator2.getType() == Id.INTEGER_LITERAL)) {				
				smanticResult.setType(Id.INT);
			}
			if (operator1.getType() == operator2.getType() &&  operator2.getType() == Id.CHAR){
				smanticResult.setType(Id.CHAR);
			}
		}

		MsgHelper.semanticMsg("Ending BINARY");
		return smanticResult;
	}

	/*
	 * Check all posibilities of Expression Cho return type tai day ne
	 */
	static SemanticCheck checkArrayNode(Node node, Env env) {
		SemanticCheck smanticResult = new SemanticCheck(node);
		MsgHelper.semanticMsg("Checking ARRAY");
		// check Identifier
		IdentifierNode identNode = (IdentifierNode)node.getChild(0);
		SemanticCheck smticIden = checkNode(identNode, env);
		Type t = env.lookup(identNode.getName());
		if (t instanceof ArrayType) {
			ArrayType tmp = ((ArrayType)t);
			smanticResult.setType(tmp.getTypeOfArray());
		}
		smanticResult.combine(smticIden);

		// check IntConst or Expr or Fcall
		Node insideNode = node.getChild(1);
		smticIden = checkNode(insideNode, env);
		smanticResult.combine(smticIden);

		MsgHelper.semanticMsg("Ending ARRAY");
		return smanticResult;
	}

	static SemanticCheck checkReturnNode(Node node, Env env) {
		MsgHelper.semanticMsg("Checking RETURN");
		SemanticCheck result = new SemanticCheck(node);
		Node childNode = node.getChild(0);
		/*if (childNode.getId() == Id.INTEGER_LITERAL) {
			result.setType(Id.INT);
		} else if (childNode.getId() == Id.IDENT) {
			// lookup in Env
			IdentifierNode identNode = (IdentifierNode) childNode;
			Type t = env.lookup(identNode.getName());
			if (t != null) {
				result.setType(t.getIdType());
			}
		} else if (childNode.getId() == Id.ARRAY) {
			
		}*/
		SemanticCheck smtChild = checkNode(childNode, env);
		result.combine(smtChild);
		result.setType(smtChild.getType());
		
		MsgHelper.semanticMsg("Ending RETURN");
		return result;
	}
	
	static SemanticCheck checkFCallNode(Node node, Env env) {
		MsgHelper.semanticMsg("Checking FCALL");
		SemanticCheck result = new SemanticCheck(node);
		// check function name is defined
		Node identNode = node.getChild(0);
		
		result.setResult(false);
		if (identNode instanceof IdentifierNode) {
			String name = ((IdentifierNode)identNode).getName();
			Type t = env.lookup(name);
			if (t !=  null && t instanceof FunctionDeclarationType){
				// get the number of parameters
				int numParameters = ((FunctionDeclarationType)t).getNumberOfParameters();
				// compare
				Node paramsNode = node.getChild(1);
				if (paramsNode.numberOfChildren() == numParameters) {
					// check data type of parameter
					
					result.setResult(true);
				} else if (paramsNode.numberOfChildren() < numParameters){
					MsgHelper.semanticError("Function is called with too few parameters : "
							+ name, identNode.getPosition());
				} else {
					MsgHelper.semanticError("Function is called with too many parameters : "
							+ name, identNode.getPosition());
				}
			} else {
				MsgHelper.semanticError("Function is not declared : "
						+ name, identNode.getPosition());
			}
		}
		
		MsgHelper.semanticMsg("Ending FCALL");
		return result;
	}
}
