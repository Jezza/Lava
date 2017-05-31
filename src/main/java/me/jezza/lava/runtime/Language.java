package me.jezza.lava.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import me.jezza.lava.lang.LavaLexer;
import me.jezza.lava.lang.Token;
import me.jezza.lava.lang.Tokens;
import me.jezza.lava.lang.base.AbstractParser;
import me.jezza.lava.lang.interfaces.Lexer;
import me.jezza.lava.runtime.Interpreter.ByteCodeWriter;
import me.jezza.lava.runtime.Interpreter.ConstantPool;
import me.jezza.lava.runtime.Interpreter.LuaChunk;
import me.jezza.lava.runtime.Interpreter.Ops;

/**
 * @author Jezza
 */
public final class Language {
	private Language() {
		throw new IllegalStateException();
	}

	public static LuaChunk parse(File root, String input) throws FileNotFoundException {
		String name = input.substring(0, input.lastIndexOf('.'));
		return parse(name, root, new LavaLexer(new File(root, input)));
	}

	public static LuaChunk parse(String name, File root, Lexer lexer) {
		try {
			LanguageParser parser = new LanguageParser(root, lexer);
			return parser.parseChunk(name, new ConstantPool());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final class LanguageParser extends AbstractParser {
		private final File root;

		protected LanguageParser(File root, Lexer lexer) {
			super(lexer);
			this.root = root;
		}

		LuaChunk parseChunk(String sourceName, ConstantPool locals) throws IOException {
			Map<String, Integer> chunks = new HashMap<>();

			LuaChunk chunk = new LuaChunk(sourceName);

			ByteCodeWriter w = new ByteCodeWriter(0);
			ConstantPool pool = new ConstantPool();

			boolean debug = false;

			Token token;
			parse:
			while ((token = current()).type != Tokens.EOS) {
				switch (token.type) {
					case Tokens.FUNCTION: {
						consume(Tokens.FUNCTION);
						String name = consume(Tokens.NAME).text;
						ConstantPool functionLocals = new ConstantPool();
						consume('(');
						int count = 0;
						if (!match(')')) {
							do {
								count++;
								functionLocals.add(consume(Tokens.NAME).text);
							} while (match(','));
							consume(')');
						}
						LuaChunk functionChunk = parseChunk(sourceName + '.' + name, functionLocals);
						functionChunk.paramCount = count;
						chunks.put(name, pool.add(functionChunk));
						break;
					}
					case Tokens.LOCAL: {
						consume(Tokens.LOCAL);
						w.write1(Ops.CONST1, pool.add(consume(Tokens.STRING).text));
						match(';');
						break;
					}
					case Tokens.END: {
						consume(Tokens.END);
						w.write1(Ops.RET);
						chunk.constants = pool.build();
						chunk.code = w.code();
//						chunk.chunks = chunks.toArray(new LuaChunk[0]);
						return chunk;
					}
					case '@': {
						Token pointer = consume('@');
						Token next = current();
						if (pointer.row == next.row && next.type == Tokens.NAME) {
							debug = true;
							continue parse;
						}
						break;
					}
					case Tokens.NAME: {
						int paramCount = 0;
						String name = consume(Tokens.NAME).text;
						switch (current().type) {
							case '(': {
								consume('(');
								// Function call
								if (!match(')')) {
									do {
										Token argument;
										switch ((argument = consume()).type) {
											case Tokens.STRING: {
												w.write1(Ops.CONST1, pool.add(argument.text));
												break;
											}
											case Tokens.FLT: {
												w.write1(Ops.CONST1, pool.add(Double.parseDouble(argument.text)));
												break;
											}
											case Tokens.INT: {
												w.write1(Ops.CONST1, pool.add(Integer.parseInt(argument.text)));
												break;
											}
											case Tokens.NAME: {
												int add = locals.add(argument.text);
												w.write2(Ops.PUSH, add);
												break;
											}
											default:
												throw new IllegalStateException("Unsupported value type: " + argument);
										}
										paramCount++;
//										functionPool.add(consume(Tokens.NAME).text);
									} while (match(','));
									consume(')');
								}
								match(';');
								Integer _index = chunks.get(name);
								if (_index == null) {
									w.write1(Ops.CONST1, pool.add(name));
									w.write1(Ops.GETGLOBAL);
								} else {
									int index = _index;
									w.write1(Ops.CONST1, index);
								}
								if (debug) {
									w.write1(Ops.DEBUG);
								}
								w.write2(Ops.CALL, paramCount, 0);
								if (debug) {
									w.write1(Ops.DEBUG);
									debug = false;
								}
								continue parse;
							}
							case ';': {
								if (debug) {
									consume(';');
									w.write1(Ops.CONST1, pool.add(name));
									w.write1(Ops.PRINT);
									w.write1(Ops.DEBUG);
									debug = false;
									continue parse;
								}
							}
							default:
								throw new IllegalStateException("Unexpected: " + current());
						}
					}
					default:
						throw new IllegalStateException("Unexpected: " + current());
				}
			}
			w.write1(Ops.RET);
			chunk.constants = pool.build();
			chunk.code = w.code();
//			chunk.chunks = chunks.toArray(new LuaChunk[0]);
			return chunk;
		}
	}
}

