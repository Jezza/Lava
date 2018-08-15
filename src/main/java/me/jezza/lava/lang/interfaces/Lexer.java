package me.jezza.lava.lang.interfaces;

import java.io.IOException;

import me.jezza.lava.lang.model.Token;

/**
 * @author Jezza
 */
public interface Lexer {
	Token next() throws IOException;
}
