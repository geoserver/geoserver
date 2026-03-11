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


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * Gets the generalized time using the "Z" form of the g-time-zone.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 725712 $
 */
public class DateUtils
{
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" );

    static
    {
        dateFormat.setTimeZone( UTC_TIME_ZONE );
    }
    
    
    public static Date getDate( String zuluTime )
    {
        Calendar cal = Calendar.getInstance( UTC_TIME_ZONE );
        cal.set( Calendar.YEAR, getYear( zuluTime ) ); 
        cal.set( Calendar.MONTH, getMonth( zuluTime ) - 1 ); 
        cal.set( Calendar.DAY_OF_MONTH, getDay( zuluTime ) ); 
        cal.set( Calendar.HOUR_OF_DAY, getHour( zuluTime ) ); 
        cal.set( Calendar.MINUTE, getMinutes( zuluTime ) );
        cal.set( Calendar.SECOND, getSeconds( zuluTime ) );
        return cal.getTime();
    }


    public static int getYear( String zuluTime )
    {
        return Integer.parseInt( zuluTime.substring( 0, 4 ) );
    }
    
    
    public static int getMonth( String zuluTime )
    {
        return Integer.parseInt( zuluTime.substring( 4, 6 ) );
    }
    
    
    public static int getDay( String zuluTime )
    {
        return Integer.parseInt( zuluTime.substring( 6, 8 ) );
    }
    
    
    public static int getHour( String zuluTime )
    {
        return Integer.parseInt( zuluTime.substring( 8, 10 ) );
    }
    
    
    public static int getMinutes( String zuluTime )
    {
        return Integer.parseInt( zuluTime.substring( 10, 12 ) );
    }
    
    
    public static int getSeconds( String zuluTime )
    {
        return Integer.parseInt( zuluTime.substring( 12, 14 ) );
    }
    
    
    /**
     * Gets the generalized time using the "Z" form of the g-time-zone described
     * by [<a href=
     * "http://ietf.org/internet-drafts/draft-ietf-ldapbis-syntaxes-09.txt">
     * SYNTAXES</a>] section 3.3.13, included below:
     * 
     * <pre>
     * 
     *  3.3.13.  Generalized Time
     * 
     *  A value of the Generalized Time syntax is a character string
     *  representing a date and time.  The LDAP-specific encoding of a value
     *  of this syntax is a restriction of the format defined in [ISO8601],
     *  and is described by the following ABNF:
     * 
     *  century = 2(%x30-39) ; &quot;00&quot; to &quot;99&quot;
     *  year    = 2(%x30-39) ; &quot;00&quot; to &quot;99&quot;
     *  month   =   ( %x30 %x31-39 ) ; &quot;01&quot; (January) to &quot;09&quot;
     *            / ( %x31 %x30-32 ) ; &quot;10&quot; to &quot;12&quot;
     *  day     =   ( %x30 %x31-39 )    ; &quot;01&quot; to &quot;09&quot;
     *            / ( %x31-32 %x30-39 ) ; &quot;10&quot; to &quot;29&quot;
     *            / ( %x33 %x30-31 )    ; &quot;30&quot; to &quot;31&quot;
     *  hour    = ( %x30-31 %x30-39 ) / ( %x32 %x30-33 ) ; &quot;00&quot; to &quot;23&quot;
     *  minute  = %x30-35 %x30-39                        ; &quot;00&quot; to &quot;59&quot;
     *  second  =   ( %x30-35 %x30-39 )  ; &quot;00&quot; to &quot;59&quot;
     *            / ( %x36 %x30 )        ; &quot;60&quot; (a leap second)
     * 
     *  GeneralizedTime = century year month day hour
     *                       [ minute [ second ] ] [ fraction ]
     *                       g-time-zone
     *  fraction        = ( DOT / COMMA ) 1*(%x30-39)
     *  g-time-zone     = %x5A  ; &quot;Z&quot;
     *                    / g-differential
     *  g-differential  = ( MINUS / PLUS ) hour [ minute ]
     *  MINUS           = %x2D  ; minus sign (&quot;-&quot;)
     * 
     *  The &lt;DOT&gt;, &lt;COMMA&gt; and &lt;PLUS&gt; rules are defined in [MODELS].
     * 
     *  The time value represents coordinated universal time (equivalent to
     *  Greenwich Mean Time) if the &quot;Z&quot; form of &lt;g-time-zone&gt; is used,
     * 
     *  otherwise the value represents a local time in the time zone
     *  indicated by &lt;g-differential&gt;.  In the latter case, coordinated
     *  universal time can be calculated by subtracting the differential from
     *  the local time.  The &quot;Z&quot; form of &lt;g-time-zone&gt; SHOULD be used in
     *  preference to &lt;g-differential&gt;.
     * 
     *  Examples:
     *     199412161032Z
     *     199412160532-0500
     * 
     *  Both example values represent the same coordinated universal time:
     *  10:32 AM, December 16, 1994.
     * 
     *  The LDAP definition for the Generalized Time syntax is:
     * 
     *  ( 1.3.6.1.4.1.1466.115.121.1.24 DESC 'Generalized Time' )
     * 
     *  This syntax corresponds to the GeneralizedTime ASN.1 type from
     *  [ASN.1], with the constraint that local time without a differential
     *  SHALL NOT be used.
     * </pre>
     * 
     * Gets the generalized time right now.
     * 
     * @return the generalizedTime right now
     */
    public static String getGeneralizedTime()
    {
        Date date = new Date();

        synchronized ( dateFormat )
        {
            return dateFormat.format( date );
        }
    }


    /**
     * 
     * @see #getGeneralizedTime()
     *
     * @param date the date to be converted to generalized time string
     * @return given date in the generalized time string format
     */
    public static String getGeneralizedTime( Date date )
    {
        synchronized ( dateFormat )
        {
            return dateFormat.format( date );
        }
    }


    /**
     * 
     * @see #getGeneralizedTime()
     *
     * @param time the time value to be converted to generalized time string
     * @return given time in generalized time string format
     */
    public static String getGeneralizedTime( long time )
    {
        return getGeneralizedTime( new Date( time ) );
    }
    
}
