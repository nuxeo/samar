<li class="${entity.entity.summary?has_content?string('entityFacet tag', 'tag')}">${entity.title}
  <a href="#"  class="removeEntityLink" entity="${entity.id}">x</a></li>
<#if entity.entity.summary?has_content>
<div class="entityTooltip">
  <h3 dir="auto">${entity.title}</h3>
  <p class="ellipsis">${entity.entity.summary}</p>
</div>
</#if>
