
--[==[

local x = 0;
::start::

do
  x = x + 1;
  print(x);
  if x > 5 then
    goto exit;
  end
end

goto start;

::exit::
print("Done", x);
]==]

--[==[
local x = 0;

::start::
repeat
	print(x);
	x = x + 1;
	if x > 0 then
		break;
	end
until false;
print("Done", x);
]==]

for i = 1, 2, 1 do
	for j = 1, 2, 1 do
    	print("test", i, j)
    end
end
