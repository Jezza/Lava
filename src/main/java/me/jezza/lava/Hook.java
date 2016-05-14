package me.jezza.lava;

/**
 * Equivalent of lua_Hook.  Callback for debug hooks.
 */
@FunctionalInterface
interface Hook {
	int luaHook(Lua L, Debug ar);
}
