/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;

/**
 * Funnel any exception through this class
 * @author krice
 *
 */
public class MoverException extends Exception {

	/**
	 * make warning go away
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Constructor, provide String message
	 * @param msg a String
	 */
	public MoverException(String msg) {
		super(msg);
	}
	/**
	 * Constructor, wrap another exception and add the local msg
	 * @param e the other exception
	 */
	public MoverException(String msg, Exception e) {
		super("CGA Exception: << " + msg + ">> Wrapped Exception: <<" + e + ">>" + "Message: " + e.getMessage(), e);
	}
	/**
	 * Constructor, wrap another exception
	 * @param e the other exception
	 */
	public MoverException(Exception e) {
		super("CGAException Wrapped Exception: <<" + e + ">>" + "Message: " + e.getMessage(), e);
	}

}
