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

import java.util.Map;


/**
 * A class is used to resolve the normalizer mapping hash used for normalization.
 * This interface is implemented and passed into several kinds of parsers that
 * need to handle the normalization of LDAP name strings.
 * 
 * Why you may ask are we doing this?  Why not just pass in the map of 
 * normalizers to these parsers and let them use that?  First off this mapping
 * will not be static when dynamic updates are enabled to schema.  So if
 * we just passed in the map then there would be no way to set a new map or
 * trigger the change of the map when schema changes.  Secondly we cannot just
 * pass server side objects that return this mapping because these parsers may
 * and will be used in client side applications.  They will not have access to
 * these server side objects that generate these mappings.  Instead when a 
 * resolver is used we can create mock or almost right implementations.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface NormalizerMappingResolver
{
    Map getNormalizerMapping() throws Exception;
}
