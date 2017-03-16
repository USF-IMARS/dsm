/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import javax.swing.table.TableCellRenderer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import java.awt.*;

/**
 * This class is a table cell renderer and editor. It presents a push button.
 * You should attach an actionListener to this class.
 * Use event.getSource().getClientProperty("TABLEROW"); to get the row number
 * as an Integer.
 */
public class PushButtonREditor extends AbstractTableCellEditor
        implements TableCellRenderer
{
    private static final Dimension BIG = new Dimension(0x0ffff,0x0ffff);
    private static final Insets MARGIN = new Insets(0,0,0,0);
    private JButton cbutton;
    private JLabel clabel;
    private Color background;        //JLabel's standard background
    private Color foreground;        //JLabel's standard foreground
    private Color neForeground = null;
    private Color neBackground = null;
    private boolean blankText = false;

    public PushButtonREditor(String text)
    {
        cbutton = new JButton(text);
        cbutton.setMaximumSize(BIG);
        cbutton.setMargin(MARGIN);
        cbutton.setOpaque(true);
        cbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                stopCellEditing();
            }
        });
        cbutton.setForeground(Color.black);
        cbutton.setBorder(BorderFactory.createRaisedBevelBorder());

        clabel = new JLabel(text);
        background = clabel.getBackground();
        foreground = clabel.getForeground();
        clabel.setBorder(BorderFactory.createRaisedBevelBorder());
        clabel.setOpaque(true);
        clabel.setHorizontalAlignment(JLabel.CENTER);
    }

    //if color is null, it uses the default foreground or background color.
    public final void setNoEditForeground(Color c)
    {
        neForeground = c;
    }

    public final void setNoEditBackground(Color c)
    {
        neBackground = c;
    }

    public void blankTextIfDisabled()
    {
        blankText = true;
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column)
    {
        if (value != null)
        {
            boolean edit = table.isCellEditable(row,column);
            clabel.setEnabled(edit);
            Color fg = foreground;
            Color bg = background;
            if (isSelected)
            {
                fg = table.getSelectionForeground();
                bg = table.getSelectionBackground();
            }
            if (!edit)
            {
                if (neForeground != null) fg = neForeground;
                if (neBackground != null) bg = neBackground;
            }
            if (blankText)
            {
                clabel.setText(edit? cbutton.getText() : "");
            }
            clabel.setForeground(fg);
            clabel.setBackground(bg);
            clabel.setFont(table.getFont());
        }
        return clabel;
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column)
    {
        cbutton.putClientProperty("TABLEROW",new Integer(row));
        return cbutton;
    }

    public Object getCellEditorValue()
    {
        return cbutton.getText();
    }

    public boolean shouldSelectCell(java.util.EventObject anEvent)
    {
        cbutton.doClick();
        return true;
    }

    public void addActionListener(ActionListener al)
    {
        cbutton.addActionListener(al);
    }

    public void removeActionListener(ActionListener al)
    {
        cbutton.removeActionListener(al);
    }
}
