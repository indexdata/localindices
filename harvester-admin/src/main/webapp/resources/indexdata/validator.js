if (!window.indexdata) {
  var indexdata = {};
}

if (!indexdata.validator) {
  indexdata.validator = {
    valid: true,
    message: null,
    cb: {},
    // All callbacks registered here must return true for the form to proceed
    register: function (name, cb) {
      if (typeof cb !== "function")
        throw "Cannot register non-function callback in indexdata.validator.register";
      indexdata.validator.cb[name] = cb;
    },
    // A callback can request confirmation by calling confirm() with a message
    // and returning true
    confirm: function (message) {
      if (indexdata.validator.message === null)
        indexdata.validator.message = "";
      else
        indexdata.validator.message += "\n";
      indexdata.validator.message += message;
    },
    // Do not submit if this returns false
    validate: function () {
      indexdata.validator.message = null;
      for (key in indexdata.validator.cb) {
        indexdata.validator.valid = indexdata.validator.cb[key]() && indexdata.validator.valid;
      }
      if (indexdata.validator.valid === true && indexdata.validator.message !== null) {
        return confirm(indexdata.validator.message);
      }
      else {
        return indexdata.validator.valid;
      }
    }
  };
}