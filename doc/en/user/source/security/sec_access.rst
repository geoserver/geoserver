.. _sec_access:

Accessing secured resources
===========================

The :ref:`web_admin` is secured by form-based authentication, with an optional "remember-me" cookie setting.  The OGC services are secured using HTTP BASIC authentication, provided on each call.

The form-based authentication is based on browser session, so if the same browser is used to access services as well, the authentication will be remembered.

Here is the process for accessing a secured resource:

#. If no authentication is provided, anonymous login will be assumed.
#. If any authentication information is included, it will be used.

   * In the case of form-based information, a session will be created to store it.
   * In the case of HTTP BASIC authentication, session integration will be performed only if a session is already available (to avoid overhead).

#. If the resource being accessed is secured and the current user is anonymous, authentication will be requested either using HTTP BASIC authentication (for services) or by using form based login (for the web adminsitration interface).
#. If the resource accessed is secured and the currently authenticated user lacks sufficient access rights, an HTTP 404 error will be returned.