
<#macro table e>
  <table class="system-table">
  <#list e.table as row>
    <tr>
    <#list row as col>
      <#if col.type = "spc">
      <td class="spc"></td>
      <#else>
        <#assign class>
          <#if col.type = "base">tbase<#elseif col.type = "interface">tinterface<#else>tcomponent<#if col['bottom']??>-bottom</#if></#if></#assign>
        <#assign colspan><#if col.columns != 1> colspan=${col.columns}</#if></#assign>
      <td class="${class}"${colspan}>${col.entity}</td>
      </#if>
    </#list>
    </tr>
  </#list>
  </table>
</#macro>

<!DOCTYPE html>
<html>
<head>
<title>${workspace} - Components</title>

<link rel="stylesheet" type="text/css" href="style.css">

</head>
<body>

<img src="../logo.png" alt="Polylith" style="width:200px;">

<h1>Components</h1>

<#list components as component>
<h3>${component.name}</h3>
  <@table e=component/>
</#list>

</body>
</html>
