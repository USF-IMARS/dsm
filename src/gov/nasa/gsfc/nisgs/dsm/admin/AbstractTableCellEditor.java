/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import java.util.EventObject;
import javax.swing.event.EventListenerList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.CellEditorListener;

/**
 * This class is the base class of my table cell editors. I wrote it because
 * I think the swing DefaultCellEditor is poorly written and is awkward to
 * extend to other components. This class provides default implementation for
 * all but the following methods:
 * <pre>
 * public Object getCellEditorValue();
 * public Component getTableCellEditorComponent(JTable table, Object value,
 *    boolean isSelected, int row, int column);
 */
public abstract class AbstractTableCellEditor implements javax.swing.table.TableCellEditor
{
    private EventListenerList listenerList = new EventListenerList();
    private ChangeEvent changeEvent = null;

    public final void addCellEditorListener(CellEditorListener listener)
    {
        listenerList.add(CellEditorListener.class,listener);
    }

    public final void removeCellEditorListener(CellEditorListener listener)
    {
        listenerList.remove(CellEditorListener.class,listener);
    }

    public boolean stopCellEditing()
    {
        fireEditingStopped(true);
        return true;
    }

    public final void cancelCellEditing()
    {
        fireEditingStopped(false);
    }

    public boolean isCellEditable(EventObject anEvent)
    {
        return true;
    }

    public boolean shouldSelectCell(EventObject anEvent)
    {
        return true;
    }

    protected final void fireEditingStopped(boolean stopped)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those who are interested in this event.
        for (int i = listeners.length-2; i >= 0; i -= 2)
        {
            if (listeners[i] == CellEditorListener.class)
            {
                if (changeEvent == null) changeEvent = new ChangeEvent(this);
                CellEditorListener cel = (CellEditorListener)listeners[i+1];
                if (stopped) cel.editingStopped(changeEvent);
                else cel.editingCanceled(changeEvent);
            }
        }
    }
}
