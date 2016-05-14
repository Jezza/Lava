/*  $Header: //info.ravenbrook.com/project/jili/version/1.1/code/mnj/lua/BaseLib.java#1 $
 * Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
 * All rights reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject
 * to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package me.jezza.lava;

import java.io.PrintStream;
import java.io.Reader;
import java.util.Iterator;

/**
 * Contains Lua's base library.  The base library is generally
 * considered essential for running any Lua program.  The base library
 * can be opened using the {@link #open} method.
 */
public final class BaseLib {
	/**
	 * Used by {@link #collectgarbage}.
	 */
	private static final String[] CGOPTS = {"stop", "restart", "collect", "count", "step", "setpause", "setstepmul"};

	/**
	 * Used by {@link #collectgarbage}.
	 */
	private static final int[] CGOPTSNUM = {Lua.GCSTOP, Lua.GCRESTART, Lua.GCCOLLECT, Lua.GCCOUNT, Lua.GCSTEP, Lua.GCSETPAUSE, Lua.GCSETSTEPMUL};

	/**
	 * Lua value that represents the generator function for ipairs.  In
	 * PUC-Rio this is implemented as an upvalue of ipairs.
	 */
	private static final LuaJavaCallback IPAIRS_AUX_FUN = BaseLib::ipairsaux;

	/**
	 * Lua value that represents the generator function for pairs.  In
	 * PUC-Rio this is implemented as an upvalue of pairs.
	 */
	private static final LuaJavaCallback PAIRS_AUX_FUN = BaseLib::pairsaux;

	/**
	 * Opens the base library into the given Lua state.  This registers
	 * the symbols of the base library in the global table.
	 *
	 * @param L The Lua state into which to open.
	 */
	public static void open(Lua L) {
		// set global _G
		L.setGlobal("_G", L.getGlobals());
		// set global _VERSION
		L.setGlobal("_VERSION", Lua.VERSION);
		r(L, "assert", BaseLib::assertFunction);
		r(L, "collectgarbage", BaseLib::collectgarbage);
		r(L, "dofile", BaseLib::dofile);
		r(L, "error", BaseLib::error);
		r(L, "getfenv", BaseLib::getfenv);
		r(L, "getmetatable", BaseLib::getmetatable);
		r(L, "ipairs", BaseLib::ipairs);
		r(L, "loadfile", BaseLib::loadfile);
		r(L, "load", BaseLib::load);
		r(L, "loadstring", BaseLib::loadstring);
		r(L, "next", BaseLib::next);
		r(L, "pairs", BaseLib::pairs);
		r(L, "pcall", BaseLib::pcall);
		r(L, "print", BaseLib::print);
		r(L, "rawequal", BaseLib::rawequal);
		r(L, "rawget", BaseLib::rawget);
		r(L, "rawset", BaseLib::rawset);
		r(L, "select", BaseLib::select);
		r(L, "setfenv", BaseLib::setfenv);
		r(L, "setmetatable", BaseLib::setmetatable);
		r(L, "tonumber", BaseLib::tonumber);
		r(L, "tostring", BaseLib::tostring);
		r(L, "type", BaseLib::type);
		r(L, "unpack", BaseLib::unpack);
		r(L, "xpcall", BaseLib::xpcall);

		// The coroutine functions (which reside in the table "coroutine") are also part of the base library.
		LuaTable t = L.register("coroutine");

		c(L, t, "create", BaseLib::create);
		c(L, t, "resume", BaseLib::resume);
		c(L, t, "running", BaseLib::running);
		c(L, t, "status", BaseLib::status);
		c(L, t, "wrap", BaseLib::wrap);
		c(L, t, "yield", BaseLib::yield);
	}

	/**
	 * Register a function.
	 */
	private static void r(Lua L, String name, LuaJavaCallback function) {
		L.setGlobal(name, function);
	}

	/**
	 * Register a function in the coroutine table.
	 */
	private static void c(Lua L, LuaTable t, String name, LuaJavaCallback function) {
		L.setField(t, name, function);
	}

	/**
	 * Private constructor.
	 */
	private BaseLib() {
		throw new IllegalStateException();
	}

	/**
	 * Implements assert.  <code>assert</code> is a keyword in some
	 * versions of Java, so this function has a mangled name.
	 */
	private static int assertFunction(Lua L) {
		L.checkAny(1);
		if (!Lua.toBoolean(L.value(1)))
			throw L.error(L.optString(2, "assertion failed!"));
		return L.getTop();
	}

	/**
	 * Implements collectgarbage.
	 */
	private static int collectgarbage(Lua L) {
		int o = L.checkOption(1, "collect", CGOPTS);
		int i = CGOPTSNUM[o];
		int res = L.gc(i, L.optInt(2, 0));
		switch (i) {
			case Lua.GCCOUNT:
				L.pushNumber(res + L.gc(Lua.GCCOUNTB, 0) / 1024);
				return 1;
			case Lua.GCSTEP:
				L.pushBoolean(res != 0);
				return 1;
			default:
				L.pushNumber(res);
				return 1;
		}
	}

	/**
	 * Implements dofile.
	 */
	private static int dofile(Lua L) {
		String name = L.optString(1, null);
		int n = L.getTop();
		if (L.loadFile(name) != 0)
			throw L.error(L.value(-1));
		L.call(0, Lua.MULTRET);
		return L.getTop() - n;
	}

	/**
	 * Implements error.
	 */
	private static int error(Lua L) {
		int level = L.optInt(2, 1);
		L.setTop(1);
		if (Lua.isString(L.value(1)) && level > 0) {
			L.insert(L.where(level), 1);
			L.concat(2);
		}
		throw L.error(L.value(1));
	}

	/**
	 * Helper for getfenv and setfenv.
	 */
	private static Object getfunc(Lua L) {
		Object o = L.value(1);
		if (Lua.isFunction(o)) {
			return o;
		} else {
			int level = L.optInt(1, 1);
			L.argCheck(level >= 0, 1, "level must be non-negative");
			Debug ar = L.getStack(level);
			if (ar == null) {
				L.argError(1, "invalid level");
			}
			L.getInfo("f", ar);
			o = L.value(-1);
			if (Lua.isNil(o)) {
				throw L.error("no function environment for tail call at level " + level);
			}
			L.pop(1);
			return o;
		}
	}

	/**
	 * Implements getfenv.
	 */
	private static int getfenv(Lua L) {
		Object o = getfunc(L);
		if (Lua.isJavaFunction(o)) {
			L.push(L.getGlobals());
		} else {
			LuaFunction f = (LuaFunction) o;
			L.push(f.getEnv());
		}
		return 1;
	}

	/**
	 * Implements getmetatable.
	 */
	private static int getmetatable(Lua L) {
		L.checkAny(1);
		Object mt = L.getMetatable(L.value(1));
		if (mt == null) {
			L.pushNil();
			return 1;
		}
		Object protectedmt = L.getMetafield(L.value(1), "__metatable");
		if (Lua.isNil(protectedmt)) {
			L.push(mt);               // return metatable
		} else {
			L.push(protectedmt);      // return __metatable field
		}
		return 1;
	}

	/**
	 * Implements load.
	 */
	private static int load(Lua L) {
		String cname = L.optString(2, "=(load)");
		L.checkType(1, Lua.TFUNCTION);
		Reader r = new BaseLibReader(L, L.value(1));
		return load_aux(L, L.load(r, cname));
	}

	/**
	 * Implements loadfile.
	 */
	private static int loadfile(Lua L) {
		return load_aux(L, L.loadFile(L.optString(1, null)));
	}

	/**
	 * Implements loadstring.
	 */
	private static int loadstring(Lua L) {
		String s = L.checkString(1);
		String chunkName = L.optString(2, s);
		// "binary" dumped into string using string.dump.
		if (s.startsWith("\033"))
			return load_aux(L, L.load(new ReaderInputStream(s), chunkName));
		return load_aux(L, L.loadString(s, chunkName));
	}

	private static int load_aux(Lua L, int status) {
		if (status == 0) // OK?
			return 1;
		// put before error message
		L.insert(Lua.NIL, -1);
		return 2; // return nil plus error message
	}

	/**
	 * Implements next.
	 */
	private static int next(Lua L) {
		L.checkType(1, Lua.TTABLE);
		L.setTop(2);        // Create a 2nd argument is there isn't one
		if (L.next(1))
			return 2;
		L.push(Lua.NIL);
		return 1;
	}

	/**
	 * Implements ipairs.
	 */
	private static int ipairs(Lua L) {
		L.checkType(1, Lua.TTABLE);
		L.push(IPAIRS_AUX_FUN);
		L.pushValue(1);
		L.pushNumber(0);
		return 3;
	}

	/**
	 * Generator for ipairs.
	 */
	private static int ipairsaux(Lua L) {
		int i = L.checkInt(2);
		L.checkType(1, Lua.TTABLE);
		++i;
		Object v = Lua.rawGetI(L.value(1), i);
		if (Lua.isNil(v))
			return 0;
		L.pushNumber(i);
		L.push(v);
		return 2;
	}

	/**
	 * Implements pairs.  PUC-Rio uses "next" as the generator for pairs.
	 * Jill doesn't do that because it would be way too slow.  We use the
	 * {@link java.util.Enumeration} returned from
	 * {@link java.util.Hashtable#keys}.  The {@link #pairsaux} method
	 * implements the step-by-step iteration.
	 */
	private static int pairs(Lua L) {
		L.checkType(1, Lua.TTABLE);
		L.push(PAIRS_AUX_FUN);                   // return generator,
		LuaTable t = (LuaTable) L.value(1);
		L.push(new Object[]{t, t.keys()});   // state,
		L.push(Lua.NIL);                            // and initial value.
		return 3;
	}

	/**
	 * Generator for pairs.  This expects a <var>state</var> and
	 * <var>var</var> as (Lua) arguments.
	 * The state is setup by {@link #pairs} and is a
	 * pair of {LuaTable, Enumeration} stored in a 2-element array.  The
	 * <var>var</var> is not used.  This is in contrast to the PUC-Rio
	 * implementation, where the state is the table, and the var is used
	 * to generated the next key in sequence.  The implementation, of
	 * pairs and pairsaux, has no control over <var>var</var>,  Lua's
	 * semantics of <code>for</code> force it to be the previous result
	 * returned by this function.  In Jill this value is not suitable to
	 * use for enumeration, which is why it isn't used.
	 */
	private static int pairsaux(Lua L) {
		Object[] a = (Object[]) L.value(1);
		LuaTable t = (LuaTable) a[0];
		Iterator e = (Iterator) a[1];
		if (!e.hasNext())
			return 0;
		Object key = e.next();
		L.push(key);
		L.push(t.get(key));
		return 2;
	}

	/**
	 * Implements pcall.
	 */
	private static int pcall(Lua L) {
		L.checkAny(1);
		int status = L.pcall(L.getTop() - 1, Lua.MULTRET, null);
		L.insert(status == 0, 1);
		return L.getTop();
	}

	/**
	 * The {@link PrintStream} used by print.  Makes it more convenient if
	 * redirection is desired.  For example, client code could implement
	 * their own instance which sent output to the screen of a JME device.
	 */
	static final PrintStream OUT = System.out;

	/**
	 * Implements print.
	 */
	private static int print(Lua L) {
		int n = L.getTop();
		Object tostring = L.getGlobal("tostring");
		for (int i = 1; i <= n; ++i) {
			L.push(tostring);
			L.pushValue(i);
			L.call(1, 1);
			String s = L.toString(L.value(-1));
			if (s == null)
				throw L.error("'tostring' must return a string to 'print'");
			if (i > 1)
				OUT.print('\t');
			OUT.print(s);
			L.pop(1);
		}
		OUT.println();
		return 0;
	}

	/**
	 * Implements rawequal.
	 */
	private static int rawequal(Lua L) {
		L.checkAny(1);
		L.checkAny(2);
		L.pushBoolean(Lua.rawEqual(L.value(1), L.value(2)));
		return 1;
	}

	/**
	 * Implements rawget.
	 */
	private static int rawget(Lua L) {
		L.checkType(1, Lua.TTABLE);
		L.checkAny(2);
		L.push(Lua.rawGet(L.value(1), L.value(2)));
		return 1;
	}

	/**
	 * Implements rawset.
	 */
	private static int rawset(Lua L) {
		L.checkType(1, Lua.TTABLE);
		L.checkAny(2);
		L.checkAny(3);
		L.rawSet(L.value(1), L.value(2), L.value(3));
		return 0;
	}

	/**
	 * Implements select.
	 */
	private static int select(Lua L) {
		int n = L.getTop();
		if (L.type(1) == Lua.TSTRING && "#".equals(L.toString(L.value(1)))) {
			L.pushNumber(n - 1);
			return 1;
		}
		int i = L.checkInt(1);
		if (i < 0) {
			i = n + i;
		} else if (i > n) {
			i = n;
		}
		L.argCheck(1 <= i, 1, "index out of range");
		return n - i;
	}

	/**
	 * Implements setfenv.
	 */
	private static int setfenv(Lua L) {
		L.checkType(2, Lua.TTABLE);
		Object o = getfunc(L);
		Object first = L.value(1);
		if (Lua.isNumber(first) && Lua.toNumber(first) == 0) {
			// :todo: change environment of current thread.
			return 0;
		} else if (Lua.isJavaFunction(o) || !L.setFenv(o, L.value(2))) {
			throw L.error("'setfenv' cannot change environment of given object");
		}
		L.push(o);
		return 1;
	}

	/**
	 * Implements setmetatable.
	 */
	private static int setmetatable(Lua L) {
		L.checkType(1, Lua.TTABLE);
		int t = L.type(2);
		L.argCheck(t == Lua.TNIL || t == Lua.TTABLE, 2,
				"nil or table expected");
		if (!Lua.isNil(L.getMetafield(L.value(1), "__metatable"))) {
			throw L.error("cannot change a protected metatable");
		}
		L.setMetatable(L.value(1), L.value(2));
		L.setTop(1);
		return 1;
	}

	/**
	 * Implements tonumber.
	 */
	private static int tonumber(Lua L) {
		int base = L.optInt(2, 10);
		if (base == 10)     // standard conversion
		{
			L.checkAny(1);
			Object o = L.value(1);
			if (Lua.isNumber(o)) {
				L.pushNumber(Lua.toNumber(o));
				return 1;
			}
		} else {
			String s = L.checkString(1);
			L.argCheck(2 <= base && base <= 36, 2, "base out of range");
			// :todo: consider stripping space and sharing some code with
			// Lua.vmTostring
			try {
				int i = Integer.parseInt(s, base);
				L.pushNumber(i);
				return 1;
			} catch (NumberFormatException e_) {
			}
		}
		L.push(Lua.NIL);
		return 1;
	}

	/**
	 * Implements tostring.
	 */
	private static int tostring(Lua L) {
		L.checkAny(1);
		Object o = L.value(1);

		if (L.callMeta(1, "__tostring"))    // is there a metafield?
		{
			return 1; // use its value
		}
		switch (L.type(1)) {
			case Lua.TNUMBER:
				L.push(L.toString(o));
				break;
			case Lua.TSTRING:
				L.push(o);
				break;
			case Lua.TBOOLEAN:
				if (Lua.toBoolean(o)) {
					L.pushLiteral("true");
				} else {
					L.pushLiteral("false");
				}
				break;
			case Lua.TNIL:
				L.pushLiteral("nil");
				break;
			default:
				L.push(o.toString());
				break;
		}
		return 1;
	}

	/**
	 * Implements type.
	 */
	private static int type(Lua L) {
		L.checkAny(1);
		L.push(L.typeNameOfIndex(1));
		return 1;
	}

	/**
	 * Implements unpack.
	 */
	private static int unpack(Lua L) {
		L.checkType(1, Lua.TTABLE);
		LuaTable t = (LuaTable) L.value(1);
		int i = L.optInt(2, 1);
		int e = L.optInt(3, t.firstNilIndex());
		int n = e - i + 1;  // number of elements
		if (n <= 0) {
			return 0;         // empty range
		}
		// i already initialised to start index, which isn't necessarily 1
		for (; i <= e; ++i) {
			L.push(t.get(i));
		}
		return n;
	}

	/**
	 * Implements xpcall.
	 */
	private static int xpcall(Lua L) {
		L.checkAny(2);
		Object errfunc = L.value(2);
		L.setTop(1);        // remove error function from stack
		int status = L.pcall(0, Lua.MULTRET, errfunc);
		L.insert(status == 0, 1);
		return L.getTop();  // return status + all results
	}

	/**
	 * Implements coroutine.create.
	 */
	private static int create(Lua L) {
		Lua NL = L.newThread();
		Object faso = L.value(1);
		L.argCheck(Lua.isFunction(faso) && !Lua.isJavaFunction(faso), 1, "Lua function expected");
		L.setTop(1);        // function is at top
		L.xmove(NL, 1);     // move function from L to NL
		L.push(NL);
		return 1;
	}

	/**
	 * Implements coroutine.resume.
	 */
	private static int resume(Lua L) {
		Lua co = L.toThread(L.value(1));
		L.argCheck(co != null, 1, "coroutine expected");
		int r = auxresume(L, co, L.getTop() - 1);
		if (r < 0) {
			L.insert(Boolean.FALSE, -1);
			return 2; // return false + error message
		}
		L.insert(Boolean.TRUE, L.getTop() - (r - 1));
		return r + 1;       // return true + 'resume' returns
	}

	/**
	 * Implements coroutine.running.
	 */
	private static int running(Lua L) {
		if (L.isMain())
			return 0; // main thread is not a coroutine
		L.push(L);
		return 1;
	}

	/**
	 * Implements coroutine.status.
	 */
	private static int status(Lua L) {
		Lua co = L.toThread(L.value(1));
		L.argCheck(co != null, 1, "coroutine expected");
		if (L == co) {
			L.pushLiteral("running");
		} else {
			switch (co.status()) {
				case Lua.YIELD:
					L.pushLiteral("suspended");
					break;
				case 0: {
					Debug ar = co.getStack(0);
					if (ar != null)       // does it have frames?
					{
						L.pushLiteral("normal");    // it is running
					} else if (co.getTop() == 0) {
						L.pushLiteral("dead");
					} else {
						L.pushLiteral("suspended"); // initial state
					}
				}
				break;
				default:        // some error occured
					L.pushLiteral("dead");
			}
		}
		return 1;
	}

	/**
	 * Implements coroutine.wrap.
	 */
	private static int wrap(Lua L) {
		create(L);
		L.push(wrapit(L.toThread(L.value(-1))));
		return 1;
	}

	/**
	 * Helper for wrap.  Returns a LuaJavaCallback that has access to the
	 * Lua thread.
	 *
	 * @param L the Lua thread to be wrapped.
	 */
	private static LuaJavaCallback wrapit(Lua L) {
		return l -> wrapaux(l, L);
	}

	/**
	 * Helper for wrap.  This implements the function returned by wrap.
	 */
	private static int wrapaux(Lua L, Lua co) {
		int r = auxresume(L, co, L.getTop());
		if (r < 0) {
			if (Lua.isString(L.value(-1)))      // error object is a string?
			{
				String w = L.where(1);
				L.insert(w, -1);
				L.concat(2);
			}
			throw L.error(L.value(-1));     // propagate error
		}
		return r;
	}

	private static int auxresume(Lua L, Lua co, int narg) {
		// if (!co.checkStack...
		if (co.status() == 0 && co.getTop() == 0) {
			L.pushLiteral("cannot resume dead coroutine");
			return -1;        // error flag;
		}
		L.xmove(co, narg);
		int status = co.resume(narg);
		if (status == 0 || status == Lua.YIELD) {
			int nres = co.getTop();
			// if (!L.checkStack...
			co.xmove(L, nres);        // move yielded values
			return nres;
		}
		co.xmove(L, 1);   // move error message
		return -1;        // error flag;
	}

	/**
	 * Implements coroutine.yield.
	 */
	private static int yield(Lua L) {
		return L.yield(L.getTop());
	}

}
