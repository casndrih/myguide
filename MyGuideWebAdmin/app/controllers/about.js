angular.module('App').controller('AboutController',
function($rootScope, $scope, $http, $mdToast, $cookies){
  var self             = $scope;
  var root             = $rootScope;

  if(!root.isCookieExist()){ window.location.href = '#login'; }
  root.closeAndDisableSearch();

  root.toolbar_menu = null;
  $rootScope.pagetitle = 'About';
  self.myVar = false;

});
