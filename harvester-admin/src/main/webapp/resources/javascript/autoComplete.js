if (!indexdata)
  var indexdata = {};
if (!indexdata.autocomplete) {
  indexdata.autocomplete = {
    
    errorHandler: function(data) {
      alert("Error occurred during Ajax call: " + data.description);
    },
            
    updateCompletionItems: function(input, event) {
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
            
    inputLostFocus: function(input) {
      var focusLostTimeout;
      var hideListbox = function() {
        document.getElementById(indexdata.autocomplete.getId(input, ":listbox"))
                .style.display = 'none';
        
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
    }
    
  };
}