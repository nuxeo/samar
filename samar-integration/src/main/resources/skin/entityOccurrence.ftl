<ul class="entityOccurrences">
  <#list result.occurrences as occurrence>
  <li class="${occurrence.targetEntity.entity.summary?has_content?string('entityOccurrence tag', 'tag')}">
    <a href="${This.currentQueryUrl}&entity=${occurrence.targetEntity.id}">${occurrence.targetEntity.title}</a></li>
  <#if occurrence.targetEntity.entity.summary?has_content>
    <div class="entityTooltip">
      <h3 dir="auto">${occurrence.targetEntity.title}</h3>
      <p class="ellipsis">${occurrence.targetEntity.entity.summary}</p>
    </div>
  </#if>
  </#list>
</ul>