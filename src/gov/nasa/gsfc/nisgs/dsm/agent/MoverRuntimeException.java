/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;


/**
 * Local customization of the RuntimeException 
 */
public class MoverRuntimeException extends RuntimeException {

	/**
	 * Default, starting point but currently unused...
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Wrap any Exception into a MoverRuntimeException
	 * @param e the Exception
	 */
	public MoverRuntimeException(Exception e) {
		super(e);
	}

	/**
	 * Make a new MoverRuntimeException with the String msg
	 * @param msg the message that goes into the exception
	 */
	public MoverRuntimeException(String msg) {
		super(msg);
	}


}
