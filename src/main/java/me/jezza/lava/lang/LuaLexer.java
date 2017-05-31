package me.jezza.lava.lang;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import me.jezza.lava.Times;

/**
 * @author Jezza
 */
public final class LuaLexer {
	static class Token{
		private final TokenType type;
		private final int start;
		private final int length;
		private final byte pairValue;

		public Token(TokenType type, int start, int length) {
			this.type = type;
			this.start = start;
			this.length = length;
			this.pairValue = 0;
		}

		public Token(TokenType type, int start, int length, byte pairValue) {
			this.type = type;
			this.start = start;
			this.length = length;
			this.pairValue = pairValue;
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Started");
		LuaLexer lexer = new LuaLexer(new FileReader(new File("C:\\Users\\Jezza\\Desktop\\JavaProjects\\Lava\\src\\test\\resources\\all.lua")));
//		Lexer lexer = new LavaLexer("(1 + 5 + 4)");
//		long ss = System.nanoTime();
//		lexer.next();
//		long ee = System.nanoTime();
//		System.out.println(ee - ss);
		final long start = System.nanoTime();
		int count = 0;
		Token t;
		while ((t = lexer.yylex()) != null) {
			count++;
		}
		final long end = System.nanoTime();
//		EXTRA.add(end - s);
		System.out.println(end - start);
		System.out.println("Count: " + count);

		Times.print();
	}

	/** This character denotes the end of file */
	public static final int YYEOF = -1;

	/** initial size of the lookahead buffer */
	private static final int ZZ_BUFFERSIZE = 16384;

	/** lexical states */
	public static final int YYINITIAL = 0;
	public static final int STRING1 = 2;
	public static final int STRING2 = 4;
	public static final int LONGSTRING = 6;
	public static final int COMMENT = 8;
	public static final int LINECOMMENT = 10;

	/**
	 * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
	 * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
	 *                  at the beginning of a line
	 * l is of the form l = 2*k, k a non negative integer
	 */
	private static final int ZZ_LEXSTATE[] = {
			0,  0,  1,  1,  2,  2,  3,  3,  4,  4,  5, 5
	};

	/**
	 * Translates characters to character classes
	 */
	private static final String ZZ_CMAP_PACKED =
			"\11\10\1\3\1\2\1\55\1\56\1\1\16\10\4\0\1\3\1\0"+
					"\1\20\1\46\1\7\1\46\1\0\1\21\1\51\1\52\1\46\1\45"+
					"\1\46\1\17\1\15\1\46\1\13\11\11\1\46\1\46\1\50\1\5"+
					"\1\50\2\0\4\12\1\16\1\12\24\7\1\4\1\22\1\6\1\46"+
					"\1\7\1\0\1\23\1\26\1\36\1\25\1\30\1\32\1\7\1\42"+
					"\1\34\1\7\1\31\1\35\1\7\1\24\1\33\1\44\1\7\1\27"+
					"\1\43\1\37\1\40\1\7\1\41\1\14\2\7\1\53\1\0\1\54"+
					"\1\47\6\10\1\57\32\10\2\0\4\7\4\0\1\7\2\0\1\10"+
					"\7\0\1\7\4\0\1\7\5\0\27\7\1\0\37\7\1\0\u01ca\7"+
					"\4\0\14\7\16\0\5\7\7\0\1\7\1\0\1\7\21\0\160\10"+
					"\5\7\1\0\2\7\2\0\4\7\10\0\1\7\1\0\3\7\1\0"+
					"\1\7\1\0\24\7\1\0\123\7\1\0\213\7\1\0\5\10\2\0"+
					"\236\7\11\0\46\7\2\0\1\7\7\0\47\7\7\0\1\7\1\0"+
					"\55\10\1\0\1\10\1\0\2\10\1\0\2\10\1\0\1\10\10\0"+
					"\33\7\5\0\3\7\15\0\5\10\6\0\1\7\4\0\13\10\5\0"+
					"\53\7\37\10\4\0\2\7\1\10\143\7\1\0\1\7\10\10\1\0"+
					"\6\10\2\7\2\10\1\0\4\10\2\7\12\10\3\7\2\0\1\7"+
					"\17\0\1\10\1\7\1\10\36\7\33\10\2\0\131\7\13\10\1\7"+
					"\16\0\12\10\41\7\11\10\2\7\4\0\1\7\5\0\26\7\4\10"+
					"\1\7\11\10\1\7\3\10\1\7\5\10\22\0\31\7\3\10\104\0"+
					"\1\7\1\0\13\7\67\0\33\10\1\0\4\10\66\7\3\10\1\7"+
					"\22\10\1\7\7\10\12\7\2\10\2\0\12\10\1\0\7\7\1\0"+
					"\7\7\1\0\3\10\1\0\10\7\2\0\2\7\2\0\26\7\1\0"+
					"\7\7\1\0\1\7\3\0\4\7\2\0\1\10\1\7\7\10\2\0"+
					"\2\10\2\0\3\10\1\7\10\0\1\10\4\0\2\7\1\0\3\7"+
					"\2\10\2\0\12\10\4\7\7\0\1\7\5\0\3\10\1\0\6\7"+
					"\4\0\2\7\2\0\26\7\1\0\7\7\1\0\2\7\1\0\2\7"+
					"\1\0\2\7\2\0\1\10\1\0\5\10\4\0\2\10\2\0\3\10"+
					"\3\0\1\10\7\0\4\7\1\0\1\7\7\0\14\10\3\7\1\10"+
					"\13\0\3\10\1\0\11\7\1\0\3\7\1\0\26\7\1\0\7\7"+
					"\1\0\2\7\1\0\5\7\2\0\1\10\1\7\10\10\1\0\3\10"+
					"\1\0\3\10\2\0\1\7\17\0\2\7\2\10\2\0\12\10\1\0"+
					"\1\7\17\0\3\10\1\0\10\7\2\0\2\7\2\0\26\7\1\0"+
					"\7\7\1\0\2\7\1\0\5\7\2\0\1\10\1\7\7\10\2\0"+
					"\2\10\2\0\3\10\10\0\2\10\4\0\2\7\1\0\3\7\2\10"+
					"\2\0\12\10\1\0\1\7\20\0\1\10\1\7\1\0\6\7\3\0"+
					"\3\7\1\0\4\7\3\0\2\7\1\0\1\7\1\0\2\7\3\0"+
					"\2\7\3\0\3\7\3\0\14\7\4\0\5\10\3\0\3\10\1\0"+
					"\4\10\2\0\1\7\6\0\1\10\16\0\12\10\11\0\1\7\7\0"+
					"\3\10\1\0\10\7\1\0\3\7\1\0\27\7\1\0\12\7\1\0"+
					"\5\7\3\0\1\7\7\10\1\0\3\10\1\0\4\10\7\0\2\10"+
					"\1\0\2\7\6\0\2\7\2\10\2\0\12\10\22\0\2\10\1\0"+
					"\10\7\1\0\3\7\1\0\27\7\1\0\12\7\1\0\5\7\2\0"+
					"\1\10\1\7\7\10\1\0\3\10\1\0\4\10\7\0\2\10\7\0"+
					"\1\7\1\0\2\7\2\10\2\0\12\10\1\0\2\7\17\0\2\10"+
					"\1\0\10\7\1\0\3\7\1\0\51\7\2\0\1\7\7\10\1\0"+
					"\3\10\1\0\4\10\1\7\10\0\1\10\10\0\2\7\2\10\2\0"+
					"\12\10\12\0\6\7\2\0\2\10\1\0\22\7\3\0\30\7\1\0"+
					"\11\7\1\0\1\7\2\0\7\7\3\0\1\10\4\0\6\10\1\0"+
					"\1\10\1\0\10\10\22\0\2\10\15\0\60\7\1\10\2\7\7\10"+
					"\4\0\10\7\10\10\1\0\12\10\47\0\2\7\1\0\1\7\2\0"+
					"\2\7\1\0\1\7\2\0\1\7\6\0\4\7\1\0\7\7\1\0"+
					"\3\7\1\0\1\7\1\0\1\7\2\0\2\7\1\0\4\7\1\10"+
					"\2\7\6\10\1\0\2\10\1\7\2\0\5\7\1\0\1\7\1\0"+
					"\6\10\2\0\12\10\2\0\4\7\40\0\1\7\27\0\2\10\6\0"+
					"\12\10\13\0\1\10\1\0\1\10\1\0\1\10\4\0\2\10\10\7"+
					"\1\0\44\7\4\0\24\10\1\0\2\10\5\7\13\10\1\0\44\10"+
					"\11\0\1\10\71\0\53\7\24\10\1\7\12\10\6\0\6\7\4\10"+
					"\4\7\3\10\1\7\3\10\2\7\7\10\3\7\4\10\15\7\14\10"+
					"\1\7\17\10\2\0\46\7\1\0\1\7\5\0\1\7\2\0\53\7"+
					"\1\0\u014d\7\1\0\4\7\2\0\7\7\1\0\1\7\1\0\4\7"+
					"\2\0\51\7\1\0\4\7\2\0\41\7\1\0\4\7\2\0\7\7"+
					"\1\0\1\7\1\0\4\7\2\0\17\7\1\0\71\7\1\0\4\7"+
					"\2\0\103\7\2\0\3\10\40\0\20\7\20\0\125\7\14\0\u026c\7"+
					"\2\0\21\7\1\0\32\7\5\0\113\7\3\0\3\7\17\0\15\7"+
					"\1\0\4\7\3\10\13\0\22\7\3\10\13\0\22\7\2\10\14\0"+
					"\15\7\1\0\3\7\1\0\2\10\14\0\64\7\40\10\3\0\1\7"+
					"\3\0\2\7\1\10\2\0\12\10\41\0\3\10\2\0\12\10\6\0"+
					"\130\7\10\0\51\7\1\10\1\7\5\0\106\7\12\0\35\7\3\0"+
					"\14\10\4\0\14\10\12\0\12\10\36\7\2\0\5\7\13\0\54\7"+
					"\4\0\21\10\7\7\2\10\6\0\12\10\46\0\27\7\5\10\4\0"+
					"\65\7\12\10\1\0\35\10\2\0\13\10\6\0\12\10\15\0\1\7"+
					"\130\0\5\10\57\7\21\10\7\7\4\0\12\10\21\0\11\10\14\0"+
					"\3\10\36\7\15\10\2\7\12\10\54\7\16\10\14\0\44\7\24\10"+
					"\10\0\12\10\3\0\3\7\12\10\44\7\122\0\3\10\1\0\25\10"+
					"\4\7\1\10\4\7\3\10\2\7\11\0\300\7\47\10\25\0\4\10"+
					"\u0116\7\2\0\6\7\2\0\46\7\2\0\6\7\2\0\10\7\1\0"+
					"\1\7\1\0\1\7\1\0\1\7\1\0\37\7\2\0\65\7\1\0"+
					"\7\7\1\0\1\7\3\0\3\7\1\0\7\7\3\0\4\7\2\0"+
					"\6\7\4\0\15\7\5\0\3\7\1\0\7\7\16\0\5\10\30\0"+
					"\1\55\1\55\5\10\20\0\2\7\23\0\1\7\13\0\5\10\5\0"+
					"\6\10\1\0\1\7\15\0\1\7\20\0\15\7\3\0\33\7\25\0"+
					"\15\10\4\0\1\10\3\0\14\10\21\0\1\7\4\0\1\7\2\0"+
					"\12\7\1\0\1\7\3\0\5\7\6\0\1\7\1\0\1\7\1\0"+
					"\1\7\1\0\4\7\1\0\13\7\2\0\4\7\5\0\5\7\4\0"+
					"\1\7\21\0\51\7\u0a77\0\57\7\1\0\57\7\1\0\205\7\6\0"+
					"\4\7\3\10\2\7\14\0\46\7\1\0\1\7\5\0\1\7\2\0"+
					"\70\7\7\0\1\7\17\0\1\10\27\7\11\0\7\7\1\0\7\7"+
					"\1\0\7\7\1\0\7\7\1\0\7\7\1\0\7\7\1\0\7\7"+
					"\1\0\7\7\1\0\40\10\57\0\1\7\u01d5\0\3\7\31\0\11\7"+
					"\6\10\1\0\5\7\2\0\5\7\4\0\126\7\2\0\2\10\2\0"+
					"\3\7\1\0\132\7\1\0\4\7\5\0\51\7\3\0\136\7\21\0"+
					"\33\7\65\0\20\7\u0200\0\u19b6\7\112\0\u51cd\7\63\0\u048d\7\103\0"+
					"\56\7\2\0\u010d\7\3\0\20\7\12\10\2\7\24\0\57\7\1\10"+
					"\4\0\12\10\1\0\31\7\7\0\1\10\120\7\2\10\45\0\11\7"+
					"\2\0\147\7\2\0\4\7\1\0\4\7\14\0\13\7\115\0\12\7"+
					"\1\10\3\7\1\10\4\7\1\10\27\7\5\10\20\0\1\7\7\0"+
					"\64\7\14\0\2\10\62\7\21\10\13\0\12\10\6\0\22\10\6\7"+
					"\3\0\1\7\4\0\12\10\34\7\10\10\2\0\27\7\15\10\14\0"+
					"\35\7\3\0\4\10\57\7\16\10\16\0\1\7\12\10\46\0\51\7"+
					"\16\10\11\0\3\7\1\10\10\7\2\10\2\0\12\10\6\0\27\7"+
					"\3\0\1\7\1\10\4\0\60\7\1\10\1\7\3\10\2\7\2\10"+
					"\5\7\2\10\1\7\1\10\1\7\30\0\3\7\2\0\13\7\5\10"+
					"\2\0\3\7\2\10\12\0\6\7\2\0\6\7\2\0\6\7\11\0"+
					"\7\7\1\0\7\7\221\0\43\7\10\10\1\0\2\10\2\0\12\10"+
					"\6\0\u2ba4\7\14\0\27\7\4\0\61\7\u2104\0\u016e\7\2\0\152\7"+
					"\46\0\7\7\14\0\5\7\5\0\1\7\1\10\12\7\1\0\15\7"+
					"\1\0\5\7\1\0\1\7\1\0\2\7\1\0\2\7\1\0\154\7"+
					"\41\0\u016b\7\22\0\100\7\2\0\66\7\50\0\15\7\3\0\20\10"+
					"\20\0\7\10\14\0\2\7\30\0\3\7\31\0\1\7\6\0\5\7"+
					"\1\0\207\7\2\0\1\10\4\0\1\7\13\0\12\10\7\0\32\7"+
					"\4\0\1\7\1\0\32\7\13\0\131\7\3\0\6\7\2\0\6\7"+
					"\2\0\6\7\2\0\3\7\3\0\2\7\3\0\2\7\22\0\3\10"+
					"\4\0\14\7\1\0\32\7\1\0\23\7\1\0\2\7\1\0\17\7"+
					"\2\0\16\7\42\0\173\7\105\0\65\7\210\0\1\10\202\0\35\7"+
					"\3\0\61\7\57\0\37\7\21\0\33\7\65\0\36\7\2\0\44\7"+
					"\4\0\10\7\1\0\5\7\52\0\236\7\2\0\12\10\u0356\0\6\7"+
					"\2\0\1\7\1\0\54\7\1\0\2\7\3\0\1\7\2\0\27\7"+
					"\252\0\26\7\12\0\32\7\106\0\70\7\6\0\2\7\100\0\1\7"+
					"\3\10\1\0\2\10\5\0\4\10\4\7\1\0\3\7\1\0\33\7"+
					"\4\0\3\10\4\0\1\10\40\0\35\7\203\0\66\7\12\0\26\7"+
					"\12\0\23\7\215\0\111\7\u03b7\0\3\10\65\7\17\10\37\0\12\10"+
					"\20\0\3\10\55\7\13\10\2\0\1\10\22\0\31\7\7\0\12\10"+
					"\6\0\3\10\44\7\16\10\1\0\12\10\100\0\3\10\60\7\16\10"+
					"\4\7\13\0\12\10\u04a6\0\53\7\15\10\10\0\12\10\u0936\0\u036f\7"+
					"\221\0\143\7\u0b9d\0\u042f\7\u33d1\0\u0239\7\u04c7\0\105\7\13\0\1\7"+
					"\56\10\20\0\4\10\15\7\u4060\0\2\7\u2163\0\5\10\3\0\26\10"+
					"\2\0\7\10\36\0\4\10\224\0\3\10\u01bb\0\125\7\1\0\107\7"+
					"\1\0\2\7\2\0\1\7\2\0\2\7\2\0\4\7\1\0\14\7"+
					"\1\0\1\7\1\0\7\7\1\0\101\7\1\0\4\7\2\0\10\7"+
					"\1\0\7\7\1\0\34\7\1\0\4\7\1\0\5\7\1\0\1\7"+
					"\3\0\7\7\1\0\u0154\7\2\0\31\7\1\0\31\7\1\0\37\7"+
					"\1\0\31\7\1\0\37\7\1\0\31\7\1\0\37\7\1\0\31\7"+
					"\1\0\37\7\1\0\31\7\1\0\10\7\2\0\62\10\u1600\0\4\7"+
					"\1\0\33\7\1\0\2\7\1\0\1\7\2\0\1\7\1\0\12\7"+
					"\1\0\4\7\1\0\1\7\1\0\1\7\6\0\1\7\4\0\1\7"+
					"\1\0\1\7\1\0\1\7\1\0\3\7\1\0\2\7\1\0\1\7"+
					"\2\0\1\7\1\0\1\7\1\0\1\7\1\0\1\7\1\0\1\7"+
					"\1\0\2\7\1\0\1\7\2\0\4\7\1\0\7\7\1\0\4\7"+
					"\1\0\4\7\1\0\1\7\1\0\12\7\1\0\21\7\5\0\3\7"+
					"\1\0\5\7\1\0\21\7\u1144\0\ua6d7\7\51\0\u1035\7\13\0\336\7"+
					"\u3fe2\0\u021e\7\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\uffff\0\u05ee\0"+
					"\1\10\36\0\140\10\200\0\360\10\uffff\0\uffff\0\ufe12\0";

	/**
	 * Translates characters to character classes
	 */
	private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

	/**
	 * Translates DFA states to action switch labels.
	 */
	private static final int [] ZZ_ACTION = zzUnpackAction();

	private static final String ZZ_ACTION_PACKED_0 =
			"\6\0\3\1\1\2\1\3\1\4\1\5\2\6\2\3"+
					"\1\7\1\10\15\5\1\3\1\1\1\11\1\12\1\13"+
					"\1\14\1\15\2\16\1\17\1\1\1\15\1\20\1\21"+
					"\2\15\1\21\1\22\2\23\1\22\2\24\1\25\1\0"+
					"\1\6\2\0\1\3\1\26\3\5\1\27\6\5\1\30"+
					"\5\5\1\31\1\0\1\32\1\33\1\0\1\6\1\0"+
					"\1\6\3\5\1\34\13\5\1\30\6\5\1\35\1\36"+
					"\1\5";

	private static int [] zzUnpackAction() {
		int [] result = new int[111];
		int offset = 0;
		offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackAction(String packed, int offset, int [] result) {
		int i = 0;       /* index in packed string  */
		int j = offset;  /* index in unpacked array */
		int l = packed.length();
		while (i < l) {
			int count = packed.charAt(i++);
			int value = packed.charAt(i++);
			do result[j++] = value; while (--count > 0);
		}
		return j;
	}


	/**
	 * Translates a state to a row index in the transition table
	 */
	private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

	private static final String ZZ_ROWMAP_PACKED_0 =
			"\0\0\0\60\0\140\0\220\0\300\0\360\0\u0120\0\u0150"+
					"\0\u0180\0\u01b0\0\u01e0\0\u0120\0\u0210\0\u0240\0\u0270\0\u02a0"+
					"\0\u02d0\0\u0120\0\u0120\0\u0300\0\u0330\0\u0360\0\u0390\0\u03c0"+
					"\0\u03f0\0\u0420\0\u0450\0\u0480\0\u04b0\0\u04e0\0\u0510\0\u0540"+
					"\0\u0120\0\u01e0\0\u0120\0\u0120\0\u0120\0\u0120\0\u0570\0\u05a0"+
					"\0\u0120\0\u0120\0\u05d0\0\u0600\0\u0120\0\u0120\0\u0630\0\u0120"+
					"\0\u0660\0\u0120\0\u0690\0\u0120\0\u06c0\0\u06f0\0\u0120\0\u0120"+
					"\0\u01b0\0\u0720\0\u0750\0\u0780\0\u07b0\0\u0120\0\u07e0\0\u0810"+
					"\0\u0840\0\u0210\0\u0870\0\u08a0\0\u08d0\0\u0900\0\u0930\0\u0960"+
					"\0\u0210\0\u0990\0\u09c0\0\u09f0\0\u0a20\0\u0a50\0\u0120\0\u0660"+
					"\0\u0120\0\u0120\0\u06c0\0\u0a80\0\u0a80\0\u0780\0\u0ab0\0\u0ae0"+
					"\0\u0b10\0\u0210\0\u0b40\0\u0b70\0\u0ba0\0\u0bd0\0\u0c00\0\u0c30"+
					"\0\u0c60\0\u0c90\0\u0cc0\0\u0cf0\0\u0d20\0\u0d50\0\u0d80\0\u0db0"+
					"\0\u0de0\0\u0e10\0\u0e40\0\u0e70\0\u0210\0\u0210\0\u0ea0";

	private static int [] zzUnpackRowMap() {
		int [] result = new int[111];
		int offset = 0;
		offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackRowMap(String packed, int offset, int [] result) {
		int i = 0;  /* index in packed string  */
		int j = offset;  /* index in unpacked array */
		int l = packed.length();
		while (i < l) {
			int high = packed.charAt(i++) << 16;
			result[j++] = high | packed.charAt(i++);
		}
		return j;
	}

	/**
	 * The transition table of the DFA
	 */
	private static final int [] ZZ_TRANS = zzUnpackTrans();

	private static final String ZZ_TRANS_PACKED_0 =
			"\1\7\1\10\1\7\1\11\1\12\1\13\1\14\1\15"+
					"\1\7\1\16\1\15\1\17\1\15\1\20\1\15\1\21"+
					"\1\22\1\23\1\7\1\24\1\25\1\26\1\27\1\30"+
					"\1\31\1\15\1\32\1\33\1\34\1\35\1\15\1\36"+
					"\1\37\1\40\3\15\2\41\1\42\1\13\1\43\1\44"+
					"\1\45\1\46\1\7\1\11\1\7\1\47\1\50\1\51"+
					"\15\47\1\52\1\47\1\53\35\47\1\54\1\50\1\51"+
					"\16\54\1\55\1\53\35\54\1\56\1\57\1\60\3\56"+
					"\1\61\46\56\3\7\1\62\1\63\1\64\1\62\1\65"+
					"\50\62\3\7\1\56\1\66\1\67\52\56\3\7\62\0"+
					"\1\7\60\0\1\11\52\0\1\11\5\0\1\70\1\71"+
					"\57\0\1\41\61\0\6\15\1\0\1\15\4\0\22\15"+
					"\12\0\1\15\11\0\1\16\1\0\1\16\1\0\1\72"+
					"\1\73\11\0\1\73\40\0\1\16\1\0\1\16\1\74"+
					"\1\72\1\73\11\0\1\73\40\0\1\72\1\0\1\72"+
					"\1\0\1\75\61\0\1\76\47\0\6\15\1\0\1\15"+
					"\4\0\1\15\1\77\20\15\12\0\1\15\7\0\6\15"+
					"\1\0\1\15\4\0\10\15\1\100\1\101\10\15\12\0"+
					"\1\15\7\0\6\15\1\0\1\15\4\0\10\15\1\102"+
					"\11\15\12\0\1\15\7\0\6\15\1\0\1\15\4\0"+
					"\4\15\1\103\15\15\12\0\1\15\7\0\6\15\1\0"+
					"\1\15\4\0\5\15\1\104\14\15\12\0\1\15\7\0"+
					"\6\15\1\0\1\15\4\0\1\15\1\105\10\15\1\106"+
					"\7\15\12\0\1\15\7\0\6\15\1\0\1\15\4\0"+
					"\1\107\7\15\1\33\4\15\1\110\4\15\12\0\1\15"+
					"\7\0\6\15\1\0\1\15\4\0\4\15\1\111\15\15"+
					"\12\0\1\15\7\0\6\15\1\0\1\15\4\0\1\15"+
					"\1\111\5\15\1\111\12\15\12\0\1\15\7\0\6\15"+
					"\1\0\1\15\4\0\10\15\1\112\11\15\12\0\1\15"+
					"\7\0\6\15\1\0\1\15\4\0\4\15\1\113\12\15"+
					"\1\114\2\15\12\0\1\15\7\0\6\15\1\0\1\15"+
					"\4\0\1\15\1\115\20\15\12\0\1\15\7\0\6\15"+
					"\1\0\1\15\4\0\17\15\1\116\2\15\12\0\1\15"+
					"\1\47\2\0\15\47\1\0\1\47\1\0\35\47\2\0"+
					"\1\51\55\0\1\117\2\0\52\117\3\0\1\54\2\0"+
					"\16\54\2\0\35\54\2\0\1\60\62\0\1\120\1\121"+
					"\53\0\1\64\61\0\1\122\1\123\54\0\1\67\66\0"+
					"\1\72\1\0\1\72\2\0\1\73\11\0\1\73\40\0"+
					"\1\124\1\0\1\124\3\0\1\125\25\0\1\125\23\0"+
					"\3\126\2\0\1\126\4\0\1\126\1\0\2\126\1\0"+
					"\1\126\1\0\1\126\3\0\1\126\36\0\1\41\51\0"+
					"\6\15\1\0\1\15\4\0\2\15\1\111\17\15\12\0"+
					"\1\15\7\0\6\15\1\0\1\15\4\0\14\15\1\111"+
					"\5\15\12\0\1\15\7\0\6\15\1\0\1\15\4\0"+
					"\12\15\1\111\7\15\12\0\1\15\7\0\6\15\1\0"+
					"\1\15\4\0\5\15\1\127\14\15\12\0\1\15\7\0"+
					"\6\15\1\0\1\15\4\0\14\15\1\130\4\15\1\131"+
					"\12\0\1\15\7\0\6\15\1\0\1\15\4\0\2\15"+
					"\1\132\17\15\12\0\1\15\7\0\6\15\1\0\1\15"+
					"\4\0\20\15\1\133\1\15\12\0\1\15\7\0\6\15"+
					"\1\0\1\15\4\0\12\15\1\134\7\15\12\0\1\15"+
					"\7\0\6\15\1\0\1\15\4\0\1\15\1\135\20\15"+
					"\12\0\1\15\7\0\6\15\1\0\1\15\4\0\13\15"+
					"\1\136\6\15\12\0\1\15\7\0\6\15\1\0\1\15"+
					"\4\0\15\15\1\137\4\15\12\0\1\15\7\0\6\15"+
					"\1\0\1\15\4\0\5\15\1\140\14\15\12\0\1\15"+
					"\7\0\6\15\1\0\1\15\4\0\14\15\1\141\5\15"+
					"\12\0\1\15\7\0\6\15\1\0\1\15\4\0\11\15"+
					"\1\142\10\15\12\0\1\15\11\0\1\124\1\0\1\124"+
					"\53\0\6\15\1\0\1\15\4\0\1\143\21\15\12\0"+
					"\1\15\7\0\6\15\1\0\1\15\4\0\15\15\1\144"+
					"\4\15\12\0\1\15\7\0\6\15\1\0\1\15\4\0"+
					"\5\15\1\145\14\15\12\0\1\15\7\0\6\15\1\0"+
					"\1\15\4\0\5\15\1\146\14\15\12\0\1\15\7\0"+
					"\6\15\1\0\1\15\4\0\20\15\1\137\1\15\12\0"+
					"\1\15\7\0\6\15\1\0\1\15\4\0\13\15\1\147"+
					"\6\15\12\0\1\15\7\0\6\15\1\0\1\15\4\0"+
					"\1\101\21\15\12\0\1\15\7\0\6\15\1\0\1\15"+
					"\4\0\5\15\1\111\14\15\12\0\1\15\7\0\6\15"+
					"\1\0\1\15\4\0\1\15\1\102\20\15\12\0\1\15"+
					"\7\0\6\15\1\0\1\15\4\0\11\15\1\150\10\15"+
					"\12\0\1\15\7\0\6\15\1\0\1\15\4\0\12\15"+
					"\1\137\7\15\12\0\1\15\7\0\6\15\1\0\1\15"+
					"\4\0\6\15\1\111\13\15\12\0\1\15\7\0\6\15"+
					"\1\0\1\15\4\0\4\15\1\151\15\15\12\0\1\15"+
					"\7\0\6\15\1\0\1\15\4\0\1\152\21\15\12\0"+
					"\1\15\7\0\6\15\1\0\1\15\4\0\11\15\1\153"+
					"\10\15\12\0\1\15\7\0\6\15\1\0\1\15\4\0"+
					"\14\15\1\154\5\15\12\0\1\15\7\0\6\15\1\0"+
					"\1\15\4\0\12\15\1\155\7\15\12\0\1\15\7\0"+
					"\6\15\1\0\1\15\4\0\1\15\1\111\20\15\12\0"+
					"\1\15\7\0\6\15\1\0\1\15\4\0\14\15\1\156"+
					"\5\15\12\0\1\15\7\0\6\15\1\0\1\15\4\0"+
					"\7\15\1\111\12\15\12\0\1\15\7\0\6\15\1\0"+
					"\1\15\4\0\11\15\1\157\10\15\12\0\1\15\7\0"+
					"\6\15\1\0\1\15\4\0\10\15\1\140\11\15\12\0"+
					"\1\15";

	private static int [] zzUnpackTrans() {
		int [] result = new int[3792];
		int offset = 0;
		offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackTrans(String packed, int offset, int [] result) {
		int i = 0;       /* index in packed string  */
		int j = offset;  /* index in unpacked array */
		int l = packed.length();
		while (i < l) {
			int count = packed.charAt(i++);
			int value = packed.charAt(i++);
			value--;
			do result[j++] = value; while (--count > 0);
		}
		return j;
	}


	/* error codes */
	private static final int ZZ_UNKNOWN_ERROR = 0;
	private static final int ZZ_NO_MATCH = 1;
	private static final int ZZ_PUSHBACK_2BIG = 2;

	/* error messages for the codes above */
	private static final String ZZ_ERROR_MSG[] = {
			"Unknown internal scanner error",
			"Error: could not match input",
			"Error: pushback value was too large"
	};

	/**
	 * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
	 */
	private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

	private static final String ZZ_ATTRIBUTE_PACKED_0 =
			"\6\0\1\11\4\1\1\11\5\1\2\11\15\1\1\11"+
					"\1\1\4\11\2\1\2\11\2\1\2\11\1\1\1\11"+
					"\1\1\1\11\1\1\1\11\2\1\2\11\1\0\1\1"+
					"\2\0\1\1\1\11\20\1\1\11\1\0\2\11\1\0"+
					"\1\1\1\0\32\1";

	private static int [] zzUnpackAttribute() {
		int [] result = new int[111];
		int offset = 0;
		offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
		return result;
	}

	private static int zzUnpackAttribute(String packed, int offset, int [] result) {
		int i = 0;       /* index in packed string  */
		int j = offset;  /* index in unpacked array */
		int l = packed.length();
		while (i < l) {
			int count = packed.charAt(i++);
			int value = packed.charAt(i++);
			do result[j++] = value; while (--count > 0);
		}
		return j;
	}

	/** the input device */
	private java.io.Reader zzReader;

	/** the current state of the DFA */
	private int zzState;

	/** the current lexical state */
	private int zzLexicalState = YYINITIAL;

	/** this buffer contains the current text to be matched and is
	 the source of the yytext() string */
	private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

	/** the textposition at the last accepting state */
	private int zzMarkedPos;

	/** the current text position in the buffer */
	private int zzCurrentPos;

	/** startRead marks the beginning of the yytext() string in the buffer */
	private int zzStartRead;

	/** endRead marks the last character in the buffer, that has been read
	 from input */
	private int zzEndRead;

	/** number of newlines encountered up to the start of the matched text */
	private int yyline;

	/** the number of characters up to the start of the matched text */
	private int yychar;

	/**
	 * the number of characters from the last newline up to the start of the
	 * matched text
	 */
	private int yycolumn;

	/**
	 * zzAtBOL == true <=> the scanner is currently at the beginning of a line
	 */
	private boolean zzAtBOL = true;

	/** zzAtEOF == true <=> the scanner is at the EOF */
	private boolean zzAtEOF;

	/** denotes if the user-EOF-code has already been executed */
	private boolean zzEOFDone;

	/**
	 * The number of occupied positions in zzBuffer beyond zzEndRead.
	 * When a lead/high surrogate has been read from the input stream
	 * into the final zzBuffer position, this will have a value of 1;
	 * otherwise, it will have a value of 0.
	 */
	private int zzFinalHighSurrogate = 0;

	private static final byte PARAN     = 1;
	private static final byte BRACKET   = 2;
	private static final byte CURLY     = 3;
	private static final byte ENDBLOCK  = 4;
	private static final byte REPEATBLOCK = 5;

	TokenType longType;
	int longLen;

	protected int tokenStart;
	protected int tokenLength;
	protected int offset;

	/**
	 * Helper method to create and return a new Token from of TokenType
	 * tokenStart and tokenLength will be modified to the newStart and
	 * newLength params
	 * @param type
	 * @param tStart
	 * @param tLength
	 * @param newStart
	 * @param newLength
	 * @return
	 */
	protected Token token(TokenType type, int tStart, int tLength,
						  int newStart, int newLength) {
		tokenStart = newStart;
		tokenLength = newLength;
		return new Token(type, tStart + offset, tLength);
	}

	/**
	 * Create and return a Token of given type from start with length
	 * offset is added to start
	 * @param type
	 * @param start
	 * @param length
	 * @return
	 */
	protected Token token(TokenType type, int start, int length) {
		return new Token(type, start + offset, length);
	}

	protected Token token(TokenType type) {
		return new Token(type, yychar + offset, yylength());
	}

	protected Token token(TokenType type, int pairValue) {
		return new Token(type, yychar + offset, yylength(), (byte) pairValue);
	}

	/**
	 * Creates a new scanner
	 *
	 * @param   in  the java.io.Reader to read input from.
	 */
	public LuaLexer(java.io.Reader in) {
		this.zzReader = in;
	}


	/**
	 * Unpacks the compressed character translation table.
	 *
	 * @param packed   the packed character translation table
	 * @return         the unpacked character translation table
	 */
	private static char [] zzUnpackCMap(String packed) {
		char [] map = new char[0x110000];
		int i = 0;  /* index in packed string  */
		int j = 0;  /* index in unpacked array */
		while (i < 2864) {
			int  count = packed.charAt(i++);
			char value = packed.charAt(i++);
			do map[j++] = value; while (--count > 0);
		}
		return map;
	}


	/**
	 * Refills the input buffer.
	 *
	 * @return      <code>false</code>, iff there was new input.
	 *
	 * @exception   java.io.IOException  if any I/O-Error occurs
	 */
	private boolean zzRefill() throws java.io.IOException {

    /* first: make room (if you can) */
		if (zzStartRead > 0) {
			zzEndRead += zzFinalHighSurrogate;
			zzFinalHighSurrogate = 0;
			System.arraycopy(zzBuffer, zzStartRead,
					zzBuffer, 0,
					zzEndRead-zzStartRead);

      /* translate stored positions */
			zzEndRead-= zzStartRead;
			zzCurrentPos-= zzStartRead;
			zzMarkedPos-= zzStartRead;
			zzStartRead = 0;
		}

    /* is the buffer big enough? */
		if (zzCurrentPos >= zzBuffer.length - zzFinalHighSurrogate) {
      /* if not: blow it up */
			char newBuffer[] = new char[zzBuffer.length*2];
			System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
			zzBuffer = newBuffer;
			zzEndRead += zzFinalHighSurrogate;
			zzFinalHighSurrogate = 0;
		}

    /* fill the buffer with new input */
		int requested = zzBuffer.length - zzEndRead;
		int numRead = zzReader.read(zzBuffer, zzEndRead, requested);

    /* not supposed to occur according to specification of java.io.Reader */
		if (numRead == 0) {
			throw new java.io.IOException("Reader returned 0 characters. See JFlex examples for workaround.");
		}
		if (numRead > 0) {
			zzEndRead += numRead;
      /* If numRead == requested, we might have requested to few chars to
         encode a full Unicode character. We assume that a Reader would
         otherwise never return half characters. */
			if (numRead == requested) {
				if (Character.isHighSurrogate(zzBuffer[zzEndRead - 1])) {
					--zzEndRead;
					zzFinalHighSurrogate = 1;
				}
			}
      /* potentially more input available */
			return false;
		}

    /* numRead < 0 ==> end of stream */
		return true;
	}


	/**
	 * Closes the input stream.
	 */
	public final void yyclose() throws java.io.IOException {
		zzAtEOF = true;            /* indicate end of file */
		zzEndRead = zzStartRead;  /* invalidate buffer    */

		if (zzReader != null)
			zzReader.close();
	}


	/**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 *
	 * All internal variables are reset, the old input stream
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>ZZ_INITIAL</tt>.
	 *
	 * Internal scan buffer is resized down to its initial length, if it has grown.
	 *
	 * @param reader   the new input stream
	 */
	public final void yyreset(java.io.Reader reader) {
		zzReader = reader;
		zzAtBOL  = true;
		zzAtEOF  = false;
		zzEOFDone = false;
		zzEndRead = zzStartRead = 0;
		zzCurrentPos = zzMarkedPos = 0;
		zzFinalHighSurrogate = 0;
		yyline = yychar = yycolumn = 0;
		zzLexicalState = YYINITIAL;
		if (zzBuffer.length > ZZ_BUFFERSIZE)
			zzBuffer = new char[ZZ_BUFFERSIZE];
	}


	/**
	 * Returns the current lexical state.
	 */
	public final int yystate() {
		return zzLexicalState;
	}


	/**
	 * Enters a new lexical state
	 *
	 * @param newState the new lexical state
	 */
	public final void yybegin(int newState) {
		zzLexicalState = newState;
	}


	/**
	 * Returns the text matched by the current regular expression.
	 */
	public final String yytext() {
		return new String( zzBuffer, zzStartRead, zzMarkedPos-zzStartRead );
	}


	/**
	 * Returns the character at position <tt>pos</tt> from the
	 * matched text.
	 *
	 * It is equivalent to yytext().charAt(pos), but faster
	 *
	 * @param pos the position of the character to fetch.
	 *            A value from 0 to yylength()-1.
	 *
	 * @return the character at position pos
	 */
	public final char yycharat(int pos) {
		return zzBuffer[zzStartRead+pos];
	}


	/**
	 * Returns the length of the matched text region.
	 */
	public final int yylength() {
		return zzMarkedPos-zzStartRead;
	}


	/**
	 * Reports an error that occured while scanning.
	 *
	 * In a wellformed scanner (no or only correct usage of
	 * yypushback(int) and a match-all fallback rule) this method
	 * will only be called with things that "Can't Possibly Happen".
	 * If this method is called, something is seriously wrong
	 * (e.g. a JFlex bug producing a faulty scanner etc.).
	 *
	 * Usual syntax/scanner level error handling should be done
	 * in error fallback rules.
	 *
	 * @param   errorCode  the code of the errormessage to display
	 */
	private void zzScanError(int errorCode) {
		String message;
		try {
			message = ZZ_ERROR_MSG[errorCode];
		}
		catch (ArrayIndexOutOfBoundsException e) {
			message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
		}

		throw new Error(message);
	}


	/**
	 * Pushes the specified amount of characters back into the input stream.
	 *
	 * They will be read again by then next call of the scanning method
	 *
	 * @param number  the number of characters to be read again.
	 *                This number must not be greater than yylength()!
	 */
	public void yypushback(int number)  {
		if ( number > yylength() )
			zzScanError(ZZ_PUSHBACK_2BIG);

		zzMarkedPos -= number;
	}


	/**
	 * Resumes scanning until the next regular expression is matched,
	 * the end of input is encountered or an I/O-Error occurs.
	 *
	 * @return      the next token
	 * @exception   java.io.IOException  if any I/O-Error occurs
	 */
	public Token yylex() throws java.io.IOException {
		int zzInput;
		int zzAction;

		// cached fields:
		int zzCurrentPosL;
		int zzMarkedPosL;
		int zzEndReadL = zzEndRead;
		char [] zzBufferL = zzBuffer;
		char [] zzCMapL = ZZ_CMAP;

		int [] zzTransL = ZZ_TRANS;
		int [] zzRowMapL = ZZ_ROWMAP;
		int [] zzAttrL = ZZ_ATTRIBUTE;

		while (true) {
			zzMarkedPosL = zzMarkedPos;

			yychar+= zzMarkedPosL-zzStartRead;

			zzAction = -1;

			zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

			zzState = ZZ_LEXSTATE[zzLexicalState];

			// set up zzAction for empty match case:
			int zzAttributes = zzAttrL[zzState];
			if ( (zzAttributes & 1) == 1 ) {
				zzAction = zzState;
			}


			zzForAction: {
				while (true) {

					if (zzCurrentPosL < zzEndReadL) {
						zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
						zzCurrentPosL += Character.charCount(zzInput);
					}
					else if (zzAtEOF) {
						zzInput = YYEOF;
						break zzForAction;
					}
					else {
						// store back cached positions
						zzCurrentPos  = zzCurrentPosL;
						zzMarkedPos   = zzMarkedPosL;
						boolean eof = zzRefill();
						// get translated positions and possibly new buffer
						zzCurrentPosL  = zzCurrentPos;
						zzMarkedPosL   = zzMarkedPos;
						zzBufferL      = zzBuffer;
						zzEndReadL     = zzEndRead;
						if (eof) {
							zzInput = YYEOF;
							break zzForAction;
						}
						else {
							zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);
							zzCurrentPosL += Character.charCount(zzInput);
						}
					}
					int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];
					if (zzNext == -1) break zzForAction;
					zzState = zzNext;

					zzAttributes = zzAttrL[zzState];
					if ( (zzAttributes & 1) == 1 ) {
						zzAction = zzState;
						zzMarkedPosL = zzCurrentPosL;
						if ( (zzAttributes & 8) == 8 ) break zzForAction;
					}

				}
			}

			// store back cached position
			zzMarkedPos = zzMarkedPosL;

			if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
				zzAtEOF = true;
				switch (zzLexicalState) {
					case STRING1: {
						yybegin(YYINITIAL);
						return token(TokenType.STRING, tokenStart, tokenLength);
					}
					case 112: break;
					case STRING2: {
						yybegin(YYINITIAL);
						return token(TokenType.STRING, tokenStart, tokenLength);
					}
					case 113: break;
					case LONGSTRING: {
						yybegin(YYINITIAL);
						return token(longType, tokenStart, tokenLength);
					}
					case 114: break;
					case COMMENT: {
						yybegin(YYINITIAL);
						return token(TokenType.COMMENT, tokenStart, tokenLength);
					}
					case 115: break;
					case LINECOMMENT: {
						yybegin(YYINITIAL);
						return token(TokenType.COMMENT, tokenStart, tokenLength);
					}
					case 116: break;
					default:
					{
						return null;
					}
				}
			}
			else {
				switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
					case 1:
					{
					}
					case 31: break;
					case 2:
					{ return token(TokenType.OPERATOR,  BRACKET);
					}
					case 32: break;
					case 3:
					{ return token(TokenType.OPERATOR);
					}
					case 33: break;
					case 4:
					{ return token(TokenType.OPERATOR, -BRACKET);
					}
					case 34: break;
					case 5:
					{ return token(TokenType.IDENTIFIER);
					}
					case 35: break;
					case 6:
					{ return token(TokenType.NUMBER);
					}
					case 36: break;
					case 7:
					{ yybegin(STRING1);
						tokenStart = yychar;
						tokenLength = 1;
					}
					case 37: break;
					case 8:
					{ yybegin(STRING2);
						tokenStart = yychar;
						tokenLength = 1;
					}
					case 38: break;
					case 9:
					{ return token(TokenType.OPERATOR,  PARAN);
					}
					case 39: break;
					case 10:
					{ return token(TokenType.OPERATOR, -PARAN);
					}
					case 40: break;
					case 11:
					{ return token(TokenType.OPERATOR,  CURLY);
					}
					case 41: break;
					case 12:
					{ return token(TokenType.OPERATOR, -CURLY);
					}
					case 42: break;
					case 13:
					{ tokenLength += yylength();
					}
					case 43: break;
					case 14:
					{ yybegin(YYINITIAL);
					}
					case 44: break;
					case 15:
					{ yybegin(YYINITIAL);
						// length also includes the trailing quote
						return token(TokenType.STRING, tokenStart, tokenLength + 1);
					}
					case 45: break;
					case 16:
					{ yybegin(YYINITIAL);
						// length also includes the trailing quote
						return token(TokenType.STRING, tokenStart, tokenLength + 1);
					}
					case 46: break;
					case 17:
					{ tokenLength++;
					}
					case 47: break;
					case 18:
					{ yybegin(LINECOMMENT);
						tokenLength += yylength();
					}
					case 48: break;
					case 19:
					{ yybegin(YYINITIAL);
						return token(TokenType.COMMENT, tokenStart, tokenLength);
					}
					case 49: break;
					case 20:
					{ yybegin(YYINITIAL);
						tokenLength += yylength();
						return token(TokenType.COMMENT, tokenStart, tokenLength);
					}
					case 50: break;
					case 21:
					{ longType = TokenType.STRING;
						yybegin(LONGSTRING);
						tokenStart = yychar;
						tokenLength = yylength();
						longLen = tokenLength;
					}
					case 51: break;
					case 22:
					{ yybegin(COMMENT);
						tokenStart = yychar;
						tokenLength = yylength();
					}
					case 52: break;
					case 23:
					{ return token(TokenType.KEYWORD, ENDBLOCK);
					}
					case 53: break;
					case 24:
					{ return token(TokenType.KEYWORD);
					}
					case 54: break;
					case 25:
					{ tokenLength += 2;
					}
					case 55: break;
					case 26:
					{ if (longLen == yylength()) {
						tokenLength += yylength();
						yybegin(YYINITIAL);
						return token(longType, tokenStart, tokenLength);
					} else {
						tokenLength++;
						yypushback(yylength() - 1);
					}
					}
					case 56: break;
					case 27:
					{ longType = TokenType.COMMENT;
						yybegin(LONGSTRING);
						tokenLength += yylength();
						longLen = yylength();
					}
					case 57: break;
					case 28:
					{ return token(TokenType.KEYWORD, -ENDBLOCK);
					}
					case 58: break;
					case 29:
					{ return token(TokenType.KEYWORD, -REPEATBLOCK);
					}
					case 59: break;
					case 30:
					{ return token(TokenType.KEYWORD, REPEATBLOCK);
					}
					case 60: break;
					default:
						zzScanError(ZZ_NO_MATCH);
				}
			}
		}
	}

	enum TokenType {
		KEYWORD,
		COMMENT,
		STRING,
		OPERATOR,
		NUMBER,
		IDENTIFIER
	}


}

