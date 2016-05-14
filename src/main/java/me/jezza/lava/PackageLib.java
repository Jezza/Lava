/*  $Header: //info.ravenbrook.com/project/jili/version/1.1/code/mnj/lua/PackageLib.java#1 $
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

import java.io.IOException;
import java.net.URL;
import java.util.function.ToIntBiFunction;

/**
 * Contains Lua's package library.
 * The library can be opened using the {@link #open} method.
 * <p>
 * <p>
 * Each function in the library corresponds to an instance of
 * this class which is associated to a function to handle the function call.
 */
public final class PackageLib {
	private static final String DIRECTORY_SEPARATOR = "/";
	private static final char PATH_SEPARATOR = ';';
	private static final String PATH_MARK = "?";
	private static final String PATH_DEFAULT = "?.lua;?/init.lua";

	private static final Object SENTINEL = new Object();

	/**
	 * Opens the library into the given Lua state.  This registers
	 * the symbols of the library in the global table.
	 *
	 * @param L The Lua state into which to open.
	 */
	public static void open(Lua L) {
		LuaTable t = L.register("package");

		g(L, t, "module", PackageLib::module);
		g(L, t, "require", PackageLib::require);

		r(L, "seeall", PackageLib::seeall);

		LuaTable loaderTable = L.newTable();
		L.setField(t, "loaders", loaderTable);
		p(L, t, loaderTable, PackageLib::loaderPreload);
		p(L, t, loaderTable, PackageLib::loaderLua);

		// Set the 'path' field to the configured default
		setPath(L, t, "path", PATH_DEFAULT);

		// Set the 'loaded' field
		L.findTable(L.getRegistry(), Lua.LOADED, 1);
		L.setField(t, "loaded", L.value(-1));
		L.pop(1);
		L.setField(t, "preload", L.newTable());
	}

	/**
	 * Register a function.
	 */
	private static void r(Lua L, String name, LuaJavaCallback function) {
		L.setField(L.getGlobal("package"), name, function);
	}

	/**
	 * Register a function in the global table.
	 */
	private static void g(Lua L, LuaTable t, String name, ToIntBiFunction<Lua, LuaTable> function) {
		L.setGlobal(name, (LuaJavaCallback) l -> function.applyAsInt(l, t));
	}

	/**
	 * Register a loader in package.loaders.
	 */
	private static void p(Lua L, LuaTable t, LuaTable loaders, ToIntBiFunction<Lua, LuaTable> function) {
		L.rawSetI(loaders, loaders.firstNilIndex() + 1, (LuaJavaCallback) l -> function.applyAsInt(l, t));
	}

	/**
	 * Private constructor.
	 */
	private PackageLib() {
	}

	/**
	 * Implements module.
	 */
	private static int module(Lua L, LuaTable me) {
		String modName = L.checkString(1);
		Object loaded = L.getField(me, "loaded");
		Object module = L.getField(loaded, modName);
		// not found?
		if (!Lua.isTable(module)) {
			// try global variable (and create one if it does not exist)
			if (L.findTable(L.getGlobals(), modName, 1) != null)
				throw L.error("name conflict for module '" + modName + "'");
			module = L.value(-1);
			L.pop(1);
			// package.loaded = new table
			L.setField(loaded, modName, module);
		}
		// check whether table already has a _NAME field
		if (Lua.isNil(L.getField(module, "_NAME"))) {
			initModule(L, module, modName);
		}
		setfenv(L, module);
		dooptions(L, module, L.getTop());
		return 0;
	}

	/**
	 * Helper for module.  <var>module</var> parameter replaces PUC-Rio
	 * use of passing it on the stack.
	 */
	private static void initModule(Lua L, Object module, String modName) {
		L.setField(module, "_M", module);   // module._M = module
		L.setField(module, "_NAME", modName);
		int dot = modName.lastIndexOf('.'); // look for last dot in module name
		// set _PACKAGE as package name (full module name minus last part)
		L.setField(module, "_PACKAGE", dot < 0 ? "" : modName.substring(0, dot + 1));
	}

	/**
	 * Helper for module.  <var>module</var> parameter replaces PUC-Rio
	 * use of passing it on the stack.
	 */
	static void setfenv(Lua L, Object module) {
		Debug ar = L.getStack(1);
		L.getInfo("f", ar);
		L.setFenv(L.value(-1), module);
		L.pop(1);
	}

	/**
	 * Helper for module.  <var>module</var> parameter replaces PUC-Rio
	 * use of passing it on the stack.
	 */
	private static void dooptions(Lua L, Object module, int n) {
		for (int i = 2; i <= n; ++i) {
			L.pushValue(i);   // get option (a function)
			L.push(module);
			L.call(1, 0);
		}
	}

	/**
	 * Implements require.
	 */
	private static int require(Lua L, LuaTable me) {
		String name = L.checkString(1);
		L.setTop(1);
		// PUC-Rio's use of lua_getfield(L, LUA_REGISTRYINDEX, "_LOADED");
		// (package.loaded is kept in the registry in PUC-Rio) is translated
		// into this:
		Object loaded = L.getField(me, "loaded");
		Object module = L.getField(loaded, name);
		// is it there?
		if (Lua.toBoolean(module)) {
			if (module == SENTINEL)   // check loops
				throw L.error("loop or previous error loading module '" + name + "'");
			L.push(module);
			return 1;
		}
		// else must load it; iterate over available loaders.
		Object loaders = L.getField(me, "loaders");
		if (!Lua.isTable(loaders))
			throw L.error("'package.loaders' must be a table");
		L.pushString("");   // error message accumulator
		for (int i = 1; ; ++i) {
			Object loader = Lua.rawGetI(loaders, i);    // get a loader
			if (Lua.isNil(loader))
				throw L.error("module '" + name + "' not found:" + L.toString(L.value(-1)));
			L.push(loader);
			L.pushString(name);
			L.call(1, 1);     // call it
			if (Lua.isFunction(L.value(-1)))    // did it find module?
				break;  // module loaded successfully
			else if (Lua.isString(L.value(-1))) // loader returned error message?
				L.concat(2);    // accumulate it
			else
				L.pop(1);
		}
		L.setField(loaded, name, SENTINEL); // package.loaded[name] = sentinel
		L.pushString(name); // pass name as argument to module
		L.call(1, 1);       // run loaded module
		// non-nil return?
		if (!Lua.isNil(L.value(-1))) {
			// package.loaded[name] = returned value
			L.setField(loaded, name, L.value(-1));
		}
		module = L.getField(loaded, name);
		// module did not set a value?
		if (module == SENTINEL) {
			// use true as result
			module = Boolean.TRUE;
			L.setField(loaded, name, Boolean.TRUE); // package.loaded[name] = true
		}
		L.push(module);
		return 1;
	}

	/**
	 * Implements package.seeall.
	 */
	private static int seeall(Lua L) {
		L.checkType(1, Lua.TTABLE);
		LuaTable mt = L.getMetatable(L.value(1));
		if (mt == null) {
			mt = L.createTable(0, 1);
			L.setMetatable(L.value(1), mt);
		}
		L.setField(mt, "__index", L.getGlobals());
		return 0;
	}

	/**
	 * Implements the preload loader.  This is conventionally stored
	 * first in the package.loaders table.
	 */
	private static int loaderPreload(Lua L, LuaTable me) {
		String name = L.checkString(1);
		Object preload = L.getField(me, "preload");
		if (!Lua.isTable(preload))
			throw L.error("'package.preload' must be a table");
		Object loader = L.getField(preload, name);
		if (Lua.isNil(loader))        // not found?
			L.pushString("\n\tno field package.preload['" + name + "']");
		L.push(loader);
		return 1;
	}

	/**
	 * Implements the lua loader.  This is conventionally stored second in
	 * the package.loaders table.
	 */
	private static int loaderLua(Lua L, LuaTable me) {
		String name = L.checkString(1);
		String filename = findFile(L, me, name, "path");
		if (filename == null)
			return 1; // library not found in this path
		if (L.loadFile(filename) != 0)
			throw loadError(L, filename);
		return 1;   // library loaded successfully
	}

	private static String findFile(Lua L, LuaTable me, String name, String fieldName) {
		String path = L.toString(L.getField(me, fieldName));
		if (path == null)
			throw L.error("'package." + fieldName + "' must be a string");
		L.pushString("");   // error accumulator
		name = gsub(name, ".", DIRECTORY_SEPARATOR);
		while (true) {
			path = pushNextTemplate(L, path);
			if (path == null)
				break;
			String fileName = gsub(L.toString(L.value(-1)), PATH_MARK, name);
			if (readable(fileName))   // does file exist and is readable?
				return fileName;        // return that file name
			L.pop(1); // remove path template
			L.pushString("\n\tno file '" + fileName + "'");
			L.concat(2);
		}
		return null; // not found
	}

	/**
	 * Method for gsub'ing a string.
	 * Basically just a really fast find and replace.
	 */
	private static String gsub(String pattern, String part, String target) {
		StringBuilder result = new StringBuilder();
		int i = 0;
		int l = part.length();
		while (true) {
			int wild = pattern.indexOf(part, i);
			if (wild < 0) {
				if (i == 0) {
					// Return early if nothing was found.
					return pattern;
				}
				break;
			}
			int diff = wild - i;
			if (diff > 0) {
				if (diff == 1) {
					result.append(pattern.charAt(i));
				} else if (diff > 1) {
					result.append(pattern.substring(i, wild));
				}
			}
			result.append(target);    // add replacement in place of pattern
			i = wild + l;        // continue after 'p'
		}
		return result.append(pattern.substring(i)).toString();
	}

	private static String pushNextTemplate(Lua L, String path) {
		int i = 0;
		// skip seperators
		while (i < path.length() && path.charAt(i) == PATH_SEPARATOR)
			++i;
		if (i == path.length())
			return null;      // no more templates
		int l = path.indexOf(PATH_SEPARATOR, i);
		if (l >= 0) {
			L.pushString(path.substring(i, l)); // template
			return path.substring(l);
		}
		L.pushString(path.substring(i));
		return "";
	}

	private static boolean readable(String fileName) {
		URL resource = PackageLib.class.getResource(fileName);
		if (resource == null)
			return false;
		try {
			// TODO Determine if there's a better way...
			resource.openStream().close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private static void setPath(Lua L, LuaTable t, String fieldName, String def) {
		// :todo: consider implementing a user-specified path via
		// javax.microedition.midlet.MIDlet.getAppProperty or similar.
		// Currently we just use a default path defined by Jill.
		L.setField(t, fieldName, def);
	}

	private static LuaError loadError(Lua L, String filename) {
		throw L.error("Error loading module '" + L.toString(L.value(1)) + "' from file '" + filename + "':\n\t" + L.toString(L.value(-1)));
	}
}
