/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.dao;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author ne
 */
public class EntityQuery {

  private String filter = "";
  private String acl = "";

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = (filter == null ? "" : filter);
  }

  public String getAcl() {
    return acl;
  }

  public void setAcl(String acl) {
    this.acl = (acl == null ? "" : acl);
  }

  public boolean hasAcl () {
    return (acl.length()>0);
  }

  public boolean hasQuery() {
    return (acl.length()>0 || filter.length()>0);
  }

  public boolean hasFilter() {
    return filter.length() > 0;
  }

  public String asUrlParameters () {
    return (hasFilter() ? "&filter="+filter : "") +
           (hasAcl() ? "&acl="+acl : "");
  }

  /**
  * Constructs where clause that would look for 'filter' in any of the provided columns.
  * @param filterString
  * @param columns
  * @param tableAlias
  * @return " where concat( [alias].[cols-1], '||', [alias].[cols-2], ...) like '%[filter]%' "
  */
  public String getFilteringWhereClause(List<String> columns, String tableAlias, boolean withWHERE) {
    StringBuilder expr = new StringBuilder("");
    if (filter != null && filter.length()>0 && columns != null && columns.size()>0) {
      int i = 0;
      expr.append(withWHERE ? " WHERE " : "");
      for (String column : columns) {
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

  public String getAclWhereClause (String tableAlias, boolean withWHERE ) {
    StringBuilder expr = new StringBuilder("");
    expr.append(withWHERE ? " WHERE " : "")
            .append(tableAlias)
            .append(".")
            .append("acl='")
            .append(acl)
            .append("' ");

    return expr.toString();
  }

  public String asWhereClause (List<String> columnsForFiltering, String tableAlias, boolean withWHERE) {
    String whereClause = "";
    String wherefilter = (hasFilter() ? getFilteringWhereClause(columnsForFiltering, tableAlias, false) : "");
    String whereacl =  (hasAcl() ? getAclWhereClause(tableAlias, false) : "");
    if (hasFilter() && hasAcl()) {
        whereClause = wherefilter + " AND " + whereacl;
    } else {
        whereClause = wherefilter + whereacl;
    }
    if (whereClause.length()>0) {
      return (withWHERE ? " WHERE " + whereClause : "");
    } else {
      return "";
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof EntityQuery) {
      EntityQuery p = (EntityQuery)o;
      return (p.filter.equals(this.filter) && p.acl.equals(this.acl));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 29 * hash + (this.filter != null ? this.filter.hashCode() : 0);
    hash = 29 * hash + (this.acl != null ? this.acl.hashCode() : 0);
    return hash;
  }

  /** Main is for testing query generation
   *
   * @param args ignored
   */
  public static void main (String[] args) {
    List<String> filterByColumns = Arrays.asList(
              "name",
              "description",
              "technicalNotes",
              "contactNotes",
              "serviceProvider",
              "usedBy",
              "managedBy",
              "currentStatus");

    EntityQuery query = new EntityQuery();
    query.setFilter("test");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause(filterByColumns, "o", true));
  }

}
