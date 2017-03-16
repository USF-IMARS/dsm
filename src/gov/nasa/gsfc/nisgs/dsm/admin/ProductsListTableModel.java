/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import gov.nasa.gsfc.nisgs.dsm.Product;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.util.*;

class ProductsListTableModel extends javax.swing.table.AbstractTableModel
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private XProduct[] productList;
    private static final int VIEW = 0;
    private static final int ID = 1;
    private static final int PRODUCT_TYPE = 2;
    private static final int START_TIME = 3;
    private static final int STOP_TIME = 4;
    private static final int CREATION = 5;
    private static final int DELETE_PROTECTED = 6;
    private static final String[] COLUMN_NAME_POWER = {"View","ID","ProductType",
            "StartTime","StopTime","Created","Protected"};
    private static final String[] COLUMN_NAME_NORMAL = {"View","ID","ProductType",
            "StartTime","StopTime","Created"};
    private boolean powerUser;
    private String[] COLUMN_NAME;
    private int[] columnWidth;
    private int rowHeight;


    ProductsListTableModel(boolean powerUser, List<Product> list)
    {
	this.powerUser = powerUser;
	COLUMN_NAME = (powerUser ? COLUMN_NAME_POWER : COLUMN_NAME_NORMAL);
	columnWidth = new int[COLUMN_NAME.length];
        createDataStructure(list);
        javax.swing.JLabel work = new javax.swing.JLabel();
        for (int i = 0; i < columnWidth.length; i++)
        {
            work.setText(COLUMN_NAME[i]);
            columnWidth[i] = work.getPreferredSize().width;
        }
        work.setText("2006-03-22 22:22:22x");
        columnWidth[START_TIME] = columnWidth[STOP_TIME] = columnWidth[CREATION] = work.getPreferredSize().width;
        work.setText("VIEWx");
        columnWidth[VIEW] = work.getPreferredSize().width;
        work.setText("MMMMM");
        columnWidth[ID] = work.getPreferredSize().width;
        work.setText("aqua.modis.firedetectionxxx");
        columnWidth[PRODUCT_TYPE] = work.getPreferredSize().width;
        rowHeight = work.getPreferredSize().height;
    }

    public int getRowCount()
    {
        return productList.length;
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

    void updateModel(java.util.List<Product> list)
    {
        createDataStructure(list);
        fireTableDataChanged();
    }

    private void createDataStructure(java.util.List<Product> list)
    {
        productList = new XProduct[list.size()];
        int n = 0;
        for (Product product : list)
        {
            productList[n++] = new XProduct(product);
        }
    }

    public Object getValueAt(int row, int column)
    {
        Object value = "";
        XProduct x = productList[row];
        switch (column) {
            case ID:
                value = new Integer(x.product.getId());
                break;
            case PRODUCT_TYPE:
                value = x.product.getProductType();
                break;
            case START_TIME:
                value = x.product.getStartTimeString();
                break;
            case STOP_TIME:
                value = x.product.getStopTimeString();
                break;
            case CREATION:
                value = Utility.format(x.product.getCreationTime());
                break;
            case DELETE_PROTECTED:
                value = x.product.isDeleteProtected()? "YES" : "no";
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
        return column == VIEW;
    }

    public void setValueAt(Object obj, int row, int column)
    {
    }

    Product getProduct(int index)
    {
        return productList[index].product;
    }
    /*
    Product getSelectedProduct(int index)
    {
        XProduct xproduct = productList[index];
        return xproduct.selected? xproduct.product : null;
    }
    */
    private static class XProduct
    {
        Product product;

        XProduct(Product product)
        {
            this.product = product;
        }
    }
}

