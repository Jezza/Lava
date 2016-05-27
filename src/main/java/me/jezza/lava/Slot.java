/**
 * Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
 * All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject
 * to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package me.jezza.lava;

/**
 * TODO: Think about abstracting the Slot one more level out, and make it use the same instance for NIL, and BOOLEAN.
 * As there are only two possible BOOLEAN values, true and false, obviously, and there's only 1 NIL.
 * No other values can really be a constant like that. Unless you include doubles, but I think it would only be worth it with whole numbers, and even then, I can't really think of a nice middle ground.
 * So, I think doubles might not be worth it. Userdata, and Tables definitely aren't.
 * Strings, quite possibly.
 * As you have an internalised value for each string that you create.
 * I think that might be amazing for consistency with the PUC-Rio, as all strings are the same instance.
 * So making a pool of all strings as slots would be for the best.
 * I think the biggest hurdle for implementing this system is definitely going to be the access that is given to the rest of Lava.
 * As the fields are accessed directly, this means I'm going to have to:
 * A) Remove all of those accesses, and see if a getter method for each would do it justice, and
 * B) Add some factory method that produces the Slots and make sure whenever a slot is written to, or created, it goes through that, so we can swap out the values with constant slots.
 * <p>
 * So, values that I think can be made constants:
 * NIL
 * BOOLEAN
 * STRING - As I said, we could use a map, and internalise the strings, as well as the slots. Win-win.
 *
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
