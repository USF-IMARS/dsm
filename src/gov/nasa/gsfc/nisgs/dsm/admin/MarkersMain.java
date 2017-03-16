/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import gov.nasa.gsfc.nisgs.dsm.*;
import gov.nasa.gsfc.nisgs.properties.Utility;

public class MarkersMain extends JFrame
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int BUTTONgroupSPACE = 40;
    
    public static void main(String[] args)
    {
        new MarkersMain();
    }

    private DSMAdministrator dsm = null;
    private MarkersJTableModel markersModel;
    private JTable markersTable;

    public MarkersMain()
    {
        super("Markers");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

       
        java.util.List<XMarker> markerList = null;
        try
        {
            dsm = new DSMAdministrator("Admin","Markers");
            markerList = getMarkers();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(0);
        }

        markersModel = new MarkersJTableModel(markerList);
        markersTable = new JTable(markersModel);
	// Automatic table sorting seems to be good enough here...
	markersTable.setAutoCreateRowSorter(true);
        TableColumnModel columnModel = markersTable.getColumnModel();
        int count = columnModel.getColumnCount();
        for (int n = 0; n < count; n++)
        {
            columnModel.getColumn(n).setPreferredWidth(markersModel.getColumnWidth(n));
        }
        JScrollPane scrollPane = new JScrollPane(markersTable);
        scrollPane.setBorder(BorderFactory.createMatteBorder(16,16,16,16,Color.gray));
        Dimension size = markersTable.getPreferredSize();
        size.width += 64;
        size.height = Math.min(31,markersModel.getRowCount()) * markersModel.getRowHeight();
        markersTable.setPreferredScrollableViewportSize(size);

        JPanel bottom = new JPanel(new FlowLayout());
        bottom.add(new RefreshButton());
	// Add a little space between these buttons
	Dimension fillerDim = new Dimension(BUTTONgroupSPACE, 1);
	bottom.add(new Box.Filler(fillerDim, fillerDim, fillerDim));
        bottom.add(new DeleteButton());

        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(scrollPane,BorderLayout.CENTER);
        cp.add(bottom,BorderLayout.SOUTH);
        pack();

        //This fragment centers the frame on the screen.
        size = getSize();
        java.awt.Dimension screen = getToolkit().getScreenSize();
        int w = (screen.width - size.width) / 2;
        int h = (screen.height - size.height) / 2;
        setLocation(w,h);
        setVisible(true);
    }

    public void dispose()
    {
        super.dispose();
        try
        {
            dsm.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private java.util.List<XMarker> getMarkers()
    {
        java.util.ArrayList<XMarker> list = new java.util.ArrayList<XMarker>(500);
        try
        {
            java.sql.ResultSet r = dsm.query("SELECT Markers.*,Products.productType,Products.startTime,Products.pass FROM Markers,Products WHERE Markers.product=Products.id ORDER BY  Products.pass DESC");
            while (r.next())
            {
                XMarker xm = new XMarker();
                xm.gopherColony = r.getString("gopherColony");
                xm.status = r.getInt("status");
                xm.location = r.getString("location");
                xm.productType = r.getString("productType");
                xm.startTime = Utility.parse(r.getString("startTime"));
                xm.productId = r.getString("product");
                xm.passId = r.getString("pass");
                list.add(xm);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return list;
    }

    private class XMarker
    {
        String gopherColony;
	int status;
	String location;
        String productType;
        java.util.Date startTime;
        String productId;
        String passId;
        boolean selected = false;
    }

    private class MarkersJTableModel extends javax.swing.table.AbstractTableModel
    {
        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String[] columnName =
	{"productId","passId","productType",
	 "startTime","group","site","status"};
        private java.util.List<XMarker> markerList;
        private int[] columnWidth = new int[columnName.length];
        private int rowHeight;

        MarkersJTableModel(java.util.List<XMarker> xmlist)
        {
            markerList = xmlist;
            javax.swing.JLabel work = new javax.swing.JLabel();
             for (int i = 0; i < columnWidth.length; i++)
            {
                work.setText(columnName[i]);
		// The + 10 allows for the sorting arrow's width
                columnWidth[i] = work.getPreferredSize().width + 10;
            }
            work.setText("2006-03-22 22:22:22x");
            columnWidth[3] = work.getPreferredSize().width;
            work.setText("aqua.modis.firedetectionxxx");
            columnWidth[2] = columnWidth[4] = work.getPreferredSize().width;
	    work.setText("NISFES-99999");
	    columnWidth[5] = work.getPreferredSize().width;
            rowHeight = work.getPreferredSize().height;
        }

        public int getRowCount()
        {
            return markerList.size();
        }

        int getRowHeight()
        {
            return rowHeight;
        }

        void updateModel(java.util.List<XMarker> list)
        {
            markerList = list;
            fireTableDataChanged();
        }

        public int getColumnCount()
        {
            return columnName.length;
        }

        int getColumnWidth(int n)
        {
            return columnWidth[n];
        }

        public String getColumnName(int column)
        {
            return columnName[column];
        }

        public Class<?> getColumnClass(int column)
        {
            if(column==0 || column==1)
		return Integer.class;
	    else
		return String.class;
        }

        public boolean isCellEditable(int row, int column)
        {
            return false;
        }

        public void setValueAt(Object obj, int row, int column)
        {
        }

        private XMarker getMarker(int n)
        {
            XMarker m = markerList.get(n);
            return m;
        }

        public Object getValueAt(int row, int column)
        {
            Object value = "";
            XMarker x = markerList.get(row);
            switch (column) {
                case 0:
                    value = new Integer(x.productId);
                    break;
                case 1:
                    value = new Integer(x.passId);
                    break;
                case 2:
                    value = x.productType;
                    break;
                case 3:
                    value = Utility.format(x.startTime);
                    break;
                case 4:
                    value = x.gopherColony;
                    break;
                case 5:
                    value = x.location;
                    break;
                case 6:
		    switch (x.status) {
		    case 0: value = "running"; break;
		    case 1: value = "done"; break;
		    case 2: value = "FAILED"; break;
		    default: value = "?" + x.status + "?"; break;
		    }
                    break;
            }
            return value;
        }
    }

    private class DeleteButton extends JButton implements ActionListener
    {
        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	DeleteButton()
        {
            super("Delete");
            setToolTipText("Delete markers.");
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event)
        {
            try {
		if(markersTable.getSelectedRowCount() != 0) {
		    int selrows[] = markersTable.getSelectedRows();
		    // Translate from view index (changeable via sort)
		    // to model index
		    for(int i=0; i< selrows.length; i++)
			selrows[i] = markersTable.convertRowIndexToModel(selrows[i]);
		    String selrowLabel[] = new String[selrows.length+1];
		    selrowLabel[0] = "About to delete "
			+ selrows.length
			+ " marker"
			// What the heck, get the plural right...
			+ (selrows.length == 1 ? "" : "s")
			+":";
		    for(int i=0; i< selrows.length; i++) {
			XMarker marker = markersModel.getMarker(selrows[i]);
			selrowLabel[i+1] = marker.productId + " " + marker.productType + " " + marker.gopherColony;
		    }
		    if(JOptionPane.showConfirmDialog(MarkersMain.this, selrowLabel, "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			for(int selrow : selrows) {
			    XMarker marker = markersModel.getMarker(selrow);
			    if (marker != null)
				{
				    dsm.update("DELETE FROM Markers WHERE product=" +
					       marker.productId +
					       " AND gopherColony=" +
					       Utility.quote(marker.gopherColony));
				}
			}
			dsm.commit();
			java.util.List<XMarker> markerList = getMarkers();
			markersModel.updateModel(markerList);
			JOptionPane.showMessageDialog(MarkersMain.this,"Selected markers deleted.");
		    }
                }
                else
                {
                    JOptionPane.showMessageDialog(MarkersMain.this,"You must select at least one marker to delete.");
                }
            }
            catch (java.sql.SQLException e)
            {
                e.printStackTrace();
                try { dsm.rollback(); } catch (Exception er) {}
            }
        }
    }

    private class RefreshButton extends JButton implements ActionListener
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		RefreshButton()
        {
            super("Refresh");
            setToolTipText("Refresh table with current database values.");
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event)
        {
            java.util.List<XMarker> markerList = null;
            try
            {
                markerList = getMarkers();
                markersModel.updateModel(markerList);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
}

