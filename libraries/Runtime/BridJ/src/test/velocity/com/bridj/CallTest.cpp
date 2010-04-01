
#foreach ($prim in $primitivesNoBool)

TEST_API j${prim.Name} test_incr_${prim.Name}(j${prim.Name} value) {
	return (j${prim.Name})(value + 1);
}

TEST_API j${prim.Name} test_callback_${prim.Name}_${prim.Name}(j${prim.Name} (*cb)(j${prim.Name}), j${prim.Name} value) {
	j${prim.Name} ret = cb(value);
	return ret;
}

#end


