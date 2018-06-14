

-- for var in test do
-- 	break;
-- end





--[==[
for v = 1, 10, 1 do
	print("test")
end


]==]

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





--do
--	goto label
--end
--
--do
--	::label::
--end