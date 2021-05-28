angular.module('App').controller('RootCtrl',
function($rootScope, $scope, $mdSidenav, $mdToast, $mdDialog, $cookies, services, focus) {

	var self = $scope;
	var root = $rootScope;

	self.bgColor  = '#d9d9d9';
	self.black    = '#000000';

	root.base_url   = window.location.origin;
	self.uid_key    = '_session_uid';
	self.uid_name   = '_session_name';
	self.uid_email  = '_session_email';
	self.uid_password  = '_session_password';

	// retrive session data
	self.data = {
		user: {
			name  : $cookies.get(root.base_url+self.uid_name),
			email : $cookies.get(root.base_url+self.uid_email),
			icon  : 'face'
		}
	};

	/* prepare category data */
	root.categories = [];
	services.getCategories().then(function(resp){
		root.categories = resp.data;
		self.sidenav.actions[1].sub_menu = resp.data;
	});

    // flag toolbar action button
    root.search_enable = false;
    root.search_show = false;

    // when bar action clicked
    root.barAction =  function(ev) {
        root.$broadcast('barAction', "");
    };

    // when search icon click
    root.searchAction =  function(ev) {
        focus('search_input');
        root.search_show = true;
        root.$broadcast('searchAction', null);
    };

    // when search close
    root.closeSearch =  function(ev) {
        root.search_show = false;
        root.$broadcast('submitSearch', "");
    };

    // when search text submit
    root.submitSearch =  function(ev, q) {
        root.$broadcast('submitSearch', q);
    };
    // when search text submit by press enter
    root.keypressAction = function(k_ev, q) {
        if (k_ev.which === 13){
            root.$broadcast('submitSearch', q);
        }
    };

    root.closeAndDisableSearch = function(){
        root.search_enable = false;
        root.search_show = false;
    };

	self.toggleSidenav = function() {
		$mdSidenav('left').toggle();
	};

	self.doLogout = function(ev){
		var confirm = $mdDialog.confirm().title('Logout Confirmation')
		.content('Are you sure want to logout from user : '+root.getSessionName()+' ?')
		.targetEvent(ev)
		.ok('OK').cancel('CANCEL');
		$mdDialog.show(confirm).then(function() {
			// clear session
			root.clearCookies();
			window.location.href = '#login';
			$mdToast.show($mdToast.simple().content('Logout Success').position('bottom right'));
		});
	};

	root.clearCookies = function(){
		// saving session
		$cookies.remove(root.base_url+self.uid_key, null);
		$cookies.remove(root.base_url+self.uid_name, null);
		$cookies.remove(root.base_url+self.uid_email, null);
		$cookies.remove(root.base_url+self.uid_password, null);
	};

	root.saveCookies = function(id, name, email, password){
		// saving session
		$cookies.put(root.base_url+self.uid_key, id);
		$cookies.put(root.base_url+self.uid_name, name);
		$cookies.put(root.base_url+self.uid_email, email);
		$cookies.put(root.base_url+self.uid_password, password);
		console.log("TOKEN A : "+$cookies.get(root.base_url+self.uid_password));
	};

	root.isCookieExist = function(){
		var uid   = $cookies.get(root.base_url+self.uid_key);
		var name  = $cookies.get(root.base_url+self.uid_name);
		var email = $cookies.get(root.base_url+self.uid_email);
		var password = $cookies.get(root.base_url+self.uid_password);
		if(uid == null || name == null || email == null || password == null){
			return false;
		}
		return true;
	};

	root.getSessionUid = function(){
		return $cookies.get(root.base_url+self.uid_key);
	};

	root.getSessionName = function(){
		return $cookies.get(root.base_url+self.uid_name);
	};

	root.getSessionEmail = function(){
		return $cookies.get(root.base_url+self.uid_email);
	};
  
	root.getSessionPassword = "asdsadadsd";

	self.sidenav = {
	actions: [{
		name: 'PLACE',
		icon: 'place',
		link: '#place',
		sub : false
	  }, {
		name: 'CATEGORIES',
		icon: 'dns',
		link: '#gcm',
		sub : true,
		sub_expand : false,
		sub_menu : []
	  }, {
        name: 'NEWS INFO',
        icon: 'subject',
        link: '#news_info',
        sub : false
      }, {
		name: 'NOTIFICATION',
		icon: 'notifications',
		link: '#gcm',
		sub : false
	  }, {
		name: 'SETTING',
		icon: 'settings',
		link: '#setting',
		sub : false
	  }, {
		name: 'ABOUT',
		icon: 'web_asset',
		link: '#about',
		sub : false
	  }]
	}

	self.directHref = function(href){
		root.sub_obj = '';
		self.toggleSidenav();
		window.location.href = href;
	};

	root.sub_obj = '';
	root.subMenuAction = function(ev, obj) {
		root.sub_obj = obj.cat_id;
		window.location.href = '#place';
		root.pagetitle = 'Place : '+obj.name;
	};

	root.sortArrayOfInt = function(array_of_int){
		array_of_int.sort(function(a, b){return a-b});
	};

    // for editing place
    root.setCurPlaceId = function(id){
        $cookies.put(root.base_url+'cur_place_id', id);
    };
    root.getCurPlaceId = function(){
        var id = $cookies.get(root.base_url+'cur_place_id');
        return (id != "") ? id : null;
    };

    // for editing news info
    root.setCurNewsId = function(id){
        $cookies.put(root.base_url+'cur_news_id', id);
    };
    root.getCurNewsId = function(){
        var id = $cookies.get(root.base_url+'cur_news_id');
        return (id != "") ? id : null;
    };

	root.getExtension = function(f){
		return (f.type == "image/jpeg" ? '.jpg' : '.png');
	};
	root.constrainFile = function(f){
		return ((f.type =="image/jpeg" || f.type =="image/png" ) && f.size <= 500000);
	}

});
