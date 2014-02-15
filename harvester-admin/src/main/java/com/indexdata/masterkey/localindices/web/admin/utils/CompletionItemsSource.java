package com.indexdata.masterkey.localindices.web.admin.utils;

import java.util.List;

import javax.faces.bean.ManagedProperty;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

public abstract class CompletionItemsSource {

  private Logger logger = Logger.getLogger(CompletionItemsSource.class);
  
  /**
   * The autocomplete composite will invoke this method to retrieve
   * completion items for its select list. 
   * 
   * @return
   */
  public abstract List<SelectItem> getSelectItems();

  @ManagedProperty("#{autocompleteListener}")
  public AutocompleteListener autocompleteListener;
  /**
   * For injection of AutocompleteListener
   * 
   * @param listener The AutocompleteListener
   */
  public void setAutocompleteListener(AutocompleteListener listener) {
    autocompleteListener = listener;
  }

  /**
   * This method should be invoked after instantiation of this object
   * -- iow after injection of the auto complete listener -- 
   * in order to set this completion items list source on the listener. 
   */
  public void setCompletionItemsSource() {
    autocompleteListener.setCompletionItemsSource(this);
  }
  
  /**
   * This method should be referenced as a parameter to the
   * autocomplete composite from client XHTML page like:
   * <br/><br/>
   * <pre>
   *   &lt;id:autoComplete 
   *        id="mylist"
   *        ...
   *        itemsSource="#{myItemsSource.itemsSource}"
   *        ...
   *   /&gt;
   * </pre>
   * 
   * @return
   */
  public String getItemsSource () {
    logger.debug("setting items source on auto complete listener ("+getSourceId()+")");
    setCompletionItemsSource();
    return getSourceId();
  }
  
  public void setItemsSource(String itemsSource) {
  }

  
  /**
   * The autocomplete component finds a particular source in the UI 
   * by its sourceId (needed in case of multiple autocomplete widgets 
   * in same page).
   * 
   * @return
   */
  public String getSourceId() {
    return this.getClass().getName();
  }
  

}
