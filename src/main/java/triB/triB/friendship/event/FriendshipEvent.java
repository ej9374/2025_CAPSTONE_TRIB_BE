package triB.triB.friendship.event;

import triB.triB.global.fcm.RequestType;

public record FriendshipEvent (
        RequestType requestType,
        Long requesterId,
        Long addresseeId
){
}
