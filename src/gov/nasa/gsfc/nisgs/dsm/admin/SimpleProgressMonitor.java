/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
/*******************************************************************************
 *
 * Little wrapper class around standard ProgressMonitor to allow simple
 * bumping of progress
 ******************************************************************************/

package gov.nasa.gsfc.nisgs.dsm.admin;
import gov.nasa.gsfc.nisgs.dsm.admin.ProgressMonitor;
import java.awt.Component;
import java.awt.Dimension;

public class SimpleProgressMonitor extends ProgressMonitor {
    int currentState;

    /**
     * It started out as a "simple" progress monitor, then a few extras were
     * added: bumpState() which increments the progress counter,
     * and setProgressLine() which sets the note line and bumps the progress
     * counter if the line Matches the progressLineHead string.
     * This is based on our extended ProgressMonitor which allows an extra
     * boolean flag to enable/disable the Cancel button (it is replaced with
     * an "OK" button) and a setPreferredSize(Dimension) method.
     */
    public SimpleProgressMonitor (Component pc, Object msg, String note, int max)
	throws Exception
    {
	super(pc, msg, note, 0, max, false);
	// Pop this up at once
	setMillisToDecideToPopup(1);
	setMillisToPopup(1);
	setPreferredSize(new Dimension(400,167));
	currentState=0;
	setProgress(currentState);
    }

    public void setMaximum(int max) {
	super.setMaximum(max);
    }

    public int getCurrentState() { return currentState; }

    public void bumpState(int newval) {
	setProgress(newval);
    }

}
