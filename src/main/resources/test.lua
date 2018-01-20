if type(i) ~= 'number' then return i,'jojo'; end;

assert(string.find("1234567890123456789", "345", 3) == 3)

local num = print(1e-4);

local x = {test = unpack(asd)}
local x = {unpack(a)}

if (test) then
	print(test.a or "asd")
end

first.second[test .. asd]:third("asd");


test = {
 a = "asd"
}

test.method1 = function(self, arg)
 print(self.a)
 print(arg)
end

function test:method2(arg)
 print(self.a)
 print(arg)
end

test:method1("dd")
test:method2("aa")

first.second:method2("aa")
first.second.third:method2("aa")
first.second.third.fourth:method2("aa")

local mal1 = first.second.third.fourth;
mal1.method2(mal1, "aa");

-- local mal = ($1);
-- mal.$2(mal, $*)

first, second, third = print("Hello?");

print(first);
print(second);
print(third);

function test()
	print(first);
end

test = function()
	print("")
end

local test = function()
	print('hello!')
	local test = "";
	local first, second = test(), "hello!";
end
test()
test = "World!";

test1 = "Test";
test2 = "Heyo";
test1 = "ASd";

print('asd');
local a = function (x)
	print(x);
end
a('1816')


local test = 'asdasd';
test = '234';
local asd = 'asd', 'asd';

local b = 7
local a = function (x)
  local res,ires = {},0 ;
  while x<b do
    local s = x*b ;
    function blah () s = s+1 ; return s end
    res[ires] = blah ; ires = ires+1;
    x = x+1 ;
  end;
  return res;
end;
return a(4)