// save-resource-as.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

if (!window.calli) {
    window.calli = {};
}

window.calli.saveFormAs = function(event, fileName, create) {
    return calli.saveResourceAs(event, fileName, create);
};
var nestedSubmit = false;
window.calli.saveResourceAs = function(event, fileName, create) {
    event = calli.fixEvent(event);
    var form = event.target;
    if(!$(form).is('form')) form = $(form).closest('form')[0];

    $(form).find("input").change(); // IE may not have called onchange before onsubmit
    var resource = $(form).attr('about') || $(form).attr('resource');
    if (event.type == 'submit') {
        if (fileName && !nestedSubmit) {
            // set resource attribute and go
            var ns = calli.listResourceIRIs(calli.getPageUrl())[0];
            if (resource) {
                ns = resource.replace(/[^\/]*\/?$/, ''); // get parent folder
            } else if (ns.lastIndexOf('/') != ns.length - 1
                    && window.location.search.indexOf('?create=') == 0) {
                ns += '/';
            } else if (window.location.search == '?create') {
                ns = null; // no location available
            } else {
                ns = ns.replace(/[^\/]*$/, ''); // get folder
            }
            if (ns) {
                var local = encodeURI(fileName).replace(/%25(\w\w)/g, '%$1').replace(/%20/g, '+');
                resource = ns + local;
                $(form).removeAttr('about');
                $(form).attr('resource', resource);
            }
        }
        if (resource) {
            // resource attribute ready set, let's go
            overrideLocation(form, resource);
            return true;
        }
    }
    // prompt for a new resource URI
    var label = fileName || findLabel(form) || localPart(resource);
    openSaveAsDialog(form, label, create, function(ns, local) {
        if (fileName) {
            local = local.replace(/\+/g,'-');
        }
        var resource = ns + local.toLowerCase();
        $(form).removeAttr('about');
        $(form).attr('resource', resource);
        if (form.getAttribute("enctype") == "application/sparql-update") {
            form.setAttribute("enctype", "application/rdf+xml");
        }
        overrideLocation(form, resource);
        try {
            nestedSubmit = true;
            $(form).submit(); // this time with a resource attribute
        } finally {
            nestedSubmit = false;
        }
    });
    $(form).removeAttr('about');
    $(form).removeAttr('resource');
    event.preventDefault();
    return false;
};

function findLabel(form) {
    var field = $($(form).find('input:not(:checkbox,:disabled,:button,:password,:radio)')[0]);
    var input = field.val();
    if (input) {
        var onchange = function() {
            if (input != $(field).val()) {
                // restore the resource attribute when this field changes
                if (resource) {
                    $(form).removeAttr('about');
                    $(form).attr('resource', resource);
                } else {
                    $(form).removeAttr('about');
                    $(form).removeAttr('resource');
                }
                field.unbind('change', onchange);
            }
        };
        field.bind('change', onchange);
    }
    return calli.slugify(input);
}

function localPart(resource) {
    if (resource)
        return resource.replace(/.*\/(.+)/, '$1');
    return null;
}

function openSaveAsDialog(form, label, create, callback) {
    var src = calli.getCallimachusUrl("pages/location-prompt.html#");
    if (label) {
        src += encodeURIComponent(label.replace(/!/g,''));
    }
    if (location.search.search(/\?create=/) == 0) {
        src += '!' + calli.viewpage(calli.getPageUrl());
    } else if (window.sessionStorage) {
        try {
            var url = sessionStorage.getItem("LastFolder");
            if (url) {
                src += '!' + calli.viewpage(url);
            } else if (url = localStorage.setItem("LastFolder")) {
                src += '!' + calli.viewpage(url);
            }
        } catch (e) {
            // ignore
        }
    }
    var dialog = window.calli.openDialog(src, 'Save As...', {
        buttons: {
            "Save": function() {
                dialog.postMessage('GET label', '*');
            },
            "Cancel": function() {
                calli.closeDialog(dialog);
            }
        },
        onmessage: function(event) {
            if (event.data == 'POST save') {
                dialog.postMessage('OK\n\n' + event.data, '*');
                dialog.postMessage('GET label', '*');
            } else if (event.data.indexOf('OK\n\nGET label\n\n') == 0) {
                var data = event.data;
                label = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
                dialog.postMessage('GET url', '*');
            } else if (event.data.indexOf('OK\n\nGET url\n\n') == 0) {
                var data = event.data;
                var src = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
                if (src.indexOf('?') >= 0) {
                    src = src.substring(0, src.indexOf('?'));
                }
                var ns = calli.listResourceIRIs(src)[0];
                if (ns.lastIndexOf('/') != ns.length - 1) {
                    ns += '/';
                }
                var local = encodeURI(label).replace(/%20/g,'+');
                updateFormAction(form, src, create);
                callback(ns, local);
                calli.closeDialog(dialog);
            }
        }
    });
    return dialog;
}

function updateFormAction(form, target, create) {
    var action = calli.getFormAction(form);
    var m;
    if (create) {
        form.setAttribute("method", "POST");
        form.action = target + '?create=' + encodeURIComponent(create);
    } else if (m = action.match(/^([^\?]*)\?create(&.*)?$/)) {
        action = target + '?create=';
        if (create) {
            action += encodeURIComponent(create);
        } else if (m[1]) {
            action += encodeURIComponent(calli.listResourceIRIs(m[1])[0]);
        } else {
            action += encodeURIComponent(calli.listResourceIRIs(location.pathname)[0]);
        }
        if (m[2]) {
            action += m[2];
        }
        form.setAttribute("method", "POST");
        form.action = action;
    } else if (m = action.match(/^([^\?]*)(\?create=[^&]+)(&.*)?$/)) {
        action = target + m[2];
        if (m[3]) {
            action += m[3];
        }
        form.setAttribute("method", "POST");
        form.action = action;
    }
}

function overrideLocation(form, uri) {
    var action = calli.getFormAction(form);
    if (action.indexOf('&location=') > 0) {
        var m = action.match(/^(.*&location=)([^&=]*)(.*)$/);
        form.action = m[1] + encodeURIComponent(uri) + m[3];
    } else if (action.indexOf('?create') >= 0) {
        form.action = action + '&location=' + encodeURIComponent(uri);
    }
    if (action.indexOf('?create') >= 0 && action.indexOf('&intermediate=') < 0 && isIntermidate(action)) {
        form.action += '&intermediate=true';
    }
}

function isIntermidate(url) {
    if (window.parent != window) {
        try {
            var childUrl = url;
            if (childUrl.indexOf('?create') > 0) {
                childUrl = childUrl.substring(0, childUrl.indexOf('?'));
                var parentUrl = window.parent.location.href;
                if (parentUrl.indexOf('?edit') > 0) {
                    parentUrl = parentUrl.substring(0, parentUrl.indexOf('?'));
                    if (parentUrl == childUrl) {
                        // they are creating a component in a dialog from an edit form
                        return true;
                    }
                }
            }
        } catch (e) {
            // I guess not
        }
    }
    return false;
}

})(jQuery);
