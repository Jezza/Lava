first, second, third = print("Hello?");

print(first);
print(second);
print(third);

--[==[
local test = function()
	print('hello!')
end
test()
test = "World!";

test1 = "Test";
test2 = "Heyo";
test1 = "ASd";
]==]


--[==[

print('asd');
local a = function (x)
	print(x);
end
a('1816')

]==]








-- local test = 'asdasd';
-- test = '234';
-- local asd = 'asd', 'asd';


-- ['asdasd']
-- test = 'ADDDD';
-- ['ADDDD']

-- test = lower(test)
-- ['adddd']



--[==[

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

]==]