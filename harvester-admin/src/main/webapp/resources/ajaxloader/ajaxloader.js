/**
 * JSF Ajax loader indicator
 * @type type
 */
if (!window.ajaxloader) {
  var ajaxloader = {};
  ajaxloader.ignore = "";
}

if (!window.ajaxloader.getProgressBox) {
  ajaxloader.getProgressBox = function () {
    var div = document.getElementById('ajaxloader');
    if (div === null) {
      div = document.createElement('div');
      div.setAttribute('id', 'ajaxloader');
      div.style.display = 'none';
      document.getElementsByTagName('body')[0].appendChild(div);
    }
    return div;
  };
}

if (!window.ajaxloader.onStatusChange) {
  ajaxloader.onStatusChange = function onStatusChange(data) {
    //ignore selected sources where id suffix matches
    var id = data.source.id;
    var ignore = ajaxloader.ignore.split(/\s+/);
    for (var i=0; i<ignore.length; i++) {
      if (ignore[i].length == 0) continue;
      if (id.indexOf(ignore[i], id.length-ignore[i].length) !== -1) return;
    }
    var status = data.status;
    if (status === "begin") { // turn on busy indicator
      ajaxloader.getProgressBox().style.display = 'block';
    } else { // turn off busy indicator, on either "complete" or "success"
      ajaxloader.getProgressBox().style.display = 'none';
    }
  };
}

jsf.ajax.addOnEvent(ajaxloader.onStatusChange);