package me.jezza.luja;

/**
 * Equivalent of lua_Hook.  Callback for debug hooks.
 */
public interface Hook {
	int luaHook(Lua L, Debug ar);
}
