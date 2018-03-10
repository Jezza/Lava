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

-- local a = "a";
-- local function test()
-- 	print(a);
-- end
-- 
-- test();

--local x = 5;
--local call = function()
--	local y = 0;
--	return function()
--		y = y + 1
--		return x + y;
--	end
--end
--
--local adder = call();
--
--print(adder());
--print(adder());
--print(adder());

local y = 0

while y < 5 do
	print(y)
	y = y + 1
end

-- local tab = {"from tab!", asd = "Hlo", "asdds"}

-- local b = "b"
-- local t = {"a", [1] = b}
-- print(t)

-- { [1] = a,[2] = c,[3] = b,["name"] = b,["asd"] = a,} 

--local x = 5;
--local call_0;
--do
--	local y = 0;
--	call_0 = function()
--		y = y + 1;
--		return x + y;
--	end
--end
--local call_1;
--do
--	local y = 0;
--	call_1 = function()
--		y = y + 1;
--        return x + y;
--	end
--end
--
--print(call_0());
--print(call_1());

-- local function change(parameter)
-- 	local function change()
-- 		a = parameter
-- 	end
-- 	change()
-- end
-- 
-- change("b");
-- 
-- print(a);

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