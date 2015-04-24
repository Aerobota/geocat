(function() {
  goog.provide('gn_formatter_viewer');


  goog.require('gn');
  goog.require('gn_catalog_service');
  goog.require('gn_formatter_lib');
  goog.require('gn_utility_directive');
  goog.require('gn_popup_directive');
  // GEOCAT
  goog.require('gn_search_geocat_mdactionmenu');
  goog.require('gn_mdactions_service');
  goog.require('gn_alert');
  goog.require('gn_popup_service');
  // END GEOCAT








  // GEOCAT
  var module = angular.module('gn_formatter_viewer',
      ['ngRoute', 'gn', 'gn_utility_directive', 'gn_catalog_service', 'gn_search_geocat_mdactionmenu',
        'gn_popup_service', 'gn_mdactions_service', 'gn_alert']);
  // END GEOCAT

  // Define the translation files to load
  module.constant('$LOCALES', ['core']);

  module.controller('GnFormatterViewer',
      ['$scope', '$http', '$sce', '$routeParams',
       function($scope, $http, $sce, $routeParams) {
         $scope.md = {
           'geonet:info': {}
         };
         $scope.metadata = '';
         $scope.loading = true;

         var formatter = $routeParams.formatter;
         var mdId = $routeParams.mdId;

         $http.get('md.format.xml?xsl=' + formatter + '&id=' + mdId).
         success(function(data) {
           $scope.loading = undefined;
           $scope.metadata = $sce.trustAsHtml(data);
         }).error(function(data) {
           $scope.loading = undefined;
           $scope.metadata = $sce.trustAsHtml(data);
         });
         $http.get('qi?_content_type=json&fast=index&_id=' + mdId).success(function(data){
           angular.copy(data.metadata, $scope.md);
         });

       }]);
  module.config(['$routeProvider', function($routeProvider) {
    var tpls = '../../catalog/templates/';

    $routeProvider.when('/:formatter/:mdId', { templateUrl: tpls +
          '/formatter-viewer.html', controller: 'GnFormatterViewer'});
  }]);
})();
