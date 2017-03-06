package me.jezza.lava.lang;

/**
 * @author Jezza
 */
public final class Tokens {
	public static final int EOS = -1;

	public static final int AND = 257;
	public static final int BREAK = 258;
	public static final int DO = 259;
	public static final int ELSE = 260;
	public static final int ELSEIF = 261;
	public static final int END = 262;
	public static final int FALSE = 263;
	public static final int FOR = 264;
	public static final int FUNCTION = 265;
	public static final int GOTO = 266;
	public static final int IF = 267;
	public static final int IN = 268;
	public static final int LOCAL = 269;
	public static final int NIL = 270;
	public static final int NOT = 271;
	public static final int OR = 272;
	public static final int REPEAT = 273;
	public static final int RETURN = 274;
	public static final int THEN = 275;
	public static final int TRUE = 276;
	public static final int UNTIL = 277;
	public static final int WHILE = 278;

	public static final int IDIV = 279;
	public static final int CONCAT = 280;
	public static final int DOTS = 281;
	public static final int EQ = 282;
	public static final int GE = 283;
	public static final int LE = 284;
	public static final int NE = 285;

	public static final int SHL = 286;
	public static final int SHR = 287;

	public static final int DBCOLON = 288;
//	public static final int EOS = 289;

	public static final int FLT = 290;
	public static final int INT = 291;
	public static final int NAME = 292;
	public static final int STRING = 293;

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
			case IDIV:
				return "IDIV";
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
			case SHL:
				return "SHL";
			case SHR:
				return "SHR";
			case DBCOLON:
				return "DBCOLON";
			case FLT:
				return "FLT";
			case INT:
				return "INT";
			case NAME:
				return "NAME";
			case STRING:
				return "STRING";
			default:
				return "'" + (char) type + '\'';
		}
	}
}
