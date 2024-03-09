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
package org.apache.directory.shared.ldap.message.extended;


import javax.naming.NamingException;
import javax.naming.ldap.ExtendedResponse;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.extended.operations.certGeneration.CertGenerationDecoder;
import org.apache.directory.shared.ldap.codec.extended.operations.certGeneration.CertGenerationObject;
import org.apache.directory.shared.ldap.message.ExtendedRequestImpl;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * An extended operation requesting the server to generate a public/private key pair and a certificate
 * and store them in a specified target entry in the DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CertGenerationRequest extends ExtendedRequestImpl
{
    /** The serial version UUID */
    private static final long serialVersionUID = 1L;

    private CertGenerationObject certGenObj;

    private static final Logger LOG = LoggerFactory.getLogger( CertGenerationRequest.class );

    public static final String EXTENSION_OID = "1.3.6.1.4.1.18060.0.1.8";

    /**
     * 
     * Creates a new instance of CertGenerationRequest.
     *
     * @param messageId the message id
     * @param targerDN the DN of target entry whose key and certificate values will be changed 
     * @param issuerDN DN to be used as the issuer's DN in the certificate
     * @param subjectDN DN to be used as certificate's subject
     * @param keyAlgorithm crypto algorithm name to be used for generating the keys
     */
    public CertGenerationRequest( int messageId, String targerDN, String issuerDN, String subjectDN, String keyAlgorithm )
    {
        super( messageId );
        setOid( EXTENSION_OID );
        
        this.certGenObj = new CertGenerationObject();
        certGenObj.setTargetDN( targerDN );
        certGenObj.setIssuerDN( issuerDN );
        certGenObj.setSubjectDN( subjectDN );
        certGenObj.setKeyAlgorithm( keyAlgorithm );
    }


    private void encodePayload() throws EncoderException
    {
        payload = certGenObj.encode().array();
    }


    public void setPayload( byte[] payload )
    {
        CertGenerationDecoder decoder = new CertGenerationDecoder();
        try
        {
            certGenObj = ( CertGenerationObject ) decoder.decode( payload );
            if ( payload != null )
            {
                this.payload = new byte[payload.length];
                System.arraycopy( payload, 0, this.payload, 0, payload.length );
            }
            else
            {
                this.payload = null;
            }
        }
        catch ( DecoderException e )
        {
            LOG.error( I18n.err( I18n.ERR_04165 ), e );
            throw new RuntimeException( e );
        }
    }


    public ExtendedResponse createExtendedResponse( String id, byte[] berValue, int offset, int length )
        throws NamingException
    {
        return ( ExtendedResponse ) getResultResponse();
    }


    public byte[] getEncodedValue()
    {
        return getPayload();
    }


    public byte[] getPayload()
    {
        if ( payload == null )
        {
            try
            {
                encodePayload();
            }
            catch ( EncoderException e )
            {
                LOG.error( I18n.err( I18n.ERR_04167 ), e );
                throw new RuntimeException( e );
            }
        }

        if ( payload == null )
        {
            return null;
        }

        final byte[] copy = new byte[payload.length];
        System.arraycopy( payload, 0, copy, 0, payload.length );
        return copy;
    }


    public InternalResultResponse getResultResponse()
    {
        if ( response == null )
        {
            response = new CertGenerationResponse( getMessageId() );
        }

        return response;
    }


    public String getTargetDN()
    {
        return certGenObj.getTargetDN();
    }


    public void setTargetDN( String targetDN )
    {
        certGenObj.setTargetDN( targetDN );
    }


    public String getIssuerDN()
    {
        return certGenObj.getIssuerDN();
    }


    public void setIssuerDN( String issuerDN )
    {
        certGenObj.setIssuerDN( issuerDN );
    }


    public String getSubjectDN()
    {
        return certGenObj.getSubjectDN();
    }


    public void setSubjectDN( String subjectDN )
    {
        certGenObj.setSubjectDN( subjectDN );
    }


    public String getKeyAlgorithm()
    {
        return certGenObj.getKeyAlgorithm();
    }


    public void setKeyAlgorithm( String keyAlgorithm )
    {
        certGenObj.setKeyAlgorithm( keyAlgorithm );
    }

}
