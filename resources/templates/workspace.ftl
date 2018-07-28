<!DOCTYPE html>
<html>
<head>
<title>${title}</title>

<link rel="stylesheet" type="text/css" href="style.css">

</head>
<body>

<table class="polylithTable">
<#list table as row>
  <tr>
  <#list row as col>
    <#if col.type = "spc">
    <td class="spc"></td><#-- blah -->
    <#else>
      <#assign base><#if col.type = "base"> class="base"</#if></#assign>
      <#assign colspan><#if col.columns != 1> colspan=${col.columns}</#if></#assign>
    <td${base}${colspan}>${col.entity}</td>
    </#if>
  </#list>
  </tr>
</#list>
</table>

</body>
</html>
