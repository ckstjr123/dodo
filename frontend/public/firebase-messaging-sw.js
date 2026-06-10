importScripts('https://www.gstatic.com/firebasejs/12.12.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/12.12.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyCYgwCkA0If2TmI0hVx_UZJy-exzem0YoQ",
  authDomain: "fcm-push-294a4.firebaseapp.com",
  projectId: "fcm-push-294a4",
  storageBucket: "fcm-push-294a4.firebasestorage.app",
  messagingSenderId: "749663107311",
  appId: "1:749663107311:web:f6c06df65881c6d78e431a",
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw.js] 백그라운드 메시지 수신:', payload);
  self.registration.showNotification(payload.notification.title, {
    body: payload.notification.body,
    icon: payload.notification.icon,
  });
});