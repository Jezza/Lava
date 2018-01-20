package me.jezza.lava.lang;

/**
 * @author Jezza
 */
public final class Tokens {
	public static final int EOS = -1;

	public static final int NAMESPACE = 65;
	public static final int AND = 66;
	public static final int BREAK = 67;
	public static final int DO = 68;
	public static final int ELSE = 69;
	public static final int ELSEIF = 70;
	public static final int END = 71;
	public static final int FALSE = 72;
	public static final int FOR = 73;
	public static final int FUNCTION = 74;
	public static final int GOTO = 75;
	public static final int IF = 76;
	public static final int IN = 77;
	public static final int LOCAL = 78;
	public static final int NIL = 79;
	public static final int NOT = 80;
	public static final int OR = 81;
	public static final int REPEAT = 82;
	public static final int RETURN = 83;
	public static final int THEN = 84;
	public static final int TRUE = 85;
	public static final int UNTIL = 86;
	public static final int WHILE = 87;

	public static final int CONCAT = 88;
	public static final int DOTS = 89;
	public static final int EQ = 90;
	public static final int GE = 97;
	public static final int LE = 98;
	public static final int NE = 99;

	public static final int DOUBLE = 100;
	public static final int INTEGER = 101;
	public static final int STRING = 102;

	private Tokens() {
		throw new IllegalStateException();
	}

	public static String name(int type) {
		switch (type) {
			case EOS:
				return "EOS";
			case AND:
				return "AND";
			case BREAK:
				return "BREAK";
			case DO:
				return "DO";
			case ELSE:
				return "ELSE";
			case ELSEIF:
				return "ELSEIF";
			case END:
				return "END";
			case FALSE:
				return "FALSE";
			case FOR:
				return "FOR";
			case FUNCTION:
				return "FUNCTION";
			case GOTO:
				return "GOTO";
			case IF:
				return "IF";
			case IN:
				return "IN";
			case LOCAL:
				return "LOCAL";
			case NIL:
				return "NIL";
			case NOT:
				return "NOT";
			case OR:
				return "OR";
			case REPEAT:
				return "REPEAT";
			case RETURN:
				return "RETURN";
			case THEN:
				return "THEN";
			case TRUE:
				return "TRUE";
			case UNTIL:
				return "UNTIL";
			case WHILE:
				return "WHILE";
				//
			case CONCAT:
				return "CONCAT";
			case DOTS:
				return "DOTS";
			case EQ:
				return "EQ";
			case GE:
				return "GE";
			case LE:
				return "LE";
			case NE:
				return "NE";
			case DOUBLE:
				return "DOUBLE";
			case INTEGER:
				return "INTEGER";
			case NAMESPACE:
				return "NAMESPACE";
			case STRING:
				return "STRING";
			default:
				return "'" + (char) type + '\'';
		}
	}
}
