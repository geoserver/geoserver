import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


public class LDAP {

    public static void main(String[] args) throws Exception {
     // Set up environment for creating initial context
        Hashtable env = new Hashtable(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        // Must use the name of the server that is found in its certificate
        env.put(Context.PROVIDER_URL, "ldap://192.168.0.103:389/dc=skunkworks,dc=org");

        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "uid=justin,ou=people,dc=skunkworks,dc=org");
        env.put(Context.SECURITY_CREDENTIALS, "foobar");

                // Create initial context
        LdapContext ctx = new InitialLdapContext(env, null);
        
//        // Start TLS
//        StartTlsResponse tls =
//            (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
//        tls.setHostnameVerifier(new HostnameVerifier() {
//            
//            @Override
//            public boolean verify(String hostname, SSLSession session) {
//                return true;
//            }
//        });
//        SSLSession sess = tls.negotiate();
//
//        // ... do something useful with ctx that requires secure connection
//
////        SearchControls ctrl = new SearchControls();
////        ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
////        NamingEnumeration enumeration = ctx.search("", query, ctrl);
////        while (enumeration.hasMore()) {
////            SearchResult result = (SearchResult) enumeration.next();
////            Attributes attribs = result.getAttributes();
////            NamingEnumeration values = ((BasicAttribute) attribs.get(attribute)).getAll();
////            while (values.hasMore()) {
////              if (output.length() > 0) {
////                output.append("|");
////              }
////              output.append(values.next().toString());
////            }
////        }
////        
//        // Stop TLS
//        tls.close();
//
//        // ... do something useful with ctx that doesn't require security

        Enumeration e= ctx.search("ou=people", "uid=justin",null);
        while(e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }
        // Close the context when we're done
        ctx.close();
    }
}
