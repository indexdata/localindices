package com.indexdata.masterkey.localindices.web.admin.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.faces.bean.ManagedBean;

import javax.faces.component.UIInput;
import javax.faces.component.UISelectItems;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

@ManagedBean(name="autocompleteListener")
public class AutocompleteListener implements Serializable {
   private static String COMPLETION_ENTRIES_ATTR = "indexdata.completionEntries";

   /**
    * Event raised when input box changes value.
    * @param e 
    */
   public void valueChanged(ValueChangeEvent e) {
      UIInput input = (UIInput) e.getSource();
      UISelectOne listbox = (UISelectOne)input.findComponent("listbox");
      if (listbox != null) {
         String query = (String) input.getValue();
         Map<String, Object> listAttrs = listbox.getAttributes();
         if (query.isEmpty()) {
          setListboxStyle(0, listAttrs);
         } else {
          UISelectItems listItems = (UISelectItems) listbox.getChildren().get(0);
          List<SelectItem> entries = getEntries(listbox, listItems, listAttrs);
          List<SelectItem> newEntries = filterEntries(query, entries);
          listItems.setValue(newEntries);
          setListboxStyle(newEntries.size(), listAttrs);
         }
      }
   }

   /**
    * Event raised when item is selected in the drop-down list.
    * @param e 
    */
   public void completionItemSelected(ValueChangeEvent e) {
     UISelectOne listbox = (UISelectOne) e.getSource();
     UIInput input = (UIInput) listbox.findComponent("input");
     if(input != null) {
        input.setValue(listbox.getValue());
     }
     Map<String, Object> attrs = listbox.getAttributes();
     attrs.put("style", "display: none");
   }

   /**
    * Filter auto-complete entries with the new keyword.
    * @param query
    * @param completionEntries
    * @return 
    */
   private List<SelectItem> filterEntries(String query, 
     List<SelectItem> completionEntries) {
      List<SelectItem> newEntries = new ArrayList<SelectItem>();
      String lquery = query.toLowerCase();
      for (SelectItem item : completionEntries) {
        String lvalue = item.getLabel().toLowerCase();
         if (lvalue.contains(lquery))
           newEntries.add(item);
      }
      return newEntries;
   }

   private void setListboxStyle(int rows, Map<String, Object> listAttrs) {
      if (rows > 0) {
         Map<String, String> reqParams = FacesContext.getCurrentInstance()
            .getExternalContext().getRequestParameterMap();
         listAttrs.put("style", "display: inline; position: absolute; left: "
             + reqParams.get("x") + "px;" + " top: " + reqParams.get("y") + "px");        
         // avoid only one row (selection of single row is not a change event)
         listAttrs.put("size", rows == 1 ? 2 : rows); 
      } else {
         listAttrs.put("style", "display: none;");
      }
   }

   private List<SelectItem> getEntries(UISelectOne listbox,
      UISelectItems items, Map<String, Object> attrs) {
         List<SelectItem> completionEntries = (List<SelectItem>) attrs.get(COMPLETION_ENTRIES_ATTR);
         if (completionEntries == null) {
            completionEntries = (List<SelectItem>) items.getValue();
            attrs.put(COMPLETION_ENTRIES_ATTR, completionEntries);
         }
      return completionEntries;
   }
}
