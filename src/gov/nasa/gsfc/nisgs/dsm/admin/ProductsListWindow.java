/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import gov.nasa.gsfc.nisgs.dsm.*;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

class ProductsListWindow extends javax.swing.JDialog
{
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    private static final int BUTTONgroupSPACE = 40;
    private ProductsListTableModel productsListTableModel;
    JTable productsListTable;
    private Frame parent;
    private boolean powerUser;
    private DSMAdministrator dsm;
    private DsmProperties dsmp = null;
    private FileMover fm = null;
    private Pass pass;

    ProductsListWindow(java.awt.Frame parent, String title, boolean powerUser, DSMAdministrator dsm,
            Pass pass) throws Exception
    {
        super(parent,title,false);
        this.parent = parent;
	this.powerUser = powerUser;
        this.dsm = dsm;
        this.pass = pass;
	dsmp = new DsmProperties();
	fm = FileMover.newMover(dsmp.getSite(), dsmp.getIS_Site(), dsmp);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);

        String sql = "SELECT * FROM Products WHERE pass=" + pass.getId();
        java.util.List<Product> productList = dsm.queryProducts(sql);
        productsListTableModel = new ProductsListTableModel(powerUser,productList);
        productsListTable = new JTable(productsListTableModel);
	// Automatic table sorting seems to be good enough here...
	productsListTable.setAutoCreateRowSorter(true);
        TableColumnModel tableColumnModel = productsListTable.getColumnModel();
        int count = tableColumnModel.getColumnCount();
        for (int n = 0; n < count; n++)
        {
            tableColumnModel.getColumn(n).setPreferredWidth(
                        productsListTableModel.getColumnWidth(n));
        }
        PushButtonREditor pbe = new PushButtonREditor("View");
        pbe.addActionListener(new PushButtonListener());
        TableColumn tc = tableColumnModel.getColumn(PassesJTableModel.VIEW);
        tc.setCellRenderer(pbe);
        tc.setCellEditor(pbe);
        Dimension size = productsListTable.getPreferredSize();
        size.width += 64;
        size.height = Math.min(31,productsListTableModel.getRowCount()) *
                productsListTableModel.getRowHeight();
        productsListTable.setPreferredScrollableViewportSize(size);
        JScrollPane scrollPane = new JScrollPane(productsListTable);
        scrollPane.setBorder(BorderFactory.createMatteBorder(16,16,16,16,Color.gray));

        JPanel bottom = new JPanel(new FlowLayout());
        bottom.add(new RefreshButton());
	// Only show these buttons to power users
	if(powerUser) {
	    // Add a little space between these buttons
	    Dimension fillerDim = new Dimension(BUTTONgroupSPACE, 1);
	    bottom.add(new Box.Filler(fillerDim, fillerDim, fillerDim));
	    bottom.add(new ProtectButton(true));
	    bottom.add(new ProtectButton(false));
	    bottom.add(new DeleteProductsButton());
	}

        java.awt.Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(scrollPane,BorderLayout.CENTER);
        cp.add(bottom,BorderLayout.SOUTH);
        pack();
    }

    private int [] selectedRows() {
	int selrows[] = productsListTable.getSelectedRows();
	// Translate from view index (changeable via sort)
	// to model index
	for(int i=0; i< selrows.length; i++)
	    selrows[i] = productsListTable.convertRowIndexToModel(selrows[i]);
	return selrows;
    }

    private class PushButtonListener implements java.awt.event.ActionListener
    {
        public void actionPerformed(ActionEvent event)
        {
            JComponent s = (JComponent)event.getSource();
            Integer i = (Integer)s.getClientProperty("TABLEROW");
            if (i == null) return;
	    // TABLEROW property is view index; as usual we want model index
	    int modelIndex = productsListTable.convertRowIndexToModel(i);
            Product product = productsListTableModel.getProduct(modelIndex);
            ProductWindow pw = new ProductWindow(parent,product.toString(),dsm,product);
            pw.setVisible(true);
        }
    }

    /**
     * Helper method usable anytime someone wants a complete table
     * refresh
     */
    public void refreshTable()
    {
	java.util.List<Product> productList = null;
	try {
	    productList = dsm.queryProducts("SELECT * FROM Products WHERE pass=" + pass.getId());
	    productsListTableModel.updateModel(productList);
	    // If the productList is empty, we might have deleted the pass,
	    // and the main window should hear about it
	    if(productList.size() == 0) {
		// There is probably a better way to do this...
		if(parent instanceof PassManager)
		    ((PassManager)parent).refreshTable();
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	    System.exit(0);
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
	    refreshTable();
	}
    }

    private class DeleteProductsButton extends JButton implements ActionListener
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		DeleteProductsButton()
        {
            super("Delete");
            setToolTipText("Delete products");
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event)
        {
            try {
		int protectedCount = 0;
		int selectedCount = 0;
		int selrows[] = selectedRows();
		// Filter out protected passes
		List<Product> safeProduct = new ArrayList<Product>();
		for (int n : selrows) {
                    Product product = productsListTableModel.getProduct(n);
                    if (product != null) {
                        ++selectedCount;
			if(product.isDeleteProtected())
			    ++protectedCount;
			else
			    safeProduct.add(product);
		    }
		}
		String dialog[]= new String[2];
		String productIDs = "";
		if(safeProduct.size() > 0) {
		    // Do the usual "last chance to not do this" thing
		    for (Product product : safeProduct) {
			productIDs += " " + product.getId();
		    }
		    dialog[0] = "About to delete";
		    dialog[1] = productIDs;
		    if(JOptionPane.showConfirmDialog(ProductsListWindow.this, dialog, "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			Statement stmt = dsm.getConnection().createStatement();
			try {
			    for (Product prod : safeProduct) {
				ProductFactory.removeOneProduct(stmt, prod.getId().toString(), fm);
			    }
			    dsm.commit();
			    dialog[0] = "Files and product records for these products are deleted:";
			    dialog[1] = productIDs;
			    JOptionPane.showMessageDialog(ProductsListWindow.this, dialog);
			    // Update the product window globally - simplest
			    // way is to pretend to push the Refresh button
			    refreshTable();
			}
			finally {
			    stmt.close();
			}
		    }
		}
                else {
		    dialog[0] = "No products deleted";
		    dialog[1] = (protectedCount == selectedCount && protectedCount == 0 ?
				 "(none selected)"
				 : "(all protected)");
		    JOptionPane.showMessageDialog(ProductsListWindow.this, dialog,
						  "OOPS",
						  JOptionPane.WARNING_MESSAGE);
                }
            }
            catch (java.sql.SQLException e)
            {
                e.printStackTrace();
                try { dsm.rollback(); } catch (Exception er) {}
		JOptionPane.showMessageDialog(ProductsListWindow.this,
					      "ERROR dumped to stderr",
					      "OUCH",
					      JOptionPane.ERROR_MESSAGE);
            }
            catch (Exception e)
            {
                e.printStackTrace();
		JOptionPane.showMessageDialog(ProductsListWindow.this,
					      "ERROR dumped to stderr",
					      "OUCH",
					      JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class ProtectButton extends JButton implements ActionListener
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	// Controls whether we are protecting/unprotecting products
	private boolean protectP;
	private String opString;

		ProtectButton(boolean which)
        {
            super((which ? "Protect" : "Unprotect"));
	    protectP = which;
	    opString = (which ? "Protect" : "Unprotect");
            setToolTipText("Protect/unprotect products from normal deletion.");
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event)
        {
            try
		{
		int protectedCount = 0;
		int selectedCount = 0;
		int selrows[] = selectedRows();
		List<Product> safeProduct = new ArrayList<Product>();
		for (int n : selrows) {
		    Product product = productsListTableModel.getProduct(n);
		    if(product != null) {
			++selectedCount;
			if(product.isDeleteProtected() == protectP)
			    ++protectedCount;
			else
			    safeProduct.add(product);
		    }
		}
		String productIDs = "";
		if(safeProduct.size() > 0) {
		    // Do the usual "last chance to not do this" thing
		    for (Product product : safeProduct) {
			productIDs += " " + product.getId();
		    }
		    String dialog[]= new String[2];
		    dialog[0] = "About to " + opString;
		    dialog[1] = productIDs;
		    if (JOptionPane.showConfirmDialog(ProductsListWindow.this, dialog, "Confirm " + opString, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			for (Product product : safeProduct) {
			    dsm.update("UPDATE Products SET deleteProtected=" +
				       (protectP ? "1" : "0") +
				       " WHERE id=" + product.getId());
                        product.setDeleteProtected(protectP);
			}
			dsm.commit();
			for (int n : selrows) {
			    productsListTableModel.fireTableRowsUpdated(n,n);
			}
			dialog[0] = "Selected products are";
			dialog[1] = opString + "ed";
			JOptionPane.showMessageDialog(ProductsListWindow.this,dialog);
		    }
                }
                else
		    {
			JOptionPane.showMessageDialog(ProductsListWindow.this,
						      (protectedCount == selectedCount && protectedCount == 0 ?
						       "You must select at least one product to " + opString
						       : "All selections are already " + opString + "ed"));
		    }
            }
            catch (java.sql.SQLException e)
            {
                e.printStackTrace();
                try { dsm.rollback(); } catch (Exception er) {}
            }
        }
    }
}
