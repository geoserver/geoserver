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
package org.apache.directory.shared.ldap.schema.normalizers;


import java.io.IOException;
import java.text.ParseException;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.PrepareString;
import org.apache.directory.shared.ldap.util.GeneralizedTime;
import org.apache.directory.shared.ldap.util.GeneralizedTime.Format;
import org.apache.directory.shared.ldap.util.GeneralizedTime.FractionDelimiter;
import org.apache.directory.shared.ldap.util.GeneralizedTime.TimeZoneFormat;


/**
 * Normalizer which normalize a time following those rules :
 * </ul>
 * <li>if minutes are ommited, then they are replaced by 00</li>
 * <li>if seconds are ommited, then they are replaced by 00</li>
 * <li>if fraction is 0 or omitted, it is replaced by 000</li>
 * <li>the time is supposed to be expressed in Zulu (GMT), so 
 * increment is applied to hours/days/yeah, and a Z is added at the end</li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 491034 $
 */
public class GeneralizedTimeNormalizer extends Normalizer
{
    /** The serial UID */
    public static final long serialVersionUID = 1L;


    /**
     * Creates a new instance of GeneralizedTimeNormalizer.
     */
    public GeneralizedTimeNormalizer()
    {
        super( SchemaConstants.GENERALIZED_TIME_MATCH_MR_OID );
    }


    /**
     * {@inheritDoc}
     */
    public Value<?> normalize( Value<?> value ) throws LdapException
    {
        try
        {
            String normalized = PrepareString.normalize( value.getString(), PrepareString.StringType.DIRECTORY_STRING );

            return new StringValue( normalized );
        }
        catch ( IOException ioe )
        {
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, I18n.err( I18n.ERR_04224, value ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public String normalize( String value ) throws LdapException
    {
        try
        {
            String prepared = PrepareString.normalize( value, PrepareString.StringType.DIRECTORY_STRING );

            GeneralizedTime time = new GeneralizedTime( prepared );
            String normalized = time.toGeneralizedTime( Format.YEAR_MONTH_DAY_HOUR_MIN_SEC_FRACTION,
                FractionDelimiter.DOT, 3, TimeZoneFormat.Z );

            return normalized;
        }
        catch ( IOException ioe )
        {
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, I18n.err( I18n.ERR_04224, value ) );
        }
        catch ( ParseException pe )
        {
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, I18n.err( I18n.ERR_04224, value ) );
        }
    }
}