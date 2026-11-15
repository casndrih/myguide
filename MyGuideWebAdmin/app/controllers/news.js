angular.module('App').controller('NewsController',
    function ($rootScope, $scope, $http, $mdToast, $cookies, $mdDialog, $route, services) {
        var self = $scope;
        var root = $rootScope;

        if (!root.isCookieExist()) {
            window.location.href = '#login';
        }
        root.pagetitle = 'News Info';
        self.loading = true;
        self.max_item = 20;
        root.search_enable = true;
        root.toolbar_menu = {title: 'Add News'};

        root.barAction = function (ev) {
            root.setCurNewsId("");
            window.location.href = '#add_news';
        };

        // receiver submitSearch from rootScope
        self.$on('submitSearch', function (event, data) {
            self.q = data;
            self.loadPages();
        });

        // load pages from database and display
        self.loadPages = function () {
            $_q = self.q ? self.q : '';
            self.paging.limit = self.max_item;
            services.getNewsInfoCount($_q).then(function (resp) {
                self.paging.total = Math.ceil(resp.data / self.paging.limit);
                self.paging.modulo_item = resp.data % self.paging.limit;
            });
            $limit = self.paging.limit;
            $current = (self.paging.current * self.paging.limit) - self.paging.limit + 1;
            if (self.paging.current == self.paging.total && self.paging.modulo_item > 0) {
                self.limit = self.paging.modulo_item;
            }
            services.getNewsInfoByPage($current, $limit, $_q).then(function (resp) {
                self.news_infos = resp.data;
                self.loading = false;
            });

        };

        //pagination property
        self.paging = {
            total: 0, // total page
            current: 1, // start page
            step: 3, // count number display
            limit: self.max_item, // max item per page
            modulo_item: 0,
            onPageChanged: self.loadPages
        };

        self.editNews = function (ev, n) {
            root.setCurNewsId(n.id);
            window.location.href = '#add_news';
        };

        self.detailsNews = function (ev, n) {
            $mdDialog.show({
                controller: DetailsNewsControllerDialog,
                templateUrl: 'templates/news/details.html',
                parent: angular.element(document.body),
                targetEvent: ev,
                clickOutsideToClose: true,
                news: n
            })
        };

        self.deleteNews = function (ev, n) {
            var confirm = $mdDialog.confirm().title('Delete Confirmation')
                .content('Are you sure want to delete News : ' + n.title + ' ?')
                .targetEvent(ev).ok('OK').cancel('CANCEL');

            $mdDialog.show(confirm).then(function () {
                services.deleteNewsInfo(n.id).then(function (res) {
                    if (res.status == 'success') {
                        services.deleteFile("/uploads/news/", n.image);
                        $mdToast.show($mdToast.simple().hideDelay(1000).content('Delete News ' + n.title + ' Success!').position('bottom right')).then(function () {
                            window.location.reload();
                        });
                    } else {
                        $mdToast.show($mdToast.simple().hideDelay(6000).action('CLOSE').content('Opps , Failed delete News ' + n.title).position('bottom right')).then(function (response) {
                        });
                    }
                });
            }, function () {

            });

        };

    });

function DetailsNewsControllerDialog($scope, $mdDialog, services, news) {
    var self = $scope;
    self.news = news;
    self.hide = function () {
        $mdDialog.hide();
    };
    self.cancel = function () {
        $mdDialog.cancel();
    };
}
