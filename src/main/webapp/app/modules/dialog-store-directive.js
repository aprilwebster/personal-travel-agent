/* Copyright IBM Corp. 2015
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
 */
(function () {
    'use strict';
    /*eslint-disable no-alert, no-console, no-trailing-spaces, no-mixed-spaces-and-tabs */
    
    angular.module('dialog.store', ['ngAnimate'])

    /**
     * @name store
     * @module modules/store
     *
     * @description
     *
     * Renders a store within the UI. The WDS API will notify the dialogapp
     * server side code to get a list of stores. At this point
     * a list of stores will be returned and a <store> element added for each store.
     * The store is a clickable element which causes the preview panel to load.
     *
     * @param {object}
     *            content - a reference to the store object the element represents.
     */
    .directive('store', function ($parse, $timeout) {
        return {
            'restrict': 'A',
            'link': function (scope, element, attr) {
                var store = $parse(attr.content)(scope); //Get the 'store' object from the content attribute
                //var htmlContent = '<span class="dialog-store-link">' + store.storeName + '</span>'; //AW - name or storeName
                var htmlContent = '<span class="dialog-store-link">' + 'PLEASE WORK' + '</span>'; //AW - name or storeName
                
                var startY = -1;
                var delayTimeout = null;
                var clickAction = function (e) {
                    //Once the movie 'button' is clicked, notify the controller
                    //that a new movie must be selected.
                    scope.dialogCtrl.selectStore(store);
                    e.preventDefault();
                    e.stopPropagation();
                };
                scope.keypressed = function ($event) {
                    if ($event.keyCode === 13 || $event.keyCode === 32) {
                        clickAction($event);
                    }
                };
                element.append(htmlContent);
                $('.watson-thinking').last().find('.dialog-line-separator').css('display', 'none');
                element.bind('touchstart', function (e) {
                    var touchobj = null;
                    if (e && e.originalEvent) {
                        touchobj = e.originalEvent.changedTouches[0]; // reference first touch point (ie: first finger)
                        startY = parseInt(touchobj.pageY);
                    }
                });
                element.bind('touchend', function (e) {
                    var touchobj = null;
                    var tmp = null;
                    if (e && e.originalEvent) {
                        touchobj = e.originalEvent.changedTouches[0];
                        tmp = parseInt(touchobj.pageY);
                        if (startY + 20 >= tmp && startY - 20 <= tmp) {
                            clickAction(e);
                        }
                    }
                    startY = -1;
                });
                element.bind('click', clickAction);
                //If it is a mobile UI or a low res UI then put focus on the first returned movie
                /*
                if ($(window).height() <= 750 && $.isArray(scope.dialogCtrl.conversation) && scope.dialogCtrl.conversation[scope.dialogCtrl.conversation.length - 1]) {
                    if (scope.dialogCtrl.conversation[scope.dialogCtrl.conversation.length - 1].stores) {
                        if ($.isArray(scope.dialogCtrl.conversation[scope.dialogCtrl.conversation.length - 1].stores)) {
                            if (store === scope.dialogCtrl.conversation[scope.dialogCtrl.conversation.length - 1].stores[0]) {
                                if (delayTimeout) {
                                    $timeout.cancel(delayTimeout);
                                }
                                //allow the dom element time to render
                                delayTimeout = $timeout(function () {
                                    //forcing focus on the first movie so that the keyboard does not pop up!
                                    element.focus();
                                }, 900);
                            }
                        }
                    }
                }*/
            }
        };
    });
}());
