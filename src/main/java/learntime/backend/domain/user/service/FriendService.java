package learntime.backend.domain.user.service;

import learntime.backend.domain.user.converter.FriendConverter;
import learntime.backend.domain.user.dto.response.FriendRequestResponseDTO;
import learntime.backend.domain.user.dto.response.FriendResponseDTO;
import learntime.backend.domain.user.enums.FriendRequestStatus;
import learntime.backend.domain.user.error.code.FriendErrorCode;
import learntime.backend.domain.user.error.exception.FriendException;
import learntime.backend.domain.user.event.FriendRequestAcceptedEvent;
import learntime.backend.domain.user.event.FriendRequestRejectedEvent;
import learntime.backend.domain.user.event.FriendRequestSentEvent;
import learntime.backend.domain.user.model.Friend;
import learntime.backend.domain.user.model.FriendRequest;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.FriendRepository;
import learntime.backend.domain.user.repository.FriendRequestRepository;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
// 친구 요청, 승인/거절, 친구 관계 조회와 삭제를 담당하는 Service
public class FriendService {

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 친구 요청 보내기
     */
    @Transactional
    public Long sendFriendRequest(Long requesterId, Long receiverId) {
        // 본인에게 친구 요청 불가
        if (requesterId.equals(receiverId)) {
            throw new FriendException(FriendErrorCode.CANNOT_REQUEST_SELF);
        }

        User requester = getUser(requesterId);
        User receiver = getUser(receiverId);

        // 이미 친구인지 확인
        if (friendRepository.existsFriendship(requesterId, receiverId)) {
            throw new FriendException(FriendErrorCode.FRIEND_ALREADY_EXISTS);
        }

        // 대기 중인 친구 요청(양방향 중 하나라도)이 이미 존재하는지 확인
        boolean hasPendingRequest = friendRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatus(
                requesterId,
                receiverId,
                FriendRequestStatus.PENDING
        ) || friendRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatus(
                receiverId,
                requesterId,
                FriendRequestStatus.PENDING
        );

        if (hasPendingRequest) {
            log.warn("[검증 실패] 이미 대기 중인 친구 요청이 존재합니다. requester={}, receiver={}", requesterId, receiverId);
            throw new FriendException(FriendErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();

        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);

        log.info("[친구 요청 성공] requestId={}, requester={}, receiver={}", savedRequest.getFriendRequestId(), requesterId, receiverId);
        // 알림 생성
        eventPublisher.publishEvent(new FriendRequestSentEvent(
                savedRequest.getFriendRequestId(),
                requester.getUserId(),
                requester.getName(),
                receiver.getUserId()
        ));

        return savedRequest.getFriendRequestId();
    }

    /**
     * 친구 요청 수락
     */
    @Transactional
    public Long acceptFriendRequest(Long receiverId, Long friendRequestId) {
        FriendRequest friendRequest = getPendingRequestForReceiver(receiverId, friendRequestId);

        Long requesterId = friendRequest.getRequester().getUserId();
        // 이미 친구 관계가 맺어진 경우, 상태만 수락으로 변경하고 예외 발생
        if (friendRepository.existsFriendship(requesterId, receiverId)) {
            friendRequest.accept();
            throw new FriendException(FriendErrorCode.FRIEND_ALREADY_EXISTS);
        }

        Friend friend = Friend.builder()
                .user(friendRequest.getRequester())
                .friendUser(friendRequest.getReceiver())
                .build();

        // 요청 상태를 ACCEPTED로 변경
        friendRequest.accept();
        Friend savedFriend = friendRepository.save(friend);

        log.info("[친구 수락 성공] friendId={}, requester={}, receiver={}", savedFriend.getFriendId(), requesterId, receiverId);
        // 알림 생성
        eventPublisher.publishEvent(new FriendRequestAcceptedEvent(
                friendRequest.getFriendRequestId(),
                friendRequest.getRequester().getUserId(),
                friendRequest.getReceiver().getUserId(),
                friendRequest.getReceiver().getName()
        ));

        return savedFriend.getFriendId();
    }

    /**
     * 친구 요청 거절
     */
    @Transactional
    public void rejectFriendRequest(Long receiverId, Long friendRequestId) {
        FriendRequest friendRequest = getPendingRequestForReceiver(receiverId, friendRequestId);
        // 요청 상태를 REJECTED로 변경
        friendRequest.reject();

        log.info("[친구 거절 성공] requestId={}, receiver={}", friendRequestId, receiverId);
        // 알림 생성
        eventPublisher.publishEvent(new FriendRequestRejectedEvent(
                friendRequest.getFriendRequestId(),
                friendRequest.getRequester().getUserId(),
                friendRequest.getReceiver().getUserId(),
                friendRequest.getReceiver().getName()
        ));
    }

    /**
     * 사용자의 전체 친구 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FriendResponseDTO> getFriends(Long userId) {
        return friendRepository.findAllByUserId(userId).stream()
                .map(friend -> FriendConverter.toFriendResponseDTO(friend, userId))
                .toList();
    }

    /**
     * 사용자가 받은 대기 중(PENDING)인 친구 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FriendRequestResponseDTO> getReceivedPendingRequests(Long receiverId) {
        return friendRequestRepository.findAllByReceiver_UserIdAndStatusOrderByCreatedAtDesc(
                        receiverId,
                        FriendRequestStatus.PENDING
                ).stream()
                .map(FriendConverter::toFriendRequestResponseDTO)
                .toList();
    }

    /**
     * 사용자가 보낸 대기 중(PENDING)인 친구 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FriendRequestResponseDTO> getSentPendingRequests(Long requesterId) {
        return friendRequestRepository.findAllByRequester_UserIdAndStatusOrderByCreatedAtDesc(
                        requesterId,
                        FriendRequestStatus.PENDING
                ).stream()
                .map(FriendConverter::toFriendRequestResponseDTO)
                .toList();
    }

    /**
     * 친구 삭제 (관계 끊기)
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendUserId) {
        Friend friend = friendRepository.findFriendship(userId, friendUserId)
                .orElseThrow(() -> new FriendException(FriendErrorCode.FRIEND_NOT_FOUND));
        log.info("[친구 삭제 성공] userId={}, friendUserId={}", userId, friendUserId);
        friendRepository.delete(friend);
    }

    /**
     * 특정 수신자가 받은 대기 중인 친구 요청 단건 조회
     */
    private FriendRequest getPendingRequestForReceiver(Long receiverId, Long friendRequestId) {
        return friendRequestRepository.findByFriendRequestIdAndReceiver_UserIdAndStatus(
                        friendRequestId,
                        receiverId,
                        FriendRequestStatus.PENDING
                )
                .orElseThrow(() -> {
                    log.warn("[검증 실패] 대기 중인 친구 요청을 찾을 수 없습니다. requestId={}, receiver={}", friendRequestId, receiverId);
                    return new FriendException(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND);
                });
    }

    /**
     * 사용자 ID로 User 엔티티 조회
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
    }
}
