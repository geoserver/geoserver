/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.util;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * A SSLSocketFactory that accepts every certificat without validation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DummySSLSocketFactory extends SSLSocketFactory
{

    /** The default instance. */
    private static SocketFactory instance;


    /**
     * Gets the default instance.
     * 
     * Note: This method is invoked from the JNDI framework when 
     * creating a ldaps:// connection.
     * 
     * @return the default instance
     */
    public static SocketFactory getDefault()
    {
        if ( instance == null )
        {
            instance = new DummySSLSocketFactory();
        }
        return instance;
    }

    /** The delegate. */
    private SSLSocketFactory delegate;


    /**
     * Creates a new instance of DummySSLSocketFactory.
     */
    public DummySSLSocketFactory()
    {
        try
        {
            TrustManager tm = new X509TrustManager()
            {
                public X509Certificate[] getAcceptedIssuers()
                {
                    return new X509Certificate[0];
                }


                public void checkClientTrusted( X509Certificate[] arg0, String arg1 ) throws CertificateException
                {
                }


                public void checkServerTrusted( X509Certificate[] arg0, String arg1 ) throws CertificateException
                {
                }
            };
            TrustManager[] tma =
                { tm };
            SSLContext sc = SSLContext.getInstance( "TLS" ); //$NON-NLS-1$
            sc.init( null, tma, new SecureRandom() );
            delegate = sc.getSocketFactory();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    /**
     * @see javax.net.ssl.SSLSocketFactory#getDefaultCipherSuites()
     */
    public String[] getDefaultCipherSuites()
    {
        return delegate.getDefaultCipherSuites();
    }


    /**
     * @see javax.net.ssl.SSLSocketFactory#getSupportedCipherSuites()
     */
    public String[] getSupportedCipherSuites()
    {
        return delegate.getSupportedCipherSuites();
    }


    /**
     * @see javax.net.ssl.SSLSocketFactory#createSocket(java.net.Socket, java.lang.String, int, boolean)
     */
    public Socket createSocket( Socket arg0, String arg1, int arg2, boolean arg3 ) throws IOException
    {
        try
        {
            return delegate.createSocket( arg0, arg1, arg2, arg3 );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * @see javax.net.SocketFactory#createSocket(java.lang.String, int)
     */
    public Socket createSocket( String arg0, int arg1 ) throws IOException, UnknownHostException
    {
        try
        {
            return delegate.createSocket( arg0, arg1 );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int)
     */
    public Socket createSocket( InetAddress arg0, int arg1 ) throws IOException
    {
        try
        {
            return delegate.createSocket( arg0, arg1 );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * @see javax.net.SocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
     */
    public Socket createSocket( String arg0, int arg1, InetAddress arg2, int arg3 ) throws IOException,
        UnknownHostException
    {
        try
        {
            return delegate.createSocket( arg0, arg1, arg2, arg3 );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int, java.net.InetAddress, int)
     */
    public Socket createSocket( InetAddress arg0, int arg1, InetAddress arg2, int arg3 ) throws IOException
    {
        try
        {
            return delegate.createSocket( arg0, arg1, arg2, arg3 );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw e;
        }
    }
}
