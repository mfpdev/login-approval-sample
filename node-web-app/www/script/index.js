/**
 *    © Copyright 2016 IBM Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
                            switch (data.event) {
                                case "approve":
                                    webUserLoginChallengeHandler.submitChallengeAnswer({});
                                    break;
                                case "revoke":
                                    webUserLoginChallengeHandler.cancel();
                                    setTimeout(function () {
                                        location.reload();
                                    }, 500);
                                    break;
                            }  
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
