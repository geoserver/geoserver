<#include "head.ftl">

Global Contact:

<ul>
  <li>Address:  "${properties.address}"</li>
  <li>Address Type  "${properties.addressType}"</li>
  <li>City:  "${properties.addressCity}"</li>
  <li>State:  "${properties.addressState}"</li>
  <li>ZIP Code:  "${properties.addressPostalCode}"</li>
  <li>Country:  "${properties.addressCountry}"</li>
  <li>Contact:  "${properties.contactPerson}"</li>
  <li>Email:  "${properties.contactEmail}"</li>
  <li>Organization:  "${properties.contactOrganization}"</li>
  <li>Position:  "${properties.contactPosition}"</li>
  <li>Telephone:  "${properties.contactVoice}"</li>
  <li>Fax:  "${properties.contactFacsimile}"</li>
</ul>


<#include "tail.ftl">