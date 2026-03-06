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
package org.apache.directory.shared.ldap.schema;


import java.util.List;
import java.util.Map;


/**
 * Utility class used to generate schema object specifications. Some of the
 * latest work coming out of the LDAPBIS working body adds optional extensions
 * to these syntaxes. Descriptions can be generated for
 * the following objects:
 * <ul>
 * <li><a href="./AttributeType.html">AttributeType</a></li>
 * <li><a href="./DITContentRule.html">DITContentRule</a></li>
 * <li><a href="./DITContentRule.html">DITStructureRule</a></li>
 * <li><a href="./LdapComparator.html">Syntax</a></li>
 * <li><a href="./MatchingRule.html">MatchingRule</a></li>
 * <li><a href="./MatchingRuleUse.html">MatchingRuleUse</a></li>
 * <li><a href="./NameForm.html">NameForm</a></li>
 * <li><a href="./Normalizer.html">Syntax</a></li>
 * <li><a href="./ObjectClass.html">ObjectClass</a></li>
 * <li><a href="./LdapSyntax.html">Syntax</a></li>
 * <li><a href="./SyntaxChecker.html">Syntax</a></li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 896579 $
 */
public class DescriptionUtils
{
    /**
     * Generates the description using the AttributeTypeDescription as defined
     * by the syntax: 1.3.6.1.4.1.1466.115.121.1.3. Only the right hand side of
     * the description starting at the opening parenthesis is generated: that
     * is 'AttributeTypeDescription = ' is not generated.
     * 
     * <pre>
     *  AttributeTypeDescription = &quot;(&quot; whsp
     *     numericoid whsp                ; AttributeType identifier
     *     [ &quot;NAME&quot; qdescrs ]             ; name used in AttributeType
     *     [ &quot;DESC&quot; qdstring ]            ; description
     *     [ &quot;OBSOLETE&quot; whsp ]
     *     [ &quot;SUP&quot; woid ]                 ; derived from parent AttributeType
     *     [ &quot;EQUALITY&quot; woid              ; Matching Rule name
     *     [ &quot;ORDERING&quot; woid              ; Matching Rule name
     *     [ &quot;SUBSTR&quot; woid ]              ; Matching Rule name
     *     [ &quot;SYNTAX&quot; whsp noidlen whsp ] ; see section 4.3 RFC 2252
     *     [ &quot;SINGLE-VALUE&quot; whsp ]        ; default multi-valued
     *     [ &quot;COLLECTIVE&quot; whsp ]          ; default not collective
     *     [ &quot;NO-USER-MODIFICATION&quot; whsp ]; default user modifiable
     *     [ &quot;USAGE&quot; whsp AttributeUsage ]; default userApplications
     *     whsp &quot;)&quot;
     * </pre>
     * 
     * @param attributeType
     *            the attributeType to generate a description for
     * @return the AttributeTypeDescription Syntax for the attributeType in a
     *         pretty formated string
     */
    public static String getDescription( AttributeType attributeType )
    {
        StringBuilder buf = new StringBuilder( "( " );
        buf.append( attributeType.getOid() );
        buf.append( '\n' );

        if ( attributeType.getNames().size() != 0 )
        {
            buf.append( " NAME " );
            getQDescrs( buf, attributeType.getNames() );
        }

        if ( attributeType.getDescription() != null )
        {
            buf.append( " DESC " );
            buf.append( attributeType.getDescription() );
            buf.append( '\n' );
        }

        if ( attributeType.isObsolete() )
        {
            buf.append( " OBSOLETE\n" );
        }

        if ( attributeType.getSuperior() != null )
        {
            buf.append( " SUP " );
            buf.append( attributeType.getSuperiorName() );
            buf.append( '\n' );
        }

        if ( attributeType.getEquality() != null )
        {
            buf.append( " EQUALITY " );
            buf.append( attributeType.getEqualityName() );
            buf.append( '\n' );
        }

        if ( attributeType.getOrdering() != null )
        {
            buf.append( " ORDERING " );
            buf.append( attributeType.getOrderingName() );
            buf.append( '\n' );
        }

        if ( attributeType.getSubstring() != null )
        {
            buf.append( " SUBSTR " );
            buf.append( attributeType.getSubstringName() );
            buf.append( '\n' );
        }

        if ( attributeType.getSyntax() != null )
        {
            buf.append( " SYNTAX " );

            buf.append( attributeType.getSyntaxName() );

            if ( attributeType.getSyntaxLength() > 0 )
            {
                buf.append( '{' ).append( attributeType.getSyntaxLength() ).append( '}' );
            }

            buf.append( '\n' );
        }

        if ( attributeType.isSingleValued() )
        {
            buf.append( " SINGLE-VALUE\n" );
        }

        if ( attributeType.isCollective() )
        {
            buf.append( " COLLECTIVE\n" );
        }

        if ( !attributeType.isUserModifiable() )
        {
            buf.append( " NO-USER-MODIFICATION\n" );
        }

        buf.append( " USAGE " );
        buf.append( UsageEnum.render( attributeType.getUsage() ) );
        buf.append( '\n' );

        if ( attributeType.getExtensions() != null )
        {
            getExtensions( buf, attributeType.getExtensions() );
        }

        buf.append( " )\n" );

        return buf.toString();
    }


    /**
     * Generates the ComparatorDescription for a LdapComparator. Only the right 
     * hand side of the description starting at the opening parenthesis is 
     * generated: that is 'ComparatorDescription = ' is not generated.
     * 
     * <pre>
     * ComparatorDescription = &quot;(&quot;
     *     numericoid                          
     *     [&quot;DESC&quot; qdstring ]
     *     &quot;FQCN&quot; whsp fqcn
     *     [&quot;BYTECODE&quot; whsp base64  ]
     *     extensions 
     *     &quot;)&quot;
     * </pre>
     * 
     * @param comparator
     *            the Comparator to generate the description for
     * @return the ComparatorDescription string
     */
    public static String getDescription( LdapComparator<?> comparator )
    {
        return getLoadableDescription( comparator );
    }


    /**
     * Generates the DITContentRuleDescription for a DITContentRule as defined
     * by the syntax: 1.3.6.1.4.1.1466.115.121.1.16. Only the right hand side of
     * the description starting at the opening parenthesis is generated: that
     * is 'DITContentRuleDescription = ' is not generated.
     * 
     * <pre>
     *   DITContentRuleDescription = &quot;(&quot;
     *       numericoid         ; Structural ObjectClass identifier
     *       [ &quot;NAME&quot; qdescrs ]
     *       [ &quot;DESC&quot; qdstring ]
     *       [ &quot;OBSOLETE&quot; ]
     *       [ &quot;AUX&quot; oids ]     ; Auxiliary ObjectClasses
     *       [ &quot;MUST&quot; oids ]    ; AttributeType identifiers
     *       [ &quot;MAY&quot; oids ]     ; AttributeType identifiers
     *       [ &quot;NOT&quot; oids ]     ; AttributeType identifiers
     *      &quot;)&quot;
     * </pre>
     * 
     * @param dITContentRule
     *            the DIT content rule specification
     * @return the specification according to the DITContentRuleDescription
     *         syntax
     */
    public static String getDescription( DITContentRule dITContentRule )
    {
        StringBuilder buf = new StringBuilder( "( " );
        buf.append( dITContentRule.getOid() );
        buf.append( '\n' );

        if ( dITContentRule.getNames() != null )
        {
            buf.append( " NAME " );
            getQDescrs( buf, dITContentRule.getNames() );
            buf.append( '\n' );
        }

        if ( dITContentRule.getDescription() != null )
        {
            buf.append( " DESC " );
            buf.append( dITContentRule.getDescription() );
            buf.append( '\n' );
        }

        if ( dITContentRule.isObsolete() )
        {
            buf.append( " OBSOLETE\n" );
        }

        // print out all the auxiliary object class oids
        List<ObjectClass> aux = dITContentRule.getAuxObjectClasses();

        if ( ( aux != null ) && ( aux.size() > 0 ) )
        {
            buf.append( " AUX " );
            getQDStrings( buf, aux );
        }

        List<AttributeType> must = dITContentRule.getMustAttributeTypes();

        if ( ( must != null ) && ( must.size() > 0 ) )
        {
            buf.append( " MUST " );
            getQDStrings( buf, must );
        }

        List<AttributeType> may = dITContentRule.getMayAttributeTypes();

        if ( ( may != null ) && ( may.size() > 0 ) )
        {
            buf.append( " MAY " );
            getQDStrings( buf, may );
        }

        List<AttributeType> not = dITContentRule.getNotAttributeTypes();

        if ( ( not != null ) && ( not.size() > 0 ) )
        {
            buf.append( " NOT " );
            getQDStrings( buf, not );
        }

        if ( dITContentRule.getExtensions() != null )
        {
            getExtensions( buf, dITContentRule.getExtensions() );
        }

        buf.append( " )\n" );
        return buf.toString();
    }


    /**
     * Generates the DITStructureRuleDescription for a DITStructureRule as
     * defined by the syntax: 1.3.6.1.4.1.1466.115.121.1.17. Only the right hand
     * side of the description starting at the opening parenthesis is
     * generated: that is 'DITStructureRuleDescription = ' is not generated.
     * 
     * <pre>
     *   DITStructureRuleDescription = &quot;(&quot; whsp
     *       ruleid                     ; rule identifier
     *       [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *       [ SP "DESC" SP qdstring ]  ; description
     *       [ SP "OBSOLETE" ]          ; not active
     *       SP "FORM" SP oid           ; NameForm
     *       [ SP "SUP" ruleids ]       ; superior rules
     *       extensions WSP             ; extensions
     *       &quot;)&quot;
     * </pre>
     * 
     * @param dITStructureRule
     *            the DITStructureRule to generate the description for
     * @return the description in the DITStructureRuleDescription syntax
     */
    public static String getDescription( DITStructureRule dITStructureRule )
    {
        StringBuilder buf = new StringBuilder( "( " );
        buf.append( dITStructureRule.getOid() );
        buf.append( '\n' );

        if ( dITStructureRule.getNames() != null )
        {
            buf.append( " NAME " );
            getQDescrs( buf, dITStructureRule.getNames() );
        }

        if ( dITStructureRule.getDescription() != null )
        {
            buf.append( " DESC " );
            buf.append( dITStructureRule.getDescription() );
            buf.append( '\n' );
        }

        if ( dITStructureRule.isObsolete() )
        {
            buf.append( " OBSOLETE\n" );
        }

        buf.append( " FORM " );
        buf.append( dITStructureRule.getForm() );
        buf.append( '\n' );

        // TODO : Shouldn't we get the ruleId OID ? 
        List<Integer> sups = dITStructureRule.getSuperRules();

        if ( ( sups != null ) && ( sups.size() > 0 ) )
        {
            buf.append( " SUP\n" );

            if ( sups.size() == 1 )
            {
                buf.append( sups.get( 0 ) );
            }
            else
            {
                boolean isFirst = true;
                buf.append( "( " );

                for ( int sup : sups )
                {
                    if ( isFirst )
                    {
                        isFirst = false;
                    }
                    else
                    {
                        buf.append( " " );
                    }

                    buf.append( sup );
                }

                buf.append( " )" );
            }

            buf.append( '\n' );
        }

        buf.append( " )\n" );

        return buf.toString();
    }


    /**
     * Generates the MatchingRuleDescription for a MatchingRule as defined by
     * the syntax: 1.3.6.1.4.1.1466.115.121.1.30. Only the right hand side of
     * the description starting at the opening parenthesis is generated: that
     * is 'MatchingRuleDescription = ' is not generated.
     * 
     * <pre>
     *  MatchingRuleDescription = &quot;(&quot; whsp
     *     numericoid whsp      ; MatchingRule object identifier
     *     [ &quot;NAME&quot; qdescrs ]
     *     [ &quot;DESC&quot; qdstring ]
     *     [ &quot;OBSOLETE&quot; whsp ]
     *     &quot;SYNTAX&quot; numericoid
     *  whsp &quot;)&quot;
     * </pre>
     * 
     * @param matchingRule
     *            the MatchingRule to generate the description for
     * @return the MatchingRuleDescription string
     */
    public static String getDescription( MatchingRule matchingRule )
    {
        StringBuilder buf = new StringBuilder( "( " );
        buf.append( matchingRule.getOid() );
        buf.append( '\n' );

        if ( matchingRule.getNames() != null )
        {
            buf.append( " NAME " );
            getQDescrs( buf, matchingRule.getNames() );
        }

        if ( matchingRule.getDescription() != null )
        {
            buf.append( " DESC " );
            buf.append( matchingRule.getDescription() );
            buf.append( '\n' );
        }

        if ( matchingRule.isObsolete() )
        {
            buf.append( " OBSOLETE\n" );
        }

        buf.append( " SYNTAX " );
        buf.append( matchingRule.getSyntaxOid() );
        buf.append( '\n' );

        if ( matchingRule.getExtensions() != null )
        {
            getExtensions( buf, matchingRule.getExtensions() );
        }

        buf.append( " ) " );
        return buf.toString();
    }


    /**
     * Generates the MatchingRuleUseDescription for a MatchingRuleUse as defined
     * by the syntax: 1.3.6.1.4.1.1466.115.121.1.31. Only the right hand side of
     * the description starting at the opening parenthesis is generated: that
     * is 'MatchingRuleUseDescription = ' is not generated.
     * 
     * <pre>
     *      MatchingRuleUseDescription = LPAREN WSP
     *          numericoid                ; object identifier
     *          [ SP &quot;NAME&quot; SP qdescrs ]  ; short names (descriptors)
     *          [ SP &quot;DESC&quot; SP qdstring ] ; description
     *          [ SP &quot;OBSOLETE&quot; ]         ; not active
     *          SP &quot;APPLIES&quot; SP oids      ; attribute types
     *          extensions WSP RPAREN     ; extensions
     *  
     *    where:
     *      [numericoid] is the object identifier of the matching rule
     *          associated with this matching rule use description;
     *      NAME [qdescrs] are short names (descriptors) identifying this
     *          matching rule use;
     *      DESC [qdstring] is a short descriptive string;
     *      OBSOLETE indicates this matching rule use is not active;
     *      APPLIES provides a list of attribute types the matching rule applies
     *          to; and
     *      [extensions] describe extensions.
     * </pre>
     * 
     * @param matchingRuleUse The matching rule from which we want to generate
     *  a MatchingRuleUseDescription.
     * @return The generated MatchingRuleUseDescription
     */
    public static String getDescription( MatchingRuleUse matchingRuleUse )
    {
        StringBuilder buf = new StringBuilder( "( " );
        buf.append( matchingRuleUse.getOid() );
        buf.append( '\n' );

        buf.append( " NAME " );
        getQDescrs( buf, matchingRuleUse.getNames() );

        if ( matchingRuleUse.getDescription() != null )
        {
            buf.append( " DESC " );
            buf.append( matchingRuleUse.getDescription() );
            buf.append( '\n' );
        }

        if ( matchingRuleUse.isObsolete() )
        {
            buf.append( " OBSOLETE\n" );
        }

        buf.append( " APPLIES " );
        List<AttributeType> attributeTypes = matchingRuleUse.getApplicableAttributes();

        if ( attributeTypes.size() == 1 )
        {
            buf.append( attributeTypes.get( 0 ).getOid() );
        }
        else
        // for list of oids we need a parenthesis
        {
            buf.append( "( " );

            boolean isFirst = true;

            for ( AttributeType attributeType : attributeTypes )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    buf.append( " $ " );
                }

                buf.append( attributeType );
            }

            buf.append( " ) " );
        }

        if ( matchingRuleUse.getExtensions() != null )
        {
            getExtensions( buf, matchingRuleUse.getExtensions() );
        }

        buf.append( " )\n" );

        return buf.toString();
    }


    /**
     * Generates the NameFormDescription for a NameForm as defined by the
     * syntax: 1.3.6.1.4.1.1466.115.121.1.35. Only the right hand side of the
     * description starting at the opening parenthesis is generated: that is
     * 'NameFormDescription = ' is not generated.
     * 
     * <pre>
     *  NameFormDescription = &quot;(&quot; whsp
     *      numericoid whsp               ; NameForm identifier
     *      [ &quot;NAME&quot; qdescrs ]
     *      [ &quot;DESC&quot; qdstring ]
     *      [ &quot;OBSOLETE&quot; whsp ]
     *      &quot;OC&quot; woid                     ; Structural ObjectClass
     *      &quot;MUST&quot; oids                   ; AttributeTypes
     *      [ &quot;MAY&quot; oids ]                ; AttributeTypes
     *  whsp &quot;)&quot;
     * </pre>
     * 
     * @param nameForm
     *            the NameForm to generate the description for
     * @return the NameFormDescription string
     */
    public static String getDescription( NameForm nameForm )
    {
        StringBuilder buf = new StringBuilder( "( " );
        buf.append( nameForm.getOid() );
        buf.append( '\n' );

        if ( nameForm.getNames() != null )
        {
            buf.append( " NAME " );
            getQDescrs( buf, nameForm.getNames() );
        }

        if ( nameForm.getDescription() != null )
        {
            buf.append( " DESC " );
            buf.append( nameForm.getDescription() );
            buf.append( '\n' );
        }

        if ( nameForm.isObsolete() )
        {
            buf.append( " OBSOLETE\n" );
        }

        buf.append( " OC " );
        buf.append( nameForm.getStructuralObjectClassOid() );
        buf.append( '\n' );

        buf.append( " MUST\n" );
        List<AttributeType> must = nameForm.getMustAttributeTypes();

        getQDStrings( buf, must );

        List<AttributeType> may = nameForm.getMayAttributeTypes();

        if ( ( may != null ) && ( may.size() > 0 ) )
        {
            buf.append( " MAY\n" );
            getQDStrings( buf, may );
        }

        if ( nameForm.getExtensions() != null )
        {
            getExtensions( buf, nameForm.getExtensions() );
        }

        buf.append( " )\n" );
        return buf.toString();
    }


    /**
     * Generates the NormalizerDescription for a Normalizer. Only the right 
     * hand side of the description starting at the opening parenthesis is 
     * generated: that is 'NormalizerDescription = ' is not generated.
     * 
     * <pre>
     * NormalizerDescription = &quot;(&quot;
     *     numericoid                          
     *     [&quot;DESC&quot; qdstring ]
     *     &quot;FQCN&quot; whsp fqcn
     *     [&quot;BYTECODE&quot; whsp base64  ]
     *     extensions 
     *     &quot;)&quot;
     * </pre>
     * 
     * @param normalizer
     *            the Normalizer to generate the description for
     * @return the NormalizerDescription string
     */
    public static String getDescription( Normalizer normalizer )
    {
        return getLoadableDescription( normalizer );
    }


    /**
     * Generates the ObjectClassDescription for an ObjectClass as defined by the
     * syntax: 1.3.6.1.4.1.1466.115.121.1.37. Only the right hand side of the
     * description starting at the opening parenthesis is generated: that is
     * 'ObjectClassDescription = ' is not generated.
     * 
     * <pre>
     *  ObjectClassDescription = &quot;(&quot; whsp
     *      numericoid whsp     ; ObjectClass identifier
     *      [ &quot;NAME&quot; qdescrs ]
     *      [ &quot;DESC&quot; qdstring ]
     *      [ &quot;OBSOLETE&quot; whsp ]
     *      [ &quot;SUP&quot; oids ]      ; Superior ObjectClasses
     *      [ ( &quot;ABSTRACT&quot; / &quot;STRUCTURAL&quot; / &quot;AUXILIARY&quot; ) whsp ]
     *                          ; default structural
     *      [ &quot;MUST&quot; oids ]     ; AttributeTypes
     *      [ &quot;MAY&quot; oids ]      ; AttributeTypes
     *  whsp &quot;)&quot;
     * </pre>
     * 
     * @param objectClass
     *            the ObjectClass to generate a description for
     * @return the description in the ObjectClassDescription syntax
     */
    public static String getDescription( ObjectClass objectClass )
    {
        StringBuilder buf = new StringBuilder( "( " );
        buf.append( objectClass.getOid() );
        buf.append( '\n' );

        if ( ( objectClass.getNames() != null ) && ( objectClass.getNames().size() != 0 ) )
        {
            buf.append( " NAME " );
            getQDescrs( buf, objectClass.getNames() );
        }

        if ( objectClass.getDescription() != null )
        {
            buf.append( " DESC " );
            buf.append( objectClass.getDescription() );
            buf.append( '\n' );
        }

        if ( objectClass.isObsolete() )
        {
            buf.append( " OBSOLETE\n" );
        }

        List<ObjectClass> sups = objectClass.getSuperiors();

        if ( ( sups != null ) && ( sups.size() > 0 ) )
        {
            buf.append( " SUP " );
            getQDStrings( buf, sups );
        }

        if ( objectClass.getType() != null )
        {
            buf.append( ' ' );
            buf.append( objectClass.getType() );
            buf.append( '\n' );
        }

        List<AttributeType> must = objectClass.getMustAttributeTypes();

        if ( ( must != null ) && ( must.size() > 0 ) )
        {
            buf.append( " MUST " );
            getQDStrings( buf, must );
        }

        List<AttributeType> may = objectClass.getMayAttributeTypes();

        if ( ( may != null ) && ( may.size() > 0 ) )
        {
            buf.append( " MAY " );
            getQDStrings( buf, may );
        }

        if ( objectClass.getExtensions() != null )
        {
            getExtensions( buf, objectClass.getExtensions() );
        }

        buf.append( " )\n" );

        return buf.toString();
    }


    /**
     * Generates the SyntaxDescription for a Syntax as defined by the syntax:
     * 1.3.6.1.4.1.1466.115.121.1.54. Only the right hand side of the
     * description starting at the opening parenthesis is generated: that is
     * 'SyntaxDescription = ' is not generated.
     * 
     * <pre>
     *  SyntaxDescription = &quot;(&quot; whsp
     *      numericoid whsp
     *      [ &quot;DESC&quot; qdstring ]
     *      [ extensions ]
     *      whsp &quot;)&quot;
     * </pre>
     * 
     * @param syntax
     *            the Syntax to generate a description for
     * @return the description in the SyntaxDescription syntax
     */
    public static String getDescription( LdapSyntax syntax )
    {
        StringBuilder buf = new StringBuilder( "( " );
        buf.append( syntax.getOid() );
        buf.append( '\n' );

        if ( syntax.getDescription() != null )
        {
            buf.append( " DESC " );
            buf.append( syntax.getDescription() );
            buf.append( '\n' );
        }

        if ( syntax.getExtensions() != null )
        {
            getExtensions( buf, syntax.getExtensions() );
        }

        buf.append( " )" );
        return buf.toString();
    }


    /**
     * Generates the SyntaxCheckerDescription for a SyntaxChecker. Only the right 
     * hand side of the description starting at the opening parenthesis is 
     * generated: that is 'SyntaxCheckerDescription = ' is not generated.
     * 
     * <pre>
     * SyntaxCheckerDescription = &quot;(&quot;
     *     numericoid                          
     *     [&quot;DESC&quot; qdstring ]
     *     &quot;FQCN&quot; whsp fqcn
     *     [&quot;BYTECODE&quot; whsp base64  ]
     *     extensions 
     *     &quot;)&quot;
     * </pre>
     * 
     * @param syntaxChecker
     *            the SyntaxChecker to generate the description for
     * @return the SyntaxCheckerDescription string
     */
    public static String getDescription( SyntaxChecker syntaxChecker )
    {
        return getLoadableDescription( syntaxChecker );
    }


    private static void getExtensions( StringBuilder sb, Map<String, List<String>> extensions )
    {
        for ( String key : extensions.keySet() )
        {
            sb.append( key ).append( " " );

            List<String> values = extensions.get( key );

            if ( ( values != null ) && ( values.size() != 0 ) )
            {
                if ( values.size() == 1 )
                {
                    sb.append( values.get( 0 ) );
                }
                else
                {
                    boolean isFirst = true;
                    sb.append( "( " );

                    for ( String value : values )
                    {
                        if ( isFirst )
                        {
                            isFirst = false;
                        }
                        else
                        {
                            sb.append( " " );
                        }

                        sb.append( value );
                    }

                    sb.append( " )" );
                }
            }

            sb.append( '\n' );
        }
    }


    private static void getQDStrings( StringBuilder sb, List<? extends SchemaObject> schemaObjects )
    {
        if ( ( schemaObjects != null ) && ( schemaObjects.size() != 0 ) )
        {
            if ( schemaObjects.size() == 1 )
            {
                sb.append( '\'' ).append( schemaObjects.get( 0 ).getName() ).append( '\'' );
            }
            else
            {
                boolean isFirst = true;
                sb.append( "( " );

                for ( SchemaObject schemaObject : schemaObjects )
                {
                    if ( isFirst )
                    {
                        isFirst = false;
                    }
                    else
                    {
                        sb.append( " " );
                    }

                    sb.append( '\'' ).append( schemaObject.getName() ).append( '\'' );
                }

                sb.append( " )" );
            }
        }

        sb.append( '\n' );
    }


    private static void getQDescrs( StringBuilder sb, List<String> names )
    {
        if ( ( names != null ) && ( names.size() != 0 ) )
        {
            if ( names.size() == 1 )
            {
                sb.append( '\'' ).append( names.get( 0 ) ).append( '\'' );
            }
            else
            {
                boolean isFirst = true;
                sb.append( "( " );

                for ( String name : names )
                {
                    if ( isFirst )
                    {
                        isFirst = false;
                    }
                    else
                    {
                        sb.append( " " );
                    }

                    sb.append( '\'' ).append( name ).append( '\'' );
                }

                sb.append( " )" );
            }
        }

        sb.append( '\n' );
    }


    /**
     * Generate the description for Comparators, Normalizers and SyntaxCheckers.
     */
    private static String getLoadableDescription( LoadableSchemaObject schemaObject )
    {
        StringBuilder buf = new StringBuilder( "( " );
        buf.append( schemaObject.getOid() );
        buf.append( '\n' );

        if ( schemaObject.getDescription() != null )
        {
            buf.append( " DESC " );
            buf.append( schemaObject.getDescription() );
            buf.append( '\n' );
        }

        if ( schemaObject.getFqcn() != null )
        {
            buf.append( " FQCN " );
            buf.append( schemaObject.getFqcn() );
            buf.append( '\n' );
        }

        if ( schemaObject.getBytecode() != null )
        {
            buf.append( " BYTECODE " );

            // We will dump only the 16 first bytes
            if ( schemaObject.getBytecode().length() > 16 )
            {
                buf.append( schemaObject.getBytecode().substring( 0, 16 ) );
            }
            else
            {
                buf.append( schemaObject.getBytecode() );
            }

            buf.append( '\n' );
        }

        if ( schemaObject.getExtensions() != null )
        {
            getExtensions( buf, schemaObject.getExtensions() );
        }

        buf.append( " ) " );

        return buf.toString();
    }
}
