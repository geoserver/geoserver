<#include "head.ftl">

<#if properties.workspace.name??>
    Workspace Name:  "${properties.workspace.name!}"
<#else>
    Local settings have not been configured for this workspace
</#if>

Contact Information: 

<ul>
  <li>Contact:  "${properties.contactPerson!}"</li>
  <li>Organization:  "${properties.contactOrganization!}"</li>
  <li>Position:  "${properties.contactPosition!}"</li>
  <li>Address Type:  "${properties.addressType!}"</li>
  <li>Address:  "${properties.address!}"</li>
  <li>City:  "${properties.addressCity!}"</li>
  <li>State:  "${properties.addressState!}"</li>
  <li>ZIP code:  "${properties.addressPostalCode!}"</li>
  <li>Country:  "${properties.addressCountry!}"</li>
  <li>Telephone:  "${properties.contactVoice!}"</li>
  <li>Fax:  "${properties.contactFacsimile!}"</li>
  <li>Email:  "${properties.contactEmail!}"</li>
</ul>

Settings:

<ul>
  <li>Verbose Messages:  "${properties.verbose!}"</li>
  <li>Verbose Exception Reporting:  "${properties.verboseExceptions!}"</li>
  <li>Number of Decimals:  "${properties.numDecimals!}"</li>
  <li>Character Set:  "${properties.charset!}"</li>
  <li>Proxy Base URL:  "${properties.proxyBaseUrl!}"</li>
</ul>

<#include "tail.ftl">