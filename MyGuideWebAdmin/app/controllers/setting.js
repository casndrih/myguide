angular.module('App').controller('SettingController',
function($rootScope, $scope, $http, $mdToast, $mdDialog, $route, $timeout, services){
  var self             = $scope;
  var root             = $rootScope;

  if(!root.isCookieExist()){ window.location.href = '#login'; }

  var cur_id = root.getSessionUid();
  self.submit_loading = false;
  self.re_passwordValid = true;
  root.toolbar_menu = null;
  var original;
  $rootScope.pagetitle = 'Setting';
  self.username_pattern = /^[A-Za-z0-9\-\_]+$/;

  root.closeAndDisableSearch();
  services.getUsers(cur_id).then(function(data){
    self.userdata = data.data;
    self.userdata.password = '*****';
    original = angular.copy(self.userdata);
  });

  self.isClean = function() {
    return angular.equals(original, self.userdata);
  }

  self.isPasswordMatch = function() {
    if(self.re_password == null || self.re_password ==''){
       return true;
    }else{
      if(self.re_password == self.userdata.password ){
        return true;
      }else{
        return false;
      }
    }
  }

  self.submit = function(is_new) {
    self.submit_loading = true;
    if(!is_new){
      console.log(JSON.stringify(self.userdata));
      services.updateUsers(cur_id, self.userdata).then(function(resp){
        if(resp.status == 'success'){
          // saving session
          root.saveCookies(resp.data.users.id, resp.data.users.name, resp.data.users.email);
        }
        self.afterSubmit(resp);
      });
    }else{
      if(self.userdata.password === '*****'){
        self.userdata.password = "";
        self.submit_loading = false;
        return;
      }
      self.re_passwordValid = true;
      if(self.re_password != self.userdata.password ){
        self.re_passwordValid = false;
        self.submit_loading = false;
        return;
      }
      self.userdata.id = null;
      services.insertUser(self.userdata).then(function(resp){
        self.afterSubmit(resp);
      });
    }

  }

  self.afterSubmit = function(resp) {
    $timeout(function(){ // give delay for good UI
      self.submit_loading = false;
      if(resp.status == 'success'){
        $mdToast.show($mdToast.simple().hideDelay(1000).content(resp.msg).position('bottom right'))
        .then(function() {
          console.log("finish");
          window.location.reload();
        });
      }else{
        $mdToast.show($mdToast.simple().hideDelay(3000).content(resp.msg).position('bottom right'))
      }
    }, 1000);
  };

});
