package me.jezza.lava.runtime;

/**
 * @author Jezza
 */

import java.lang.reflect.Field;

/**
 * A class containing static values of all the op codes used throughout this interpreter.
 * <p>
 * All javadocs on the following fields follow the pattern:
 * <p>
 * <b>Name: (Other bytes)</b>
 * <p>
 * <b>Other Bytes:</b>
 * <ol>
 *     <li>An ordered list of bytes that need to follow the instruction.</li>
 * </ol>
 * <b>Stack:</b>
 * <blockquote>
 *     [before] -> [after]
 * </blockquote>
 * <b>Description:</b>
 * <blockquote>
 *     A short description of what the instruction does.<br>
 * </blockquote>
 */
public final class OpCodes {
	/**
	 * <b>CONST1: (1)</b>
	 * <p>
	 * <b>Other Bytes:</b>
	 * <ol>
	 *     <li>index into the constant pool</li>
	 * </ol>
	 * <b>Stack:</b>
	 * <blockquote>
	 *     [] -> [value]
	 * </blockquote>
	 * <b>Description:</b>
	 * <blockquote>
	 *     Load a constant onto the stack from the constant pool identified by the given index<br>
	 *     Note: if you wish to load a constant that has an index greater than 1 byte, use CONST2.<br>
	 * </blockquote>
	 */
	private static final byte CONST1 = 0;

	/**
	 * <b>CONST2: (2)</b>
	 * <p>
	 * <b>Other Bytes:</b>
	 * <ol>
	 *     <li>indexbyte1</li>
	 *     <li>indexbyte2</li>
	 * </ol>
	 * <b>Stack:</b>
	 * <blockquote>
	 *     [] -> [value]
	 * </blockquote>
	 * <b>Description:</b>
	 * <blockquote>
	 *     Load a constant onto the stack from the constant pool as identified by the index (indexbyte1 << 8 + indexbyte2)<br>
	 * </blockquote>
	 */
	private static final byte CONST2 = 1;
	private static final byte CONST_NIL = 2;
	private static final byte CONST_TRUE = 3;
	private static final byte CONST_FALSE = 4;

	private static final byte LOAD_FUNCTION = 5;

	private static final byte MOV = 6;
	private static final byte POP = 7;
	private static final byte DUP = 8;


	private static final byte GET_UPVAL = 9;
	private static final byte SET_UPVAL = 10;

	private static final byte NEW_TABLE = 11;
	private static final byte GET_TABLE = 12;
	private static final byte SET_TABLE = 13;

	// TODO: 30/05/2017 Might be able to merge SET_LIST with NEW_TABLE
	private static final byte SET_LIST = 14;

	private static final byte GET_GLOBAL = 15;
	private static final byte SET_GLOBAL = 16;


	private static final byte SELF = 17;


	private static final byte ADD = 18;
	private static final byte SUB = 19;
	private static final byte MUL = 20;
	private static final byte DIV = 21;
	private static final byte MOD = 22;
	private static final byte POW = 23;
	private static final byte NEG = 24;
	private static final byte NOT = 25;

	private static final byte LEN = 26;
	private static final byte CONCAT = 27;

	// Control flow ops don't have dispatchers.

	private static final byte CALL = 28;
	private static final byte GOTO = 29;
	private static final byte IFZ = 30;
	private static final byte IFNZ = 31;
	// TODO: 30/05/2017 Think about encoding equal and not equal
	private static final byte LT = 32;
	private static final byte LE = 33;
	private static final byte TEST = 34;
	private static final byte TEST_SET = 35;
	private static final byte TAILCALL = 36;

	private static final byte FOR_PREP = 37;
	private static final byte FOR_LOOP = 38;
	private static final byte FOR_LOOP_TABLE = 39;

	private static final byte CLOSE = 40;
	private static final byte RET = 41;

	private static final byte PRINT = 42;
	private static final byte DEBUG = 43;

	public static void main(String[] args) {
		// Used when we move around values.
		StringBuilder b = new StringBuilder();
		Field[] fields = OpCodes.class.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			b.append("private static final byte ").append(fields[i].getName()).append(" = ").append(i).append(";\n");
		}
		System.out.println(b);
	}
}
