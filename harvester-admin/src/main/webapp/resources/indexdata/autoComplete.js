if (!window.indexdata)
  var indexdata = {};
if (!indexdata.autocomplete) {
  indexdata.autocomplete = {
    
    errorHandler: function(data) {
      throw new Error("Error occurred during Ajax call: " + data.description);
    },
            
    markLBFocused: function (listbox) {
      listbox.focused = true;
    },
            
    //called on input box value change
    updateCompletionItems: function(input, event) {
      //focus list-box on arrow-down
      if (event.keyCode == 40) {
        var listbox = document.getElementById(this.getId(input, ":listbox"));
        listbox.focused = true;
        listbox.focus();
        listbox.selectedIndex = 0;
        //fire-off on change event manually
        if ("createEvent" in document) {
          var evt = document.createEvent("HTMLEvents");
          evt.initEvent("change", false, true);
          listbox.dispatchEvent(evt);
        }
        else
          listbox.fireEvent("onchange");
        return;
      }
      //filter control-chars
      if (!input.value || !this.isPrintableCharOrBS(event.keyCode)) return;
      var keystrokeTimeout;
      jsf.ajax.addOnError(indexdata.autocomplete.errorHandler);
      var offset = this.getCumulativeOffset(input);
      var ajaxRequest = function() {
        jsf.ajax.request(input, event,
                {render: indexdata.autocomplete.getId(input, ":listbox"),
                  x: offset[0],
                  y: offset[1] + input.offsetHeight
                });
      };
      window.clearTimeout(keystrokeTimeout);
      keystrokeTimeout = window.setTimeout(ajaxRequest, 350);
    },
            
    selectionChange: function (listbox, event) {
      //enter or escape hides drop-down
      if (event.keyCode == 13 || event.keyCode == 27)
        indexdata.autocomplete.listboxLostFocus(listbox);
      //prevent form submit on enter
      if (event.keyCode == 13) {
        event.preventDefault();
        return false;
      }
      return true;
    },
            
    //called in input box blur
    inputLostFocus: function(input) {
      var focusLostTimeout;
      var hideListbox = function() {
        var listbox = document.getElementById(
                indexdata.autocomplete.getId(input, ":listbox"));
        if (!listbox.focused)
          listbox.style.display = 'none';
      };
      focusLostTimeout = window.setTimeout(hideListbox, 200);
    },
            
    listboxLostFocus: function(listbox) {
       var focusLostTimeout;
       var hideListbox = function() {
        listbox.focused = false;
        listbox.style.display = 'none';
      };
      focusLostTimeout = window.setTimeout(hideListbox, 200);
    },
            
    getId: function(input, control) {
      var clientId = new String(input.name);
      var lastIndex = clientId.lastIndexOf(':');
      return clientId.substring(0, lastIndex) + control;
    },
            
    getCumulativeOffset: function(element) {
      var cumLeft = 0;
      var cumTop = 0;
      if (element.offsetParent) {
        do {
          cumLeft += element.offsetLeft;
          cumTop += element.offsetTop;
        } while (element = element.offsetParent);
      }
      return [cumLeft, cumTop];
    },
            
    isPrintableCharOrBS: function(keycode) {
      return (keycode > 47 && keycode < 58) || // number keys
        keycode == 32                     || // spacebar
        keycode == 8                      || //backspace
        (keycode > 64 && keycode < 91)    || // letter keys
        (keycode > 95 && keycode < 112)   || // numpad keys
        (keycode > 185 && keycode < 193)  || // ;=,-./` (in order)
        (keycode > 218 && keycode < 223);    // [\]' (in order)
    }
    
  };
}