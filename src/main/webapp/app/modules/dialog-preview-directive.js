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
    /*eslint-disable no-alert, no-console, no-trailing-spaces, no-mixed-spaces-and-tabs, space-infix-ops */

    angular.module('dialog.preview', [])

    /**
     * @name preview
     * @module module/preview
     * @description
     *
     * Renders the preview panel within the UI. When a movie is clicked within the list
     * of movie results the controller's "selectedMovie" property is updated. Once selectedMovie
     * contains a movie this directive is invoked. This directive is responsible for rendering
     * the entire preview pane (movie, name, description etc.).
     *
     * @param {object}
     *            content - a reference to movie object
     */
    .directive('preview', function ($parse, $sce, $log) {

        return {
        	'template': '<div><span class="dialog-drawer-toggle"></span>' +
                        '<div class="dialog-preview-scroll">'+
                        '<iframe id="trailerIFrame" class="dialog-trailer" allowfullscreen="true" frameborder="0" src={{trustedUrl}}></iframe>', 
                        
            'restrict': 'E',
            'link': function (scope, element, attr) {
                var closeButton = null;
                var resizeContents = function () {
                    var docHeight = $(window).height();
                    var headerHeight = $('#dialog-header').outerHeight(true);
                    var previewParentHeight = $('#preview-parent')[0].scrollHeight;
                    var innerHeaderHeight = $('.dialog-drawer-toggle').outerHeight(true);
                    var previewAvailHeight = 0;
                    if (previewParentHeight === docHeight) {
                        //mobile
                        previewAvailHeight = docHeight - (innerHeaderHeight + 5);
                    }
                    else {
                        //desktop
                        previewAvailHeight = docHeight - (headerHeight + innerHeaderHeight);
                    }
                    if (docHeight < (headerHeight + previewParentHeight)) {
                        //we need to scroll the preview panel
                        $('.dialog-preview-scroll').height(previewAvailHeight);
                    }
                };
                scope.hideReleaseDate = true;
                scope.hideCertification = true;
                scope.playerClass = '';
                scope.isFavorite = false;

                closeButton = $('.dialog-drawer-toggle');
                closeButton.bind('touchstart click', function (e) {
                    scope.$apply(scope.dialogCtrl.clearStoreSelection());
                    $(window).off('resize', resizeContents);
                    e.preventDefault();
                    e.stopPropagation();
                });
                $(window).resize(resizeContents);
                scope.$watch(function () {
                    return scope.dialogCtrl.getCurrentStore();
                }, function () {
                    var trustedUrl = null;
                	var store = $parse(attr.content)(scope);
                	var address = null;
                	var id = null;
                    var iframe = $('#trailerIFrame');
                    var div = $('#noTrailerText');
                    //_.assign(scope.movie, movie);
                    scope.store = store;
                    
                    if (store.address) {
                    	address = store.address;
                    	scope.address = address;
                    }
                    else {
                    	scope.address = null;
                    }
                    
                    if (store.mapUrl) {
                        trustedUrl = $sce.trustAsResourceUrl(store.mapUrl);
                        scope.trustedUrl = trustedUrl;
                        iframe.removeClass('dialog-trailer-hidden');
                        div.addClass('dialog-trailer-hidden');
                    }
                    else {
                        scope.trustedUrl = null;
                        iframe.addClass('dialog-trailer-hidden');
                        div.removeClass('dialog-trailer-hidden');
                    }
                    
                    resizeContents();
                }, true);
            }
        };
    });
}());
