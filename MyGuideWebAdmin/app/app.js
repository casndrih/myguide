
angular.module('App', [ 'ngMaterial', 'ngRoute', 'ngMessages', 'ngCookies', 'ngSanitize', 'cl.paging', 'textAngular']);

angular.module('App').config(function($mdThemingProvider) {

    // Extend the cyan theme with a few different colors
    var pink = $mdThemingProvider.extendPalette('pink', {
      '500': 'E91E63',
      'contrastDefaultColor': 'light'
    });

    // Extend the amber theme with a few different colors
    var purple = $mdThemingProvider.extendPalette('purple', {
      '500': '673AB7'
    });

    // Register the new color palette
    $mdThemingProvider.definePalette('pink', pink);

    // Register the new color palette
    $mdThemingProvider.definePalette('purple', purple);

    $mdThemingProvider.theme('default')
    .primaryPalette('pink')
    .accentPalette('purple');
  }
);

angular.module('App').config(['$routeProvider', function($routeProvider) {
    $routeProvider.
      when('/place', {
        templateUrl : 'templates/place/place.html',
        controller  : 'PlaceController'
      }).
      when('/add_place', {
        templateUrl : 'templates/place/create.html',
        controller  : 'AddPlaceController'
      }).
      when('/news_info', {
        templateUrl : 'templates/news/news.html',
        controller  : 'NewsController'
      }).
      when('/add_news', {
        templateUrl : 'templates/news/create.html',
        controller  : 'AddNewsController'
      }).
      when('/gcm', {
        templateUrl : 'templates/gcm/gcm.html',
        controller  : 'GcmController'
      }).
      when('/setting', {
        templateUrl : 'templates/setting/setting.html',
        controller  : 'SettingController'
      }).
      when('/about', {
        templateUrl : 'templates/about/about.html',
        controller  : 'AboutController'
      }).
      when('/login', {
        templateUrl : 'templates/login/login.html',
        controller  : 'LoginController'
      }).
      otherwise({
        redirectTo  : '/login'
      });
}]);

angular.module('App').run(function($location, $rootScope, $cookies) {
  $rootScope.$on('$routeChangeSuccess', function (event, current, previous) {
    // $rootScope.title = current.$$route.title;
  });
});

angular.module('App').factory('focus', function($timeout, $window) {
    return function(id) {
		// timeout makes sure that is invoked after any other event has been triggered.
		// e.g. click events that need to run before the focus or inputs elements that are in a disabled state but are enabled when those events are triggered.
		$timeout(function() {
			var element = $window.document.getElementById(id);
			if(element)element.focus();
		});
	};
});

angular.module('App').filter('trustAsHtml', function($sce) {
    return function(html) {
        return $sce.trustAsHtml(html);
    };
});

angular.module('App').filter('cut', function () {
    return function (value, wordwise, max, tail) {
        if (!value) return '';

        max = parseInt(max, 10);
        if (!max) return value;
        if (value.length <= max) return value;

        value = value.substr(0, max);
        if (wordwise) {
            var lastspace = value.lastIndexOf(' ');
            if (lastspace != -1) {
                //Also remove . and , so its gives a cleaner result.
                if (value.charAt(lastspace-1) == '.' || value.charAt(lastspace-1) == ',') {
                    lastspace = lastspace - 1;
                }
                value = value.substr(0, lastspace);
            }
        }

        return value + (tail || ' …');
    };
});
