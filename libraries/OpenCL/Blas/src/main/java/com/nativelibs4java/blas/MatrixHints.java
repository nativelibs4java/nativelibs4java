/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

import com.nativelibs4java.blas.java.DefaultMatrix;
import java.util.Arrays;
import java.util.EnumSet;

/**
 *
 * @author ochafik
 */
public class MatrixHints {
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

    public void guessQualifiers(DefaultMatrix matrix) {
        boolean diagonal = true;
        boolean upper = true;
        boolean lower = true;
        boolean symmetrical = true;
        boolean unit = true;
        for (int i = 0, rows = matrix.getRows(); i < rows; i++) {
            for (int j = 0; j <= i; j++) {
                double v1 = matrix.get(i, j);
                if (i == j) {
                    unit = unit && v1 == 1;
                } else {
                    double v2 = matrix.get(j, i);
                    diagonal = diagonal && v1 == 0 && v2 == 0;
                    symmetrical = symmetrical && v1 == v2;
                    upper = upper && v1 == 0;
                    lower = lower && v2 == 0;
                }
            }
        }
        unit = unit && diagonal;
        clearQualifiers();
        if (diagonal) {
            qualifiers.add(Qualifier.Diagonal);
        }
        if (upper) {
            qualifiers.add(Qualifier.UpperTriangular);
        }
        if (lower) {
            qualifiers.add(Qualifier.LowerTriangular);
        }
        if (symmetrical) {
            qualifiers.add(Qualifier.Symmetrical);
        }
        if (unit) {
            qualifiers.add(Qualifier.Unit);
        }
    }

}
