package me.jezza.lava;

/**
 * @author Jezza
 */
final class Slot {
	Object r;
	double d;
	boolean b;
	int t;

	Slot() {
	}

	Slot(boolean b) {
		setObject(b);
	}

	Slot(double d) {
		setObject(d);
	}

	Slot(Object o) {
		setObject(o);
	}

	Slot(Slot s) {
		setObject(s);
	}

	void setObject(boolean b) {
		r = Lua.BYPASS_TYPE;
		t = Lua.TBOOLEAN;
		this.b = b;
		d = 0;
	}

	void setObject(double d) {
		r = Lua.BYPASS_TYPE;
		t = Lua.TNUMBER;
		b = false;
		this.d = d;
	}

	void setObject(String o) {
		r = o;
		t = Lua.TSTRING;
		b = false;
		d = 0;
	}

	void setObject(LuaTable o) {
		r = o;
		t = Lua.TTABLE;
		b = false;
		d = 0;
	}

	void setObject(LuaFunction function) {
		r = function;
		t = Lua.TFUNCTION;
		b = false;
		d = 0;
	}

	void setObject(LuaJavaCallback function) {
		r = function;
		t = Lua.TFUNCTION;
		b = false;
		d = 0;
	}

	void setObject(LuaUserdata data) {
		r = data;
		t = Lua.TUSERDATA;
		b = false;
		d = 0;
	}

	void setObject(Lua l) {
		r = l;
		t = Lua.TTHREAD;
		b = false;
		d = 0;
	}

	void setObject(Object o) {
		if (o == Lua.NIL) {
			r = o;
			t = Lua.TNIL;
			b = false;
			d = 0D;
			return;
		}
		switch (t = Lua.type(o)) {
			case Lua.TNUMBER:
				r = Lua.BYPASS_TYPE;
				d = (Double) o;
				b = false;
				break;
			case Lua.TBOOLEAN:
				r = Lua.BYPASS_TYPE;
				b = (Boolean) o;
				d = 0;
				break;
			case Lua.TNONE:
			default:
				r = o;
				b = false;
				d = 0;
		}
	}

	void setObject(Slot s) {
		this.r = s.r;
		this.d = s.d;
		this.b = s.b;
		this.t = s.t;
	}

	boolean isFalse() {
		// Check if this represents a boolean, and if the boolean is false, or if it's NIL
		return r == Lua.BYPASS_TYPE ? t == Lua.TBOOLEAN && !b : r == Lua.NIL;
	}

	Object asObject() {
		if (r == Lua.BYPASS_TYPE) {
			switch (t) {
				case Lua.TNUMBER:
					return d;
				case Lua.TBOOLEAN:
					return b;
				default:
					throw new UnsupportedOperationException("Slot Type not supported: " + Lua.typeName(t));
			}
		}
		return r;
	}

	@Override
	public String toString() {
		if (r == Lua.BYPASS_TYPE) {
			switch (t) {
				case Lua.TNUMBER:
					return Double.toString(d);
				case Lua.TBOOLEAN:
					return b ? "true" : "false";
			}
		}
		return r != null ? r.toString() : "null";
	}
}
