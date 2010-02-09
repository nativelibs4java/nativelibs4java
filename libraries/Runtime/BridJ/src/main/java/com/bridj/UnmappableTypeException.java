/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

/**
 *
 * @author Olivier Chafik
 */
public class UnmappableTypeException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -5748442361297147821L;

	public UnmappableTypeException(String message) {
        super(message);
    }
}
