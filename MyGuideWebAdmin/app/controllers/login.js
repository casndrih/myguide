angular.module('App').controller('LoginController', 
function($rootScope, $scope, $http, $mdToast, $route, $timeout, services){
	var self 						= $scope;
	var root 						= $rootScope;

	if(root.isCookieExist()){
		root.isLogin	= false;
		window.location.href = '#place';
		$mdToast.show($mdToast.simple().content('Login Success').position('bottom right'));
		window.location.reload();
	}

	root.isLogin		= true;
    root.toolbar_menu 	= null;
    root.closeAndDisableSearch();

	$rootScope.pagetitle = 'Login';
	self.submit_loading  = false;

	self.doLogin = function(){
		self.submit_loading = true;
		services.doLogin(self.userdata).then(function(result){
    	$timeout(function(){ // give delay for good UI
			if(result.data != ""){
				// saving session
				root.saveCookies(result.data.id, result.data.name, result.data.email, result.data.password);
				$mdToast.show($mdToast.simple().content('Login Success').position('bottom right'));
			$route.reload();
			}else{
				$mdToast.show($mdToast.simple().content('Login Failed').position('bottom right'));
			}
			self.submit_loading = false;
		}, 1000);
    	console.log(JSON.stringify(result.data));
  	});
	};

});