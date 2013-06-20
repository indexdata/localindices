/**
 * JSF Ajax loader indicator
 * @type type
 */
if (!window["busystatus"]) {
  var busystatus = {};
}

if (!window.busystatus.onStatusChange) {
  busystatus.onStatusChange = function onStatusChange(data) {
    //ignore poll evenens
    if (data.source.id.indexOf("poll", this.length - "poll".length) !== -1) return;
    var status = data.status;
    if (status === "begin") { // turn on busy indicator
      document.body.style.cursor = 'wait';
    } else { // turn off busy indicator, on either "complete" or "success"
      document.body.style.cursor = 'auto';
    }
  };
}

jsf.ajax.addOnEvent(busystatus.onStatusChange);