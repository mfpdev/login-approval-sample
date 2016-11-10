var socket = io();

var wlInitOptions = {
    mfpContextRoot: '/mfp',
    applicationId: 'com.github.mfpdev.loginapprovals'
};

var clientId = null;
var webUserLoginChallengeHandler = WL.Client.createSecurityCheckChallengeHandler("WebUserLogin");
var userLoginChallengeHandler = WL.Client.createSecurityCheckChallengeHandler("UserLogin");


userLoginChallengeHandler.handleChallenge = function (challenge) {
    showDiv("content", false);
    showDiv("login", true);
    showDiv("waitingForApproval", false);
};

webUserLoginChallengeHandler.handleChallenge = function (challenge) {
    showDiv("waitingForApproval", true);
    showDiv("login", false);
};

var onLoginClicked = function () {
    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    userLoginChallengeHandler.submitChallengeAnswer({ "username": username, "password": password });
} 


function showDiv(id, show) {
    document.getElementById(id).style.display = show ? "block" : "none";
}


function sendWebData(callback) {
    console.log("Sending web data to server");
    var resourceRequest = new WLResourceRequest("/adapters/LoginApprovalsAdapter/webClientData", WLResourceRequest.POST);
    resourceRequest.setQueryParameter("date", new Date().toString());
    navigator.geolocation.getCurrentPosition(function (position) {
        // Set location to be latitude + longitude
        console.log("Getting client location");
        if (position.coords) {
            console.log("Success getting client location");
            resourceRequest.setQueryParameter("locationDescription", location);
            resourceRequest.setQueryParameter("latitude", position.coords.latitude);
            resourceRequest.setQueryParameter("longitude", position.coords.longitude);
            resourceRequest.send().then(
                function (response) {
                    clientId = response.responseJSON["clientId"];
                    console.log("Success getting client ID: " + clientId);

                    callback();
                    //Start listen to socket
                    socket.on(clientId, function (data) {
                        if (data.refresh) {
                            webUserLoginChallengeHandler.submitChallengeAnswer({})
                        }
                    });
                },
                function (error) {
                    alert(JSON.stringify(error));
                }
            );
        }
    });
}

function getWebUser() {
    var resourceRequest = new WLResourceRequest("/adapters/LoginApprovalsAdapter/user", WLResourceRequest.GET);
    resourceRequest.send().then(
        function (response) {
            showDiv("waitingForApproval", false);
            showDiv("content", true);
            document.getElementById("helloApprovedUser").innerText = "Hello " + response.responseJSON.displayName;
        },
        function (error) {
            showDiv("waitingForApproval", false);
            document.getElementById("helloApprovedUser").innerText = "Something went wrong...";
        }
    );
}

WL.Client.init(wlInitOptions).then(
    function () {
        sendWebData(getWebUser);
    }, function (error) {
        alert(error);
    }
);
