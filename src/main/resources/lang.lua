-- a = {}
-- a.b = ""
-- a.b.c = {}
-- a.b.c["d"] = {}
--
-- a.c.b = ""
-- // set_table (a.c) b ""
--
-- c = a.c.b
-- // get (a.c.b)
--

-- local a0,b0,c0,d0;

-- local function test()
-- end

-- a, c = test()

-- local a = "a";
-- local b = "b";
-- b, a = a, b
-- local test, asdsd = "asdasddsasd", "asasdasd"


local function test(a, b)
	return b, "third";
end

local a = test("first", "second");

test(a, "fourth")

local a = test;

a = a("hello", a)

-- table = "table"

-- local first = "a", "b", test()


-- local a = 0;

-- a = 0;

-- a = c + 2

-- function test(a, b, c, d)
-- end


-- table.key = "value"

--a, d = "vv", function() end
--
--function test()
--end
--
--function d()
--	print("")
--end
--
--function test(as)
--	t = "t"
--	d = "d"
--	e = "e"
--
--	return "aa", "bb"
--end
--
--test("first", "rubbish", "rubbish", "rubbish", "rubbish", "rubbish")
--
--d = "my value"

-- a e a a

-- a, b = b, a

-- a, b = b[asd], a

--
-- print(b)