/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.DCPType;
import net.opengis.ows11.DomainMetadataType;
import net.opengis.ows11.ExceptionReportType;
import net.opengis.ows11.ExceptionType;
import net.opengis.ows11.KeywordsType;
import net.opengis.ows11.LanguageStringType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.ows11.RequestMethodType;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.geotools.xsd.EMFUtils;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

public class Ows11Util {

    static Ows11Factory f = Ows11Factory.eINSTANCE;

    public static LanguageStringType languageString(InternationalString value) {
        if (value != null) {
            return languageString(value.toString(Locale.getDefault()));
        } else {
            return null;
        }
    }

    public static LanguageStringType languageString(String value) {
        LanguageStringType ls = f.createLanguageStringType();
        ls.setValue(value);
        return ls;
    }

    public static KeywordsType keywords(List<String> keywords) {
        if (keywords == null || keywords.size() == 0) {
            return null;
        }
        KeywordsType kw = f.createKeywordsType();
        for (String keyword : keywords) {
            kw.getKeyword().add(languageString(keyword));
        }
        return kw;
    }

    public static CodeType code(String value) {
        CodeType code = f.createCodeType();
        code.setValue(value);

        return code;
    }

    public static CodeType code(Name name) {
        CodeType code = f.createCodeType();
        //        code.setCodeSpace(name.getNamespaceURI());
        //        code.setValue(name.getLocalPart());
        code.setValue(name.getURI());

        return code;
    }

    public static Name name(CodeType code) {
        // mushy translation, code type seems to never have a code space in practice
        if (code.getCodeSpace() != null) {
            return new NameImpl(code.getCodeSpace(), code.getValue());
        } else {
            return name(code.getValue());
        }
    }

    /** Turns a prefix:localName into a Name */
    public static Name name(String URI) {
        String[] parsed = URI.trim().split(":");
        if (parsed.length == 1) {
            return new NameImpl(parsed[0]);
        } else {
            return new NameImpl(parsed[0], parsed[1]);
        }
    }

    public static CodeType code(CodeType value) {
        return code(value.getValue());
    }

    public static DomainMetadataType type(String name) {
        DomainMetadataType type = f.createDomainMetadataType();
        type.setValue(name);

        return type;
    }

    public static ExceptionReportType exceptionReport(
            ServiceException exception, boolean verboseExceptions) {
        return exceptionReport(exception, verboseExceptions, null);
    }

    public static ExceptionReportType exceptionReport(
            ServiceException exception, boolean verboseExceptions, String version) {

        ExceptionType e = f.createExceptionType();

        if (exception.getCode() != null) {
            e.setExceptionCode(exception.getCode());
        } else {
            // set a default
            e.setExceptionCode("NoApplicableCode");
        }

        e.setLocator(exception.getLocator());

        // add the message
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(exception, sb, true);
        e.getExceptionText().add(sb.toString());
        e.getExceptionText().addAll(exception.getExceptionText());

        if (verboseExceptions) {
            // add the entire stack trace
            // exception.
            e.getExceptionText().add("Details:");
            ByteArrayOutputStream trace = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(trace));
            e.getExceptionText().add(new String(trace.toByteArray()));
        }

        ExceptionReportType report = f.createExceptionReportType();

        version = version != null ? version : "1.1.0";
        report.setVersion(version);
        report.getException().add(e);

        return report;
    }

    public static DCPType dcp(String service, EObject request) {
        String baseUrl = (String) EMFUtils.get(request, "baseUrl");
        if (baseUrl == null) {
            throw new IllegalArgumentException(
                    "Request object" + request + " has no 'baseUrl' property.");
        }
        String href =
                ResponseUtils.buildURL(
                        baseUrl, service, new HashMap<String, String>(), URLType.SERVICE);

        DCPType dcp = f.createDCPType();
        dcp.setHTTP(f.createHTTPType());

        RequestMethodType get = f.createRequestMethodType();
        get.setHref(href);
        dcp.getHTTP().getGet().add(get);

        RequestMethodType post = f.createRequestMethodType();
        post.setHref(href);
        dcp.getHTTP().getPost().add(post);

        return dcp;
    }
}
