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
import java.util.Set;
import java.util.HashSet;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class PassManager extends javax.swing.JFrame
{
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    private static final int BUTTONgroupSPACE = 40;

    public static void main(String[] args)
    {
	boolean powerUser = false;
	for (String s : args)
	    if("-powerUser".equals(s))
		powerUser = true;
        new PassManager(powerUser);
    }

    private boolean powerUser;
    private DSMAdministrator dsm = null;
    private DsmProperties dsmp = null;
    private FileMover fm = null;
    private PassesJTableModel passesTableModel;
    private PassesJTable ptable;

    public PassManager(boolean powerUser)
    {
	this.powerUser = powerUser;

        setTitle("Pass Manager");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        java.util.List<Pass> passList = null;
        try
        {
            dsm = new DSMAdministrator("Admin","PassManager");
	    dsmp = new DsmProperties();
	    fm = FileMover.newMover(dsmp.getSite(), dsmp.getIS_Site(), dsmp);
            passList = dsm.getPasses();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(0);
        }

        passesTableModel = new PassesJTableModel(powerUser, passList,getBusyPasses());
        ptable = new PassesJTable(powerUser,this,passesTableModel,dsm);
	// Automatic table sorting seems to be good enough here...
	ptable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(ptable);
        scrollPane.setBorder(BorderFactory.createMatteBorder(16,16,16,16,Color.gray));

        JPanel bottom = new JPanel(new FlowLayout());
        bottom.add(new RefreshPassesJTableButton());
	// Add a little space between these buttons
	Dimension fillerDim = new Dimension(BUTTONgroupSPACE, 1);
	bottom.add(new Box.Filler(fillerDim, fillerDim, fillerDim));
	// Only show these to power users
	if(powerUser) {
	    bottom.add(new ProtectPassesButton(true));
	    bottom.add(new ProtectPassesButton(false));
	}
        bottom.add(new ReprocessPassProductsButton());
        bottom.add(new DeletePassProductsButton());

        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(scrollPane,BorderLayout.CENTER);
        cp.add(bottom,BorderLayout.SOUTH);
        pack();

        //ImageIcon icon = new ImageIcon(getClass().getResource("images/globe.gif"));
        //setIconImage(icon.getImage());

        //This fragment centers the frame on the screen.
        java.awt.Dimension size = getSize();
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

    private java.util.List<Integer> getBusyPasses()
    {
        ArrayList<Integer> list = new ArrayList<Integer>(100);
        try
        {
            java.sql.ResultSet r = dsm.query("SELECT DISTINCT id FROM Passes");
            while (r.next())
            {
                int id = r.getInt(1);
                list.add(new Integer(id));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return list;
    }

    private int [] selectedRows() {
	int selrows[] = ptable.getSelectedRows();
	// Translate from view index (changeable via sort)
	// to model index
	for(int i=0; i< selrows.length; i++)
	    selrows[i] = ptable.convertRowIndexToModel(selrows[i]);
	return selrows;
    }

    private class DeletePassProductsButton extends JButton implements ActionListener
    {
        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	DeletePassProductsButton()
        {
            super("Delete");
            setToolTipText("Delete all product records for this pass from database.");
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event)
        {
		// IPOPP 2.4: Run this as a new Thread to enable ProgressMonitor and not have it blocked
		new Thread(){
			public void run(){
			    try {
				int protectedCount = 0;
				int selectedCount = 0;
				int selrows[] = selectedRows();
				List<Pass> safePass = new ArrayList<Pass>();
				for (int n : selrows) {
				    Pass pass = passesTableModel.getPass(n);
				    if(pass != null
				       && passesTableModel.isBusy(n)) {
					   ++selectedCount;
					   if(pass.isDeleteProtected())
					       ++protectedCount;
					   else
					       safePass.add(pass);
				    }
				}
				String dialog[]= new String[2];
				if(safePass.size() > 0) {
				    // IPOPP 2.4: Create the SimpleProgressMonitor
				    SimpleProgressMonitor spm = new SimpleProgressMonitor(PassManager.this, "Deleting Passes...", "", safePass.size());
				    int passCounter = 0;
				    // Do the usual "last chance to not do this" thing
				    dialog[0] = "About to delete "+ safePass.size() + " pass(es). Continue?";
			    	    dialog[1] = "In cross-granule mode, dependent products will be deleted.";
				    if(JOptionPane.showConfirmDialog(PassManager.this, dialog, "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				    // This may take some time, so blip the cursor
				    Cursor oldCursor = getCursor();
				    try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				    for (Pass pass : safePass) {
					Connection connection = dsm.getConnection();
					// Collect the products to be deleted.  Start with the roots
					Set<String> deleters = ProductFactory.rootProducts(connection, pass.getId());
					// Then collect all the children
					Set<String> deleterKids = new HashSet<String>();
					ProductFactory.addAllChildren(connection, deleters, deleterKids);
					// Collect the markers to be removed
					Set<MarkerRef> mrs = new HashSet<MarkerRef>();
					Statement stmt = connection.createStatement();
					try {
					    for (String pid : deleters) {
						MarkerRef mr = ProductFactory.findControllingMarker(stmt, pid);
						if(mr != null) {
						    mrs.add(mr);
						}
					    }
					    for (String pid : deleterKids) {
						MarkerRef mr = ProductFactory.findControllingMarker(stmt, pid);
						if(mr != null) {
						    mrs.add(mr);
						}
					    }
					}
					finally {
					    stmt.close();
					}
					// Delete the products
					ProductFactory.removeProducts(connection, deleters, fm);
					// Delete any markers that didn't get nailed already
					// (only for products outside the current pass, really)
					stmt = connection.createStatement();
					try {
					    for (MarkerRef mr : mrs) {
						ProductFactory.deleteMarker(stmt, mr);
					    }
					}
					finally {
					    stmt.close();
					}
					// Belt and suspenders time - if somehow a Pass has
					// no products, removeProducts() won't delete it, so...
					dsm.update("DELETE FROM Passes WHERE id="+pass.getId());

					// IPOPP 2.4: Increase the ProgressMonitor's progress:
					passCounter++;
					spm.bumpState(passCounter);
				    }
				    dsm.commit();
				    }
				    finally {
					setCursor(oldCursor);
				    }
				    // IPOPP 2.4: Finally, close the ProgressMonitor
				    spm.close();
				    dialog[0] = "Files and product records for " + safePass.size() + " pass(es) were successfully deleted.";
			    	    dialog[1] = "";
				    JOptionPane.showMessageDialog(PassManager.this,dialog);
				    }
				}
				else {
				    dialog[0] = "No passes deleted";
				    dialog[1] = (protectedCount == selectedCount && protectedCount == 0 ?
						 "(none selected)"
						 : "(all protected)");
				    JOptionPane.showMessageDialog(PassManager.this, dialog,
								  "WARNING",
								  JOptionPane.WARNING_MESSAGE);
				}
			    }
			    catch (java.sql.SQLException sqle)
			    {
				sqle.printStackTrace();
				try { dsm.rollback(); } catch (Exception er) {}
			    }
			    catch (Exception e)
			    {
				e.printStackTrace();
			    }
			} // end run
		}.start(); // end Thread
        } // end ActionPerformed
    }

    private class ReprocessPassProductsButton extends JButton implements ActionListener
    {
        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	ReprocessPassProductsButton()
        {
            super("Reprocess");
            setToolTipText("Reprocess all products for this pass from raw data.");
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event) {
		new Thread(){
			public void run(){
			    try {
				int protectedCount = 0;
				int selectedCount = 0;
				int selrows[] = selectedRows();
				// Filter out protected passes
				List<Pass> safePass = new ArrayList<Pass>();
				for (int n : selrows) {
				    Pass pass = passesTableModel.getPass(n);
				    if(pass != null
				       && passesTableModel.isBusy(n)) {
					   ++selectedCount;
					   if(pass.isDeleteProtected())
					       ++protectedCount;
					   else
					       safePass.add(pass);
				    }
				}
				String dialog[]= new String[2];
				if(safePass.size() > 0) {
				    // IPOPP 2.4: Create the SimpleProgressMonitor
				    SimpleProgressMonitor spm = new SimpleProgressMonitor(PassManager.this, "Reprocessing Passes...", "", safePass.size());
				    int passCounter = 0;
				    // Do the usual "last chance to not do this" thing
				    dialog[0] = "About to reprocess " + safePass.size() + " pass(es). Continue?";
				    dialog[1] = "";
				    if(JOptionPane.showConfirmDialog(PassManager.this, dialog, "Confirm Reprocess", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					// This may take some time, so blip the cursor
					Cursor oldCursor = getCursor();
					try {
					    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					    for (Pass pass : safePass) {
						Connection connection = dsm.getConnection();
						// Get the list of root products
						Set<String> rootIDs = ProductFactory.rootProducts(connection, pass.getId());
						// Use it to generate the list of children of the roots
						Set<String> rootChildProducts = new HashSet<String>();
						for (String r : rootIDs) {
						    ProductFactory.addChildren(connection, r, rootChildProducts);
						}
						// And the list of everybody else to be deleted
						Set<String> otherChildren = new HashSet<String>();
						ProductFactory.addAllChildren(connection, rootChildProducts, otherChildren);
						// Get the list of MarkerRefs for the markers we're
						// about to remove
						Set<MarkerRef> mrs = new HashSet<MarkerRef>();
						Statement stmt = connection.createStatement();
						try {
						    for (String pid : rootChildProducts) {
							MarkerRef mr = ProductFactory.findControllingMarker(stmt, pid);
							if(mr != null) {
							    mrs.add(mr);
							}
						    }
						    for (String pid : otherChildren) {
							MarkerRef mr = ProductFactory.findControllingMarker(stmt, pid);
							if(mr != null) {
							    mrs.add(mr);
							}
						    }
						}
						finally {
						    stmt.close();
						}
						// Delete the child products
						ProductFactory.removeProducts(connection, rootChildProducts, fm);
						// Finally, remove the markers
						stmt = connection.createStatement();
						try {
						    for (String r : rootIDs) {
							ProductFactory.deleteMarkersOnProduct(stmt, r);
						    }
						    for (MarkerRef mr : mrs) {
							ProductFactory.deleteMarker(stmt, mr);
						    }
						}
						finally {
						    stmt.close();
						}
						// for (String rid : rootIDs) {
						//     dsm.update("DELETE FROM Markers where product = "
						// 	       + rid);
						// }
						dsm.commit();
						
						// IPOPP 2.4: Increase the ProgressMonitor's progress
						passCounter++;
						spm.bumpState(passCounter);
					    }
					}
				    finally {
					setCursor(oldCursor);
				    }
				    // IPOPP 2.4: Finally, close the ProgressMonitor
				    spm.close();
				    String[] message = {"Files and product records for " + safePass.size() + " pass(es) were deleted.",
							"Reprocessing will start immediately if SPA services are running."};
				    JOptionPane.showMessageDialog(PassManager.this,message);
				    }
				}
				else {
				    dialog[0] = "No passes reprocessed";
				    dialog[1] = (protectedCount == selectedCount && protectedCount == 0 ?
						 "(none selected)"
						 : "(all protected)");
				    JOptionPane.showMessageDialog(PassManager.this, dialog,
								  "WARNING",
								  JOptionPane.WARNING_MESSAGE);
				}
			    }
			    catch (java.sql.SQLException sqle) {
				sqle.printStackTrace();
				try { dsm.rollback(); } catch (Exception er) {}
			    }
			    catch (Exception e) {
				e.printStackTrace();
			    }
			} //end run
		}.start(); // end Thread
        } // end actionPerformed
    }

    /**
     * Helper method usable anytime someone wants a complete table
     * refresh
     */
    public void refreshTable()
    {
	java.util.List<Pass> passList = null;
	try
            {
                passList = dsm.getPasses();
                passesTableModel.updateModel(passList,getBusyPasses());
            }
	catch (Exception e)
            {
                e.printStackTrace();
                System.exit(0);
            }
    }
    private class RefreshPassesJTableButton extends JButton implements ActionListener
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		RefreshPassesJTableButton()
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

    private class ProtectPassesButton extends JButton implements ActionListener
    {
        /**
		 * 
		 */
	private static final long serialVersionUID = 1L;
	// Controls whether we are protecting/unprotecting passes
	private boolean protectP;
	private String opString;

	ProtectPassesButton(boolean which)
        {
            super((which ? "Protect" : "Unprotect"));
	    protectP = which;
	    opString = (which ? "Protect" : "Unprotect");
            setToolTipText(opString + " passes from normal deletion.");
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event)
        {
            try
            {
		int protectedCount = 0;
		int selectedCount = 0;
		int selrows[] = selectedRows();
		List<Pass> safePass = new ArrayList<Pass>();
		for (int n : selrows) {
		    Pass pass = passesTableModel.getPass(n);
		    if(pass != null
		       && passesTableModel.isBusy(n)) {
			++selectedCount;
			if(pass.isDeleteProtected() == protectP)
			    ++protectedCount;
			else
			    safePass.add(pass);
		    }
		}
		String passIDs = "";
		if(safePass.size() > 0) {
		    // Do the usual "last chance to not do this" thing
		    for (Pass pass : safePass) {
			passIDs += " " + pass.getId();
		    }
		    String dialog[]= new String[2];
		    dialog[0] = "About to " + opString;
		    dialog[1] = passIDs;
		    if (JOptionPane.showConfirmDialog(PassManager.this, dialog, "Confirm " + opString, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			for (Pass pass : safePass) {
			    dsm.update("UPDATE Passes SET deleteProtected=" +
				       (protectP ? "1" : "0") +
				       " WHERE id="+ pass.getId());
			    pass.setDeleteProtection(protectP);
			}
			dsm.commit();
			for (int n : selrows) {
			    passesTableModel.fireTableRowsUpdated(n,n);
			}
			dialog[0] = "Selected passes are";
			dialog[1] = opString + "ed";
			JOptionPane.showMessageDialog(PassManager.this,dialog);
		    }
                }
                else
		    {
			JOptionPane.showMessageDialog(PassManager.this,
						      (protectedCount == selectedCount && protectedCount == 0 ?
						       "You must select at least one pass to " + opString
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
