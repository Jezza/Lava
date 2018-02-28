package me.jezza.lava.runtime;

/**
 * @author Jezza
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
public final class OpCode {

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Implemented {
	}

	/**
	 * <b>CONST: (2)</b>
	 * <p>
	 * <b>Other Bytes:</b>
	 * <ol>
	 *     <li>pool_index_byte_1</li>
	 *     <li>pool_index_byte_2</li>
	 *     <li>stack_index_byte_1</li>
	 *     <li>stack_index_byte_2</li>
	 * </ol>
	 * <b>Stack:</b>
	 * <blockquote>
	 *     [] -> [value]
	 * </blockquote>
	 * <b>Description:</b>
	 * <blockquote>
	 *     Loads a constant from the constant pool as identified
	 *     by the index (pool_index_byte_1 << 8 + pool_index_byte_2)
	 *     into the stack with the index (stack_index_byte_1 << 8 + stack_index_byte_2)
	 * </blockquote>
	 */
	@Implemented
	public static final byte CONST = 0;

	/**
	 * <b>CONST_NIL: (0)</b>
	 * <p>
	 * <b>Other Bytes:</b>
	 * <ol>
	 *     <li>N/A</li>
	 * </ol>
	 * <b>Stack:</b>
	 * <blockquote>
	 *     [] -> [nil]
	 * </blockquote>
	 * <b>Description:</b>
	 * <blockquote>
	 *     Load a nil reference onto the stack<br>
	 * </blockquote>
	 */
	@Implemented
	public static final byte CONST_NIL = 1;

	/**
	 * <b>CONST_TRUE: (0)</b>
	 * <p>
	 * <b>Other Bytes:</b>
	 * <ol>
	 *     <li>N/A</li>
	 * </ol>
	 * <b>Stack:</b>
	 * <blockquote>
	 *     [] -> [true]
	 * </blockquote>
	 * <b>Description:</b>
	 * <blockquote>
	 *     Load true onto the stack<br>
	 * </blockquote>
	 */
	@Implemented
	public static final byte CONST_TRUE = 2;

	/**
	 * <b>CONST_FALSE: (0)</b>
	 * <p>
	 * <b>Other Bytes:</b>
	 * <ol>
	 *     <li>N/A</li>
	 * </ol>
	 * <b>Stack:</b>
	 * <blockquote>
	 *     [] -> [false]
	 * </blockquote>
	 * <b>Description:</b>
	 * <blockquote>
	 *     Load false onto the stack<br>
	 * </blockquote>
	 */
	@Implemented
	public static final byte CONST_FALSE = 3;

	/**
	 * <b>LOAD_FUNC: (1)</b>
	 * <p>
	 * <b>Other Bytes:</b>
	 * <ol>
	 *     <li>indexbyte</li>
	 * </ol>
	 * <b>Stack:</b>
	 * <blockquote>
	 *     [] -> [function]
	 * </blockquote>
	 * <b>Description:</b>
	 * <blockquote>
	 *     Load a function onto the stack from the constant pool identified by the given index<br>
	 *     This function also prepares the function's upvalues.<br>
	 * </blockquote>
	 */
	@Implemented
	public static final byte LOAD_FUNC = 4;

	@Implemented
	public static final byte MOVE = 5;
	public static final byte POP = 6;
	public static final byte DUP = 7;

	@Implemented
	public static final byte GET_UPVAL = 8;

	@Implemented
	public static final byte SET_UPVAL = 9;

	public static final byte NEW_TABLE = 10;
	public static final byte GET_TABLE = 11;
//	@Implemented
	public static final byte SET_TABLE = 12;

	// TODO: 30/05/2017 Might be able to merge SET_LIST with NEW_TABLE
	public static final byte SET_LIST = 13;

//	/**
//	 * <b>LOAD_FUNCTION: (1)</b>
//	 * <p>
//	 * <b>Other Bytes:</b>
//	 * <ol>
//	 *     <li>N/A</li>
//	 * </ol>
//	 * <b>Stack:</b>
//	 * <blockquote>
//	 *     [name] -> [value]
//	 * </blockquote>
//	 * <b>Description:</b>
//	 * <blockquote>
//	 *     Loads a global onto the stack identified by the current value on top of the stack<br>
//	 * </blockquote>
//	 */
	@Implemented
	public static final byte GET_GLOBAL = 14;

	@Implemented
	public static final byte SET_GLOBAL = 15;


	public static final byte SELF = 16;

	/**
	 * <b>ADD: (0)</b>
	 * <p>
	 * <b>Other Bytes:</b>
	 * <ol>
	 *     <li>indexbyte</li>
	 * </ol>
	 * <b>Stack:</b>
	 * <blockquote>
	 *     [value1, value2] -> [result]
	 * </blockquote>
	 * <b>Description:</b>
	 * <blockquote>
	 *     Pops two values off of the stack, adds them, and places the resulting value back on the stack.<br>
	 * </blockquote>
	 */
	@Implemented
	public static final byte ADD = 17;
	public static final byte SUB = 18;
	@Implemented
	public static final byte MUL = 19;
	public static final byte DIV = 20;
	public static final byte MOD = 21;
	public static final byte POW = 22;
	public static final byte NEG = 23;
	public static final byte NOT = 24;

	public static final byte LEN = 25;
	public static final byte CONCAT = 26;

	// Control flow ops don't have dispatchers.

	/**
	 * <b>CALL: (1)</b>
	 * <p>
	 * <b>Other Bytes:</b>
	 * <ol>
	 *     <li>indexbyte</li>
	 *     <li>indexbyte</li>
	 *     <li>indexbyte</li>
	 *     <li>indexbyte</li>
	 * </ol>
	 * <b>Stack:</b>
	 * <blockquote>
	 *     [] -> [function]
	 * </blockquote>
	 * <b>Description:</b>
	 * <blockquote>
	 *     Load a function onto the stack from the constant pool identified by the given index<br>
	 *     This function also prepares the function's upvalues.<br>
	 * </blockquote>
	 */
	@Implemented
	public static final byte CALL = 27;
	public static final byte GOTO = 28;
	public static final byte IFZ = 29;
	public static final byte IFNZ = 30;
	// TODO: 30/05/2017 Think about encoding equal and not equal
	public static final byte LT = 31;
	public static final byte LE = 32;
	public static final byte TEST = 33;
	public static final byte TEST_SET = 34;
	public static final byte TAILCALL = 35;

	public static final byte FOR_PREP = 36;
	public static final byte FOR_LOOP = 37;
	public static final byte FOR_LOOP_TABLE = 38;

	public static final byte CLOSE = 39;
	@Implemented
	public static final byte RETURN = 40;

	public static final byte PRINT = 41;
	@Implemented
	public static final byte DEBUG = 42;

	public static void main(String[] args) {
		// Used when we move around values.
		StringBuilder b = new StringBuilder();
		Field[] fields = OpCode.class.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			b.append("public static final byte ").append(fields[i].getName()).append(" = ").append(i).append(";\n");
		}
		System.out.println(b);
	}
}
