<!DOCTYPE html>
<html>
<head>
<title>${workspace}</title>

<link rel="stylesheet" type="text/css" href="style.css">

</head>
<body>

<img src="../logo.png" alt="Polylith" style="width:200px;">

<h1>${workspace}</h1>

<h4>libraries:</h4>
...
<p class="clear"/>

<h4>Interfaces:</h4>
<#list interfaces as interface>
<div class="interface">${interface}</div>
</#list>
<p class="clear"/>

<h4>Components:</h4>
<#list components as component>
<div class="component">${component}</div>
</#list>
<p class="clear"/>

<h4>Bases:</h4>
<#list bases as base>
<div class="base">${base}</div>
</#list>
<p class="clear"/>

<#list systems as system>
<h4>${system.name}:</h4>
 <table class="design">
  <#list system.table as row>
  <tr>
    <#list row as col>
      <#if col.type = "spc">
    <td class="spc"></td>
      <#else>
        <#assign class><#if col.type = "base">tbase<#else>comp</#if></#assign>
        <#assign colspan><#if col.columns != 1> colspan=${col.columns}</#if></#assign>
    <td class="${class}"${colspan}>${col.entity}</td>
      </#if>
    </#list>
  </tr>
  </#list>
</table>
</#list>

</body>
</html>
