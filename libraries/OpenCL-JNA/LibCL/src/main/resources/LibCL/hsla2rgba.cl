#ifndef _LIBCL_HSLA_2_RGBA_CL_
#define _LIBCL_HSLA_2_RGBA_CL_

inline float hsla2rgba_sub(float x, float y, float z) {
	if (z < 0)
		z += 1.f;
	else if (z > 1)
		z -= 1.f;

	if (6 * z < 1)
		return x + (y - x) * 6 * z;
	if (2 * z < 1)
		return y;
	if (3 * z < 2)
		return x + (y - x) * ((2.f / 3.f) - z) * 6;
	return x;
}

inline float4 hsla2rgba(float4 hsla) {
	float 
		h = hsla.x, 
		s = hsla.y, 
		l = hsla.z, 
		a = hsla.w;

	float r, g, b;
	if (s == 0)
		r = g = b = l;
	else {
		float y;
		if (l < 0.5f)
			y = l * (1.0f + s);
		else
			y = l + s - l * s;
		float x = 2.0f * l - y;

		r = hsla2rgba_sub(x, y, h + 1.f / 3.f);
		g = hsla2rgba_sub(x, y, h);
		b = hsla2rgba_sub(x, y, h - 1.f / 3.f);
	}
	return (float4)(r, g, b, a);
}

#endif // _LIBCL_HSLA_2_RGBA_CL_
