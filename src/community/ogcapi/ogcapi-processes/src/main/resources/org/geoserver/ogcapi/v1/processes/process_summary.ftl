<div class="card-body">
  <ul>
    <#if process.title??>
      <li><b>Title</b>: <span id="${process.htmlId}_title">${process.title}</span><br/></li>
      </#if>
      <#if process.description??>
      <li><b>Description</b>: <span id="${process.htmlId}_description">${process.description!}</span><br/></li>
      </#if>
  </ul>
</div>

