/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import gov.nasa.gsfc.nisgs.dsm.Pass;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.util.*;

class PassesJTableModel extends javax.swing.table.AbstractTableModel
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private XPass[] passList;
    public static final int VIEW = 0;
    //private static final int SELECT = 1;
    private static final int ID = 1;
    private static final int SPACECRAFT = 2;
    private static final int AOS = 3;
    private static final int LOS = 4;
    private static final int DELETE_PROTECTED = 5;
    private static final String[] COLUMN_NAME_POWER = {"View","ID","Spacecraft",
            "AOS","LOS","Prot"};
    private static final String[] COLUMN_NAME_NORMAL = {"View","ID","Spacecraft",
            "AOS","LOS"};
    private boolean powerUser;
    private String[] COLUMN_NAME;
    private int[] columnWidth;
    private int rowHeight;


    PassesJTableModel(boolean powerUser, List<Pass> list, List<Integer> busyPasses)
    {
	this.powerUser = powerUser;
	COLUMN_NAME = (powerUser ? COLUMN_NAME_POWER : COLUMN_NAME_NORMAL);
	columnWidth = new int[COLUMN_NAME.length];
        createDataStructure(list,busyPasses);
        javax.swing.JLabel work = new javax.swing.JLabel();
         for (int i = 0; i < columnWidth.length; i++)
        {
            work.setText(COLUMN_NAME[i]);
	    // The + 10 below allows for the sorting arrow's width
            columnWidth[i] = work.getPreferredSize().width + 10;
        }
        work.setText("2006-03-22 22:22:22x");
        columnWidth[AOS] = columnWidth[LOS] = work.getPreferredSize().width;
        work.setText("VIEWx");
        columnWidth[VIEW] = work.getPreferredSize().width;
        work.setText("MMMMM");
        columnWidth[ID] = work.getPreferredSize().width;
        rowHeight = work.getPreferredSize().height;
    }

    public int getRowCount()
    {
        return passList.length;
    }

    public int getColumnCount()
    {
        return COLUMN_NAME.length;
    }

    public int getColumnWidth(int n)
    {
        return columnWidth[n];
    }

    int getRowHeight()
    {
        return rowHeight;
    }

    void updateModel(java.util.List<Pass> list, List<Integer> busyPasses)
    {
        createDataStructure(list,busyPasses);
        fireTableDataChanged();
    }

    private void createDataStructure(java.util.List<Pass> list,
            java.util.List<Integer> busyPasses)
    {
        int n = list.size();
        passList = new XPass[n];
        for (Pass pass : list)
        {
            passList[--n] = new XPass(pass);
        }
        for (Integer i : busyPasses)
        {
            for (XPass x : passList)
            {
                if (i.toString().equals(x.pass.getId()))
                {
                    x.busy = true;
                }
            }
        }
    }

    public Object getValueAt(int row, int column)
    {
        Object value = "";
        XPass x = passList[row];
        switch (column) {
            case ID:
                value = new Integer(x.pass.getId());
                break;
            case SPACECRAFT:
                value = x.pass.getSpacecraft();
                break;
            case AOS:
                value = Utility.format(x.pass.getAos());
                break;
            case LOS:
                value = Utility.format(x.pass.getLos());
                break;
            case DELETE_PROTECTED:
                value = x.pass.isDeleteProtected()? "YES" : "no";
                break;
        }
        return value;
    }

    public Class<?> getColumnClass(int column)
    {
        Class c = String.class;
        if (column == ID) c = Integer.class;
        return c;
    }

    public String getColumnName(int column)
    {
        return COLUMN_NAME[column];
    }

    public boolean isCellEditable(int row, int column)
    {
        return /*column == SELECT ||*/ (column == VIEW && isBusy(row));
    }

    public void setValueAt(Object obj, int row, int column)
    {
    }

    Pass getPass(int row)
    {
        return passList[row].pass;
    }

    boolean isBusy(int row)
    {
        return passList[row].busy;
    }

    private static class XPass
    {
        Pass pass;
        boolean busy = false;

        XPass(Pass pass)
        {
            this.pass = pass;
        }
    }
}
