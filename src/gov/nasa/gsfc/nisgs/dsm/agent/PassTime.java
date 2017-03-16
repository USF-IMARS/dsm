/*
Copyright (c) 1999-2012, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;

import java.util.Date;
import java.util.List;


public class PassTime {
    private RDRrec rdrRec;
	private Date startTime;
	private Date stopTime;
	
	
    public PassTime(RDRrec rdrRec, Date startTime, Date stopTime) {
	this.rdrRec = rdrRec;
		this.startTime = startTime;
		this.stopTime = stopTime;
		
	}
    public RDRrec getRDRrec() {
	return rdrRec;
    }
	public Date getStartTime() {
		return startTime;
	}
	public Date getStopTime() {
		return stopTime;
	}
        public void setStopTime(Date newStopTime) {
                stopTime = newStopTime;
        }

}
