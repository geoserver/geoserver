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

package org.apache.directory.shared.ldap.trigger;

import java.util.List;

import org.apache.commons.lang.NullArgumentException;
import org.apache.directory.shared.i18n.I18n;

/**
 * The Trigger Specification Bean.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class TriggerSpecification
{
    
    private LdapOperation ldapOperation;
    
    private ActionTime actionTime;
    
    private List<SPSpec> spSpecs; 
    
    
    public TriggerSpecification( LdapOperation ldapOperation, ActionTime actionTime, List<SPSpec> spSpecs )
    {
        super();
        if ( ldapOperation == null || 
            actionTime == null || 
            spSpecs == null )
        {
            throw new NullArgumentException( I18n.err( I18n.ERR_04331 ) );
        }
        if ( spSpecs.size() == 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04332 ) );
        }
        this.ldapOperation = ldapOperation;
        this.actionTime = actionTime;
        this.spSpecs = spSpecs;
    }

    public ActionTime getActionTime()
    {
        return actionTime;
    }

    public LdapOperation getLdapOperation()
    {
        return ldapOperation;
    }

    public List<SPSpec> getSPSpecs() {
        return spSpecs;
    }
    
    public static class SPSpec
    {
        private String name;
        
        private List<StoredProcedureOption> options;
        
        private List<StoredProcedureParameter> parameters;

        public SPSpec(String name, List<StoredProcedureOption> options, List<StoredProcedureParameter> parameters) {
            super();
            this.name = name;
            this.options = options;
            this.parameters = parameters;
        }
        
        public String getName() {
            return name;
        }

        public List<StoredProcedureOption> getOptions() {
            return options;
        }

        public List<StoredProcedureParameter> getParameters() {
            return parameters;
        }

        @Override
        /**
         * Compute the instance's hash code
         * @return the instance's hash code 
         */
        public int hashCode() {
            int h = 37;
            
            h = h*17 + ((name == null) ? 0 : name.hashCode());
            h = h*17 + ((options == null) ? 0 : options.hashCode());
            h = h*17 + ((parameters == null) ? 0 : parameters.hashCode());
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final SPSpec other = (SPSpec) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (options == null) {
                if (other.options != null)
                    return false;
            } else if (!options.equals(other.options))
                return false;
            if (parameters == null) {
                if (other.parameters != null)
                    return false;
            } else if (!parameters.equals(other.parameters))
                return false;
            return true;
        }

    }
    
}
