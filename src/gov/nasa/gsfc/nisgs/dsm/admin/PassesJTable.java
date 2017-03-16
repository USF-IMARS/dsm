/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import gov.nasa.gsfc.nisgs.dsm.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

class PassesJTable extends javax.swing.JTable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    private boolean powerUser;
	private DSMAdministrator dsm;
    private PassesJTableModel ptm;
    private JFrame parent;

    PassesJTable(boolean powerUser, JFrame parent, PassesJTableModel ptm, DSMAdministrator dsm)
    {
        super(ptm);
	this.powerUser = powerUser;
        this.dsm = dsm;
        this.ptm = ptm;
        this.parent = parent;
        TableColumnModel tcm = getColumnModel();
        int count = tcm.getColumnCount();
        for (int n = 0; n < count; n++)
        {
            tcm.getColumn(n).setPreferredWidth(ptm.getColumnWidth(n));
        }
        PushButtonREditor pbe = new PushButtonREditor("View");
        pbe.blankTextIfDisabled();
        pbe.addActionListener(new PushButtonListener());
        TableColumn tc = tcm.getColumn(PassesJTableModel.VIEW);
        tc.setCellRenderer(pbe);
        tc.setCellEditor(pbe);
        Dimension size = getPreferredSize();
        size.width += 64;
        size.height = Math.min(31,ptm.getRowCount()) * ptm.getRowHeight();
        setPreferredScrollableViewportSize(size);
    }

    private class PushButtonListener implements java.awt.event.ActionListener
    {
        public void actionPerformed(java.awt.event.ActionEvent event)
        {
            JComponent s = (JComponent)event.getSource();
            Integer i = (Integer)s.getClientProperty("TABLEROW");
            if (i == null) return;
	    // Map from view row space to model row space
	    i = convertRowIndexToModel(i);
            Pass pass = ptm.getPass(i);
            try
            {
                ProductsListWindow pw = new ProductsListWindow(parent,
							       "Products for Pass " + pass.getId(),
							       powerUser,dsm,pass);
                pw.setVisible(true);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
