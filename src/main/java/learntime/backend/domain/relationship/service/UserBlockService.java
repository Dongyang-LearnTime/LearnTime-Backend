package learntime.backend.domain.relationship.service;

import learntime.backend.domain.relationship.converter.UserBlockConverter;
import learntime.backend.domain.relationship.error.code.RelationShipCode;
import learntime.backend.domain.relationship.error.exception.RelationShipException;
import learntime.backend.domain.relationship.repository.FriendRepository;
import learntime.backend.domain.relationship.repository.UserBlockRepository;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBlockService {

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final UserBlockRepository userBlockRepository;

    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new RelationShipException(RelationShipCode.USER_TO_BLOCK_NOT_FOUND));

        // 본인 차단 방지
        if (blockerId.equals(blockedId)) {
            throw new RelationShipException(RelationShipCode.CANNOT_BLOCK_SELF);
        }

        // 친구 차단 방지
        if (friendRepository.existsFriendship(blockerId, blockedId)) {
            throw new RelationShipException(RelationShipCode.FRIEND_CANNOT_BE_BLOCKED);
        }

        // 중복 차단 방지
        if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw new RelationShipException(RelationShipCode.USER_ALREADY_BLOCKED);
        }

        userBlockRepository.save(
                UserBlockConverter.toUserBlock(blocker, blocked)
        );

    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        if (!userRepository.existsById(blockedId)) {
            throw new RelationShipException(RelationShipCode.USER_TO_UNBLOCK_NOT_FOUND);
        }

        userBlockRepository.deleteByBlocker_UserIdAndBlocked_UserId(
                blockerId,
                blockedId
        );
    }


}
