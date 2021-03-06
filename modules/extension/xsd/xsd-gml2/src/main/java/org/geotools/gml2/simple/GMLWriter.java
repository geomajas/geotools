/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gml2.simple;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Locale;

import org.geotools.gml2.GML;
import org.geotools.xml.XMLUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.NamespaceSupport;

import com.vividsolutions.jts.geom.CoordinateSequence;

/**
 * Helper class writing out GML elements and coordinates. Geared towards efficiency, write out
 * elements and ordinate lists with the minimim amount of garbage generation
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class GMLWriter {

    static final QualifiedName COORDINATES = new QualifiedName(GML.NAMESPACE, "coordinates", "gml");

    static final QualifiedName POS_LIST = new QualifiedName(GML.NAMESPACE, "posList", "gml");

    /**
     * The min value at which the decimal notation is used (below it, the computerized scientific
     * one is used instead)
     */
    private static final double DECIMAL_MIN = Math.pow(10, -3);

    /**
     * The max value at which the decimal notation is used (above it, the computerized scientific
     * one is used instead)
     */
    private static final double DECIMAL_MAX = Math.pow(10, 7);

    /**
     * Used in coordinate formatting
     */
    private static final FieldPosition ZERO = new FieldPosition(0);

    /**
     * The actual XML encoder
     */
    ContentHandler handler;

    /**
     * All the namespaces known to the Encoder
     */
    NamespaceSupport namespaces;

    /**
     * We use a StringBuffer because the date formatters cannot take a StringBuilder
     */
    StringBuffer sb = new StringBuffer();

    /**
     * The StringBuffer above gets dumped into this char buffer in order to pass the chars to the
     * handler
     */
    char[] buffer;

    /**
     * Coordinates qualified name, with the right prefix
     */
    private QualifiedName coordinates;

    /**
     * posList qualified name, with the right prefix
     */
    private QualifiedName posList;

    /**
     * Scale used in truncate to reduce the number of decimals
     */
    private double scale;

    /** To be used for formatting numbers, uses US locale. */
    private final NumberFormat coordFormatter = NumberFormat.getInstance(Locale.US);

    /**
     * Whether we have to format in plain decimal numbers, or we can use scientific notation
     */
    private boolean forceDecimal;

    /**
     * Create a new content handler
     * 
     * @param delegate The actual XML writer
     * @param namespaces The namespaces known to the Encoder
     * @param numDecimals How many decimals to preserve when writing ordinates
     * @param forceDecimal If xs:decimal compliant encoding should be used, or not
     * @param gmlPrefix The GML namespace prefix
     */
    public GMLWriter(ContentHandler delegate, NamespaceSupport namespaces, int numDecimals,
            boolean forceDecimal, String gmlPrefix) {
        this.handler = delegate;
        this.namespaces = namespaces;
        this.coordinates = COORDINATES.derive(gmlPrefix);
        this.posList = POS_LIST.derive(gmlPrefix);

        this.coordFormatter.setMaximumFractionDigits(numDecimals);
        this.coordFormatter.setGroupingUsed(false);

        this.scale = Math.pow(10, numDecimals);
        this.forceDecimal = forceDecimal;
    }

    /**
     * @param locator
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator(Locator locator) {
        handler.setDocumentLocator(locator);
    }

    /**
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        handler.startDocument();
    }

    /**
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException {
        handler.endDocument();
    }

    /**
     * @param prefix
     * @param uri
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        handler.startPrefixMapping(prefix, uri);
    }

    /**
     * @param prefix
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        handler.endPrefixMapping(prefix);
    }

    /**
     * @param uri
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String,
     *      java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(QualifiedName qn, Attributes atts) throws SAXException {
        String qualifiedName = qn.getQualifiedName();
        if (qualifiedName == null) {
            qualifiedName = qualify(qn.getNamespaceURI(), qn.getLocalPart(), null);
        }
        if (qualifiedName != null) {
            handler.startElement(null, null, qualifiedName, atts);
        } else {
            handler.startElement(qn.getNamespaceURI(), qn.getLocalPart(), null, atts);
        }
    }

    private String qualify(String uri, String localName, String qName) {
        if (qName == null) {
            String prefix = namespaces.getPrefix(uri);
            if (prefix == null) {
                return localName;
            } else {
                return prefix + ":" + localName;
            }
        }
        return qName;
    }

    /**
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public void endElement(QualifiedName qn) throws SAXException {
        String qualifiedName = qn.getQualifiedName();
        if (qualifiedName == null) {
            qualifiedName = qualify(qn.getNamespaceURI(), qn.getLocalPart(), null);
        }
        if (qualifiedName != null) {
            handler.endElement(null, null, qualifiedName);
        } else {
            handler.endElement(qn.getNamespaceURI(), qn.getLocalPart(), null);
        }
    }

    /**
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    private void characters(char[] ch, int start, int length) throws SAXException {
        handler.characters(ch, start, length);
    }

    void characters(StringBuffer sb) throws SAXException {
        int length = sb.length();
        if (buffer == null || buffer.length < length) {
            buffer = new char[length];
        }
        sb.getChars(0, length, buffer, 0);
        characters(buffer, 0, length);
    }

    void characters(String s) throws SAXException {
        s = XMLUtils.removeXMLInvalidChars(s);
        int length = s.length();
        if (buffer == null || buffer.length < length) {
            buffer = new char[length];
        }
        s.getChars(0, length, buffer, 0);
        characters(buffer, 0, length);
    }

    /**
     * Writes a GML2 coordinates element
     * 
     * @param cs
     * @throws SAXException
     */
    public void coordinates(CoordinateSequence cs) throws SAXException {
        startElement(coordinates, null);
        coordinates(cs, ',', ' ', sb);
        characters(sb);
        endElement(coordinates);
    }

    /**
     * Writes a single x/y position, without wrapping it in any element
     * 
     * @param x
     * @param y
     * @throws SAXException
     */
    public void position(double x, double y) throws SAXException {
        position(x, y, sb);
        characters(sb);
    }

    void position(double x, double y, StringBuffer sb) {
        sb.setLength(0);
        appendDecimal(x);
        if (!Double.isNaN(y)) {
            sb.append(" ");
            appendDecimal(y);
        }
    }

    void positions(CoordinateSequence coordinates) {
        coordinates(coordinates, ' ', ' ', sb);
    }

    void coordinates(CoordinateSequence coordinates, char cs, char ts, StringBuffer sb) {
        sb.setLength(0);
        int n = coordinates.size();
        for (int i = 0; i < n; i++) {
            appendDecimal(coordinates.getX(i)).append(cs);
            appendDecimal(coordinates.getY(i));
            sb.append(ts);
        }
        sb.setLength(sb.length() - 1);
    }

    /**
     * Writes a single ordinate, without wrapping it inside any element
     * 
     * @param x
     * @throws SAXException
     */
    public void ordinate(double x) throws SAXException {
        sb.setLength(0);
        appendDecimal(x);
        characters(sb);
    }

    private StringBuffer appendDecimal(double x) {
        if ((Math.abs(x) >= DECIMAL_MIN && x < DECIMAL_MAX) || x == 0) {
            x = truncate(x);
            long lx = (long) x;
            if (lx == x)
                sb.append(lx);
            else
                sb.append(x);
        } else {
            if (forceDecimal) {
                coordFormatter.format(x, sb, ZERO);
            } else {
                sb.append(truncate(x));
            }
        }

        return sb;
    }

    final double truncate(double x) {
        return Math.floor(x * scale + 0.5) / scale;
    }

    /**
     * Write a GML3 posList
     * 
     * @param coordinateSequence
     * @throws SAXException
     */
    public void posList(CoordinateSequence coordinateSequence) throws SAXException {
        startElement(posList, null);
        positions(coordinateSequence);
        characters(sb);
        endElement(posList);
    }

}