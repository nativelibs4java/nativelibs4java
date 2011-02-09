void dft(
	const double *in, // complex values input (packed real and imaginary)
	double *out,      // complex values output
	int length,       // number of input and output values
	int sign)         // sign modifier in the exponential :
					 // 1 for forward transform, -1 for backward.
{
	for (int i = 0; i < length; i++)
	{
		// Initialize sum and inner arguments
		double totReal = 0, totImag = 0;
		double param = (-2 * sign * i) * M_PI / (double)length;
		
		for (int k = 0; k < length; k++) {
			double valueReal = in[k * 2], valueImag = in[k * 2 + 1];
			double arg = k * param;
			double c = cos(arg), sin(arg);
			
			totReal += valueReal * c - valueImag * s;
			totImag += valueReal * s + valueImag * c;
		}
		
		if (sign == 1) {
			// forward transform (space -> frequential)
			out[i * 2] = totReal;
			out[i * 2 + 1] = totImag;
		} else {
			// backward transform (frequential -> space)
			out[i * 2] = totReal / (double)length;
			out[i * 2 + 1] = totImag / (double)length;
		}
	}
}
