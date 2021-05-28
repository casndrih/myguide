angular.module('App').factory("services", function ($http, $cookies) {

    var serviceBase = 'app/services/';

    var obj = {};
    var token = $cookies.get(window.location.origin + '_session_password');
    var config = {headers: {'Token': token}};

    /*
     * USER -------------------------------------------------------------------------------------------------------------
     */
    obj.doLogin = function (userdata) {
        return $http.post(serviceBase + 'login', userdata).then(function (results) {
            return results;
        });
    };

    obj.getUsers = function (id) {
        return $http.get(serviceBase + 'users?id=' + id);
    };

    obj.updateUsers = function (id, users) {
        var data = {id: id, users: users};
        return $http.post(serviceBase + 'updateUsers', data, config).then(function (results) {
            return results.data;
        });
    };

    obj.insertUser = function (user) {
        return $http.post(serviceBase + 'insertUser', user, config).then(function (results) {
            return results.data;
        });
    };

    /*
     * PLACE ------------------------------------------------------------------------------------------------------------
     */
    obj.getPlaces = function (cat_id) {
        return $http.get(serviceBase + 'getPlaces?cat_id=' + cat_id);
    };

    obj.getPlace = function (place_id) {
        return $http.get(serviceBase + 'getPlace?place_id=' + place_id);
    };

    obj.getPlaceDetails = function (place_id) {
        return $http.get(serviceBase + 'getPlaceDetails?place_id=' + place_id);
    };

    obj.insertPlace = function (place) {
        return $http.post(serviceBase + 'insertPlace', place, config).then(function (results) {
            return results.data;
        });
    };

    obj.updatePlace = function (place_id, place) {
        var data = {place_id: place_id, place: place};
        return $http.post(serviceBase + 'updatePlace', data, config).then(function (results) {
            return results.data;
        });
    };

    obj.deletePlace = function (place_id) {
        return $http.delete(serviceBase + 'deletePlace?place_id=' + place_id, config).then(function (results) {
            return results.data;
        });
    };

    obj.getPlaceCount = function (cat_id, q) {
        return $http.get(serviceBase + 'getPlaceCount?cat_id=' + cat_id + '&q=' + q);
    };

    obj.getPlacesByPage = function (page, limit, cat_id, q) {
        return $http.get(serviceBase + 'getPlacesByPage?page=' + page + '&limit=' + limit + '&cat_id=' + cat_id + '&q=' + q);
    };

    /*
     * NEWS INFO -------------------------------------------------------------------------------------------------------
     */
    obj.getNewsInfo = function (id) {
        return $http.get(serviceBase + 'getNewsInfo?id=' + id);
    };

    obj.insertNewsInfo = function (news_info) {
        return $http.post(serviceBase + 'insertNewsInfo', news_info, config).then(function (results) {
            return results.data;
        });
    };

    obj.updateNewsInfo = function (id, news_info) {
        var data = {id: id, news_info: news_info};
        return $http.post(serviceBase + 'updateNewsInfo', data, config).then(function (results) {
            return results.data;
        });
    };

    obj.deleteNewsInfo = function (id) {
        return $http.delete(serviceBase + 'deleteNewsInfo?id=' + id, config).then(function (results) {
            return results.data;
        });
    };

    obj.getNewsInfoCount = function (q) {
        return $http.get(serviceBase + 'getNewsInfoCount?q=' + q);
    };

    obj.getNewsInfoByPage = function (page, limit, q) {
        return $http.get(serviceBase + 'getNewsInfoByPage?page=' + page + '&limit=' + limit + '&q=' + q);
    };

    /*
     * CATEGORY ---------------------------------------------------------------------------------------------------------
     */
    obj.getCategories = function () {
        return $http.get(serviceBase + 'getCategories');
    };

    obj.getCategory = function (cat_id) {
        return $http.get(serviceBase + 'getCategory?cat_id=' + cat_id);
    };
    obj.getCategoriesByPlaceId = function (place_id) {
        return $http.get(serviceBase + 'getCategoriesByPlaceId?place_id=' + place_id);
    };

    /*
     * PLACE CATEGORY ---------------------------------------------------------------------------------------------------
     */
    obj.placeCategoriesByPlaceId = function (place_id) {
        return $http.get(serviceBase + 'placeCategoriesByPlaceId?place_id=' + place_id);
    };

    obj.insertPlaceCategories = function (place_category) {
        return $http.post(serviceBase + 'insertPlaceCategories', place_category, config).then(function (results) {
            return results.data;
        });
    };

    /*
     * IMAGES -----------------------------------------------------------------------------------------------------------
     */
    obj.imagesByPlaceId = function (place_id) {
        return $http.get(serviceBase + 'imagesByPlaceId?place_id=' + place_id);
    };

    obj.insertImages = function (images) {
        return $http.post(serviceBase + 'insertImages', images, config).then(function (results) {
            return results.data;
        });
    };

    obj.deleteImage = function (name) {
        return $http.delete(serviceBase + 'deleteImage?name=' + name, config).then(function (results) {
            return results.data;
        });
    };

    /*
     * GCM --------------------------------------------------------------------------------------------------------------
     */
    obj.getGcms = function () {
        return $http.get(serviceBase + 'gcms', config);
    };

    obj.sendNotifications = function (body) {
        return $http.post(serviceBase + 'sendNotif', body, config).then(function (results) {
            return results.data;
        });
    };

    obj.getGcmCount = function (q) {
        return $http.get(serviceBase + 'getGcmCount?q=' + q);
    };

    obj.getGcmByPage = function (page, limit, q) {
        return $http.get(serviceBase + 'getGcmByPage?page=' + page + '&limit=' + limit + '&q=' + q, config);
    };

    /*
     * FILE -------------------------------------------------------------------------------------------------------------
     */

    obj.deleteFile = function (dir, name) {
        var data = {dir: dir, name: name};
        return $http.post(serviceBase + 'deleteFile', data, config).then(function (results) {
            return results.data;
        });
    };

    obj.uploadFileToUrl = function (f, dir, name, oldname) {
        var fd = new FormData();
        fd.append("file", f);
        fd.append("target_dir", dir);
        fd.append("file_name", name);
        fd.append("old_name", oldname);
        var request = {
            method: 'POST',
            url: 'app/uploader/uploader.php',
            data: fd,
            headers: {'Content-Type': undefined}
        };

        // SEND THE FILES.
        return $http(request).then(function (resp) {
            //console.log(JSON.stringify(resp.data));
            return resp.data;
        });
    };

    obj.getBase64 = function (f) {
        return $http.post(serviceBase + 'getBase64', f).then(function (results) {
            //console.log(JSON.stringify(results.data));
            return results.data;
        });
    };

    return obj;
});
