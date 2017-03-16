/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin.editor;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;








public class View extends JTable {

	protected String name;
   	// a view is every object that makes
	// an individual scroll/table gooo
	private ColumnNames columnNames; 
	//private JTable table;
	private MyTableModel2 tm;
	private TableSorter sorter;
	private ProductTypesRows rows;
	private ProductTypesRows clipBoard;
    private Undo undo;
    
    private boolean tableDirty = false;
    private int currentRow = 0;
    
	private class ColumnNames extends Vector<String> {
		private static final long serialVersionUID = 1L;
	};
	
    private class MyTableModel2 extends DefaultTableModel {
    	 
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private boolean undoIt = true;
    	
        public int getColumnCount() {
            return columnNames.size();
        }

        public int getRowCount() {
             return rows.size();
        }

        public String getColumnName(int col) {
            return columnNames.get(col);
        }

        public Object getValueAt(int row, int col) {
        	ProductTypesRow r = rows.get(row);
      	//System.out.println("getValueAt: " + (String)r.get(col));
        	return r.get(col);
        }
        
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
        	//System.out.println("Current Row = " + row);
        	currentRow = row;
        	
 
                return true;
            
        }
        
        public void setValueAt(Object value, int row, int col) {
         
        	//System.out.println("Setting value at " + row + "," + col
            //                       + " to " + value
            //                       + " (an instance of "
             //                      + value.getClass() + ")");
          
        	ProductTypesRow r = rows.get(row);
        	
        	if (r != null) {
        		tableDirty = true;
        		r.set(col, (String)value);
        		
        		fireTableCellUpdated(row, col);
        	}
            
       
        }
        public void updateTable() {
        	
        	this.fireTableDataChanged();
        }
        
        public void newEmptyRow() {
 
        	ProductTypesRow row = new ProductTypesRow("","","","","","");
        	rows.add(currentRow, row);
        	updateTable();
        	
      		// put in the Undo list
        	undo.pushRow(row);
        	undo.pushRecord(Undo.Mode.insert, currentRow, 1);
        }
        
        public void insertEmptyRow() {
 
        	ProductTypesRow row = new ProductTypesRow("","","","","","");
        	rows.add(currentRow+1, row);
    		updateTable();
    		
    		// put in the Undo list
    		undo.pushRow(row);
    		undo.pushRecord(Undo.Mode.insert, currentRow+1, 1);

        }
        
        public void insertUndoRow(ProductTypesRow row) {
        	 
        	
        	rows.add(currentRow, row);
    		updateTable();
    		
  

        }
        
        public void deleteCurrentRow() {
        	// take one off
        	ProductTypesRow dRow = rows.remove(currentRow);
        	
        	// put it on the undo
        	if (this.undoIt) {
        		undo.pushRow(dRow);
        		undo.pushRecord(Undo.Mode.delete, currentRow, 1);
        	}
    		
    		// now back to making the delete work
        	currentRow = currentRow - 1;
        	if (currentRow < 0) currentRow = 0;
        	updateTable();
        	tableDirty = true;
        	// special case
        	if (rows.size() == 0) {

        		//ProductTypesRow row = new ProductTypesRow("","","","","","");
        		//rows.add(currentRow, row);
        		//updateTable();
        		this.newEmptyRow();
        		tableDirty = false;
        	}
        }
        public void copyRows() {
			int[] selectedRows = getSelectedRows();
			int rowCount = getSelectedRowCount();
			
			if (rowCount > 0) {
				//	blow away old clipBoard

				clipBoard = new ProductTypesRows();
			
				for (int i = 0; i < rowCount; i++) {
					//System.out.println("Copy from row = " + selectedRows[i]);
					
					clipBoard.add(i, rows.get(selectedRows[i]));
				}
			}
        }
        public void pasteRows() {
			int[] selectedRows = getSelectedRows();
			int rowCount = getSelectedRowCount();
			
			int copyCount = rowCount;
			if (rowCount > clipBoard.size()) 
				copyCount = clipBoard.size();
			else if (rowCount < clipBoard.size()) 
				copyCount = clipBoard.size();
			
				
			
			if (clipBoard != null) {
				int baseRowIndex = selectedRows[0];
				for (int i = 0; i < copyCount; i++) {
					//System.out.println("Copy to row = " + baseRowIndex);
					//if (baseRowIndex >= rows.size()) {
					//	rows.add(baseRowIndex, clipBoard.get(i));
					//} else
					rows.set(baseRowIndex, clipBoard.get(i));
					baseRowIndex = baseRowIndex + 1;
				}
				tm.updateTable();
			}       	
        }
    
        public void cutRows() {
			//System.out.println("Delete");
			int[] selectedRows = getSelectedRows();
			int rowCount = getSelectedRowCount();
			
			if (rowCount <= 0) {
					System.out.println("CurrentRow = " + currentRow + "rowCount = " + rowCount );
					return;
			}
			// set the currentRow to the END of the block
			// then simply call "deleteCurrenRow()" for the number
			// of rows... (currentRow may be TOP of the block, so
			// it must be adjusted)
			currentRow = selectedRows[rowCount-1];
			for (int i = 0; i < rowCount; i++) {
				tm.deleteCurrentRow();
			}
			
			//System.out.println("New CurrentRow = " + currentRow);
        }
    
        public void undoInsertRows(int rowStart, int rowCount) {
	
			// slight varient on above and MOST IMPORTANT
        	// it does NOT manipulate the UNDO
		
			// set the currentRow to the END of the block
			// then simply call "deleteCurrentRow()" for the number
			// of rows... (currentRow may be TOP of the block, so
			// it must be adjusted)
        	currentRow = rowStart;
			
			for (int i = 0; i < rowCount; i++) {
				tm.deleteCurrentRow();
			}
			
        }
        
        public void undoDeleteRows(int rowStart, int rowCount) {
        	
			// slight varient on above and MOST IMPORTANT
        	// it does NOT manipulate the UNDO
		
			// set the currentRow to the END of the block
			// then simply call "deleteCurrentRow()" for the number
			// of rows... (currentRow may be TOP of the block, so
			// it must be adjusted)
        	currentRow = rowStart;
			for (int i = 0; i < rowCount; i++) {
				ProductTypesRow oldRow = undo.popRow();
				tm.insertUndoRow(oldRow);
			}
        }
        
        // attempt to undo the last 
        // recorded action in the Undo
        // list -- invert the action found
        public void undoLastAction() {
        	Undo.Record rec = undo.popRecord();
        	if (rec != null) {
        		int rowCount = rec.getCount();
        		int rowIndex = rec.getRowIndex();
        		Undo.Mode mode = rec.getMode();
        		// hmm here's the big guess
        		this.undoIt = false; //ugly
        		if (mode.equals(Undo.Mode.insert)) {
        			tm.undoInsertRows(rowIndex, rowCount);
        		} else {
        			tm.undoDeleteRows(rowIndex, rowCount);
        		}
        		this.undoIt = true;
        	}
        }
    }

    public View() {
    	 
    	super();

    	init();
 	
    }
    
    public View(String name) {
 
    	super();
    	
    	init();
    	
        this.name = name;
    	
    }

    private void init() {
    	this.initTableData();
    	
    	this.tm = new MyTableModel2();
    
    	this.undo = new Undo();

    	this.sorter = new TableSorter(tm); 	
    	//this.table = new JTable(sorter); 
    	this.setModel(sorter);
    	this.clipBoard = null;

        sorter.setTableHeader(this.getTableHeader()); 

        ListSelectionModel lsm  = this.getSelectionModel();
        lsm.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        //lsm.addListSelectionListener(new ListSelectionListenerTest());
        
    
        
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK,false);
        KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK,false);
        KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0,false);
        KeyStroke backspace = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE,0,false);
        KeyStroke undoKeys = KeyStroke.getKeyStroke(KeyEvent.VK_Z,ActionEvent.CTRL_MASK,false);
        
        this.registerKeyboardAction(new CopyStroke(),"Copy",copy,JComponent.WHEN_FOCUSED);
        this.registerKeyboardAction(new PasteStroke(),"Paste",paste,JComponent.WHEN_FOCUSED);
        
        DeleteStroke deleteStroke = new DeleteStroke();
        //table.registerKeyboardAction(deleteStroke,"Backspace",delete,JComponent.WHEN_FOCUSED);
        this.registerKeyboardAction(deleteStroke,"Delete",backspace,JComponent.WHEN_FOCUSED);

        this.registerKeyboardAction(new UndoStroke(),"Undo",undoKeys,JComponent.WHEN_FOCUSED);
        
        //table.setPreferredScrollableViewportSize(new Dimension(700, 70));


        this.setPreferredScrollableViewportSize(new Dimension(900, 400));
        
    	
    }
    private class CopyStroke implements ActionListener
    {

		public void actionPerformed(ActionEvent arg0) {

			tm.copyRows();
		}
    	
    }
    
    private class PasteStroke implements ActionListener
    {

		public void actionPerformed(ActionEvent arg0) {

			tm.pasteRows();
		}
    	
    }
    
    private class DeleteStroke implements ActionListener
    {

		public void actionPerformed(ActionEvent arg0) {

			tm.cutRows();
		}
    }
    
    private class UndoStroke implements ActionListener
    {

		public void actionPerformed(ActionEvent arg0) {

			tm.undoLastAction();
		}
    	
    }

    public void initTableData() {
    		this.rows = new ProductTypesRows();
        	
        	columnNames = new ColumnNames();

        	columnNames.add("Product Name");
        	columnNames.add("Spacecraft");
        	columnNames.add("Sensor");
        	columnNames.add("Description");
        	columnNames.add("Level");
        	columnNames.add("Directory");
        	
        	ProductTypesRow row = new ProductTypesRow("","","","","","");
        	this.rows.add(row);
 
    }
    	
	public int getMaxWidthOfData(int index) {
		int maxWidth = 0;
		for (ProductTypesRow r : this.rows) {
			String s = r.get(index);
			if (s.length() > maxWidth)
				maxWidth = s.length();
		}
		return maxWidth;
	}

	public boolean isTableDirty() {
		return tableDirty;
	}
	
	public void setRows(ProductTypesRows rows) {
		this.rows = rows;
	}
	
	public ProductTypesRows getRows() {
		return(this.rows);
	}
	
	public void updateTable() {
		tm.updateTable();
	}
	
	public boolean isRowsEmpty() {
		if (this.rows == null) return true;
		return false;
	}
	
	public int numRows() {
		return(this.rows.size());
	}
	
	public void setCurrentRowIndex(int rowIndex) {
		this.currentRow = rowIndex;
	}
	
	public void deleteCurrentRow() {
	
		tm.deleteCurrentRow();
	}
	
	public void newEmptyRow() {
		
		tm.newEmptyRow();
	}
	
	public void insertEmptyRow() {
		
		tm.insertEmptyRow();
	}
	
	public void copyRows() {
		
		tm.copyRows();
	}
	
	public void pasteRows() {
		
		tm.pasteRows();
	}
	
	public void cutRows() {
		
		tm.cutRows();
	}
}
