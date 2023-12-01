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

package org.apache.directory.shared.ldap.message.internal;


/**
 * Super interface used as a marker for all protocol response type messages.
 * Note that only 2 response interfaces directly extend this interfaces. They
 * are listed below:
 * <ul>
 * <li> SearchResponseEntry </li>
 * <li> SearchResponseReference </li>
 * </ul>
 * <br>
 * All other responses derive from the ResultResponse interface. These responses
 * unlike the three above have an LdapResult component. The ResultResponse
 * interface takes this into account providing a Response with an LdapResult
 * property.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 905344 $
 */
public interface InternalResponse extends InternalMessage
{
}
