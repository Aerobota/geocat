var links = [ "../../apps/shared-objects/app/lib/angular/angular-route.js",
		"../../apps/shared-objects/app/js/factories.js",
		"../../apps/shared-objects/app/js/controllers.js",
		"../../apps/shared-objects/app/js/filters.js",
		"../../apps/shared-objects/app/js/directives.js" ];

for (link in links) {
	jQuery.ajax(links[link], {
		async : false,
		cache : true,
		dataType : 'script'
	});
}

'use strict';
// Declare app level module which depends on filters, and services
angular.module(
		'SharedObjects',
		[ 'SharedObjects.filters', 'SharedObjects.factories',
				'SharedObjects.directives', 'SharedObjects.controllers',
				'ngRoute' ]).config(
		[ '$routeProvider', function($routeProvider) {
			$routeProvider.when('/:validated/keywords', {
				templateUrl : 'partials/shared.html',
				controller : 'KeywordControl'
			});
			$routeProvider.otherwise({
				templateUrl : 'partials/shared.html',
				controller : 'KeywordControl'
			});
		} ]);

// Check language on url
changePageLanguage = function(lang) {
	window.location.search = '?lang=' + lang;
};

var languageHolder = 'eng';
var init = function() {
	try {
		var matches = window.location.search.match(/lang=(\w\w\w)/);
		if (matches.length == 2) {
			languageHolder = matches[1];
		}

		if (languageHolder === 'deu') {
			languageHolder = 'ger';
		} else if (languageHolder === 'fra') {
			languageHolder = 'fre';
		}

		jQuery.ajax('../../srv/' + languageHolder + '/strings.js', {
			async : false,
			cache : true,
			dataType : 'script'
		});
		Geonet.language = languageHolder;
	} catch (e) {
		if (window.location.href.indexOf('?') === -1) {
			window.location.search = '?lang=eng';
		}
	}
};

init();

// Share input between keyword and group (for UI reasons)
$(function() {

	$("#xpathtype").change(function() {
		$("#xpath").val("");
		$("#keyword").val("");
		$("#group").val("");

		$("#keyword").removeClass("error");
		$("#group").removeClass("error");
		if ($("#xpathtype").val() == "KEYWORD") {
			$("#group").hide();
			$("#keyword").show();
		} else {
			$("#keyword").hide();
			$("#group").show();
		}
	});
});

// Load javascript translations
$.ajax('../../srv/' + languageHolder + '/strings.js', {
	async : false,
	cache : true,
	dataType : 'script'
});

var t_controller = angular.module('table_module', []);

t_controller.controller('table_controller',

// Load existing schematron rules
function($scope, $http) {

	// Remove item from table list
	$scope.removeItem = function(a) {
		if (confirm(confirmDelete) == true) {
			$http({
				method : 'GET',
				url : '',
				params : {
					action : 'delete',
					id : a.row.id
				}
			}).success(function() {
				$scope.update($scope, $http);
			});
		}
	};

	// update items on table list
	$scope.update = function($scope, $http) {
		$http({
			method : 'GET',
			url : 'reusable.list.js',
			params : {
				type : 'schematronrules'
			}
		}).success(function(data) {
			$scope.data = data;
			
			angular.forEach($scope.data, function(item) {
				if(item.required) {
					item.required =  "check";
				} else {
					item.required = "";
				}
				if(item.type == 0) {
					item.type = "group" ;
				} else {
					item.type = "keyword";
				}
			});
		});
	};

	$scope.update($scope, $http);
});

function checkErrors() {
	$("#keyword").removeClass("error");
	$("#group").removeClass("error");
	if (criteria.keyword.value == '') {
		$(criteria.keyword).addClass("error");
	}
	if (criteria.group.value == '') {
		$(criteria.group).addClass("error");
	}
}

function TypeaheadCtrl($scope, $http, limitToFilter) {
	$scope.getKeywords = function(val) {
		return $http.get('reusable.list.js', {
			params : {
				validated : true,
				type : 'keywords'
			}
		}).then(function(data) {
			var res = [];
			angular.forEach(data.data, function(item) {
				res.push({
					label : item.search,
					value : item.search
				});
			});
			return limitToFilter(res, 8);
		});
	};

	$scope.updateVal = function($item, $model, $label) {
		$('#xpath').val($item.value);
	}

	$scope.getGroups = function(val) {
		return $http.get(
				'http://localhost:8190/geonetwork/srv/eng/xml.group.list', {
					dataType : "xml"
				}).then(function(data) {
			var res = [];

			xml = data.responseXML;
			if (!xml) {
				xml = data.xml;
			}

			if (!xml) {
				if (window.DOMParser) {
					parser = new DOMParser();
					xml = parser.parseFromString(data.data, "text/xml");
				} else // Internet Explorer
				{
					xml = new ActiveXObject("Microsoft.XMLDOM");
					xml.async = false;
					xml.loadXML(data.data);
				}
			}

			angular.forEach(xml.getElementsByTagName("record"), function(item) {
				var id = item.getElementsByTagName("id")[0];
				var name = item.getElementsByTagName("name")[0];
				res.push({
					label : name.innerText || name.textContent,
					value : id.innerText || id.textContent
				});
			});
			return limitToFilter(res, 8);
		});
	};
}

var app = angular.module('metadataSchemaValidation', [ 'table_module',
		'ui.bootstrap' ]);

app.controller('addNewEntry',
// Add new entry on table list
function($scope, $http) {
	$scope.submit = function() {
		checkErrors();
		$http({
			method : 'GET',
			url : '',
			params : {
				action : 'add',
				schematron : $scope.formData.schematron,
				value : $('#xpath').val(),
				type : $scope.formData.type
			}
		}).success(function(data) {
			$("#resultTable").scope().update($("#resultTable").scope(), $http);
			$("#xpath").val("");
			$("#keyword").val("");
			$("#group").val("");
		});
	};
});
