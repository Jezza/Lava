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

package me.jezza.lava;

import java.util.Random;

/**
 * Contains Lua's math library.
 * The library can be opened using the {@link #open} method.
 * Because this library is implemented on top of CLDC 1.1 it is not as
 * complete as the PUC-Rio math library.  Trigononmetric inverses
 * (EG <code>acos</code>) and hyperbolic trigonometric functions (EG
 * <code>cosh</code>) are not provided.
 */
public final class MathLib {
	private static final Random rng = new Random();

	/**
	 * Opens the library into the given Lua state.  This registers
	 * the symbols of the library in the global table.
	 *
	 * @param L The Lua state into which to open.
	 */
	public static void open(Lua L) {
		LuaTable t = L.register("math");

		r(L, t, "abs", MathLib::abs);
		r(L, t, "ceil", MathLib::ceil);
		r(L, t, "cos", MathLib::cos);
		r(L, t, "deg", MathLib::deg);
		r(L, t, "exp", MathLib::exp);
		r(L, t, "floor", MathLib::floor);
		r(L, t, "fmod", MathLib::fmod);
		r(L, t, "max", MathLib::max);
		r(L, t, "min", MathLib::min);
		r(L, t, "modf", MathLib::modf);
		r(L, t, "pow", MathLib::pow);
		r(L, t, "rad", MathLib::rad);
		r(L, t, "random", MathLib::random);
		r(L, t, "randomseed", MathLib::randomseed);
		r(L, t, "sin", MathLib::sin);
		r(L, t, "sqrt", MathLib::sqrt);
		r(L, t, "tan", MathLib::tan);

		L.setField(t, "pi", Math.PI);
		L.setField(t, "huge", Double.POSITIVE_INFINITY);
	}

	/**
	 * Register a function.
	 */
	private static void r(Lua L, LuaTable t, String name, LuaJavaCallback function) {
		L.setField(t, name, function);
	}

	/**
	 * Private constructor.
	 */
	private MathLib() {
		throw new IllegalStateException();
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
		L.pushNumber(Math.exp(L.checkNumber(1)));
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
		double max = L.checkNumber(1);
		for (int i = 2; i <= n; ++i)
			max = Math.max(max, L.checkNumber(i));
		L.pushNumber(max);
		return 1;
	}

	private static int min(Lua L) {
		int n = L.getTop(); // number of arguments
		double min = L.checkNumber(1);
		for (int i = 2; i <= n; ++i)
			min = Math.min(min, L.checkNumber(i));
		L.pushNumber(min);
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
		L.pushNumber(Math.pow(L.checkNumber(1), L.checkNumber(2)));
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
		// check number of arguments
		switch (L.getTop()) {
			case 0:   // no arguments
				L.pushNumber(rng.nextDouble());
				break;

			case 1:   // only upper limit
			{
				int u = L.checkInt(1);
				L.argCheck(1 <= u, 1, "interval is empty");
				L.pushNumber(rng.nextInt(u) + 1);
				break;
			}

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
