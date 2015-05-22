/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008-2014, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.wfs.internal.parsers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.wfs.internal.DescribeFeatureTypeRequest;
import org.geotools.data.wfs.internal.DescribeFeatureTypeResponse;
import org.geotools.data.wfs.internal.WFSOperationType;
import org.geotools.data.wfs.internal.WFSRequest;
import org.geotools.data.wfs.internal.WFSResponse;
import org.geotools.data.wfs.internal.WFSResponseFactory;
import org.geotools.ows.ServiceException;

public class DescribeFeatureTypeResponseFactory implements WFSResponseFactory {

    private static final List<String> SUPPORTED_FORMATS = Collections.unmodifiableList(Arrays.asList(//
            "text/xml", //
            "text/xml; subtype=gml/3.1.1", //
            "text/xml; subtype=gml/3.2", //
            "text/xml; subType=gml/3.1.1", //
            "text/xml; subType=gml/3.2", //
            "XMLSCHEMA",//
            "text/gml; subtype=gml/3.1.1", //
            "text/gml; subType=gml/3.1.1", //
            "application/gml+xml", //
            "application/gml+xml; subType=gml/3.1.1",//
            "application/gml+xml; version=3.2"));

    @Override
    public boolean isAvailable() {
        return true;
    }
    
    public boolean canProcess(final WFSRequest request, final String contentType) {
        if (!canProcess(request.getOperation())) {
            return false;
        }
        boolean matches = getSupportedOutputFormats().contains(contentType);
        if (!matches) {
            // fuzzy match
            for (String supported : getSupportedOutputFormats()) {
                if (supported.startsWith(contentType) || contentType.startsWith(supported)) {
                    matches = true;
                    break;
                }
            }
        }
        return matches;
    }


    @Override
    public boolean canProcess(WFSOperationType operation) {
        return WFSOperationType.DESCRIBE_FEATURETYPE.equals(operation);
    }

    @Override
    public List<String> getSupportedOutputFormats() {
        return SUPPORTED_FORMATS;
    }

    @Override
    public WFSResponse createResponse(WFSRequest request, HTTPResponse response) throws IOException {
        try {
            return new DescribeFeatureTypeResponse((DescribeFeatureTypeRequest) request, response);
        } catch (ServiceException e) {
            throw new IOException(e);
        }
    }

}
