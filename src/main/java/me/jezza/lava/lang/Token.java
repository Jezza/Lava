package me.jezza.lava.lang;

/**
 * @author Jezza
 */
public final class Token {
	public static final Token EOS = new Token(Tokens.EOS, "", -1, -1);

	public final int type;
	public final String text;
	public final int col;
	public final int row;

	public Token(int type, String text, int col, int row) {
		this.type = type;
		this.text = text;
		this.col = col;
		this.row = row;
	}

	@Override
	public String toString() {
		return "Token{" + Tokens.name(type) + ":\"" + text + "\":[" + row + "," + col + "->" + (col + text.length()) + ")}";
	}
}

