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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.Filterable;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;

/**
 *
 * @author ne
 */
public class EntityQuery {

  private Query query;
  private String filter = "";
  private List<String> keywordAllFields = new ArrayList<>();
  private String acl = "";
  private String startsWithField = "";
  private String startsWith = "";
  private static final Logger LOGGER = Logger.getLogger("com.indexdata.masterkey.harvester");


  private Query getQuery () {
    return query;
  }

  public void setQuery(String query) {
    this.query = new Query(query);
  }

  private boolean hasQuery () {
    return query != null && query.hasQuery();
  }

  private class Query {
    String query= "";
    String term = "";
    String operator = "";
    String value = "";

    Pattern qry = Pattern.compile("\\(?([.\\w]+)[ ]*(!?=)[ ]*(.+)\\)?");
    Query (String query) {
      if (query != null && !query.isEmpty())
      {
        LOGGER.info( "Constructing Query object from " + query );
        if (query.startsWith("(") && query.endsWith(")"))
          query = query.substring(1, query.length()-1);
        Matcher matcher = qry.matcher( query );
        if ( matcher.find() )
        {
          term = matcher.group( 1 );
          operator = matcher.group( 2 );
          value = matcher.group( 3 );
          this.query = query;
          LOGGER.info( "term: " + term);
          LOGGER.info( "operator: " + operator );
          LOGGER.info( "value: " + value );
        }
        else
        {
          LOGGER.info( "No Query object constructed, couldn't parse query " + query );
        }
      } else {
        LOGGER.info("No query requested, constructing empty Query object.");
      }
    }

    String getTerm() {
      return term;
    }

    String getOperator() {
      return operator;
    }

    String getValue() {
      return value;
    }
    String toUrlParameters() {
      return "query="+query;
    }

    boolean hasQuery () {
      return query.length()>0;
    }

    String asWhereClause (String tableAlias) {
      return new StringBuilder()
         .append(" (")
         .append(tableAlias)
         .append(".")
         .append(term)
         .append(value.contains("*") ?
                 (operator.equals("!=") ? " NOT " : "") + " LIKE '" + value.replaceAll("\\*","%") + "'"
                 :  "")
         .append(!value.contains("*") ? operator + "'" + value + "'" : "")
         .append(" )").toString();
    }
  }

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

  private boolean hasFilter() {
    return filter.length() > 0;
  }

  private String getAcl() {
    return acl;
  }

  public void setAcl(String acl) {
    this.acl = (acl == null ? "" : acl);
  }

  public EntityQuery withAcl(String acl) {
    if (isNotEmpty(acl)) setAcl(acl);
    return this;
  }

  public EntityQuery withQuery(String query) {
    if (isNotEmpty(query)) setQuery(query);
    return this;
  }

  public void setStartsWith(String startsWith, String field) {
    if (isNotEmpty(startsWith) && isNotEmpty(field)) {
      this.startsWith = startsWith;
      this.startsWithField = field;
    }
  }

  private boolean hasStartsWith () {
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

  private String getStartsWithWhereClause (String tableAlias) {
    return tableAlias + "." + startsWithField + " LIKE '" + startsWith +"%'";
  }

  private boolean hasAcl () {
    return (acl.length()>0);
  }

  private boolean hasSearchCriteria() {
    return (hasStartsWith() || hasFilter() || hasAcl() || hasQuery());
  }

  public String asUrlParameters () {
    String UTF8 = "UTF-8";
    try {
      return (hasFilter() ? "&filter="+URLEncoder.encode(filter, UTF8) : "") +
             (hasAcl() ? "&acl="+URLEncoder.encode(acl, UTF8) : "") +
             (hasStartsWith() ? "&prefix="+URLEncoder.encode(startsWith, UTF8) : "") +
             (hasQuery() ? "&query="+URLEncoder.encode(getQuery().toUrlParameters(), UTF8) : "");
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Error generating URL query parameters: "+e);
      return "";
    }
  }


  /**
  * Constructs where clause that would look for 'filter' in any of the provided columns.
  * @param tableAlias The SQL query's alias for the table to create WHERE clause for
  * @return " where concat( [alias].[cols-1], '||', [alias].[cols-2], ...) like '%[filter]%' "
  */
  private String getFilteringWhereClause(String tableAlias, boolean withWHERE) {
    StringBuilder expr = new StringBuilder();
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

  private String getFilteringWhereClause(String tableAlias) {
    return getFilteringWhereClause(tableAlias, false);
  }

  private String getAclWhereClause (String tableAlias) {
    return getAclWhereClause(tableAlias,false);
  }

  private String getAclWhereClause (String tableAlias, boolean withWHERE ) {
    StringBuilder expr = new StringBuilder();
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

  public String asWhereClause (String tableAlias) {
    StringBuilder whereClause = new StringBuilder();
    if (hasSearchCriteria()) {
      whereClause.append( " WHERE " );
      List<String> whereClauses = new ArrayList<>();
      if (hasFilter()) {
        whereClauses.add(getFilteringWhereClause(tableAlias));
      }
      if (hasAcl()) {
        whereClauses.add(getAclWhereClause(tableAlias));
      }
      if (hasStartsWith()) {
        whereClauses.add(getStartsWithWhereClause(tableAlias));
      }
      if (hasQuery()) {
        whereClauses.add(getQuery().asWhereClause(tableAlias));
      }
      Iterator<String> iter = whereClauses.iterator();
      while (iter.hasNext()) {
        whereClause.append( iter.next());
        if (iter.hasNext()) whereClause.append(" AND ");
      }
    }
    return whereClause.toString();
  }

  private static boolean isNotEmpty(String str) {
    return str != null && !str.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof EntityQuery) {
      EntityQuery p = (EntityQuery)o;
      return (p.asWhereClause("alias").equals(this.asWhereClause("alias")));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 29 * hash + (this.asWhereClause("alias") != null ? this.asWhereClause("alias").hashCode() : 0);
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
    System.out.println(query.asWhereClause("a"));
    query.setQuery("(usedBy=library)");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause("x"));

    query = new EntityQuery();
    query.setQuery("(usedBy=library)");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause("b"));
    query.setQuery("usedBy=*library");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause("c"));
    query.setQuery("(=library");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause("d"));
    query.setQuery("usedBy=");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause("e"));
    query.setQuery("(usedBy!=library)");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause("f"));
    query.setQuery("(usedBy==library)");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause("g"));
    query.setQuery("(usedBy===library)");
    System.out.println(query.asUrlParameters());
    System.out.println(query.asWhereClause("h"));
  }

}
