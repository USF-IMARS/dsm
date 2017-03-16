/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin.editor;

import java.util.Stack;

// a very simple and perhaps braindead way
// to hold insert/delete rows ... if a row
// is inserted, it also goes here.  If it is
// deleted, it goes here too.  Then to back out,
// the mode they were put in is retrieved, and the count
// its left up to the user to "do the right thing" 
// and keep this stuff sync'd...
// Experiement #1
public class Undo {
	private Stack<ProductTypesRow> rows;
	private Stack<Record> records;
	
	public enum Mode { insert, delete };
	
	public class Record {
		private int count;
		private int rowIndex; 
		private Mode mode;
		public Record(Mode mode, int rowIndex, int count) {
			this.mode = mode;
			this.rowIndex = rowIndex;
			this.count = count;
		}
		Mode getMode() { return this.mode; }
		int getCount() { return this.count; }
		int getRowIndex() { return this.rowIndex; }
	}
	
	public Undo() {
		rows = new Stack<ProductTypesRow>();
		records = new Stack<Record>();
	}
	
	// rows are pushed on one at time
	void pushRow(ProductTypesRow row) {
		rows.push(row);
	}
	// after they are pushed, the mode is pushed
	// on -- there's a one to one correspondance
	// but not explicit link. that's left up to
	// the user to keep straight, at least for now
	void pushRecord(Mode mode, int rowIndex, int count) {
		records.push(new Record(mode, rowIndex, count));
	}
	
	// retrieving is the opposite, get the top
	// 'record' row -- determine the mode, and the
	// number of rows to pop off, see below
	Record popRecord() {
		try {
			return records.pop();
		} catch (Exception e) {
			return null;
		}
	}

	// pop rows one at a time, again its left
	// up to the user to keep this mess in sync
	// .. cause this is the easy way to try this out
	ProductTypesRow popRow() {
		try {
			return rows.pop();
		} catch (Exception e) {
			return null;
		}
	}
}
