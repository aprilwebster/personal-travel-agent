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
    	
    	//var test = $parse(attr.content)(scope);
        return {
        	
        	 
        	
        	'template': '<div><span class="dialog-drawer-toggle"></span>' +
                        //'<favorite class="dialog-favorite-sm" content="{{store}}"></favorite>' +
                        '<div class="dialog-preview-scroll">'+
                        //'<iframe id="trailerIFrame" class="dialog-trailer" allowfullscreen="true" frameborder="0" src="https://www.google.com/maps/embed/v1/place?q=place_id:ChIJY6dkgDO7j4ARDxWs4SyOIVU&key=AIzaSyB1RD2gilBuJjZPQP500vCZPMoDqGfBav8"></iframe>', 
                        //'<iframe id="trailerIFrame" class="dialog-trailer" allowfullscreen="true" frameborder="0" src="https://www.google.com/maps/embed/v1/search?q=' + 'j+crew' + '+near+505+Cypress+Point+Drive+Mountain+View' + '&key=AIzaSyB1RD2gilBuJjZPQP500vCZPMoDqGfBav8"></iframe>', 
                        //'<iframe id="trailerIFrame" class="dialog-trailer" allowfullscreen="true" frameborder="0" src=store.mapURL></iframe>', 
                        //'<iframe id="trailerIFrame" class="dialog-trailer" allowfullscreen="true" frameborder="0" src="https://www.google.com/maps/embed/v1/search?q=j+crew+near+' + test.address + '&key=AIzaSyB1RD2gilBuJjZPQP500vCZPMoDqGfBav8"></iframe>', 
                        
                        // this works
                        //'<iframe id="trailerIFrame" class="dialog-trailer" allowfullscreen="true" frameborder="0" src="https://www.google.com/maps/embed/v1/directions?key=AIzaSyB1RD2gilBuJjZPQP500vCZPMoDqGfBav8&origin=505+cypress+point+drive+mountain+view&destination=2400+Forest+Ave%2C+San+Jose"></iframe>', 
                        
                        // this works, except for Topshop - it doesn't show the name in the destination...
                        '<iframe id="trailerIFrame" class="dialog-trailer" allowfullscreen="true" frameborder="0" src={{trustedUrl}}></iframe>', 
                        
                        // this still doesn't work for Topshop - why?
                        //'<iframe id="trailerIFrame" class="dialog-trailer" allowfullscreen="true" frameborder="0" src=https://www.google.com/maps/embed/v1/directions?key=AIzaSyB1RD2gilBuJjZPQP500vCZPMoDqGfBav8&origin=250+castro+street+mountain+view&destination=Topshop+@2400+Forest+Ave%2C+San+Jose"></iframe>', 
                        
                        //+	'<div class="dialog-movie-info-spacing">' +
                        //    	'<div class="dialog-movie-name-rating-spacing"></div>'+
                        //        	'<span class="dialog-movie-name-rating">' +
                        //            	'<h1 class="dialog-movie-name">DEBUG dialog-preview-directive.js</h1>' +
                        //                '<h5 class="dialog-movie-name">Store name is {{store.name}}</h5>' +
                        //                '<h5 class="dialog-movie-name">Address is {{store.address}}</h5>' +
                        //                '<h5 class="dialog-movie-name">Id is {{store.id}}</h5>' +
                        //                '<h5 class="dialog-release-label" ng-hide="hideReleaseDate">Release date:' +
                        //                '<span class="dialog-release-date"> {{store.name}},{{store.address}}</span>' +
                        //                '</h5>' +
                        //                '<showtoggle></showtoggle>' +
                        //                '<div class="dialog-rating-spacing"></div>' + 
                        //        '</div>' + 
                        // '</div>'             
            'restrict': 'E',
            'link': function (scope, element, attr) {
            //'link': function (element) {	
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
                    //scope.$apply(scope.dialogCtrl.clearMovieSelection());
                    scope.$apply(scope.dialogCtrl.clearStoreSelection());
                    $(window).off('resize', resizeContents);
                    e.preventDefault();
                    e.stopPropagation();
                });
                $(window).resize(resizeContents);
                //<iframe width="560" height="315" src="https://www.youtube.com/embed/vCqiNF94yDw?controls=0&amp;showinfo=0" frameborder="0" allowfullscreen></iframe>
                scope.$watch(function () {
                    return scope.dialogCtrl.getCurrentStore();
                }, function () {
                    var trustedUrl = null;
                    //var movie = $parse(attr.content)(scope);
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
