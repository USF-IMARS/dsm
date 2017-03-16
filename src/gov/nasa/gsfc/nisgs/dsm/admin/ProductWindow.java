/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import gov.nasa.gsfc.nisgs.dsm.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.html.HTMLEditorKit;
import java.util.TreeSet;
import java.util.Collection;
import gov.nasa.gsfc.nisgs.properties.Utility;

class ProductWindow extends javax.swing.JDialog
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	ProductWindow(java.awt.Frame parent, String title, DSMAdministrator dsm,
            Product product)
    {
        super(parent,title,false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);

        HTMLEditorKit editor = new HTMLEditorKit();
        JEditorPane ep = new JEditorPane();
        ep.setEditorKit(editor);

        String text = createProductString(dsm,product);
        ep.setText(text);
        ep.setBorder(BorderFactory.createMatteBorder(16,16,16,16,java.awt.Color.gray));

	// IPOPP 2.4: Create a JScrollPane to make product windows scrollable
	JScrollPane scrp = new JScrollPane(ep);

        //JPanel bottom = new JPanel(new FlowLayout());
        //bottom.add(new Closer());

        java.awt.Container cp = getContentPane();
        cp.setLayout(new java.awt.BorderLayout());
        //getContentPane().add(ep,BorderLayout.CENTER);

	// IPOPP 2.4: Add the JScrollPane to the Java Container
	getContentPane().add(scrp,BorderLayout.CENTER);

        //cp.add(bottom,BorderLayout.SOUTH);
        pack();
    }

    private String createProductString(DSMAdministrator dsm, Product product)
    {
        StringBuffer sb = new StringBuffer(2048);
        sb.append("<html><body><table border=1>");
        sb.append("<tr><td>ID<td>");
        sb.append(product.getId());
        sb.append("<tr><td>product type<td>");
        sb.append(product.getProductType());
        sb.append("<tr><td>start time<td>");
        sb.append(product.getStartTimeString());
        sb.append("<tr><td>stop time<td>");
        sb.append(product.getStopTimeString());
        sb.append("<tr><td>creation<td>");
        sb.append(Utility.format(product.getCreationTime()));
        appendPass(sb,product.getPass());
        sb.append("<tr><td>delete protected<td>");
        sb.append(product.isDeleteProtected()? "yes" : "no");
        sb.append("<tr><td>marker ID<td>");
        sb.append(product.getMarkerId());
        sb.append("<tr><td>agent<td>");
        sb.append(product.getAgent());
        sb.append("<tr><td>algorithm<td>");
        sb.append(product.getAlgorithm());
        sb.append("<tr><td>algorithm version<td>");
        sb.append(product.getAlgorithmVersion());
        appendSubproduct(sb,product);
        sb.append("</table><p>");
        appendResourcesTable(sb,product);
        appendContributingResources(sb,product);
        appendContributingProducts(sb,product,dsm);
        return sb.toString();
    }

    private void appendPass(StringBuffer sb, Pass pass)
    {
        if (pass != null)
        {
            sb.append("<tr><td>pass ID<td>");
            sb.append(pass.getId());
            sb.append("<tr><td>spacecraft<td>");
            sb.append(pass.getSpacecraft());
            sb.append("<tr><td>station<td>");
            sb.append(pass.getStationName());
            sb.append("<tr><td>pass AOS<td>");
            sb.append(pass.getAos());
            sb.append("<tr><td>pass LOS<td>");
            sb.append(pass.getLos());
        }
        else
        {
            sb.append("<tr><td>pass ID<td>No pass!");
        }
    }

    private void appendContributingResources(StringBuffer sb, Product product)
    {
        Collection<Resource> crlist = product.getContributingResources();
        if (crlist == null)
        {
            sb.append("This product does not have contributing resources.<p>");
        }
        else
        {
            sb.append("<table border=1><caption align=left>Contributing Resources</caption>");
            sb.append("<tr><th>name<th>description");
            for (Resource r : crlist)
            {
                sb.append("<tr><td>");
                sb.append(r.getName());
                sb.append("<td>");
                if (r.getDescription() != null)
                {
                    sb.append(r.getDescription());
                }
            }
            sb.append("</table><p>");
        }
    }

    private void appendSubproduct(StringBuffer sb, Product product)
    {
        if (product.getSubproduct() != null)
        {
            Collection<Attribute> alist = product.getAttributes();
            for (Attribute a : alist)
            {
                if (a.getName().equals("product")) continue;
                sb.append("<tr><td>");
                sb.append(a.getName());
                sb.append("<td>");
                sb.append(a.getValue());
            }
        }
    }

    private void appendResourcesTable(StringBuffer sb, Product product)
    {
        sb.append("<table border=1><caption align=left>Resources</caption>");
        Collection<Resource> rlist = product.getResources();
        sb.append("<tr><th>ID<th>key<th>name<th>description");
        for (Resource r : rlist)
        {
            sb.append("<tr><td>");
            sb.append(r.getId());
            sb.append("<td>");
            sb.append(r.getKey());
            sb.append("<td>");
            sb.append(r.getName());
            sb.append("<td>");
            if (r.getDescription() != null)
            {
                sb.append(r.getDescription());
            }
        }
        sb.append("</table><p>");
    }

    private void appendContributingProducts(StringBuffer sb, Product product,
            DSMAdministrator dsm)
    {
        if (product.getContributingProductsIds() == null)
        {
            sb.append("This product does not have contributing products.<p>");
        }
        else
        {
            TreeSet<Product> ancestors = new TreeSet<Product>();
            try
            {
                addAncestors(product,ancestors,dsm);
            }
            catch (Exception e)
            {
                System.err.println("Error getting product ancestors for " + product);
                e.printStackTrace();
            }
            sb.append("<table border=1><caption align=left>Contributing Products</caption>");
            sb.append("<tr><th>ID<th>product type<th>start<th>s/c<th>station");
            sb.append("<th>algorithm<th>pass ID");
            for (Product p : ancestors)
            {
                sb.append("<tr><td>");
                sb.append(p.getId());
                sb.append("<td>");
                sb.append(p.getProductType());
                sb.append("<td>");
                sb.append(p.getStartTimeString());
                sb.append("<td>");
                sb.append(p.getSpacecraft());
                sb.append("<td>");
                sb.append(p.getStation());
                sb.append("<td>");
                sb.append(p.getAlgorithm());
                sb.append(" ");
                sb.append(p.getAlgorithmVersion());
                sb.append("<td>");
                sb.append(p.getPass().getId());
            }
            sb.append("</table>");
        }
    }

    private void addAncestors(Product product, TreeSet<Product> ancestors,
            DSMAdministrator dsm) throws Exception
    {
        Collection<String> pids = product.getContributingProductsIds();
        if (pids != null)
        {
            for (String pid : pids)
            {
                Product p = dsm.getProduct(pid);
                if (p != null)
                {
                    ancestors.add(p);
                    addAncestors(p,ancestors,dsm);
                }
            }
        }
    }

    private class Closer extends JButton implements ActionListener
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Closer()
        {
            super("Close");
            setToolTipText("Close this window.");
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event)
        {
            ProductWindow.this.dispose();
        }
    }
}
