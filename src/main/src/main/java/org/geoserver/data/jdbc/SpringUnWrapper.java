/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.geotools.data.jdbc.datasource.UnWrapper;
import org.springframework.jdbc.support.nativejdbc.C3P0NativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.JBossNativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.Jdbc4NativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.SimpleNativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.WebLogicNativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.WebSphereNativeJdbcExtractor;

// import org.springframework.jdbc.support.nativejdbc.XAPoolNativeJdbcExtractor;

/**
 * Wires up the rich set of Spring native connection and statements un-wrappers to the GeoTools
 * UnWrapper API, whose model is different (they assume you know in advance which un-wrapper you'll
 * need, each un-wrapper works only in the environment it was designed for)
 *
 * @author Andrea Aime - OpenGeo
 */
public class SpringUnWrapper implements UnWrapper {

    static final List<NativeJdbcExtractor> EXTRACTORS;

    static {
        List<NativeJdbcExtractor> extractors = new ArrayList<NativeJdbcExtractor>();

        // some of these extractors will just blow up during initialization if
        // the environment does not contain the classes they are looking for, so we
        // guard their initialization and just skip them
        try {
            extractors.add(new CommonsDbcpNativeJdbcExtractor());
        } catch (Throwable e) {
        }
        ;
        try {
            extractors.add(new JBossNativeJdbcExtractor());
        } catch (Throwable e) {
        }
        ;
        try {
            extractors.add(new Jdbc4NativeJdbcExtractor());
        } catch (Throwable e) {
        }
        ;
        try {
            extractors.add(new SimpleNativeJdbcExtractor());
        } catch (Throwable e) {
        }
        ;
        try {
            extractors.add(new WebLogicNativeJdbcExtractor());
        } catch (Throwable e) {
        }
        ;
        try {
            extractors.add(new WebSphereNativeJdbcExtractor());
        } catch (Throwable e) {
        }
        ;
        //        try {
        //            extractors.add(new XAPoolNativeJdbcExtractor());
        //        } catch(Throwable e) {};
        try {
            extractors.add(new C3P0NativeJdbcExtractor());
        } catch (Throwable e) {
        }
        ;

        // use a concurrent enabled data structure so that we can modify
        // the order of extractors at run time, in a way that the extractors
        // that can actually do the work end up first (the code is executed in
        // tight loops over features and handling over and over exceptions is expensive)
        EXTRACTORS = new CopyOnWriteArrayList<NativeJdbcExtractor>(extractors);
    }

    public boolean canUnwrap(Connection conn) {
        Connection unwrapped = unwrapInternal(conn);
        return unwrapped != null;
    }

    public Connection unwrap(Connection conn) {
        Connection unwrapped = unwrapInternal(conn);
        if (unwrapped != null) return unwrapped;
        else
            throw new IllegalArgumentException(
                    "This connection is not unwrappable, "
                            + "check canUnwrap before calling unwrap");
    }

    private Connection unwrapInternal(Connection conn) {
        for (int i = 0; i < EXTRACTORS.size(); i++) {
            NativeJdbcExtractor extractor = EXTRACTORS.get(i);
            try {
                // the contract is that the original connection is returned
                // if unwrapping was not possible
                Connection unwrapped = extractor.getNativeConnection(conn);

                if (conn != unwrapped) {
                    if (i != 0) {
                        // move the extractor to the top, so that we don't do
                        // many useless attempts at unwrapping with the others
                        // (this code is typically executed for each feature)
                        EXTRACTORS.add(0, extractor);
                        EXTRACTORS.remove(i);
                    }
                    return unwrapped;
                }
            } catch (Throwable t) {
                // catch a throwable since some of the unwrappers do not blow up
                // during initialization when the enviroment does not help, but
                // they do at unwrap time and they throw Error suclasses
                // We just want to skip the unwrapper and move on
            }
        }
        return null;
    }

    public boolean canUnwrap(Statement st) {
        Statement unwrapped = unwrapInternal(st);
        return unwrapped != null;
    }

    public Statement unwrap(Statement statement) {
        Statement unwrapped = unwrapInternal(statement);
        if (unwrapped != null) return unwrapped;
        else
            throw new IllegalArgumentException(
                    "This statement is not unwrappable, "
                            + "check canUnwrap before calling unwrap");
    }

    private Statement unwrapInternal(Statement st) {
        for (int i = 0; i < EXTRACTORS.size(); i++) {
            NativeJdbcExtractor extractor = EXTRACTORS.get(i);
            try {
                // the contract is that the original connection is returned
                // if unwrapping was not possible
                Statement unwrapped = extractor.getNativeStatement(st);
                if (st != unwrapped) {
                    if (i != 0) {
                        // move the extractor to the beginning, so that we don't do
                        // many useless attempts at unwrapping with the others
                        // (this code is typically executed for each feature)
                        EXTRACTORS.add(0, extractor);
                        EXTRACTORS.remove(i);
                    }

                    return unwrapped;
                }
            } catch (SQLException e) {
                // no problem, skip it
            }
        }
        return null;
    }
}
