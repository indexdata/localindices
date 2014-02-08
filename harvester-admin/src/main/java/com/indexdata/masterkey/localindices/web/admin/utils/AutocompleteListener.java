package com.indexdata.masterkey.localindices.web.admin.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItems;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

/**
 */
@ManagedBean(name = "autocompleteListener")
@SessionScoped
public class AutocompleteListener implements Serializable {

  private static final long serialVersionUID = -5218720495530743082L;
  private final static Logger logger = Logger.getLogger(AutocompleteListener.class);
  
  // Map of list items sources by list-box client IDs 
  private Map<String,CompletionItemsSource> completionItemsSources = new HashMap<String,CompletionItemsSource>();

  /**
   * Event raised when input box changes value.
   * 
   * @param e
   */
  public void valueChanged(ValueChangeEvent e) {    
    UIInput input = (UIInput) e.getSource();
    UISelectOne listbox = (UISelectOne) input.findComponent("listbox");
    if (listbox != null) {
      String query = (String) input.getValue();
      Map<String, Object> listAttrs = listbox.getAttributes();
      if (query.isEmpty()) {
        setListboxStyle(0, listAttrs);
      } else {
        UISelectItems listItems = (UISelectItems) listbox.getChildren().get(0);
        List<SelectItem> entries = completionItemsSources.get(listbox.getClientId()).getSelectItems();
        List<SelectItem> newEntries = filterEntries(query, entries);
        listItems.setValue(newEntries);
        setListboxStyle(newEntries.size(), listAttrs);
      }
    }
  }

  /**
   * Event raised when item is selected in the drop-down list.
   * 
   * @param e
   */
  public void completionItemSelected(ValueChangeEvent e) {
    logger.debug("Completion Item Selected");
    UISelectOne listbox = (UISelectOne) e.getSource();
    UIInput input = (UIInput) listbox.findComponent("input");
    if (input != null) {
      input.setValue(listbox.getValue());
    }
    Map<String, Object> attrs = listbox.getAttributes();
    attrs.put("style", "display: none");
    logger.debug("List box display set to none");
  }

  /**
   * Filter auto-complete entries with the new keyword.
   * 
   * @param query
   * @param completionEntries
   * @return
   */
  private List<SelectItem> filterEntries(String query, List<SelectItem> completionEntries) {
    List<SelectItem> newEntries = new ArrayList<SelectItem>();
    String lquery = query.toLowerCase();
    for (SelectItem item : completionEntries) {
      String lvalue = item.getLabel().toLowerCase();
      if (lvalue.contains(lquery))
        newEntries.add(item);
    }
    return newEntries;
  }

  /**
   * Position and size, or hide, a list box
   * @param rows
   * @param listAttrs
   */
  private void setListboxStyle(int rows, Map<String, Object> listAttrs) {
    
    if (rows > 0) {
      Map<String, String> reqParams = FacesContext.getCurrentInstance().getExternalContext()
          .getRequestParameterMap();
      listAttrs.put("style", "display: inline; position: absolute; left: " + reqParams.get("x") 
          + "px;" + " top: " + reqParams.get("y") + "px");
      // avoid only one row (selection of single row is not a change event)
      listAttrs.put("size", rows == 1 ? 2 : rows);
      logger.debug("Set listbox style, displayed");
    } else {
      listAttrs.put("style", "display: none;");
      logger.debug("Set listbox style, no display");
    }
  }

  /**
   * When a new select items source is set, the input box is cleared and the list box hidden.
   * 
   * @param itemsSource
   */
  public void setCompletionItemsSource(CompletionItemsSource itemsSource) {
    logger.debug("Set completion items source");
    UIInput input = (UIInput) itemsSource.getCompletionItems().findComponent("input");
    UISelectOne listbox = (UISelectOne) itemsSource.getCompletionItems().findComponent("listbox");
    if (input != null && listbox != null) {
      input.setValue("");
      this.completionItemsSources.put(listbox.getClientId(), itemsSource);
      Map<String, Object> listAttrs = listbox.getAttributes();
      setListboxStyle(0, listAttrs);
    } else {
      logger.error("No input box or no list box found");
    }
  }
  
}
