package me.jezza.lava.lang.interfaces;

import java.io.IOException;

import me.jezza.lava.lang.Token;

/**
 * @author Jezza
 */
public interface Lexer {
	int EOS = -1;

	Token next() throws IOException;
}
