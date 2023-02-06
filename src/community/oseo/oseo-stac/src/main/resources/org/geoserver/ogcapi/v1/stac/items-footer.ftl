<#-- 
Footer section of the STAC Items HTML output. Should close the body and the html tag.
-->

  <#if previous?? || next??>
  <div>
  <nav>
    <ul class="pagination">
    <#if previous??>
      <li class="page-item">
        <a class="page-link" id="prevPage" href="${previous}">Previous page</a>
      </li>
    <#else>
      <li class="page-item disabled">
        <a class="page-link" href="#" tabindex="-1" aria-disabled="true">Previous page</a>
      </li>
    </#if>
    <#if next??>
      <li class="page-item">
        <a class="page-link"id="nextPage" href="${next}">Next page</a>
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