angular.module('App').controller('GcmController',
function($rootScope, $scope, $http, $mdToast, $cookies, $mdDialog, $route, services){
  var self             = $scope;
  var root             = $rootScope;

  if(!root.isCookieExist()){ window.location.href = '#login'; }
  root.pagetitle       = 'GCM';
  self.loading         = true;
  self.max_item        = 20;
  root.search_enable   = true;
  root.toolbar_menu    = { title : 'Send GCM' }

  root.closeSearch();
  root.barAction =  function(ev) {
    self.sendGcm(ev);
  }

  // receiver submitSearch from rootScope
  self.$on('submitSearch', function (event, data) {
    self.q = data;
    self.loadPages();
  });

  // load pages from database and display
  self.loadPages = function () {
    $_q = self.q ? self.q : '';
    self.paging.limit = self.max_item;
    services.getGcmCount($_q).then(function(resp){
      self.paging.total = Math.ceil(resp.data / self.paging.limit);
      self.paging.modulo_item = resp.data % self.paging.limit;
    });
    $limit = self.paging.limit;
    $current = (self.paging.current * self.paging.limit) - self.paging.limit + 1;
    if (self.paging.current == self.paging.total && self.paging.modulo_item > 0) {
        self.limit = self.paging.modulo_item;
    }
    services.getGcmByPage($current, $limit, $_q).then(function(resp){
      self.gcm = resp.data;
      self.loading = false;
    });

  };

  self.sendGcm = function(ev) {
    $mdDialog.show({
      controller          : GcmControllerDialog,
      templateUrl         : 'templates/gcm/send.html',
      parent              : angular.element(document.body),
      targetEvent         : ev,
      clickOutsideToClose : false,
      recipe              : null
    })
  };
  
  //pagination property
  self.paging = {
    total : 0, // total page
    current : 1, // start page
    step : 3, // count number display
    limit : self.max_item, // max item per page
    modulo_item : 0,
    onPageChanged: self.loadPages,
  };

});


function GcmControllerDialog($rootScope, $scope, $mdDialog, services, $mdToast, $route, $timeout, recipe) {
  var self = $scope;
  var root = $rootScope;

  self.title      = 'Send GCM';
  self.submit_loading = false;
  self.hide = function() { $mdDialog.hide(); };
  self.cancel = function() { $mdDialog.cancel(); };
  self.showResult = false;
  self.body={
    data:null,
    registatoin_ids:null
  };

  self.submit = function() {
    self.body.data = self.data;
    self.submit_loading = true;
    self.showResult = false;

    services.sendNotifications(self.body).then(function(resp){
      console.log(JSON.stringify(resp.data));
      if( resp.data!= null && resp.data!= '' ){
        self.showResult = true;
        self.result = resp.data;
      }else{
        $mdToast.show($mdToast.simple().content("Failed send GCM").position('bottom right'));
      }
      self.submit_loading = false;
    });
  }

}
