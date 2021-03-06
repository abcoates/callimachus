// etag.js
/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

window.calli = window.calli || {};

try {
    window.calli.etag = function(url, value) {
        var uri = url;
        if (uri.indexOf('http') != 0) {
            if (document.baseURIObject && document.baseURIObject.resolve) {
                uri = document.baseURIObject.resolve(uri);
            } else {
                var a = document.createElement('a');
                a.setAttribute('href', uri);
                if (a.href) {
                    uri = a.href;
                }
            }
        }
        if (uri.indexOf('?') > 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }
        if (value) {
            return window.sessionStorage.setItem(uri + " ETag", value);
        } else {
            return window.sessionStorage.getItem(uri + " ETag");
        }
    };
    if (!window.sessionStorage) {
        window.calli.etag = function(){return null;};
    }
} catch(e) {
    window.calli.etag = function(){return null;};
}
