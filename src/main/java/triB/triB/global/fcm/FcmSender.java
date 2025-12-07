package triB.triB.global.fcm;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Component;

@Component
public class FcmSender {

    public void sendPushNotification(FcmSendRequest fcmSendRequest) throws FirebaseMessagingException {
        Message message = Message.builder()
                .putData("type", fcmSendRequest.getRequestType().toString())
                .putData("id", fcmSendRequest.getId() != null ? fcmSendRequest.getId().toString() : "0")
                .setNotification(
                        Notification.builder()
                                .setTitle(fcmSendRequest.getTitle())
                                .setBody(fcmSendRequest.getContent())
                                .setImage(fcmSendRequest.getImage())
                                .build())
                .setToken(fcmSendRequest.getToken())
                .setAndroidConfig(AndroidConfig.builder()
                        .setCollapseKey(fcmSendRequest.getRequestType().toString())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setContentAvailable(true)
                                .setMutableContent(true)
                                .build())
                        .build())
                .build();
        FirebaseMessaging.getInstance().send(message);
    }
}
