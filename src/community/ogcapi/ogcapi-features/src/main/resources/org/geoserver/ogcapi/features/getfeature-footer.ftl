<#-- 
Footer section of the GetFeature HTML output. Should close the body and the html tag.
If the WFS version built them, the default template will also add links in the output 
-->

  <#if response.previous?? || response.next??>
  <div>
  <nav>
    <ul class="pagination">
    <#if response.previous??>
      <li class="page-item">
        <a class="page-link" id="prevPage" href="${response.previous}">Previous page</a>
      </li>
    <#else>
      <li class="page-item disabled">
        <a class="page-link" href="#" tabindex="-1" aria-disabled="true">Previous page</a>
      </li>
    </#if>
    <#if response.next??>
      <li class="page-item">
        <a class="page-link"id="nextPage" href="${response.next}">Next page</a>
      </li>
    <#else>
      <li class="page-item disabled">
        <a class="page-link" href="#" tabindex="-1" aria-disabled="true">Next page</a>
      </li>
    </#if>
    </ul>
  </nav>
  </#if>
  
<#include "common-footer.ftl">