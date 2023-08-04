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
package org.apache.directory.shared.ldap.filter;

/**
 * All the different kind of assertions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 470116 $
 */
public enum AssertionType
{
    /** equality assertion node */
    EQUALITY,

    /** presence assertion node */
    PRESENCE,

    /** substring match assertion node */
    SUBSTRING,

    /** greater than or equal to assertion node */
    GREATEREQ,

    /** less than or equal to assertion node */
    LESSEQ,

    /** approximate assertion node */
    APPROXIMATE,

    /** extensible match assertion node */
    EXTENSIBLE,

    /** scope assertion node */
    SCOPE,

    /** Predicate assertion node */
    ASSERTION,

    /** OR operator constant */
    OR,

    /** AND operator constant */
    AND,

    /** NOT operator constant */
    NOT
}
