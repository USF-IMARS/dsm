/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
/*
 * TableDemo.java is a 1.4 application that requires no other files.
 */
package gov.nasa.gsfc.nisgs.dsm.admin.editor;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.io.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.*;
import javax.swing.ImageIcon;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.sql.*;

import gov.nasa.gsfc.nisgs.dsm.admin.editor.View;
/** 
 * TableDemo is just like SimpleTableDemo, except that it
 * uses a custom TableModel.
 */
public class ProductTypesEditor extends JPanel {
        
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
   
    // new proposed answer for multiple view
	Views views;
    View currentView;
    View view1;
    View view2;
    private int tabCount = 1;
    
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
    
    private JTabbedPane tp;
    
 
    private class Views extends HashMap<String, View>
    {
    	// All the View(s) go here

    }
    public ProductTypesEditor() throws Exception {
    	
        //super(new GridLayout(3,0));
    	super(new BorderLayout());
    	
    	views = new Views();
    	
    	view1 = new View("Table 1");
    	view2 = new View("Table 2");
    	tabCount = 3;
    	currentView = view1;
    	
    	
        //new MakeTableData(); // makes columns and blank row
        //ParseDBXML parstIt = new ParseDBXML(dbXMLFile);
        fc = new JFileChooser();
        
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
        tp = new JTabbedPane();

/*
		tp.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JTabbedPane tp = (JTabbedPane)e.getSource();
				int index = tp.getSelectedIndex();
				String s = tp.getTitleAt(index);
				System.out.println("Selected: " + s + " Index: " + index);
				//if (index <= 0) currentView = view1;
				//else currentView = view2;
				currentView = views.get(s);
				
				// temp hack, can't seem to throw an
				// exception
				if (currentView == null) {
					System.out.println("DISASTER...");
					currentView = view1;
				}
			}
		});
*/
        tp.addChangeListener(new TabbedPaneListener());
		
		views.put(view1.name, view1);
        tp.add(new JScrollPane(view1), view1.name);
        
        views.put(view2.name, view2);
        tp.add(new JScrollPane(view2), view2.name);
        
        tp.setSelectedIndex(0);
        
        //add(new JTabbedPane(new JScrollPane(table)), BorderLayout.CENTER);
        add(tp, BorderLayout.CENTER);
 
        fftest = new MakeFloatingFrame();
        
    }
 
    private class TabbedPaneListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			JTabbedPane tp = (JTabbedPane)e.getSource();
			int index = tp.getSelectedIndex();
			String s = tp.getTitleAt(index);
			System.out.println("Selected: " + s + " Index: " + index);
			//if (index <= 0) currentView = view1;
			//else currentView = view2;
			currentView = views.get(s);
			
			// temp hack, can't seem to throw an
			// exception
			if (currentView == null) {
				System.out.println("DISASTER...");
				
			//	currentView = view1;
			}
			// something else will have to blow up
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
            menu.add(new NewMenuItem());
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
		private class NewMenuItem extends JMenuItem implements ActionListener {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public NewMenuItem() {
				super("New...");
				this.setToolTipText("New view");
				addActionListener(this);
			}
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Open File");
				//				Create a file chooser
				
				View openView;
				

				openView = new View();


	  
	                	openView.updateTable();
	                	

	                	openView.name = "Table " + tabCount++;
	                	views.put(openView.name, openView);
	                	tp.add(new JScrollPane(openView), openView.name);

	
			}
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
				
				View openView;
				
				Boolean proceed = true;
				if (currentView.isTableDirty() == true) {
					//proceed = popupConfirm("Open will destroy table changes, continue?");
					openView = new View();
				} else {
					openView = currentView;
				}
				//if (proceed == false) return;
				
		        int returnVal = fc.showOpenDialog(ProductTypesEditor.this);

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fc.getSelectedFile();
	                //This is where a real application would open the file.
	                //System.out.println("really Open File: " + file);
	                if (views.containsKey(file.getName()) == false) {
	                	
	                
	                try {
	                	
	                		ParseDBXML parseXML = new ParseDBXML(file);
	                		parseXML.parseDBFile();
	                		openView.setRows(parseXML.getRows());
	                		//System.out.println("Row size: " + rows.size());
	                	
	                		openView.updateTable();
	                	
	                		if (openView != currentView) {
	                			openView.name = file.getName();
	                			views.put(openView.name, openView);
	                			tp.add(new JScrollPane(openView), file.getName());
	                		} else {
	                			
	                			
	                			int index = tp.indexOfTab(openView.name);
	                			tp.setTitleAt(index, file.getName());
	                			
	                			// remove the OLD view
	                			// change name, resave it
	                			views.remove(openView.name);
	                			openView.name = file.getName();
	                			views.put(openView.name, openView);
	                			
	                		}
	                	
	                	} catch (SAXException saxe) {
	                		//System.out.println("XML parse failed: XML" + saxe.getException() + " :: " + saxe.getMessage());
	                	
	                		JOptionPane.showMessageDialog(
	        					ProductTypesEditor.this,
	        					"Open Failed - XML Parsing Failure",
	        					"",
	        					JOptionPane.OK_OPTION);
	                	
	                	} catch (IOException ioex) {
	                		//System.out.println("XML parse failed: I/O" + " :: " + ioex.getMessage());
	                		JOptionPane.showMessageDialog(
	        					ProductTypesEditor.this,
	        					"Open Failed - I/O Error",
	        					"",
	        					JOptionPane.OK_OPTION);
	                	}
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
				if (currentView.isTableDirty() == true)
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
				if (currentView.isRowsEmpty() || (currentView.isTableDirty() == false))
						proceed = popupConfirm("Table is empty, proceed with file save?");

				if (proceed == false) return;
				
		        int returnVal = fc.showSaveDialog(ProductTypesEditor.this);

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fc.getSelectedFile();
	                //This is where a real application would open the file.
	                //System.out.println("really save File");
	                try {
	                	// double check
	                	if (currentView.isRowsEmpty()) throw new Exception();
	                	new OutputDBXML(file, currentView.getRows());
	                } catch (Exception ex) {
	                	JOptionPane.showMessageDialog(ProductTypesEditor.this, "XML Generation Failed.");
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
				if (currentView.isTableDirty() == true) {
					proceed = popupConfirm("Importing from DB will overwrite Table, continue?");
					if (proceed == false) return;
				} 
				
	         	try {
	         		if ((hostName == null) ||
	         			(userName == null) || 
	         			(portNum == null)  ||
	         			(dbName == null))
	         		{
	         			JOptionPane.showMessageDialog(ProductTypesEditor.this, "Check Configuration.");
	         		} else {
		         		OpenDB db = new OpenDB(hostName, userName, dbName, portNum, password);
		         		connection = db.getConnection();
		         		JOptionPane.showMessageDialog(ProductTypesEditor.this, "Connection established.");
		         		
		         		if (db.buildTable() > 0) {
		         			currentView.setRows(db.getRows());
		         			currentView.updateTable();
		         		}
		         		connection.close();
	         		}
	  
	         	} catch (Exception ex) {
	         		//System.out.println(ex.getMessage());
	         		JOptionPane.showMessageDialog(ProductTypesEditor.this, "Connection failed.");
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
				if (currentView.isTableDirty() == true) {
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
	         			JOptionPane.showMessageDialog(ProductTypesEditor.this, "Check Configuration.");
	         		} else {
		         		OpenDB db = new OpenDB(hostName, userName, dbName, portNum, password);
		         		connection = db.getConnection();
		         		JOptionPane.showMessageDialog(ProductTypesEditor.this, "Connection established.");
		         		
		         		if (currentView.isRowsEmpty()) throw new Exception();
		         		
		         		db.sendTable(currentView.getRows());
		         		
		         		connection.close();
	         		}
	  
	         	} catch (Exception ex) {
	         		//System.out.println(ex.getMessage());
	         		JOptionPane.showMessageDialog(ProductTypesEditor.this, "Connection failed.");
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
		        
				
		        Point location = ProductTypesEditor.this.getLocationOnScreen();
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
				if (currentView.numRows() > 0) {
					int rowCount = currentView.numRows();
					currentView.setCurrentRowIndex(rowCount - 1);
					for (int i = 0; i < rowCount; i++) {
						currentView.deleteCurrentRow();
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
				currentView.copyRows();
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
				currentView.pasteRows();
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
				currentView.cutRows();
			}
  		}
		
		private boolean popupConfirm(String confirmMsg) {
			int n = JOptionPane.showConfirmDialog(
					ProductTypesEditor.this,
					confirmMsg,
					"",
					JOptionPane.YES_NO_OPTION);
			if (n == JOptionPane.NO_OPTION || n == JOptionPane.CLOSED_OPTION)
				return false;
			
			return true;
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
	    	   currentView.newEmptyRow();
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
	    	   currentView.insertEmptyRow();
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
	    	   currentView.deleteCurrentRow();
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
	   public OutputDBXML(File file, ProductTypesRows rows) throws Exception {
		   
		   PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));

		   out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		   out.println("<!DOCTYPE ROOT SYSTEM \"ProductTypes.dtd\">");
		   out.println("<ROOT>");
		   outputRows(out, rows);
		   out.println("</ROOT>");
		   out.close();
	   }
	   void outputRows(PrintWriter out, ProductTypesRows rows) {
		   for (ProductTypesRow r : rows) {
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
	        Point location = ProductTypesEditor.this.getLocation();
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
	   
	   private void sendTable(ProductTypesRows rows) throws Exception {
		   
		   Statement stmt = connection.createStatement();
		   //int rs;
		   // clear the table in a safe way...
		   stmt.executeUpdate("DELETE FROM producttypes;");
		   for (ProductTypesRow r : rows) {
			   
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
    private static void createAndShowGUI() throws Exception {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        JFrame frame = new JFrame("ProductTable Editor");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE /*EXIT_ON_CLOSE*/);
        frame.setLocation(50,50);
        //Create and set up the content pane.
        ProductTypesEditor newContentPane = new ProductTypesEditor();
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
    					createAndShowGUI();
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
