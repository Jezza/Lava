/*  $Header: //info.ravenbrook.com/project/jili/version/1.1/code/mnj/lua/MathLib.java#1 $
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

package me.jezza.luja;

import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Contains Lua's math library.
 * The library can be opened using the {@link #open} method.
 * Because this library is implemented on top of CLDC 1.1 it is not as
 * complete as the PUC-Rio math library.  Trigononmetric inverses
 * (EG <code>acos</code>) and hyperbolic trigonometric functions (EG
 * <code>cosh</code>) are not provided.
 */
public final class MathLib extends LuaJavaCallback {
	private static final Random rng = new Random();

	/**
	 * Opens the library into the given Lua state.  This registers
	 * the symbols of the library in the global table.
	 *
	 * @param L The Lua state into which to open.
	 */
	public static void open(Lua L) {
		LuaTable t = L.register("math");

		r(L, "abs", MathLib::abs);
		r(L, "ceil", MathLib::ceil);
		r(L, "cos", MathLib::cos);
		r(L, "deg", MathLib::deg);
		r(L, "exp", MathLib::exp);
		r(L, "floor", MathLib::floor);
		r(L, "fmod", MathLib::fmod);
		r(L, "max", MathLib::max);
		r(L, "min", MathLib::min);
		r(L, "modf", MathLib::modf);
		r(L, "pow", MathLib::pow);
		r(L, "rad", MathLib::rad);
		r(L, "random", MathLib::random);
		r(L, "randomseed", MathLib::randomseed);
		r(L, "sin", MathLib::sin);
		r(L, "sqrt", MathLib::sqrt);
		r(L, "tan", MathLib::tan);

		L.setField(t, "pi", Math.PI);
		L.setField(t, "huge", Double.POSITIVE_INFINITY);
	}

	/**
	 * Register a function.
	 */
	private static void r(Lua L, String name, ToIntFunction<Lua> function) {
		L.setField(L.getGlobal("math"), name, new MathLib(function));
	}

	/**
	 * Which library function this object represents.  This value should
	 * be one of the "enums" defined in the class.
	 */
	private ToIntFunction<Lua> function;

	/**
	 * Constructs instance, filling in the 'which' member.
	 */
	private MathLib(ToIntFunction<Lua> function) {
		this.function = function;
	}

	/**
	 * Implements all of the functions in the Lua math library.  Do not
	 * call directly.
	 *
	 * @param L the Lua state in which to execute.
	 * @return number of returned parameters, as per convention.
	 */
	@Override
	public int luaFunction(Lua L) {
		return function.applyAsInt(L);
	}

	private static int abs(Lua L) {
		L.pushNumber(Math.abs(L.checkNumber(1)));
		return 1;
	}

	private static int ceil(Lua L) {
		L.pushNumber(Math.ceil(L.checkNumber(1)));
		return 1;
	}

	private static int cos(Lua L) {
		L.pushNumber(Math.cos(L.checkNumber(1)));
		return 1;
	}

	private static int deg(Lua L) {
		L.pushNumber(Math.toDegrees(L.checkNumber(1)));
		return 1;
	}

	private static int exp(Lua L) {
		// CLDC 1.1 has Math.E but no exp, pow, or log.  Bizarre.
		L.pushNumber(Lua.iNumpow(Math.E, L.checkNumber(1)));
		return 1;
	}

	private static int floor(Lua L) {
		L.pushNumber(Math.floor(L.checkNumber(1)));
		return 1;
	}

	private static int fmod(Lua L) {
		L.pushNumber(L.checkNumber(1) % L.checkNumber(2));
		return 1;
	}

	private static int max(Lua L) {
		int n = L.getTop(); // number of arguments
		double dmax = L.checkNumber(1);
		for (int i = 2; i <= n; ++i)
			dmax = Math.max(dmax, L.checkNumber(i));
		L.pushNumber(dmax);
		return 1;
	}

	private static int min(Lua L) {
		int n = L.getTop(); // number of arguments
		double dmin = L.checkNumber(1);
		for (int i = 2; i <= n; ++i) {
			double d = L.checkNumber(i);
			dmin = Math.min(dmin, d);
		}
		L.pushNumber(dmin);
		return 1;
	}

	private static int modf(Lua L) {
		double x = L.checkNumber(1);
		double fp = x % 1;
		double ip = x - fp;
		L.pushNumber(ip);
		L.pushNumber(fp);
		return 2;
	}

	private static int pow(Lua L) {
		L.pushNumber(Lua.iNumpow(L.checkNumber(1), L.checkNumber(2)));
		return 1;
	}

	private static int rad(Lua L) {
		L.pushNumber(Math.toRadians(L.checkNumber(1)));
		return 1;
	}

	private static int random(Lua L) {
		// It would seem better style to associate the java.util.Random
		// instance with the Lua instance (by implementing and using a
		// registry for example).  However, PUC-rio uses the ISO C library
		// and so will share the same random number generator across all Lua
		// states.  So we do too.
		switch (L.getTop()) // check number of arguments
		{
			case 0:   // no arguments
				L.pushNumber(rng.nextDouble());
				break;

			case 1:   // only upper limit
			{
				int u = L.checkInt(1);
				L.argCheck(1 <= u, 1, "interval is empty");
				L.pushNumber(rng.nextInt(u) + 1);
			}
			break;

			case 2:   // lower and upper limits
			{
				int l = L.checkInt(1);
				int u = L.checkInt(2);
				L.argCheck(l <= u, 2, "interval is empty");
				L.pushNumber(rng.nextInt(u) + l);
			}
			break;

			default:
				throw L.error("wrong number of arguments");
		}
		return 1;
	}

	private static int randomseed(Lua L) {
		rng.setSeed((long) L.checkNumber(1));
		return 0;
	}

	private static int sin(Lua L) {
		L.pushNumber(Math.sin(L.checkNumber(1)));
		return 1;
	}

	private static int sqrt(Lua L) {
		L.pushNumber(Math.sqrt(L.checkNumber(1)));
		return 1;
	}

	private static int tan(Lua L) {
		L.pushNumber(Math.tan(L.checkNumber(1)));
		return 1;
	}
}
