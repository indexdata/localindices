/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.dao;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.Filterable;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;

/**
 *
 * @author ne
 */
public class EntityQuery {

  private String filter = "";
  private List<String> keywordAllFields = new ArrayList();
  private String acl = "";
  private String startsWithField = "";
  private String startsWith = "";
  private static Logger LOGGER = Logger.getLogger("com.indexdata.masterkey.harvester");
  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter, Filterable entity) {
    setFilter(filter, entity.getKeywordAllFields());
  }

  public void setFilter(String filter, List<String> keywordAllFields) {
    this.filter = filter == null ? "" : filter;
    this.keywordAllFields = keywordAllFields;
  }

  public EntityQuery withFilter(String filter, Filterable entity) {
    setFilter(filter, entity);
    return this;
  }

  public EntityQuery withFilter(String filter, List<String> keywordAllFields) {
    setFilter(filter, keywordAllFields);
    return this;
  }

  public boolean hasFilter() {
    return filter.length() > 0;
  }

  public String getAcl() {
    return acl;
  }

  public void setAcl(String acl) {
    this.acl = (acl == null ? "" : acl);
  }

  public EntityQuery withAcl(String acl) {
    if (isNotEmpty(acl)) setAcl(acl);
    return this;
  }

  public void setStartsWith(String startsWith, String field) {
    if (isNotEmpty(startsWith) && isNotEmpty(field)) {
      this.startsWith = startsWith;
      this.startsWithField = field;
    }
  }

  public boolean hasStartsWith () {
    return startsWith != null && startsWith.length()>0
            && startsWithField != null && startsWithField.length()>0;
  }

  public String getStartsWith () {
    return startsWith;
  }

  public EntityQuery withStartsWith(String startsWith, String field) {
    setStartsWith(startsWith, field);
    return this;
  }

  public String getStartsWithWhereClause (String tableAlias) {
    return tableAlias + "." + startsWithField + " LIKE '" + startsWith +"%'";
  }

  public boolean hasAcl () {
    return (acl.length()>0);
  }

  public boolean hasQuery() {
    return (hasStartsWith() || hasFilter() || hasAcl());
  }

  public String asUrlParameters () {
    String UTF8 = "UTF-8";
    try {
      return (hasFilter() ? "&filter="+URLEncoder.encode(filter, UTF8) : "") +
             (hasAcl() ? "&acl="+URLEncoder.encode(acl, UTF8) : "") +
             (hasStartsWith() ? "&prefix="+URLEncoder.encode(startsWith, UTF8) : "");
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Error generating URL query parameters: "+e);
      return "";
    }
  }


  /**
  * Constructs where clause that would look for 'filter' in any of the provided columns.
  * @param filterString
  * @param columns
  * @param tableAlias
  * @return " where concat( [alias].[cols-1], '||', [alias].[cols-2], ...) like '%[filter]%' "
  */
  public String getFilteringWhereClause(String tableAlias, boolean withWHERE) {
    StringBuilder expr = new StringBuilder("");
    if (filter != null && filter.length()>0 && keywordAllFields != null && keywordAllFields.size()>0) {
      int i = 0;
      expr.append(withWHERE ? " WHERE " : "");
      for (String column : keywordAllFields) {
        if (i++==0) expr.append("concat(");
        else expr.append(",'||',");
        if (tableAlias != null && tableAlias.length()>0) {
          expr.append("COALESCE(");
          expr.append(tableAlias);
          expr.append(".");
        }
        expr.append(column);
        expr.append(", '')");
      }
      expr.append(") like '%");
      expr.append(filter);
      expr.append("%'");
    }
    return expr.toString();
  }

  public String getFilteringWhereClause(String tableAlias) {
    return getFilteringWhereClause(tableAlias, false);
  }

  public String getAclWhereClause (String tableAlias) {
    return getAclWhereClause(tableAlias,false);
  }

  public String getAclWhereClause (String tableAlias, boolean withWHERE ) {
    StringBuilder expr = new StringBuilder("");
    if (hasAcl()) {
      expr.append(withWHERE ? " WHERE " : "")
              .append(tableAlias)
              .append(".")
              .append("acl='")
              .append(acl)
              .append("' ");
    }
    return expr.toString();
  }

  public String asWhereClause (String tableAlias, boolean withWHERE) {
    String whereClause = "";
    if (hasQuery()) {
      List<String> whereClauses = new ArrayList();
      if (hasFilter()) {
        System.out.println("has filter");
        whereClauses.add(getFilteringWhereClause(tableAlias));
      }
      if (hasAcl()) {
        System.out.println("has acl");
        whereClauses.add(getAclWhereClause(tableAlias));
      }
      if (hasStartsWith()) {
        System.out.println("has startswith");
        whereClauses.add(getStartsWithWhereClause(tableAlias));
      }
      Iterator iter = whereClauses.iterator();
      while (iter.hasNext()) {
        whereClause += iter.next();
        if (iter.hasNext()) whereClause += " AND ";
      }
      if (withWHERE) {
        whereClause = " WHERE " + whereClause;
      }
    }
    return whereClause;
  }

  private static boolean isNotEmpty(String str) {
    return str != null && !str.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    String.join(", ", keywordAllFields);
    if (o instanceof EntityQuery) {
      EntityQuery p = (EntityQuery)o;
      return (p.filter.equals(this.filter)
              && p.acl.equals(this.acl)
              && p.startsWith.equals(this.startsWith)
              && p.startsWithField.equals(this.startsWithField)
              && String.join(",", p.keywordAllFields).equals(String.join(",", this.keywordAllFields)));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 29 * hash + (this.filter != null ? this.filter.hashCode() : 0);
    hash = 29 * hash + (this.acl != null ? this.acl.hashCode() : 0);
    hash = 29 * hash + (this.startsWith != null ? this.startsWith.hashCode() : 0);
    hash = 29 * hash + (this.startsWithField != null ? this.startsWithField.hashCode() : 0);
    hash = 29 * hash + (this.keywordAllFields != null ? this.keywordAllFields.hashCode() : 0);
    return hash;
  }

  /** Main is for testing query generation
   *
   * @param args ignored
   */
  public static void main (String[] args) {
    EntityQuery query = new EntityQuery();
    Harvestable res = new OaiPmhResource();
    query.setFilter("test", res);
    query.setAcl("diku");
    query.setStartsWith("cf","name");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause("o", true));
  }

}
