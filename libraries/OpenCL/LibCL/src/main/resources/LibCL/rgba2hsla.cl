#ifndef _LIBCL_RGBA_2_HSLA_CL_
#define _LIBCL_RGBA_2_HSLA_CL_

inline float4 rgba2hsla(float4 rgba) {
	float 
		r = rgba.x, 
		g = rgba.y, 
		b = rgba.z, 
		a = rgba.w;

	float mn = min(r, min(g, b));
	float mx = max(r, max(g, b));

	float l = (mn + mx) / 2.f;
	float s, h;
	if (mn == mx) {
		s = h = 0;
	} else {
		float diff = mx - mn;
		float sum = mx + mn;
		if (l < 0.5f)
			s = diff / sum;
		else
			s = diff / (2.0f - sum);
		if (r == mx)
			h = (g - b) / diff;
		else if (g == mx)
			h = 2.0f + (b - r) / diff;
		else //if (b == mx)
			h = 4.0f + (r - g) / diff;
	}
	h = clamp(h / 6.0f, 0, 1);
	return (float4)(h, s, l, a);
}

#endif // _LIBCL_HSLA_2_RGBA_CL_
