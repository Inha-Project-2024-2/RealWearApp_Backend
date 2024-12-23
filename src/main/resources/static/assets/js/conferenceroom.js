/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var ws = new WebSocket("wss://" + location.host + "/groupcall");
var participants = {};
var name;

window.onbeforeunload = function () {
  ws.close();
};

ws.onmessage = function (message) {
  var parsedMessage = JSON.parse(message.data);
  console.info("Received message: " + message.data);

  switch (parsedMessage.id) {
    case "existingParticipants":
      onExistingParticipants(parsedMessage);
      break;
    case "newParticipantArrived":
      onNewParticipant(parsedMessage);
      break;
    case "participantLeft":
      onParticipantLeft(parsedMessage);
      break;
    case "receiveVideoAnswer":
      receiveVideoResponse(parsedMessage);
      break;
    case "iceCandidate":
      participants[parsedMessage.name].rtcPeer.addIceCandidate(
        parsedMessage.candidate,
        function (error) {
          if (error) {
            console.error("Error adding candidate: " + error);
            return;
          }
        }
      );
      break;
    default:
      console.error("Unrecognized message", parsedMessage);
  }
};

function register() {
  name = document.getElementById("name").value;
  var room = document.getElementById("roomName").value;

  const container = document.querySelector(".container");
  const children = container.children;

  // 현재 화면 페이드아웃
  for (let i = 0; i < children.length; i++) {
    if (children[i].id !== "room") {
      children[i].classList.add('fade-transition', 'fade-out');
    }
  }

  setTimeout(() => {
    // 기존 요소들 숨기기
    for (let i = 0; i < children.length; i++) {
      if (children[i].id !== "room") {
        children[i].style.display = "none";
        children[i].classList.add('hidden');
      }
    }

    document.getElementById("room-header").innerText = "ROOM : " + room;
    const roomElement = document.getElementById("room");
    roomElement.style.display = "block";
    roomElement.classList.add('fade-in');

    // 배경색 변경 애니메이션
    changeBackground();

    var message = {
      method: "joinRoom",
      id: 100,
      params: {
        dataChannel: true,
        user: name,
        room: room,
        device: "web",
      },
    };
    sendMessage(message);
  }, 800);
}

function onNewParticipant(request) {
  receiveVideo(request.name);
}

function receiveVideoResponse(result) {
  participants[result.name].rtcPeer.processAnswer(
    result.sdpAnswer,
    function (error) {
      if (error) return console.error(error);
    }
  );
}

function callResponse(message) {
  if (message.response != "accepted") {
    console.info("Call not accepted by peer. Closing call");
    stop();
  } else {
    webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
      if (error) return console.error(error);
    });
  }
}

let isBlack = true;

// function changeColor() {
//   const htmlElement = document.documentElement;
//   if (isBlack) {
//     htmlElement.style.color = '#0ca3d2';
//     htmlElement.style.backgroundColor = '#0ca3d2';
//   } else {
//     htmlElement.style.color = 'black';
//     htmlElement.style.backgroundColor = 'black';
//   }
//   isBlack = !isBlack;
// }

function changeBackground() {
  document.body.classList.add('animate-bg');
}

function onExistingParticipants(msg) {
  var constraints = {
    audio: true,
    video: {
      mandatory: {
        maxWidth: 320,
        maxFrameRate: 15,
        minFrameRate: 15,
      },
    },
  };
  console.log(name + " registered in room " + room);
  var participant = new Participant(name);
  participants[name] = participant;
  var video = participant.getVideoElement();

  var options = {
    localVideo: video,
    mediaConstraints: constraints,
    onicecandidate: participant.onIceCandidate.bind(participant),
  };
  participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(
    options,
    function (error) {
      if (error) {
        return console.error(error);
      }
      this.generateOffer(participant.offerToReceiveVideo.bind(participant));
    }
  );

  msg.data.forEach(receiveVideo);
}

function leaveRoom() {
  const roomElement = document.getElementById("room");
  roomElement.style.animation = "leaveRoom 0.8s ease-in-out forwards";

  sendMessage({
    method: "leaveRoom",
    id: "103",
    params: {},
  });



  for (var key in participants) {
    participants[key].dispose();
  }

  setTimeout(() => {
    // room 숨기기
    roomElement.style.display = "none";

    // 컨테이너와 그 자식 요소들 가져오기
    const container = document.querySelector(".container");
    const children = container.children;

    // 배경 애니메이션 제거
    document.body.classList.remove('animate-bg');

    // 모든 원래 컨텐츠를 보이게 하되 처음에는 투명하게
    for (let i = 0; i < children.length; i++) {
      if (children[i].id !== "room") {
        children[i].style.display = "";
        children[i].classList.remove('hidden');
        children[i].classList.add('fade-transition', 'fade-out');
      }
    }

    // viewport와 wrapper 표시
    const viewport = document.getElementById("viewport");
    const wrapper = document.querySelector(".l-wrapper");
    viewport.style.display = "block";
    wrapper.style.display = "block";

    // 잠시 후 페이드인 애니메이션 시작
    setTimeout(() => {
      for (let i = 0; i < children.length; i++) {
        if (children[i].id !== "room") {
          children[i].classList.remove('fade-out');
        }
      }

      // 입력 필드 초기화
      document.getElementById("roomName").value = "";
      document.getElementById("name").value = "";
    }, 100);

  }, 700); // room 퇴장 애니메이션이 거의 완료된 시점

  ws.close();
}

function receiveVideo(sender) {
  var participant = new Participant(sender);
  participants[sender] = participant;
  var video = participant.getVideoElement();

  var options = {
    remoteVideo: video,
    onicecandidate: participant.onIceCandidate.bind(participant),
  };

  participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(
    options,
    function (error) {
      if (error) {
        return console.error(error);
      }
      this.generateOffer(participant.offerToReceiveVideo.bind(participant));
    }
  );
}



function onParticipantLeft(request) {
  console.log("Participant " + request.name + " left");
  var participant = participants[request.name];
  participant.dispose();
  delete participants[request.name];
}

function sendMessage(message) {
  var jsonMessage = JSON.stringify(message);
  console.log("Sending message: " + jsonMessage);
  ws.send(jsonMessage);
}
