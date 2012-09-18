<security>
  <roleServiceName>default</roleServiceName>
  <authProviderNames>
    <string>default</string>
  </authProviderNames>
  <anonymousAuth>true</anonymousAuth>
  <configPasswordEncrypterName>pbePasswordEncoder</configPasswordEncrypterName>
  <encryptingUrlParams>false</encryptingUrlParams>
  <filterChain>
    <filters name="web" path="/web/**,/gwc/rest/web/**">
      <filter>contextAsc</filter>
      <filter>rememberme</filter>
      <filter>anonymous</filter>
      <filter>guiException</filter>
      <filter>interceptor</filter>
    </filters>
    <filters name="webLogin" path="/j_spring_security_check,/j_spring_security_check/">
      <filter>contextAsc</filter>
      <filter>form</filter>
    </filters>
    <filters name="webLogout" path="/j_spring_security_logout,/j_spring_security_logout/">
      <filter>contextAsc</filter>
      <filter>formLogout</filter>
    </filters>
    <filters name="rest" path="/rest/**">
      <filter>contextNoAsc</filter>
      <filter>basic</filter>
      <filter>anonymous</filter>
      <filter>exception</filter>
      <filter>restInterceptor</filter>
    </filters>
    <filters name="gwc" path="/gwc/rest/**">
      <filter>contextNoAsc</filter>
      <filter>basic</filter>
      <filter>exception</filter>
      <filter>restInterceptor</filter>
    </filters>
    <filters name="default" path="/**">
      <filter>contextNoAsc</filter>
      <filter>basic</filter>
      <filter>anonymous</filter>
      <filter>exception</filter>
      <filter>interceptor</filter>
    </filters>
  </filterChain>
  <rememberMeService>
    <className>org.geoserver.security.rememberme.GeoServerTokenBasedRememberMeServices</className>
    <key>geoserver</key>
  </rememberMeService>
</security>