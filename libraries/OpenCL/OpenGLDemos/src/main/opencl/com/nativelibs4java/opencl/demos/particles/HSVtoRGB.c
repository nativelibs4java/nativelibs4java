
// Copied from http://www.cs.rit.edu/~ncs/color/t_convert.html

uchar4 HSVAtoRGBA(float4 hsva)
{
    float h = hsva.x, s = hsva.y, v = hsva.z, a = hsva.w;
    float r, g, b;

	int i;
	float f, p, q, t;
	if (s == 0) {
		// achromatic (grey)
		r = g = b = v;
		return (uchar4)(r * 255, g * 255, b * 255, a * 255);
	}
	h /= 60;			// sector 0 to 5
	i = floor( h );
	f = h - i;			// factorial part of h
	p = v * ( 1 - s );
	q = v * ( 1 - s * f );
	t = v * ( 1 - s * ( 1 - f ) );
	switch( i ) {
		case 0:
			r = v;
			g = t;
			b = p;
			break;
		case 1:
			r = q;
			g = v;
			b = p;
			break;
		case 2:
			r = p;
			g = v;
			b = t;
			break;
		case 3:
			r = p;
			g = q;
			b = v;
			break;
		case 4:
			r = t;
			g = p;
			b = v;
			break;
		default:		// case 5:
			r = v;
			g = p;
			b = q;
			break;
	}
    return (uchar4)(r * 255, g * 255, b * 255, a * 255);
}