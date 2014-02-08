package com.indexdata.masterkey.localindices.web.admin.utils;

import java.util.List;

import javax.faces.bean.ManagedProperty;
import javax.faces.component.UISelectItems;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

public abstract class CompletionItemsSource {

  private Logger logger = Logger.getLogger(CompletionItemsSource.class);
  protected UISelectItems uiSelectItems = null;
  
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
   * The completion items control should be bound to the autocomplete composite by
   * a parameter to the composite from the client XHTML page like:
   * <br/><br/>
   * <pre>
   *   &lt;id:autoComplete 
   *        id="mylist"
   *        ...
   *        completionItems="#{myItemsSource.completionItems}"
   *        ...
   *   /&gt;
   * </pre>
   * 
   * @param items  The Select list items
   */
  public void setCompletionItems(UISelectItems items) { uiSelectItems = items; }
  public UISelectItems getCompletionItems() { return uiSelectItems; }
  
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
    logger.debug("setting items source on auto complete listener");
    setCompletionItemsSource();
    return "";
  }
  
  public void setUiSelectItemsValue() {
    uiSelectItems.setValue(getSelectItems());
  }

  /**
   * This method should be invoked whenever changing the source of 
   * completion items, like when performing a new HTTP or database 
   * request for a list items. 
   */
  public void resetCompletionItemsList() {
    setCompletionItemsSource();
    setUiSelectItemsValue();
  }
  
  

}
