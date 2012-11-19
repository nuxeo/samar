<ul class="entityOccurrences lang-ar">
  <!-- lang-ar is hardcoded for now:

   TODO: support multilingual entity fields at least for names and summaries.
   -->
  <#list result.occurrences as occurrence>
  <#assign entity = occurrence.targetEntity>
  <li class="entityOccurrence tag entity-${entity.type}">
    <a href="${This.currentQueryUrl}&entity=${entity.id}">${entity.title}</a></li>
  <div class="entityTooltip">
    <div class="entityDepiction">
      <#if entity.entity.depiction.filename?has_content>
      <img src="${This.bigFileUrl(entity, 'entity:depiction', '')}" />
      <#else>
      <img src="${This.baseURL}icons/${entity.type}_100.png" />
      </#if>
    </div>
    <div class="entitySummary">
      <h3 dir="auto"><a href="${This.getBackofficeURL(entity)}">${entity.title}</a></h3>
      <p class="ellipsis" dir="auto">${entity.entity.summary}</p>
    </div>
    <div style="clear: both"></div>
    <#if occurrence.getOccurrences(3)?has_content>
    <h4 dir="auto">${Context.getMessage('heading.mentionsInCurrentDocument')}</h4>
    <#list occurrence.getOccurrences(3) as mentionContext>
      <p dir="auto">...${mentionContext.prefixContext} <span class="mention">${mentionContext.mention}</span>
        ${mentionContext.suffixContext}...</p>
    </#list>
    </#if>
  </div>
  </#list>
</ul>