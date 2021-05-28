angular.module('App').controller('AddNewsController',
    function ($rootScope, $scope, $http, $mdToast, $mdDialog, $route, $timeout, services) {
        var self = $scope;
        var root = $rootScope;

        if (!root.isCookieExist()) {
            window.location.href = '#login';
        }

        root.toolbar_menu = null;
        var isNew = (root.getCurNewsId() == null);
        var original;
        var now = new Date().getTime();
        var dir = "/uploads/news/";
        self.buttonText = (isNew) ? 'SAVE' : 'UPDATE';

        root.pagetitle = (isNew) ? 'Add News Info' : 'Edit News Info';
        self.buttonText = (isNew) ? 'SAVE' : 'UPDATE';
        root.closeAndDisableSearch();
        self.submit_loading = false;

        /* check edit or add new place*/
        if (isNew) {
            self.imageValid = false;
            original = {
                title: null, brief_content: null, full_content: null,
                image: null, last_update: now
            };
            self.news = angular.copy(original);
        } else {
            self.imageValid = true;
            services.getNewsInfo(root.getCurNewsId()).then(function (resp) {
                original = resp.data;
                self.news = angular.copy(original);
            });
        }

        /* for selecting primary image file */
        self.onFileSelect = function (files) {
            self.image_primary = null;
            self.imageValid = false;
            var f = files[0];
            if (root.constrainFile(f)) {
                self.image_primary = f;
                self.imageValid = true;
            }
            $mdToast.show($mdToast.simple().content("Selected file").position('bottom right'));
        };

        /* method for submit action */
        self.done_arr = [false, false];
        self.submit = function (n) {
            self.submit_loading = true;
            self.submit_done = false;
            if (isNew) { // new entry
                n.image = "news_info_" + new Date().getTime() + root.getExtension(self.image_primary);
                services.insertNewsInfo(n).then(function (resp) {
                    self.resp_submit = resp;
                    if (resp.status == 'success') {
                        services.uploadFileToUrl(self.image_primary, dir, n.image, "").then(function () {
                            self.done_arr[0] = true;
                        }); // upload primary image
                    } else {
                        self.submit_done = true;
                    }
                });

            } else { // update existing
                n.last_update = now;
                var oldname = angular.copy(n.image);
                n.image = (self.image_primary != null) ? "news_info_" + new Date().getTime() + root.getExtension(self.image_primary) : n.image;
                services.updateNewsInfo(n.id, n).then(function (resp) {
                    self.resp_submit = resp;
                    if (resp.status == 'success') {
                        if (self.image_primary != null) {
                            services.uploadFileToUrl(self.image_primary, dir, n.image, oldname).then(function () {
                                self.done_arr[0] = true;
                            }); // upload primary image
                        } else {
                            self.done_arr[0] = true;
                        }
                    } else {
                        self.submit_done = true;
                    }
                });
            }

        };

        /* Submit onFinish Checker */
        self.$watchCollection('done_arr', function (new_val, old_val) {
            if (self.submit_done || new_val[0]) {
                loop_run = false;
                $timeout(function () { // give delay for good UI
                    if (self.resp_submit.status == 'success') {
                        $mdToast.show($mdToast.simple().hideDelay(1000).content(self.resp_submit.msg).position('bottom right')).then(function () {
                            if (self.send_gcm) {
                                self.sendGcmNotification();
                            }
                            window.location.href = '#news_info';
                        });
                    } else {
                        $mdToast.show($mdToast.simple().hideDelay(3000).content(self.resp_submit.msg).position('bottom right'));
                    }
                    self.submit_loading = false;
                }, 1000);
            }
        });

        /* checker when all data ready to submit */
        self.isReadySubmit = function () {
            if (isNew) {
                self.isClean = angular.equals(original, self.news);
                return (!self.isClean && self.imageValid);
            } else {
                self.isClean = angular.equals(original, self.news);
                if (self.image_primary != null) {
                    return self.imageValid;
                } else {
                    return !self.isClean;
                }
            }
        };

        /* for gcm notification */
        self.body = {data: null, registatoin_ids: null};
        var data = {};
        self.sendGcmNotification = function () {
            services.getNewsInfo(self.resp_submit.data.id).then(function (resp) {
                data.title = 'My Guide';
                data.type = 'NEWS_INFO';
                data.content = 'News Info : ' + resp.data.title;
                data.news = resp.data;
                // assign data to body
                self.body.data = data;
                services.sendNotifications(self.body).then(function (resp) {
                    console.log(JSON.stringify(resp.data));
                });
            });
        };

        self.cancel = function () {
            window.location.href = '#news_info';
        };
        self.isNewEntry = function () {
            return isNew;
        };

        /* dialog View Image*/
        self.viewImage = function (ev, f) {
            $mdDialog.show({
                controller: ViewImageDialogController,
                template: '<md-dialog ng-cloak aria-label="viewImage">' +
                '  <md-dialog-content style="max-width:800px;max-height:810px;" >' +
                '   <img style="margin: auto; max-width: 100%; max-height= 100%;" ng-src="{{file_url}}">' +
                '  </md-dialog-content>' +
                '</md-dialog>',
                parent: angular.element(document.body),
                targetEvent: ev,
                clickOutsideToClose: true,
                file_url: f
            })
        };
    });

function ViewImageDialogController($scope, $mdDialog, $mdToast, file_url) {
    $scope.file_url = file_url;
}
