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

const PARTICIPANT_MAIN_CLASS = "participant main";
const PARTICIPANT_CLASS = "participant";

/**
 * Creates a video element for a new participant
 *
 * @param {String} name - the name of the new participant, to be used as tag
 *                        name of the video element.
 *                        The tag of the new element will be 'video<name>'
 * @return
 */



const style = document.createElement('style');
style.textContent = `
.participants-list {
    position: fixed;
    bottom: 20px;
    left: 20px;
    background: rgba(0, 0, 0, 0.7);
    padding: 15px;
    border-radius: 8px;
    color: white;
    z-index: 1000;
    min-width: 200px;
}

.participants-list h3 {
    margin: 0 0 10px 0;
    font-size: 16px;
    color: #0ca3d2;
}

.participants-list ul {
    list-style: none;
    margin: 0;
    padding: 0;
}

.participants-list li {
    padding: 5px 0;
    display: flex;
    align-items: center;
}

.participants-list .participant-status {
    width: 8px;
    height: 8px;
    background: #4CAF50;
    border-radius: 50%;
    margin-right: 8px;
    display: inline-block;
}

.participant-count {
    font-size: 14px;
    color: #888;
    margin-bottom: 8px;
}
`;

document.head.appendChild(style);

// 참가자 목록을 표시할 div 생성 함수
function createParticipantsList() {
  const participantsList = document.createElement('div');
  participantsList.className = 'participants-list';
  participantsList.innerHTML = `
        <h3>Participants</h3>
        <div class="participant-count">Active: 0</div>
        <ul></ul>
    `;
  document.body.appendChild(participantsList);
  return participantsList;
}

function Participant(name) {
  this.name = name;

  // 참가자 목록이 없으면 생성
  if (!document.querySelector('.participants-list')) {
    createParticipantsList();
  }

  var container = document.createElement("div");
  container.className = isPresentMainParticipant()
    ? PARTICIPANT_CLASS
    : PARTICIPANT_MAIN_CLASS;
  container.id = name;
  var span = document.createElement("span");
  var video = document.createElement("video");
  var rtcPeer;

  container.appendChild(video);
  container.appendChild(span);
  container.onclick = switchContainerClass;
  document.getElementById("participants").appendChild(container);

  span.appendChild(document.createTextNode(name));

  video.id = "video-" + name;
  video.autoplay = true;
  video.controls = false;

  // 참가자 목록에 추가
  const participantsList = document.querySelector('.participants-list ul');
  const participantItem = document.createElement('li');
  participantItem.innerHTML = `
        <span class="participant-status"></span>
        ${name}
    `;
  participantsList.appendChild(participantItem);

  // 참가자 수 업데이트
  updateParticipantCount();


  this.getElement = function () {
    return container;
  };

  this.getVideoElement = function () {
    return video;
  };

  function switchContainerClass() {
    if (container.className === PARTICIPANT_CLASS) {
      var elements = Array.prototype.slice.call(
        document.getElementsByClassName(PARTICIPANT_MAIN_CLASS)
      );
      elements.forEach(function (item) {
        item.className = PARTICIPANT_CLASS;
      });

      container.className = PARTICIPANT_MAIN_CLASS;
    } else {
      container.className = PARTICIPANT_CLASS;
    }
  }

  function isPresentMainParticipant() {
    return document.getElementsByClassName(PARTICIPANT_MAIN_CLASS).length != 0;
  }

  this.offerToReceiveVideo = function (error, offerSdp, wp) {
    if (error) return console.error("sdp offer error");
    console.log("Invoking SDP offer callback function");
    var msg = {
      method: "receiveVideoFrom",
      id: 101,
      params: {
        sender: name,
        sdpOffer: offerSdp,
      },
    };
    sendMessage(msg);
  };

  this.onIceCandidate = function (candidate, wp) {
    console.log("Local candidate" + JSON.stringify(candidate));

    var message = {
      method: "onIceCandidate",
      id: 102,
      params: {
        candidate: candidate,
        name: name,
      },
    };
    sendMessage(message);
  };

  Object.defineProperty(this, "rtcPeer", { writable: true });

  this.dispose = function () {
    console.log("Disposing participant " + this.name);
    this.rtcPeer.dispose();
    container.parentNode.removeChild(container);

    // 참가자 목록에서도 제거
    const participantItem = Array.from(participantsList.children)
        .find(item => item.textContent.trim() === name);
    if (participantItem) {
      participantItem.remove();
    }

    // 참가자 수 업데이트
    updateParticipantCount();

  };

}

function updateParticipantCount() {
  const participantCount = document.querySelector('.participant-count');
  const count = document.querySelector('.participants-list ul').children.length;
  participantCount.textContent = `Active: ${count}`;
}
