local a = 1;
local b = 2;
local t = {};

t[a], a, t[a] = b, 3, 4

print(t[1])
print(t[2])
print(t[3])
print(t[4])

--[==[
local a = 1;
local b = 2;
a, b = b, a;
print(a);
print(b);

local a = {}
local b = {}
a.a = "a.a"
a.b = "a.b"
b.a = "b.a"
b.b = "b.b"

local a1 = a;
a, a.a = b, {c = "_.c"}

print(a1)
print(b)
]==]


-- do
	-- local t_1_t = t;
	-- local t_1_k = "test";
	-- t_1_t[t_1_k] = "asd";
-- end
-- t.test = "asd"

-- local t = {};

-- local function val(value)
	-- print(value);
	-- print(t);
	-- return value;
-- end

-- t[val("test")], t[val("other")] = val("value1"), val("value2");
-- t[val("test")] = val("value1");
-- t[val("test")] = "value";

-- val("asd");
-- print(t);

--[==[
local function def(data)
	print(data)
end

local function class(name)
	return {name}
end

local OPALModule = class "de.bps.onyx.repo.modules.opal.OPALModule";

def {
	module = OPALModule,
	url = "hello!",
	data = {
		root = "test"
	}
}
]==]

-- t.test, t.other = "value1", "value2", "asd"

-- t["test"], t["other"] = "value1", "value2"
--[==[ do
	-- local t_1_t = t;
	-- local t_1_k = "test";

	-- local t_2_t = t;
    -- local t_2_k = "other";

	-- t_1_t["test"] = "value1";
	-- t_2_t["other"] = "value2";
end
]==]

-- print(t)

--[==[
======================
local a = "value";
local b = "dasd";

a, b = b, a;

print(a);
print(b);

======================

function v(val)
  print(val)
  return val;
end


local t = {}
t["1"] = {}
t["2"] = {}
t["3"] = 3

t[v("1")][v("2")], t[v("1")][v("2")] = "54", t[v("3")]

======================

for v = 1, 10, 1 do
	print("test")
end



for v = e1, e2, e3 do
	print("{block}")
end

do
	local var, limit, step = tonumber(e1), tonumber(e2), tonumber(e3)
	if not (var and limit and step) then
		error("for loop parameters must evaluate to numbers")
	end
	var = var - step
	while true do
		var = var + step
		if (step >= 0 and var > limit) or (step < 0 and var < limit) then
			break
		end
		local v = var
		print("{block}")
	end
end



]==]


--do
--	goto label
--end
--
--do
--	::label::
--end