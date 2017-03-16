/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin.editor;



import java.io.FileReader;


import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.*;
import org.xml.sax.helpers.*;



import java.io.*;
import java.util.Vector;



public class ParseDBXML extends DefaultHandler
{
	private class Row extends Vector<String>{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;};
	private class Rows extends Vector<Row>{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;};
	private Rows rows = null;
	private Row row = null;
	private XMLReader xr = null;
	private String altDTDLocation = "C:\\Documents and Settings\\krice\\Desktop\\DSMdocs\\ProductTypes.dtd";
	private boolean DTDretry = false;
	private StringBuffer str; 
	private int dataIndex = 0;
	String data = null;
	
    public ParseDBXML(String file) throws Exception
    {
    	super();
    	rows = new Rows();
    	
    	xr = XMLReaderFactory.createXMLReader();
    	
		xr.setDTDHandler(this);
		xr.setContentHandler(this);
		xr.setErrorHandler(this);
		xr.setEntityResolver(this);

		// Parse each file provided on the
		// command line.
		
		FileReader r = new FileReader(file);
		xr.parse(new InputSource(r));
		
    }

    public InputSource resolveEntity(String publicID, String systemID) throws SAXException, IOException {
    	if (DTDretry) throw new IOException();
    	DTDretry = true;
    	return new InputSource(altDTDLocation);
    }

    public void setDocumentLocator (Locator locator)
    {
       //System.out.println("SetDocumentLocator: " + locator.getSystemId());
       //this.locator = locator;
    }

    
    public void startDocument ()
    {
    	//System.out.println("Start document");
    }


    public void endDocument ()
    {
    	//System.out.println("End document");
    }


    public void startElement (String uri, String name, String qName, Attributes atts)
    {
    	if (name.equals("row")) {
    		row = new Row();
    	} else if (name.equals("field")) {
    		str = new StringBuffer();
    		//System.out.println("Attribute: " + atts.getValue(0));
     	}
    }


    public void endElement (String uri, String name, String qName)
    {
    	if (name.equals("field")) {
    		dataIndex = 0;
    		if (row != null) {
        		row.add(data);
        	}
    	}
     	if (name.equals("row")) {
    		if (row != null) {
    			rows.add(row);
    			System.out.println("New Row Created: " + rows.size());
    		}
    	}
    }


    public void characters (char ch[], int start, int length)
    {
     	str.insert(dataIndex, ch, start, length);
    	dataIndex = length;
    	data = str.toString().trim();
    	
    	//System.out.println("Data: " + data);
    	
    }
    
    public void getType(String name) {
    	
    	
    	//System.out.println("getType: " + name);
    	
 
    }
    
    

    
    public static void main (String args[]) throws Exception
    {
    	new ParseDBXML(args[0]);
 
    }
    
}

