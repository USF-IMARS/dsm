/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
/*
 * TableDemo.java is a 1.4 application that requires no other files.
 */
package gov.nasa.gsfc.nisgs.dsm.admin.editor;

import java.text.NumberFormat;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


import org.xml.sax.*;
import org.xml.sax.helpers.*;



import java.io.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import javax.swing.BoxLayout;
import javax.swing.text.*;
import javax.swing.ImageIcon;


import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.sql.*;


/** 
 * TableDemo is just like SimpleTableDemo, except that it
 * uses a custom TableModel.
 */
public class TableDemo extends JPanel {
        
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// basically some typeDefs
    private class ColumnNames extends Vector<String>{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	};

    //private JTable table;
    //private MyTableModel2 tm;
    //private TableSorter sorter;
    
    //private boolean tableDirty = false;
    //private int currentRow = 0;
    
    // table data stored here
    //private ColumnNames columnNames=null; 
    //private ProductTypesRows rows = null;
    //private ProductTypesRows clipBoard = null;
    //private Undo undo = null;
    
    // new proposed answer for multiple view
    View currentView = null;
    
    //private MakeInternalFrame iftest = null;
    private MakeFloatingFrame fftest = null;
     
    // some swing stuff
    private JFileChooser fc = null; 
    private String hostName = null;
    private String portNum = null;
    private String dbName = null;
    private String userName = null;
    private String password = null;
    private Connection connection = null;
    
    
    private class View {
    	// a view is every object that makes
    	// an individual scroll/table gooo
    	ColumnNames columnNames; 
        JTable table;
        MyTableModel2 tm;
        TableSorter sorter;
        ProductTypesRows rows;
        ProductTypesRows clipBoard;
        private Undo undo;
        
        boolean tableDirty = false;
        int currentRow = 0;
        
        public View() {
        	/*
        	this.initTableData();
        	
        	this.tm = new MyTableModel2();
        
        	this.undo = new Undo();
  
        	this.sorter = new TableSorter(tm); 	
        	this.table = new JTable(sorter); 
        	this.clipBoard = null;
        	*/
        	
        }
 
        // this is a HACK to get around some horrible
        // dependency problems with CURRENT view
        // and the order of initialization...
        // MUST fix, KR
        public void initView() {
        	this.initTableData();
        	
        	this.tm = new MyTableModel2();
        
        	this.undo = new Undo();
  
        	this.sorter = new TableSorter(tm); 	
        	this.table = new JTable(sorter); 
        	this.clipBoard = null;
        	
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
 
        
    }
    private class Views extends Vector<View>
    {
    	// All the View(s) go here

    }
    public TableDemo(String dbXMLFile) throws Exception {
    	
        //super(new GridLayout(3,0));
    	super(new BorderLayout());
    	
    	currentView = new View();
    	currentView.initView();
    	
        //new MakeTableData(); // makes columns and blank row
        //ParseDBXML parstIt = new ParseDBXML(dbXMLFile);
        fc = new JFileChooser();
        
        
        
        //tm = new MyTableModel2();
        
        //undo = new Undo();
  
        //sorter = new TableSorter(tm); 	
        //table = new JTable(sorter);             
        currentView.sorter.setTableHeader(currentView.table.getTableHeader()); 

        ListSelectionModel lsm  = currentView.table.getSelectionModel();
        lsm.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        //lsm.addListSelectionListener(new ListSelectionListenerTest());
        
    
        
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK,false);
        KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK,false);
        KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0,false);
        KeyStroke backspace = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE,0,false);
        KeyStroke undoKeys = KeyStroke.getKeyStroke(KeyEvent.VK_Z,ActionEvent.CTRL_MASK,false);
        
        currentView.table.registerKeyboardAction(new CopyStroke(),"Copy",copy,JComponent.WHEN_FOCUSED);
        currentView.table.registerKeyboardAction(new PasteStroke(),"Paste",paste,JComponent.WHEN_FOCUSED);
        
        DeleteStroke deleteStroke = new DeleteStroke();
        //table.registerKeyboardAction(deleteStroke,"Backspace",delete,JComponent.WHEN_FOCUSED);
        currentView.table.registerKeyboardAction(deleteStroke,"Delete",backspace,JComponent.WHEN_FOCUSED);

        currentView.table.registerKeyboardAction(new UndoStroke(),"Undo",undoKeys,JComponent.WHEN_FOCUSED);
        
        //table.setPreferredScrollableViewportSize(new Dimension(700, 70));


        currentView.table.setPreferredScrollableViewportSize(new Dimension(900, 400));
        
        
        // make a top-panel for the menu and buttons
        // that control the usage of the table.
        //JPanel topCtrlPanel = new JPanel(new GridLayout(0,3));
        JPanel topCtrlPanel = new JPanel(new GridLayout(2,0));
       
        topCtrlPanel.add(new MakeMenuBar());
        
        topCtrlPanel.add(new MakeTableRowButtons());
 

        // put the panel at the top
        add(topCtrlPanel, BorderLayout.PAGE_START);
 
        // Add the scroll pane to this panel, put it 
        // in the CENTER so it can expand to fit the 
        // available area
        JTabbedPane tp = new JTabbedPane();
        tp.add(new JScrollPane(currentView.table), "Table 1");
        
        //add(new JTabbedPane(new JScrollPane(table)), BorderLayout.CENTER);
        add(tp, BorderLayout.CENTER);
 
        fftest = new MakeFloatingFrame();
        
    }

    private class CopyStroke implements ActionListener
    {

		public void actionPerformed(ActionEvent arg0) {

			currentView.tm.copyRows();
		}
    	
    }
    
    private class PasteStroke implements ActionListener
    {

		public void actionPerformed(ActionEvent arg0) {

			currentView.tm.pasteRows();
		}
    	
    }
    
    private class DeleteStroke implements ActionListener
    {

		public void actionPerformed(ActionEvent arg0) {

			currentView.tm.cutRows();
		}
    }
    
    private class UndoStroke implements ActionListener
    {

		public void actionPerformed(ActionEvent arg0) {

			currentView.tm.undoLastAction();
		}
    	
    }
    
    
    ////this seems USELESS!!!
    @SuppressWarnings("unused")
	private class ListSelectionListenerTest implements ListSelectionListener

    {
    	public void valueChanged(ListSelectionEvent e) {
    		
    		//System.out.println("Here... " + e.getFirstIndex() + " " + e.getLastIndex());
    		
//    		 See if this is a valid table selection
    		if( e.getSource() == currentView.table.getSelectionModel()
    						&& e.getFirstIndex() >= 0 )
    		{
    			
    			//System.out.println("Here... r:" + table.getSelectedRow() + " " + e.getFirstIndex() + " " + e.getLastIndex());
    			// Display the selected item
    			//System.out.println( "Value selected = " + string );
    		}
   		
    	
    	}
    }
    // our table MODEL
    private class MyTableModel2 extends DefaultTableModel {
 
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private boolean undoIt = true;
    	
        public int getColumnCount() {
            return currentView.columnNames.size();
        }

        public int getRowCount() {
             return currentView.rows.size();
        }

        public String getColumnName(int col) {
            return currentView.columnNames.get(col);
        }

        public Object getValueAt(int row, int col) {
        	ProductTypesRow r = currentView.rows.get(row);
      	//System.out.println("getValueAt: " + (String)r.get(col));
        	return r.get(col);
        }
        
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
        	//System.out.println("Current Row = " + row);
        	currentView.currentRow = row;
        	
 
                return true;
            
        }
        
        public void setValueAt(Object value, int row, int col) {
         
        	//System.out.println("Setting value at " + row + "," + col
            //                       + " to " + value
            //                       + " (an instance of "
             //                      + value.getClass() + ")");
          
        	ProductTypesRow r = currentView.rows.get(row);
        	
        	if (r != null) {
        		currentView.tableDirty = true;
        		r.set(col, (String)value);
        		
        		fireTableCellUpdated(row, col);
        	}
            
       
        }
        public void updateTable() {
        	
        	this.fireTableDataChanged();
        }
        
        public void newEmptyRow() {
 
        	ProductTypesRow row = new ProductTypesRow("","","","","","");
        	currentView.rows.add(currentView.currentRow, row);
        	updateTable();
        	
      		// put in the Undo list
        	currentView.undo.pushRow(row);
        	currentView.undo.pushRecord(Undo.Mode.insert, currentView.currentRow, 1);
        }
        
        public void insertEmptyRow() {
 
        	ProductTypesRow row = new ProductTypesRow("","","","","","");
        	currentView.rows.add(currentView.currentRow+1, row);
    		updateTable();
    		
    		// put in the Undo list
    		currentView.undo.pushRow(row);
    		currentView.undo.pushRecord(Undo.Mode.insert, currentView.currentRow+1, 1);

        }
        
        public void insertUndoRow(ProductTypesRow row) {
        	 
        	
        	currentView.rows.add(currentView.currentRow, row);
    		updateTable();
    		
  

        }
        
        public void deleteCurrentRow() {
        	// take one off
        	ProductTypesRow dRow = currentView.rows.remove(currentView.currentRow);
        	
        	// put it on the undo
        	if (this.undoIt) {
        		currentView.undo.pushRow(dRow);
        		currentView.undo.pushRecord(Undo.Mode.delete, currentView.currentRow, 1);
        	}
    		
    		// now back to making the delete work
        	currentView.currentRow = currentView.currentRow - 1;
        	if (currentView.currentRow < 0) currentView.currentRow = 0;
        	updateTable();
        	currentView.tableDirty = true;
        	// special case
        	if (currentView.rows.size() == 0) {

        		//ProductTypesRow row = new ProductTypesRow("","","","","","");
        		//rows.add(currentRow, row);
        		//updateTable();
        		this.newEmptyRow();
        		currentView.tableDirty = false;
        	}
        }
        public void copyRows() {
			int[] selectedRows = currentView.table.getSelectedRows();
			int rowCount = currentView.table.getSelectedRowCount();
			
			if (rowCount > 0) {
				//	blow away old clipBoard

				currentView.clipBoard = new ProductTypesRows();
			
				for (int i = 0; i < rowCount; i++) {
					//System.out.println("Copy from row = " + selectedRows[i]);
					
					currentView.clipBoard.add(i, currentView.rows.get(selectedRows[i]));
				}
			}
        }
        public void pasteRows() {
			int[] selectedRows = currentView.table.getSelectedRows();
			int rowCount = currentView.table.getSelectedRowCount();
			
			int copyCount = rowCount;
			if (rowCount > currentView.clipBoard.size()) 
				copyCount = currentView.clipBoard.size();
			else if (rowCount < currentView.clipBoard.size()) 
				copyCount = currentView.clipBoard.size();
			
				
			
			if (currentView.clipBoard != null) {
				int baseRowIndex = selectedRows[0];
				for (int i = 0; i < copyCount; i++) {
					//System.out.println("Copy to row = " + baseRowIndex);
					//if (baseRowIndex >= rows.size()) {
					//	rows.add(baseRowIndex, clipBoard.get(i));
					//} else
					currentView.rows.set(baseRowIndex, currentView.clipBoard.get(i));
					baseRowIndex = baseRowIndex + 1;
				}
				currentView.tm.updateTable();
			}       	
        }
    
        public void cutRows() {
			//System.out.println("Delete");
			int[] selectedRows = currentView.table.getSelectedRows();
			int rowCount = currentView.table.getSelectedRowCount();
			
			if (rowCount <= 0) {
					System.out.println("CurrentRow = " + currentView.currentRow + "rowCount = " + rowCount );
					return;
			}
			// set the currentRow to the END of the block
			// then simply call "deleteCurrenRow()" for the number
			// of rows... (currentRow may be TOP of the block, so
			// it must be adjusted)
			currentView.currentRow = selectedRows[rowCount-1];
			for (int i = 0; i < rowCount; i++) {
				currentView.tm.deleteCurrentRow();
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
        	currentView.currentRow = rowStart;
			
			for (int i = 0; i < rowCount; i++) {
				currentView.tm.deleteCurrentRow();
			}
			
        }
        
        public void undoDeleteRows(int rowStart, int rowCount) {
        	
			// slight varient on above and MOST IMPORTANT
        	// it does NOT manipulate the UNDO
		
			// set the currentRow to the END of the block
			// then simply call "deleteCurrentRow()" for the number
			// of rows... (currentRow may be TOP of the block, so
			// it must be adjusted)
        	currentView.currentRow = rowStart;
			for (int i = 0; i < rowCount; i++) {
				ProductTypesRow oldRow = currentView.undo.popRow();
				currentView.tm.insertUndoRow(oldRow);
			}
        }
        
        // attempt to undo the last 
        // recorded action in the Undo
        // list -- invert the action found
        public void undoLastAction() {
        	Undo.Record rec = currentView.undo.popRecord();
        	if (rec != null) {
        		int rowCount = rec.getCount();
        		int rowIndex = rec.getRowIndex();
        		Undo.Mode mode = rec.getMode();
        		// hmm here's the big guess
        		this.undoIt = false; //ugly
        		if (mode.equals(Undo.Mode.insert)) {
        			currentView.tm.undoInsertRows(rowIndex, rowCount);
        		} else {
        			currentView.tm.undoDeleteRows(rowIndex, rowCount);
        		}
        		this.undoIt = true;
        	}
        }
    }
    
 
    
    private class MakeMenuBar extends JMenuBar {
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		JMenu menu;

        public MakeMenuBar() {
            super();
           
            menu = new JMenu("File");
            menu.add(new OpenMenuItem());
            menu.add(new CloseMenuItem());
            menu.add(new SaveMenuItem());
            menu.setToolTipText("File manipulation");
            add(menu); 
            
            menu = new JMenu("Edit");
            
            menu.add(new CopyMenuItem());
            menu.add(new PasteMenuItem());
            menu.add(new CutMenuItem());
            menu.addSeparator();
            menu.add(new ClearMenuItem());
            menu.setToolTipText("Editing Tables");
            add(menu);
            
            menu = new JMenu("Database");
            menu.add(new ImportDBMenuItem());
            menu.add(new ExportDBMenuItem());
            menu.addSeparator();
            menu.add(new ConfigDBMenuItem());
            menu.setToolTipText("Database Interaction");
            add(menu);
            
        }
      
		private class OpenMenuItem extends JMenuItem implements ActionListener {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public OpenMenuItem() {
				super("Open...");
				this.setToolTipText("Open file");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Open File");
				//				Create a file chooser
				
				Boolean proceed = true;
				if (currentView.tableDirty == true)
					proceed = popupConfirm("Open will destroy table changes, continue?");

				if (proceed == false) return;
				
		        int returnVal = fc.showOpenDialog(TableDemo.this);

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fc.getSelectedFile();
	                //This is where a real application would open the file.
	                //System.out.println("really Open File: " + file);
	                try {
	                	ParseDBXML parseXML = new ParseDBXML(file);
	                	parseXML.parseDBFile();
	                	currentView.rows = parseXML.getRows();
	                	//System.out.println("Row size: " + rows.size());
	                	
	                	currentView.tm.updateTable();
	                	
	                } catch (SAXException saxe) {
	                	//System.out.println("XML parse failed: XML" + saxe.getException() + " :: " + saxe.getMessage());
	                	
	                	JOptionPane.showMessageDialog(
	        					TableDemo.this,
	        					"Open Failed - XML Parsing Failure",
	        					"",
	        					JOptionPane.OK_OPTION);
	                	
	                } catch (IOException ioex) {
	                	//System.out.println("XML parse failed: I/O" + " :: " + ioex.getMessage());
	                	JOptionPane.showMessageDialog(
	        					TableDemo.this,
	        					"Open Failed - I/O Error",
	        					"",
	        					JOptionPane.OK_OPTION);
	                }
	            } else {
	                
	            }
	            
			}
 			
 		}
		private class CloseMenuItem extends JMenuItem implements ActionListener {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public CloseMenuItem() {
				super("Close");
				this.setToolTipText("Close & Exit Editor");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Close File");
				Boolean proceed = true;
				if (currentView.tableDirty == true)
					proceed = popupConfirm("Closing Table will destroy table changes, continue?");

				if (proceed == false) return;
				System.exit(0);
			}
 			
 		}
		private class SaveMenuItem extends JMenuItem implements ActionListener {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public SaveMenuItem() {
				super("Save As...");
				this.setToolTipText("Save table as File");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Save File As");
				//				Create a file chooser
				
				Boolean proceed = true;
				if ((currentView.rows == null) || (currentView.tableDirty == false))
						proceed = popupConfirm("Table is empty, proceed with file save?");

				if (proceed == false) return;
				
		        int returnVal = fc.showSaveDialog(TableDemo.this);

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fc.getSelectedFile();
	                //This is where a real application would open the file.
	                //System.out.println("really save File");
	                try {
	                	new OutputDBXML(file);
	                } catch (Exception ex) {
	                	JOptionPane.showMessageDialog(TableDemo.this, "XML Generation Failed.");
	                	//System.out.println("XML output failed");	
	                }
	            } else {
	                
	            }
			}
 			
 		}
		
		private class ImportDBMenuItem extends JMenuItem implements ActionListener {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public ImportDBMenuItem() {
				super("Import from DB...");
				this.setToolTipText("Import from DB");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Importing");
				//				Create a file chooser
		        
				Boolean proceed = true;
				if (currentView.tableDirty == true) {
					proceed = popupConfirm("Importing from DB will overwrite Table, continue?");
					if (proceed == false) return;
				} 
				
	         	try {
	         		if ((hostName == null) ||
	         			(userName == null) || 
	         			(portNum == null)  ||
	         			(dbName == null))
	         		{
	         			JOptionPane.showMessageDialog(TableDemo.this, "Check Configuration.");
	         		} else {
		         		OpenDB db = new OpenDB(hostName, userName, dbName, portNum, password);
		         		connection = db.getConnection();
		         		JOptionPane.showMessageDialog(TableDemo.this, "Connection established.");
		         		
		         		if (db.buildTable() > 0) {
		         			currentView.rows = db.getRows();
		         			currentView.tm.updateTable();
		         		}
		         		connection.close();
	         		}
	  
	         	} catch (Exception ex) {
	         		//System.out.println(ex.getMessage());
	         		JOptionPane.showMessageDialog(TableDemo.this, "Connection failed.");
	      		}
				

			}
 			
 		}

		private class ExportDBMenuItem extends JMenuItem implements ActionListener {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public ExportDBMenuItem() {
				super("Export to DB...");
				this.setToolTipText("Export to DB");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Exporting");
				//				Create a file chooser
		        
				
				Boolean proceed = true;
				if (currentView.tableDirty == true) {
					proceed = popupConfirm("Exporting will overwrite Database Table, continue?");
					if (proceed == false) return;
				} else {
					// empty table test...
					proceed = popupConfirm("Exporting EMPTY table will ERASE Database Table, continue?");
					if (proceed == false) return;
				}
				
	         	try {
	         		if ((hostName == null) ||
	         			(userName == null) || 
	         			(portNum == null)  ||
	         			(dbName == null))
	         		{
	         			JOptionPane.showMessageDialog(TableDemo.this, "Check Configuration.");
	         		} else {
		         		OpenDB db = new OpenDB(hostName, userName, dbName, portNum, password);
		         		connection = db.getConnection();
		         		JOptionPane.showMessageDialog(TableDemo.this, "Connection established.");
		         		
		         		if (currentView.rows == null) throw new Exception();
		         		
		         		db.sendTable();
		         		
		         		connection.close();
	         		}
	  
	         	} catch (Exception ex) {
	         		//System.out.println(ex.getMessage());
	         		JOptionPane.showMessageDialog(TableDemo.this, "Connection failed.");
	      		}
		        
			}
 			
 		}
		
		private class ConfigDBMenuItem extends JMenuItem implements ActionListener {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public ConfigDBMenuItem() {
				super("Config DB connection...");
				this.setToolTipText("Config DB connection");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Configure");
				//				Create a file chooser
		        
				
		        Point location = TableDemo.this.getLocationOnScreen();
		        location.translate(50,10);
		        fftest.setLocation(location);
				fftest.setVisible(true);
				
		        
			}
 			
 		}
		
		private class ClearMenuItem extends JMenuItem implements ActionListener {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public ClearMenuItem() {
				super("Clear Table");
				this.setToolTipText("Clear Table");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("clear");
				//				Create a file chooser
				
				
				boolean proceed = popupConfirm("Really Clear Table?");
				if (proceed == false) return;
					
				// there are several ways to do this but...
				// this the SAFE way.  Simply set the currentRow
				// to the END of the table, then delete all the rows
				// one at time from the end back...
				//System.out.println("ROWS size() " + rows.size());
				if (currentView.rows.size() > 0) {
					int rowCount = currentView.rows.size();
					currentView.currentRow = rowCount - 1;
					for (int i = 0; i < rowCount; i++) {
						currentView.tm.deleteCurrentRow();
					}
				}
			
				//System.out.println("new ROWS size() " + rows.size());
			}
 			
 		}
		
		private class CopyMenuItem extends JMenuItem implements ActionListener {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public CopyMenuItem() {
				super("Copy           Ctrl+c");
				this.setToolTipText("Copy highlighted Rows");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("clear");
				//				Create a file chooser
				currentView.tm.copyRows();
			}
  		}
		
		private class PasteMenuItem extends JMenuItem implements ActionListener {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public PasteMenuItem() {
				super("Paste         Ctrl+v");
				this.setToolTipText("Paste highlighted Rows");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("paste");
				//				Create a file chooser
				currentView.tm.pasteRows();
			}
  		}
		
		private class CutMenuItem extends JMenuItem implements ActionListener {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public CutMenuItem() {
				super("Cut         DEL,BCKSP");
				this.setToolTipText("Delete highlighted Rows");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("paste");
				//				Create a file chooser
				currentView.tm.cutRows();
			}
  		}
		
		private boolean popupConfirm(String confirmMsg) {
			int n = JOptionPane.showConfirmDialog(
					TableDemo.this,
					confirmMsg,
					"",
					JOptionPane.YES_NO_OPTION);
			if (n == JOptionPane.NO_OPTION || n == JOptionPane.CLOSED_OPTION)
				return false;
			
			return true;
		}
		

        
    }
    
    @SuppressWarnings("unused")
	private class MakeButton implements ActionListener {
    	
        public JButton createButton(String name) {
        	JButton button = new JButton(name);	
        	
        	button.addActionListener(this);
        	
        	return(button);
        }
        public void actionPerformed(ActionEvent e) {
        	//System.out.println("Hello...");
        }
    }
    
  
   private class UpdateButton extends JButton implements ActionListener {
    	
        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		public UpdateButton() {
        	super("Update");
        	
        	
        	addActionListener(this);
 
        }
        public void actionPerformed(ActionEvent e) {
        	//System.out.println("Update...");
        }
    }
   
   private class AddRowButton extends JButton implements ActionListener {
   	
       /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public AddRowButton() {
       	super();
        	this.setToolTipText("Insert Row Before");
        //ImageIcon appendIcon = new ImageIcon("C:\\Documents and Settings\\krice\\workspace\\AppendRowIcon.gif");
       	ImageIcon appendIcon = new ImageIcon("../InsertRowIcon.gif");
       	this.setIcon(appendIcon);
        //ImageIcon pressedIcon = new ImageIcon("C:\\Documents and Settings\\krice\\workspace\\PressedRowIcon.gif");
       	ImageIcon pressedIcon = new ImageIcon("../PressedRowIcon.gif");
       	this.setPressedIcon(pressedIcon);
        Dimension d = new Dimension( appendIcon.getIconWidth(), appendIcon.getIconHeight());
        this.setMaximumSize(d);
        this.setMinimumSize(d); 
        
        addActionListener(this);
        
       }
       public void actionPerformed(ActionEvent e) {
       	//System.out.println("New Row...");
    	   currentView.tm.newEmptyRow();
       }
   }
   
   private class InsertRowButton extends JButton implements ActionListener {
   	
       /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public InsertRowButton() {
       	super();
       	this.setToolTipText("Insert Row After");
        //ImageIcon insertIcon = new ImageIcon("C:\\Documents and Settings\\krice\\workspace\\InsertRowIcon.gif");
        ImageIcon insertIcon = new ImageIcon("../AppendRowIcon.gif");
        this.setIcon(insertIcon);
        //ImageIcon pressedIcon = new ImageIcon("C:\\Documents and Settings\\krice\\workspace\\PressedRowIcon.gif");
        ImageIcon pressedIcon = new ImageIcon("../PressedRowIcon.gif");
        this.setPressedIcon(pressedIcon);
        
        
        Dimension d = new Dimension( insertIcon.getIconWidth(), insertIcon.getIconHeight());
        this.setMaximumSize(d);
        this.setMinimumSize(d);
        

       	addActionListener(this);

       }
       public void actionPerformed(ActionEvent e) {
       	//System.out.println("InsertRow...");
    	   currentView.tm.insertEmptyRow();
       }
   }
   
   private class DeleteRowButton extends JButton implements ActionListener {
	   	
       /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public DeleteRowButton() {
       	super();
       	this.setToolTipText("Delete Current Row");
        //ImageIcon deleteIcon = new ImageIcon("C:\\Documents and Settings\\krice\\workspace\\DeleteRowIcon.gif");
        ImageIcon deleteIcon = new ImageIcon("../DeleteRowIcon.gif");
        this.setIcon(deleteIcon);
        //ImageIcon pressedIcon = new ImageIcon("C:\\Documents and Settings\\krice\\workspace\\PressedRowIcon.gif");
        ImageIcon pressedIcon = new ImageIcon("../PressedRowIcon.gif");
        this.setPressedIcon(pressedIcon);
        
        Dimension d = new Dimension( deleteIcon.getIconWidth(), deleteIcon.getIconHeight());
        this.setMaximumSize(d);
        this.setMinimumSize(d); 
        
       	addActionListener(this);

       }
       public void actionPerformed(ActionEvent e) {
       	//System.out.println("Delete Row...");
    	   currentView.tm.deleteCurrentRow();
       }
   }
  
   public class MakeTableRowButtons extends JPanel {
	   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MakeTableRowButtons() {
	       
		   //super(new GridLayout(0, 3, 5, 10));
		   super();
		   this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		   this.add(Box.createRigidArea(new Dimension(5, 0)));
	       this.add(new InsertRowButton(), Box.LEFT_ALIGNMENT);
	       this.add(Box.createRigidArea(new Dimension(5, 0)));
	       this.add(new AddRowButton());
	       this.add(Box.createRigidArea(new Dimension(5, 0)));
	       this.add(new DeleteRowButton());
	   }
   }
   
   @SuppressWarnings("unused")
private class MakeTableDBButtons extends JPanel {
	   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MakeTableDBButtons() {
		   super(new BorderLayout());
	       this.add(new UpdateButton(), BorderLayout.EAST);
	   }
   }

   // private XML parser class
   private class ParseDBXML extends DefaultHandler
   {
	   private ProductTypesRows localRows = null; 
	   private ProductTypesRow localRow = null;
	   private XMLReader xr = null;
	   private String altDTDDirLocation = null;
	   private FileReader r = null;
		private StringBuffer str; 
		private int dataIndex = 0;
		String data = null;		
		

       
       public ParseDBXML(File file) throws SAXException, FileNotFoundException 
       {
       	super();
       	
       	localRows = new ProductTypesRows();
       	
       	xr = XMLReaderFactory.createXMLReader();
       	
   		xr.setDTDHandler(this);
   		xr.setContentHandler(this);
   		xr.setErrorHandler(this);
   		xr.setEntityResolver(this);

   		// Parse each file provided on the
   		// command line.
   		altDTDDirLocation = file.getParent();
   		
   		r = new FileReader(file);
   		
       }
       
       public void parseDBFile() throws IOException, SAXException 
       {
   		
   		xr.parse(new InputSource(r));
   		
       }
       
       public ProductTypesRows getRows() {
    	   return(localRows);
       }
       
       public InputSource resolveEntity(String publicID, String systemID) throws SAXException, IOException {
    	   	//System.out.println("publicID: " + publicID + " SystemID: " + systemID);
    	   	File cheat = new File(systemID);
    	   	
        	return new InputSource(altDTDDirLocation+ "/" + cheat.getName());
       }

       public void setDocumentLocator (Locator locator)
       {
          //System.out.println("SetDocumentLocator: " + locator.getSystemId());
          //this.locator = locator;
       }

       
       public void startDocument ()
       {
       	//System.out.println("Start document");
       }


       public void endDocument ()
       {
       	//System.out.println("End document");
       }


       public void startElement (String uri, String name, String qName, Attributes atts)
       {
       	if (name.equals("row")) {
       		localRow = new ProductTypesRow(); // a NO DATA row... less than empty, KR
       	} else if (name.equals("field")) {
       		str = new StringBuffer();
       		//System.out.println("Attribute: " + atts.getValue(0));
        	}
       }


       public void endElement (String uri, String name, String qName)
       {
       	if (name.equals("field")) {
       		dataIndex = 0;
       		if (localRow != null) {
           		localRow.add(data);
           	}
       	}
        	if (name.equals("row")) {
       		if (localRow != null) {
       			localRows.add(localRow);
       			//System.out.println("New Row Created: " + localRows.size());
       		}
       	}
       }


       public void characters (char ch[], int start, int length) throws SAXException
       {
    	   if (str == null) {
    		   throw new SAXException();
    	   }
        	str.insert(dataIndex, ch, start, length);
        	dataIndex = length;
        	data = str.toString().trim();
       	
        	//System.out.println("Data: " + data);
       	
       }
       public void getType(String name) {
       	
       	
       	//System.out.println("getType: " + name);
       	
    
       }
  
       
   }


   private class OutputDBXML
   {
	   public OutputDBXML(File file) throws Exception {
		   if (currentView.rows == null) throw new Exception();
		   PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));

		   out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		   out.println("<!DOCTYPE ROOT SYSTEM \"ProductTypes.dtd\">");
		   out.println("<ROOT>");
		   outputRows(out);
		   out.println("</ROOT>");
		   out.close();
	   }
	   void outputRows(PrintWriter out) {
		   for (ProductTypesRow r : currentView.rows) {
			   out.println("\t<row>");
			   String name = r.get(0);
			   String spacecraft = r.get(1);
			   String sensor = r.get(2);
			   String description = r.get(3);
			   String level = r.get(4);
			   String is_directory = r.get(5);
			   out.println("\t\t" + "<field name=\"name\">" + name + "</field>");
			   out.println("\t\t" + "<field name=\"spacecraft\">" + spacecraft + "</field>");
			   out.println("\t\t" + "<field name=\"sensor\">" + sensor + "</field>");
			   out.println("\t\t" + "<field name=\"description\">" + description + "</field>");
			   out.println("\t\t" + "<field name=\"level\">" + level + "</field>");
			   out.println("\t\t" + "<field name=\"is_directory\">" + is_directory + "</field>");
			   out.println("\t</row>");
		   }
	   }
   }
   
   
   @SuppressWarnings("unused")
private class MakeInternalFrame extends JInternalFrame {
		
		
		/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

		public MakeInternalFrame() {
			super();
			
			//Make the big window be indented 50 pixels from each edge
			//of the screen.
			int inset = 50;
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			//this.setBounds(inset, inset, screenSize.width  - inset*2, screenSize.height - inset*2);
			
			
				
			//this.setDefaultLookAndFeelDecorated(true);


//			3. Optional: What happens when the frame closes?
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//			4. Create components and put them in the frame.
//			...create emptyLabel...
			

//			5. Size the frame.
			this.pack();

//			6. Show it.



//			setContentPane(desktop);

		}
		


/*		
		//Create a new internal frame.
		protected void createFrame() {
//			MyInternalFrame frame = new MyInternalFrame();
			frame.setVisible(true); //necessary as of 1.3
			TableDemo.add(frame);
			try {
				frame.setSelected(true);
			} catch (java.beans.PropertyVetoException e) {}
		}
*/
   }
   
   private class MakeFloatingFrame extends JFrame implements ActionListener, PropertyChangeListener {
	    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		//Labels to identify the fields
	    private JLabel hostLabel;
	    private JLabel portLabel;
	    private JLabel dbLabel;
	    private JLabel userLabel;
	    private JLabel passwdLabel;
	    //private JLabel paymentLabel;

	    //Strings for the labels
	    private String hostString = "Server Host: ";
	    private String portString = "Port: ";
	    private String dbString = "DB Name: ";
	    private String userString = "Username: ";
	    private String passwdString = "Password: ";

	    //Fields for data entry
	    private JFormattedTextField hostField;
	    private JFormattedTextField portField;
	    private JFormattedTextField dbField;
	    private JFormattedTextField userField;
	    //private JFormattedTextField passwdField;
	    private JPasswordField passwdField;

	    //Formats to format and parse numbers
	    //private NumberFormat portFormat;
	   
	    // local data holders
	    String localHost;
	    String localPort;
	    String localDBname;
	    String localUser;
	    String localPasswd;


	    /**
	     * Create the GUI and show it.  For thread safety,
	     * this method should be invoked from the
	     * event-dispatching thread.
	     */
	    private MakeFloatingFrame() {
	    	super("Configure Connection");
	    	
	    	localHost = null;
	    	localPort = null;
	    	localDBname = null;
	    	localUser = null;
	    	localPasswd = null;
	    	
	        //Suggest that the L&F (rather than the system)
	        //decorate all windows.  This must be invoked before
	        //creating the JFrame.  Native look and feels will
	        //ignore this hint.
	        JFrame.setDefaultLookAndFeelDecorated(true);

	        //Create and set up the window.
	        Point location = TableDemo.this.getLocation();
	        location.translate(50,50);
	        this.setLocation(location);
	        
	        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

	        // populate the frame with some user input fields
//	        portFormat = NumberFormat.getNumberInstance();

	        //stringFormat = StringFormat.getStringInstance();

	        //Create the labels.
	        hostLabel = new JLabel(hostString);
	        portLabel = new JLabel(portString);
	        dbLabel = new JLabel(dbString);
	        userLabel = new JLabel(userString);
	        passwdLabel = new JLabel(passwdString);	        

	        //Create the text fields and set them up.
	        hostField = new JFormattedTextField();
	        hostField.setValue("");
	        hostField.setColumns(20);
	        hostField.addPropertyChangeListener("value", this);

	        try {
	        	portField = new JFormattedTextField(new MaskFormatter("#####"));
	        } catch (Exception e){
	        	portField = new JFormattedTextField();
	        }
	        portField.setValue("");
	        portField.setColumns(20);
	        portField.addPropertyChangeListener("value", this);

	        dbField = new JFormattedTextField();
	        dbField.setValue("DSM");
	        dbField.setColumns(20);
	        dbField.addPropertyChangeListener("value", this);
	        
	        userField = new JFormattedTextField();
	        userField.setValue("");
	        userField.setColumns(20);
	        userField.addPropertyChangeListener("value", this);

	        //passwdField = new JFormattedTextField();
	        passwdField = new JPasswordField();
	        //passwdField.setValue("");
	        passwdField.setColumns(20);
	        passwdField.addPropertyChangeListener("value", this);
	        
	        //Tell accessibility tools about label/textfield pairs.
	        hostLabel.setLabelFor(hostField);
	        portLabel.setLabelFor(portField);
	        userLabel.setLabelFor(userField);
	        passwdLabel.setLabelFor(passwdField);

	        //Lay out the labels in a panel.
	        JPanel labelPane = new JPanel(new GridLayout(0,1));
	        labelPane.add(hostLabel);
	        labelPane.add(portLabel);
	        labelPane.add(dbLabel);
	        labelPane.add(userLabel);
	        labelPane.add(passwdLabel);

	        //Layout the text fields in a panel.
	        JPanel fieldPane = new JPanel(new GridLayout(0,1));
	        fieldPane.add(hostField);
	        fieldPane.add(portField);
	        fieldPane.add(dbField);
	        fieldPane.add(userField);
	        fieldPane.add(passwdField);

	        //Put the panels in this panel, labels on left,
	        //text fields on right.
	        //this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	        JPanel inputPane = new JPanel(new BorderLayout());
	        inputPane.add(labelPane, BorderLayout.LINE_START);
	        inputPane.add(fieldPane, BorderLayout.CENTER);
	        
	
	        // alignment is a huge pain... 
	        //  KR
	        JPanel buttonPane = new JPanel();
	        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));
	        
	        /*
	        JButton button = new JButton("OK");
	        button.addActionListener(this);
	        
	        button.setAlignmentX(button.CENTER_ALIGNMENT);

	        buttonPane.add(button, Box.CENTER_ALIGNMENT);
	        */
	        // attempt at TWO buttons, centered...
	        JPanel twoButtonPane = new JPanel();
	        twoButtonPane.setLayout(new BoxLayout(twoButtonPane, BoxLayout.LINE_AXIS));
	        JButton okButton = new JButton("OK");
	        JButton cButton = new JButton("Cancel");
	        //okButton.setSize(cButton.getSize());
	        //okButton.setPreferredSize(cButton.getPreferredSize());
	        okButton.setMaximumSize(cButton.getMaximumSize());
	        okButton.setMinimumSize(cButton.getMinimumSize());
	        okButton.addActionListener(this);
	        cButton.addActionListener(this);
	        twoButtonPane.add(okButton, Box.LEFT_ALIGNMENT);
	        twoButtonPane.add(Box.createRigidArea(new Dimension(10,10)), Box.CENTER_ALIGNMENT);
	        twoButtonPane.add(cButton, Box.RIGHT_ALIGNMENT);
	        buttonPane.add(twoButtonPane, Box.CENTER_ALIGNMENT);

	        inputPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	        
	        this.getContentPane().add(inputPane,BorderLayout.CENTER);
	        this.getContentPane().add(buttonPane,BorderLayout.PAGE_END);
	        
	        
	      
	        //Display the window.
	        this.pack();
	        //frame.setVisible(true);
	    }
	    
	    
	    /** Called when a field's "value" property changes. */
	    public  void propertyChange(PropertyChangeEvent e) {
	        Object source = e.getSource();
	        if (source == hostField) {
	        	if (hostField.getValue() != null)
	        		localHost = hostField.getValue().toString();
	        } else if (source == portField) {
	        	if (portField.getValue() != null)
	        		localPort = portField.getValue().toString();
	        } else if (source == dbField) {
	        	if (dbField.getValue() != null)
	        		localDBname = dbField.getValue().toString();
	        } else if (source == userField) {
	        	if (userField.getValue() != null)
	        		localUser = userField.getValue().toString();
	        } else if (source == passwdField) {
	        	//if (passwdField.getValue() != null)
	        		//localPasswd = passwdField.getValue().toString();
	        	if (passwdField.getPassword() != null)
	        		localPasswd = passwdField.getPassword().toString();
	        }
	    }
	    
	      public void actionPerformed(ActionEvent e) {
	         	//System.out.println("Configure... does nothing");
	         	fftest.setVisible(false);
	         	
	         	//System.out.println(e.getActionCommand());
	         	if (e.getActionCommand().equals("OK")) {
	         		hostName = localHost;
	         		portNum = localPort;
	         		dbName = localDBname;
	         		userName = localUser;
	         		password = localPasswd;
	         	} else {
	         		hostField.setValue(hostName);
	         		portField.setValue(portNum);
	         		dbField.setValue(dbName);
	         		userField.setValue(userName);
	         		//passwdField.setValue(password);
	         		
	         	}
	      }
   }
   
   
   @SuppressWarnings("unused")
private class MakeFloatingDialog extends JDialog implements ActionListener, PropertyChangeListener {
	    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		//Labels to identify the fields
	    private JLabel hostLabel;
	    private JLabel portLabel;
	    private JLabel userLabel;
	    private JLabel passwdLabel;
	    //private JLabel paymentLabel;

	    //Strings for the labels
	    private String hostString = "Server Host: ";
	    private String portString = "Port: ";
	    private String userString = "Username: ";
	    private String passwdString = "Password: ";

	    //Fields for data entry
	    private JFormattedTextField hostField;
	    private JFormattedTextField portField;
	    private JFormattedTextField userField;
	    private JFormattedTextField passwdField;

	    //Formats to format and parse numbers
	    private NumberFormat portFormat;
	   
	    // local data holders
	    String localHost;
	    String localPort;
	    String localUser;
	    String localPasswd;


	    /**
	     * Create the GUI and show it.  For thread safety,
	     * this method should be invoked from the
	     * event-dispatching thread.
	     */
	    private MakeFloatingDialog() {
	    	super();
	    	
	    	localHost = null;
	    	localPort = null;
	    	localUser = null;
	    	localPasswd = null;
	    	
	        //Suggest that the L&F (rather than the system)
	        //decorate all windows.  This must be invoked before
	        //creating the JFrame.  Native look and feels will
	        //ignore this hint.
	        JDialog.setDefaultLookAndFeelDecorated(true);

	        //Create and set up the window.
	        
	        
	        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

	        // populate the frame with some user input fields
//	        portFormat = NumberFormat.getNumberInstance();

	        //stringFormat = StringFormat.getStringInstance();

	        //Create the labels.
	        hostLabel = new JLabel(hostString);
	        portLabel = new JLabel(portString);
	        userLabel = new JLabel(userString);
	        passwdLabel = new JLabel(passwdString);	        

	        //Create the text fields and set them up.
	        hostField = new JFormattedTextField();
	        hostField.setValue("");
	        hostField.setColumns(20);
	        hostField.addPropertyChangeListener("value", this);

	        try {
	        	portField = new JFormattedTextField(new MaskFormatter("#####"));
	        } catch (Exception e){
	        	portField = new JFormattedTextField();
	        }
	        portField.setValue("");
	        portField.setColumns(20);
	        portField.addPropertyChangeListener("value", this);

	        userField = new JFormattedTextField();
	        userField.setValue("");
	        userField.setColumns(20);
	        userField.addPropertyChangeListener("value", this);

	        passwdField = new JFormattedTextField();
	        passwdField.setValue("");
	        passwdField.setColumns(20);
	        passwdField.addPropertyChangeListener("value", this);
	        
	        //Tell accessibility tools about label/textfield pairs.
	        hostLabel.setLabelFor(hostField);
	        portLabel.setLabelFor(portField);
	        userLabel.setLabelFor(userField);
	        passwdLabel.setLabelFor(passwdField);

	        //Lay out the labels in a panel.
	        JPanel labelPane = new JPanel(new GridLayout(0,1));
	        labelPane.add(hostLabel);
	        labelPane.add(portLabel);
	        labelPane.add(userLabel);
	        labelPane.add(passwdLabel);

	        //Layout the text fields in a panel.
	        JPanel fieldPane = new JPanel(new GridLayout(0,1));
	        fieldPane.add(hostField);
	        fieldPane.add(portField);
	        fieldPane.add(userField);
	        fieldPane.add(passwdField);

	        //Put the panels in this panel, labels on left,
	        //text fields on right.
	        //this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	        JPanel inputPane = new JPanel(new BorderLayout());
	        inputPane.add(labelPane, BorderLayout.LINE_START);
	        inputPane.add(fieldPane, BorderLayout.CENTER);
	        
	
	        // alignment is a huge pain... 
	        //  KR
	        JPanel buttonPane = new JPanel();
	        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));
	        
	        /*
	        JButton button = new JButton("OK");
	        button.addActionListener(this);
	        
	        button.setAlignmentX(button.CENTER_ALIGNMENT);

	        buttonPane.add(button, Box.CENTER_ALIGNMENT);
	        */
	        // attempt at TWO buttons, centered...
	        JPanel twoButtonPane = new JPanel();
	        twoButtonPane.setLayout(new BoxLayout(twoButtonPane, BoxLayout.LINE_AXIS));
	        JButton okButton = new JButton("OK");
	        JButton cButton = new JButton("Cancel");
	        //okButton.setSize(cButton.getSize());
	        //okButton.setPreferredSize(cButton.getPreferredSize());
	        okButton.setMaximumSize(cButton.getMaximumSize());
	        okButton.setMinimumSize(cButton.getMinimumSize());
	        okButton.addActionListener(this);
	        cButton.addActionListener(this);
	        twoButtonPane.add(okButton, Box.LEFT_ALIGNMENT);
	        twoButtonPane.add(Box.createRigidArea(new Dimension(10,10)), Box.CENTER_ALIGNMENT);
	        twoButtonPane.add(cButton, Box.RIGHT_ALIGNMENT);
	        buttonPane.add(twoButtonPane, Box.CENTER_ALIGNMENT);

	        
	        this.getContentPane().add(inputPane,BorderLayout.CENTER);
	        this.getContentPane().add(buttonPane,BorderLayout.PAGE_END);

	        //Display the window.
	        this.pack();
	        //frame.setVisible(true);
	    }
	    
	    
	    /** Called when a field's "value" property changes. */
	    public  void propertyChange(PropertyChangeEvent e) {
	        Object source = e.getSource();
	        if (source == hostField) {
	        	if (hostField.getValue() != null)
	        		localHost = hostField.getValue().toString();
	        } else if (source == portField) {
	        	if (portField.getValue() != null)
	        		localPort = portField.getValue().toString();;
	        } else if (source == userField) {
	        	if (userField.getValue() != null)
	        		localUser = userField.getValue().toString();;
	        } else if (source == passwdField) {
	        	if (passwdField.getValue() != null)
	        		localPasswd = passwdField.getValue().toString();;
	        }
	    }
	    
	      public void actionPerformed(ActionEvent e) {
	         //	System.out.println("Configure... does nothing");
	         	fftest.setVisible(false);
	         	
	         	//System.out.println(e.getActionCommand());
	         	if (e.getActionCommand().equals("OK")) {
	         		hostName = localHost;
	         		portNum = localPort;
	         		userName = localUser;
	         		password = localPasswd;
	         	} else {
	         		hostField.setValue(hostName);
	         		portField.setValue(portNum);
	         		userField.setValue(userName);
	         		passwdField.setValue(password);
	         	}
	      }
  }
   
   private class OpenDB {
	   private Connection db;
	   private ProductTypesRows localRows = null; 
	   
	   private OpenDB(String host, String user, String dbname, String port, String passwd) throws Exception {
		  	
		   Class.forName("com.mysql.jdbc.Driver");
	        String dsm = "jdbc:mysql://" + host + ":" + port + "/" + dbname;
	        db = DriverManager.getConnection(dsm,user,passwd);
	        
	        db.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
	        db.setAutoCommit(false);
	        
	   }
	   private Connection getConnection() {
		   return db;
	   }
	   
	   private int buildTable() throws Exception {
		   Statement stmt = connection.createStatement();
		   ResultSet rs = stmt.executeQuery("SELECT * FROM producttypes;");
		   //System.out.println("Fetchsize1 = " + rs.getFetchSize());
		   //if (rs.getFetchSize() > 0) {
			  // System.out.println("Fetchsize = " + rs.getFetchSize());
			   localRows = new ProductTypesRows();
			   while( rs.next() ) {
				   String name = rs.getString("name");
				   String spacecraft = rs.getString("spacecraft");
				   String sensor = rs.getString("sensor");
				   String description = rs.getString("description");
				   String level = rs.getString("level");
				   String is_directory = rs.getString("is_directory");
				   ProductTypesRow row = new ProductTypesRow(name, spacecraft, sensor, description, 
						   										level, is_directory);

				   localRows.add(row); 
			   }
		  // }
		   if (localRows == null) return 0;
		   return localRows.size();
	   }
	   public ProductTypesRows getRows() {
		   return localRows;
	   }
	   
	   private void sendTable() throws Exception {
		   if (currentView.rows == null) throw new Exception();
		   Statement stmt = connection.createStatement();
		   //int rs;
		   // clear the table in a safe way...
		   stmt.executeUpdate("DELETE FROM producttypes;");
		   for (ProductTypesRow r : currentView.rows) {
			   
			   stmt.executeUpdate(
					   "INSERT INTO ProductTypes VALUES (" +
					   "'" + r.get(0) + "'," +
					   "'" + r.get(1) + "'," +
					   "'" + r.get(2) + "'," +
					   "'" + r.get(3) + "'," +
					   "'" + r.get(4) + "'," +
					   "'" + r.get(5) + "');"
					   );
		   }
		   connection.commit();
		   
	   }
   }
   
   public void dispose() throws Exception
   {
       if (connection != null)
       {
           connection.close();
           connection = null;
       }
   }

   /**
    * This method will close the connection as part of cleanup.
    * Do not call it directly; use dispose() instead.
    */
   public void finalize()
   {
       try { dispose(); }
       catch (Exception e) {}
   }
   
   /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI(String dbXMLFile) throws Exception {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        JFrame frame = new JFrame("ProductTable Editor");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE /*EXIT_ON_CLOSE*/);
        frame.setLocation(50,50);
        //Create and set up the content pane.
        TableDemo newContentPane = new TableDemo(dbXMLFile);
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
        
    }

    public static void main(final String[] args) throws Exception {
    	
    	
    	try {
    		//Schedule a job for the event-dispatching thread:
    		//creating and showing this application's GUI.
    		javax.swing.SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    				try {
    					createAndShowGUI(args[0]);
    				} catch( Exception e) {
    					e.printStackTrace();
    				}
    			}
    		});
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
}
