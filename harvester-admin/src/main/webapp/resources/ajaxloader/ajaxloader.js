/**
 * JSF Ajax loader indicator
 * @type type
 */
if (!window["busystatus"]) {
  var busystatus = {};
}

if (!window.busystatus.getProgressBox) {
  busystatus.getProgressBox = function () {
    var div = document.getElementById('progress-meter');
    if (div === null) {
      div = document.createElement('div');
      div.setAttribute('id', 'progress-meter');
      div.style.display = 'none';
      document.getElementsByTagName('body')[0].appendChild(div);
    }
    return div;
  };
}

if (!window.busystatus.onStatusChange) {
  busystatus.onStatusChange = function onStatusChange(data) {
    //ignore poll evenens
    if (data.source.id.indexOf("poll", this.length - "poll".length) !== -1) return;
    var status = data.status;
    if (status === "begin") { // turn on busy indicator
      busystatus.getProgressBox().style.display = 'block';
    } else { // turn off busy indicator, on either "complete" or "success"
      busystatus.getProgressBox().style.display = 'none';
    }
  };
}

jsf.ajax.addOnEvent(busystatus.onStatusChange);