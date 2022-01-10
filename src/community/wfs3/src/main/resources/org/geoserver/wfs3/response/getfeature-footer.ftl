<#-- 
Footer section of the GetFeature HTML output. Should close the body and the html tag.
If the WFS version built them, the default template will also add links in the output 
-->

  <#if response.previous?? || response.next??>
  <div>
  <#if response.previous??>
    <a id="prevPage" href="${response.previous}">Previous page</a>
  <#else>
     Previous page
  </#if>
  -
  <#if response.next??>
    <a id="nextPage" href="${response.next}">Next page</a>
  <#else>
    Next page   
  </#if>
  </#if>
  
<#include "common-footer.ftl">