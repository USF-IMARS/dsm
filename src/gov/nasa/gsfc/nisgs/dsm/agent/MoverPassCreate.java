/*
Copyright (c) 1999-2012, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;

/**
 * An interface all the Movers must implement for their specific PassTypes.  Examples include
 * @link PdsPassCreate and {@link RDRPassCreate}.
 * @author krice
 *
 */
public interface MoverPassCreate {
	/**
	 * Create passes for the DSM that are at least as large as the given size in seconds
	 * @param sliverSeconds the passes created must contain data no smaller than this size in seconds
	 * @throws MoverException throw for significant but not fatal pass creation issues
	 */
	public void createPassesForDSM(int sliverSeconds) throws MoverException;
	
	/**
	 * Send any created passes to the DSM for processing.
	 * @throws MoverException throw for significant but not fatal pass sending issues
	 */
	public void sendPassesToDSM() throws MoverException;
	
	/**
	 * Log the passes created and sent in some manner
	 */
	public void logPasses();
}
