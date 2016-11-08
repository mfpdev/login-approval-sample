var socket = io();

var wlInitOptions = {
    mfpContextRoot: '/mfp',
    applicationId: 'com.github.mfpdev.loginapprovals'
};

var clientId = null;
var geocoder = new google.maps.Geocoder();


var WebUserLoginChallengeHandler = WL.Client.createSecurityCheckChallengeHandler("WebUserLogin");
var UserLoginChallengeHandler = WL.Client.createSecurityCheckChallengeHandler("UserLogin");

UserLoginChallengeHandler.handleChallenge = function (challenge) {
     UserLoginChallengeHandler.submitChallengeAnswer({"username" : "a", "password" : "a"});
};

WebUserLoginChallengeHandler.handleChallenge = function (challenge) {
    if (challenge.waitingForApproval) {
        alert(1);
    } 
};

function sendWebData() {
    var resourceRequest = new WLResourceRequest("/adapters/LoginApprovalsAdapter/webClientData", WLResourceRequest.POST);
    resourceRequest.setQueryParameter("date", new Date().toString());
    resourceRequest.setQueryParameter("agent", navigator.userAgent);

    var location = "not available"
    // Find the current location    
    
    navigator.geolocation.getCurrentPosition(function (position) {
        // Set location to be latitude + longitude
        location = position.coords.latitude + ":" + position.coords.longitude
        if (position.coords) {
            var latlng = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
            geocoder.geocode({ 'latLng': latlng }, function (results, status) {
                if (status == google.maps.GeocoderStatus.OK) {
                    console.log(results)
                    if (results[1]) {
                        // Set location to be formatted_address
                        location = results[0].formatted_address;
                    }
                }
                //Set the formatted_address as location 
                resourceRequest.setQueryParameter("location", location);
                
                resourceRequest.send().then(
                    function (response) {
                        clientId = response.responseJSON["clientId"];
                        console.log("Client ID: " + clientId);
                        
                        //Start listen to socket
                        socket.on(clientId, function (data) {
                            if (data.refresh) {
                                alert(1);
                            }
                        });
                    },
                    function (error) {
                        alert(JSON.stringify(error));
                    }
                );
            });
        }
    });
}

function getWebUser() {
    var resourceRequest = new WLResourceRequest("/adapters/LoginApprovalsAdapter/user", WLResourceRequest.GET);
    resourceRequest.send().then(
        function (response) {
            alert(JSON.stringify(response));
        },
        function (error) {
            alert(JSON.stringify(error));
        }
    );
}

WL.Client.init(wlInitOptions).then(
    function () {
        sendWebData();
    }, function (error) {
        alert(error);
    }
);
