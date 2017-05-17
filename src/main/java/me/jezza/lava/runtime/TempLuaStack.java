package me.jezza.lava.runtime;

import me.jezza.lava.Lua;
import me.jezza.lava.LuaFunction;
import me.jezza.lava.LuaJavaCallback;
import me.jezza.lava.LuaTable;
import me.jezza.lava.LuaUserdata;

/**
 * @author Jezza
 */
public interface TempLuaStack {
	void push(boolean value);

	void push(int value);

	void push(double value);

	void push(String value);

	void push(LuaUserdata data);

	void push(LuaTable data);

	void push(LuaFunction data);

	void push(LuaJavaCallback data);

	void push(Lua data);
}
