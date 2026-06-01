package learntime.backend.domain.user.enums;


public enum FriendRequestStatus {
    /** 상대방의 승인 또는 거절을 기다리는 상태 */
    PENDING,

    /** 상대방이 친구 요청을 승인한 상태 */
    ACCEPTED,

    /** 상대방이 친구 요청을 거절한 상태 */
    REJECTED,

    /** 요청자가 보낸 친구 요청을 직접 취소한 상태 */
    CANCELLED
}
