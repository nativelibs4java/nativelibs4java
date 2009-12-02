package com.nativelibs4java.opencl.blas;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import java.util.*;
import java.nio.*;
public class JavaCLBLAS {
	public static class Vector {
		DoubleBuffer buffer;
		int size;
		public Vector(int size) {
			this.size = size;
			buffer = NIOUtils.directDoubles(size);
		}
		public double get(int i) {
			return buffer.get(i);
		}
		public void set(int i, double value) {
			buffer.put(i, value);
		}
		public int size() {
			return size;
		}
	}
	public static class Matrix {
		public enum Qualifier {
			UpperTriangular, LowerTriangular, Symmetrical, Diagonal, Unit
		}
		EnumSet<Qualifier> qualifiers;
		
		public EnumSet<Qualifier> getQualifiers() {
			return EnumSet.copyOf(qualifiers);
		}
		public void setQualifiers(EnumSet<Qualifier> qualifiers) {
			this.qualifiers = qualifiers;
		}
		public void addQualifiers(Qualifier... qualifiers) {
			this.qualifiers.addAll(Arrays.asList(qualifiers));
		}
		public void clearQualifiers() {
			qualifiers.clear();
		}
		public void guessQualifiers() {
			boolean diagonal = true, upper = true, lower = true, symmetrical = true, unit = true;
			for (int i = 0; i < rows; i++) {
				for (int j = 0; j <= i; j++) {
					double v1 = get(i, j);
					if (i == j) {
						unit = unit && v1 == 1;
					} else {
						double v2 = get(j, i);
						diagonal = diagonal && v1 == 0 && v2 == 0;
						symmetrical = symmetrical && v1 == v2;
						upper = upper && v1 == 0;
						lower = lower && v2 == 0;
					}
				}
			}
			unit = unit && diagonal;
			clearQualifiers();
			if (diagonal)
				qualifiers.add(Qualifier.Diagonal);
			if (upper)
				qualifiers.add(Qualifier.UpperTriangular);
			if (lower)
				qualifiers.add(Qualifier.LowerTriangular);
			if (symmetrical)
				qualifiers.add(Qualifier.Symmetrical);
			if (unit)
				qualifiers.add(Qualifier.Unit);
		}
		public double get(int row, int column) {
			return data.get(getIndex(row, column));
		}
		protected int getIndex(int row, int column) {
			return rowsFirst ? 
				row * columns + column :
				column * rows + row;
		}
		public void set(int row, int column, double value) {
			data.put(getIndex(row, column), value);
		}
		public int getRows() {
			return rows;
		}
		
		public int getColumns() {
			return columns;
		}
		public Matrix(int rows, int columns) {
			this.rows = rows;
			this.columns = columns;
			data = NIOUtils.directDoubles(rows * columns);
		}
		int rows, columns;
		boolean rowsFirst = true;
		DoubleBuffer data;
	}
	interface ComputationEvent {
		void waitFor();
	}
	public static class DummyComputationEvent implements ComputationEvent {
		public void waitFor() {}
	}
	public static class CLComputationEvent implements ComputationEvent {
		CLEvent event;
		public void waitFor() {
			event.waitFor();
		}
	}
	public static ComputationEvent multiply(Matrix a, Matrix b, Matrix out) {
		for (int i = 0; i < a.getRows(); i++) {
			for (int j = 0; j < b.getColumns(); j++) {
				double sum = 0;
				for (int k = 0; k < a.getColumns(); k++) {
					sum += a.get(i, k) * b.get(k, j);
				}
				out.set(i, j, sum);
			}
		}
		return null;
	}
	
	public static ComputationEvent multiply(Matrix a, Vector b, Vector out) {
		for (int i = 0; i < a.getRows(); i++) {
			double sum = 0;
			for (int k = 0; k < a.getColumns(); k++) {
				sum += a.get(i, k) * b.get(k);
			}
			out.set(i, sum);
		}
		return null;
	}
}
