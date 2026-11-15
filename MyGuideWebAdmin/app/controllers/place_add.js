angular.module('App').controller('AddPlaceController',
    function ($rootScope, $scope, $http, $mdToast, $mdDialog, $route, $timeout, services) {
        var self = $scope;
        var root = $rootScope;

        if (!root.isCookieExist()) {
            window.location.href = '#login';
        }

        root.toolbar_menu = null;
        var isNew = (root.getCurPlaceId() == null);
        var original;
        var now = new Date().getTime();
        var dir = "/uploads/place/";

        root.pagetitle = (isNew) ? 'Add Place' : 'Edit Place';
        self.buttonText = (isNew) ? 'SAVE' : 'UPDATE';
        root.closeAndDisableSearch();

        self.submit_loading = false;
        self.send_gcm = false;
        self.images = [null, null, null, null];
        self.cat_selected = [];
        self.place_category = [];
        self.imagesValid = [true, true, true, true];
        self.images_file = [null, null, null, null];
        services.getCategories().then(function (resp) {
            self.categories_data = resp.data;
        });

        /* check edit or add new place*/
        if (isNew) {
            self.categoryValid = false;
            self.imageValid = false;
            original = {
                name: null, image: null, address: null, fee: null, status: null, phone: null, website: null,
                description: null, lat: 0, lng: 0, last_update: now
            };
            self.place = angular.copy(original);
        } else {
            self.imageValid = true;
            self.categoryValid = true;
            self.ori_cat_selected = [];
            services.getPlace(root.getCurPlaceId()).then(function (resp) {
                original = resp.data;
                self.place = angular.copy(original);
            });
            services.placeCategoriesByPlaceId(root.getCurPlaceId()).then(function (resp) {
                for (var i = 0; i < resp.data.length; i++) {
                    self.ori_cat_selected.push(resp.data[i].cat_id);
                }
                root.sortArrayOfInt(self.ori_cat_selected);
                self.cat_selected = angular.copy(self.ori_cat_selected);
            });
            services.imagesByPlaceId(root.getCurPlaceId()).then(function (resp) {
                for (var i = 0; i < 4; i++) {
                    self.images[i] = (resp.data[i] == null || resp.data[i] == "") ? null : resp.data[i];
                }
            });
        }

        /* for selecting category */
        self.toggleCategory = function (i, list) {
            var idx = list.indexOf(i.cat_id);
            if (idx > -1) {
                list.splice(idx, 1);
            } else {
                list.push(i.cat_id);
            }
            root.sortArrayOfInt(list);
            self.categoryValid = (self.cat_selected.length > 0);
        };
        self.isCategorySelected = function (i, list) {
            return list.indexOf(i.cat_id) > -1;
        };

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

        /* for selecting optional images file */
        self.onFileSelectImages = function (files, idx) {
            self.images_file[idx] = null;
            self.imagesValid[idx] = false;
            var f = files[0];
            if (root.constrainFile(f)) {
                self.images_file[idx] = f;
                self.imagesValid[idx] = true;
            }
            $mdToast.show($mdToast.simple().content("Selected file").position('bottom right'));
        };

        /* method for submit action */
        // [0] place cat done, [1] primary image done, [2] optional images done
        self.done_arr = [false, false, false, false];
        self.submit = function (p) {
            self.submit_loading = true;
            self.submit_done = false;
            self.resp_submit = null;
            self.done_arr = [false, false, false];
            if (isNew) { // new entry
                p.image = "place_" + new Date().getTime() + root.getExtension(self.image_primary);
                self.images = [null, null, null, null];
                services.insertPlace(p).then(function (resp) {
                    self.resp_submit = resp;
                    if (resp.status == 'success') {
                        self.preparePlaceCategory(resp.data.place_id);
                        services.insertPlaceCategories(self.place_category).then(function () {
                            self.done_arr[0] = true;
                        }); // insert table relation
                        services.uploadFileToUrl(self.image_primary, dir, p.image, "").then(function () {
                            self.done_arr[1] = true;
                        }); // upload primary image
                        if (!angular.equals(self.images_file, [null, null, null, null])) {
                            self.uploadOptionalImages(resp.data.place_id, 0, 0); // upload optional image
                        } else {
                            self.done_arr[2] = true;
                        }
                    } else {
                        self.done_arr[0] = true;
                        self.submit_done = true;
                    }
                });

            } else { // update existing
                p.last_update = now;
                var oldname = angular.copy(p.image);
                p.image = (self.image_primary != null) ? "place_" + new Date().getTime() + root.getExtension(self.image_primary) : p.image;
                services.updatePlace(p.place_id, p).then(function (resp) {
                    self.resp_submit = resp;
                    if (resp.status == 'success') {
                        self.preparePlaceCategory(resp.data.place_id);
                        services.insertPlaceCategories(self.place_category).then(function () {
                            self.done_arr[0] = true;
                        }); // insert table relation
                        if (self.image_primary != null) {
                            services.uploadFileToUrl(self.image_primary, dir, p.image, oldname).then(function () {
                                self.done_arr[1] = true;
                            }); // upload primary image
                        } else {
                            self.done_arr[1] = true;
                        }
                        if (!angular.equals(self.images_file, [null, null, null, null])) {
                            self.uploadOptionalImages(resp.data.place_id, 0, 0); // upload optional image
                        } else {
                            self.done_arr[2] = true;
                        }
                    } else {
                        self.done_arr[0] = true;
                        self.submit_done = true;
                    }
                });
            }

        };

        /* Submit onFinish Checker */
        self.$watchCollection('done_arr', function (new_val, old_val) {
            if (self.submit_done || (new_val[0] && new_val[1] && new_val[2])) {
                loop_run = false;
                $timeout(function () { // give delay for good UI
                    if (self.resp_submit.status == 'success') {
                        $mdToast.show($mdToast.simple().hideDelay(1000).content(self.resp_submit.msg).position('bottom right')).then(function () {
                            if (self.send_gcm) {
                                self.sendGcmNotification();
                            }
                            window.location.href = '#place';
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
                self.isClean = angular.equals(original, self.place);
                return (!self.isClean && self.imageValid && self.categoryValid && self.isImagesValid());
            } else {
                self.isClean = ( angular.equals(original, self.place) && angular.equals(self.ori_cat_selected, self.cat_selected) && angular.equals(self.images_file, [null, null, null, null]));
                if (self.image_primary != null) {
                    return (self.categoryValid && self.imageValid);
                } else {
                    return (!self.isClean && self.categoryValid);
                }
            }
        };
        self.isImagesValid = function () {
            for (var i = 0; i < self.images_file.length; i++) {
                if (!self.imagesValid[i]) {
                    return false;
                }
            }
            return true;
        };

        /* normalize place category object by adding place id */
        self.preparePlaceCategory = function (p_id) {
            self.place_category = [];
            for (var i = 0; i < self.cat_selected.length; i++) {
                var item_place_cat = {place_id: p_id, cat_id: self.cat_selected[i]};
                self.place_category.push(item_place_cat);
            }
        };

        /* uploader for optional images, using recursive method*/
        self.uploadOptionalImages = function (p_id, n, idx) {
            if (n < self.images_file.length) {
                if (self.images_file[n] && self.imagesValid[n]) {
                    var name = "place_" + self.place.name + "_" + n + new Date().getTime() + root.getExtension(self.images_file[n]);
                    var oldname = ( self.images[n] != null && self.images[n].name != null ) ? self.images[n].name : "";
                    services.uploadFileToUrl(self.images_file[n], dir, name, oldname).then(function (resp) {
                        if (resp.status == 'success') {
                            self.images[n] = {place_id: p_id, name: name};
                            self.uploadOptionalImages(p_id, (n + 1), idx + 1);
                        } else {
                            self.uploadOptionalImages(p_id, (n + 1), idx);
                        }
                    });
                } else {
                    self.uploadOptionalImages(p_id, (n + 1), idx);
                }
            } else {
                var _index = 0;
                for (var i = 0; i < 4; i++) {
                    if (_index < self.images.length && self.images[_index] == null) {
                        self.images.splice(_index, 1);
                    } else {
                        _index++;
                    }
                }
                services.insertImages(self.images).then(function (resp) {
                    self.done_arr[2] = true;
                });
            }
        };

        /* for gcm notification */
        self.body = {data: null, registatoin_ids: null};
        var data = {};
        self.sendGcmNotification = function () {
            console.log("place_id : " + self.resp_submit.data.place_id);
            services.getPlace(self.resp_submit.data.place_id).then(function (resp) {
                data.title = 'My Guide';
                data.type = 'PLACE';
                data.content = (isNew) ? 'New Place Added : ' + self.place.name : 'New Update Place : ' + self.place.name;
                data.place = resp.data;

                // assign data to body
                self.body.data = data;
                services.sendNotifications(self.body).then(function (resp) {
                    console.log(JSON.stringify(resp));
                });
            });
        };

        self.cancel = function () {
            window.location.href = '#place';
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

        /* delete optional image dialog */
        self.deleteImage = function (ev, img) {
            var confirm = $mdDialog.confirm().title('Delete Confirmation').content('Are you sure want to delete one image ?')
                .targetEvent(ev).ok('OK').cancel('CANCEL');
            $mdDialog.show(confirm).then(function () {
                services.deleteImage(img.name).then(function (res) {
                    if (res.status == 'success') {
                        $mdToast.show($mdToast.simple().hideDelay(1000).content('Delete one image Success!').position('bottom right')).then(function () {
                            window.location.reload();
                        });
                    } else {
                        $mdToast.show($mdToast.simple().hideDelay(6000).action('CLOSE').content('Opps , Failed delete one image').position('bottom right')).then(function (response) {
                        });
                    }
                });
            }, function () {
            });
        };

    });

function ViewImageDialogController($scope, $mdDialog, $mdToast, file_url) {
    $scope.file_url = file_url;
}
