/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin.editor;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ProductTypeEditor extends JTabbedPane {

	public ProductTypeEditor(String dbXMLFile) throws Exception {
		super();
		
		JMenuBar mb = new JMenuBar();
		JMenu menu = new JMenu("File");
		mb.add(menu);
		this.add(mb);
		
		TableDemo table1 = new TableDemo(dbXMLFile);
		
		this.add(table1, "Table1");
		
		TableDemo table2 = new TableDemo(dbXMLFile);
		
		this.add(table2, "Table2");
		
		this.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JTabbedPane tp = (JTabbedPane)e.getSource();
				int index = tp.getSelectedIndex();
				String s = tp.getTitleAt(index);
				System.out.println("Selected: " + s);
			}


		});
		
	}
	
	private static void createAndShowGUI(String dbXMLFile) throws Exception {
	        //Make sure we have nice window decorations.
	    JFrame.setDefaultLookAndFeelDecorated(true);
	
	    //Create and set up the window.
	    JFrame frame = new JFrame("ProductTable Editor");
	    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE /*EXIT_ON_CLOSE*/);
	    frame.setLocation(50,50);
	    //Create and set up the content pane.
	    ProductTypeEditor newContentPane = new ProductTypeEditor(dbXMLFile);
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
