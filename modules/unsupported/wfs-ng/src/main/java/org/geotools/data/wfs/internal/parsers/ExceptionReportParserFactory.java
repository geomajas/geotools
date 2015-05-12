package org.geotools.data.wfs.internal.parsers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.wfs.internal.WFSException;
import org.geotools.data.wfs.internal.WFSOperationType;
import org.geotools.data.wfs.internal.WFSRequest;
import org.geotools.data.wfs.internal.WFSResponse;
import org.geotools.data.wfs.internal.WFSResponseFactory;


public class ExceptionReportParserFactory  implements WFSResponseFactory {

    private static final String EXCEPTION_FORMAT = "application/vnd.ogc.se_xml;charset=UTF-8";

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean canProcess(WFSRequest originatingRequest, String contentType) {
        return contentType.startsWith(EXCEPTION_FORMAT);
    }

    @Override
    public WFSResponse createResponse(WFSRequest request, HTTPResponse response) throws IOException {
        // Should extract message here !
        String message = IOUtils.toString(response.getResponseStream());
        throw new WFSException(message);
    }

    @Override
    public boolean canProcess(WFSOperationType operation) {
        return true;
    }

    @Override
    public List<String> getSupportedOutputFormats() {
        return Arrays.asList(EXCEPTION_FORMAT);
    }

}