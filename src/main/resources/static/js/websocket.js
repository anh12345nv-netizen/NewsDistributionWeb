// js/websocket.js
// CDN dependencies (thêm vào HTML trước khi include file này):
// <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
// <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>

let stompClient = null;

function connectWebSocket(onOrderUpdate, onSyncStatus) {
  const socket = new SockJS('/ws');
  stompClient = new StompJs.Client({ 
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  });

  stompClient.onConnect = () => {
    console.log('WebSocket connected');

    stompClient.subscribe('/topic/orders', (msg) => {
      const data = JSON.parse(msg.body);
      if (onOrderUpdate) onOrderUpdate(data);
    });

    stompClient.subscribe('/topic/sync', (msg) => {
      const data = JSON.parse(msg.body);
      if (onSyncStatus) onSyncStatus(data);
    });
  };

  stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
  };

  stompClient.activate();
}

function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate();
    console.log('WebSocket disconnected');
  }
}
