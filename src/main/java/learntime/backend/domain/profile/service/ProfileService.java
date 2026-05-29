package learntime.backend.domain.profile.service;

import learntime.backend.domain.badge.repository.UserBadgeRepository;
import learntime.backend.domain.community.converter.PostConverter;
import learntime.backend.domain.community.dto.response.PostListResponseDTO;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.repository.CommentRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.relationship.model.FriendRequest;
import learntime.backend.domain.relationship.repository.FriendRepository;
import learntime.backend.domain.relationship.repository.FriendRequestRepository;
import learntime.backend.domain.point.enums.PointMilestone;
import learntime.backend.domain.profile.converter.ProfileConverter;
import learntime.backend.domain.profile.dto.request.ProfileUpdateRequestDTO;
import learntime.backend.domain.profile.dto.response.ProfileResponseDTO;
import learntime.backend.domain.profile.enums.ProfileVisibility;
import learntime.backend.domain.profile.model.Profile;
import learntime.backend.domain.profile.repository.ProfileRepository;
import learntime.backend.domain.user.converter.UserConverter;

import learntime.backend.domain.user.dto.response.UserBadgeResponseDTO;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import learntime.backend.global.infra.s3.S3Service;
import learntime.backend.global.utils.FileValidatorUtil;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final S3Service s3Service;
    private final FileValidatorUtil fileValidatorUtil;

    @Transactional(readOnly = true)
    public ProfileResponseDTO getProfile(Long targetUserId, Long currentUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        Profile profile = profileRepository.findByUser_UserId(targetUserId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 비공개 프로필 검증 (본인이 아니면 접근 불가)
        if (profile.getProfileVisibility() == ProfileVisibility.PRIVATE && !targetUserId.equals(currentUserId)) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 친구 수
        Long friendCount = friendRepository.countFriendsByUserId(targetUserId);

        // 뱃지 목록 (전체)
        List<UserBadgeResponseDTO> badges = userBadgeRepository.findAllByUserId(targetUserId).stream()
                .map(UserConverter::toUserBadgeResponseDTO)
                .toList();

        // 최신 게시글 5개
        List<Post> recentPosts = postRepository.findRecentPostsByUserId(targetUserId, PageRequest.of(0, 5));
        
        // 댓글 수 매핑을 위한 리스트
        List<Long> postIds = recentPosts.stream().map(Post::getPostId).toList();
        Map<Long, Long> commentCounts = commentRepository.countCommentsByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));

        List<PostListResponseDTO> recentPostDTOs = recentPosts.stream()
                .map(post -> PostConverter.toPostListResponseDTO(post, commentCounts.getOrDefault(post.getPostId(), 0L)))
                .toList();

        PointMilestone tier = PointMilestone.getTier(targetUser.getPoint());

        Boolean isFriend = (currentUserId != null) && friendRepository.existsFriendRelation(targetUserId, currentUserId);

        boolean hasPendingSentRequest = false;
        boolean hasPendingReceivedRequest = false;
        Long pendingFriendRequestId = null;

        if (currentUserId != null) {
            java.util.Optional<FriendRequest> pendingRequest = friendRequestRepository.findPendingRequest(currentUserId, targetUserId);
            if (pendingRequest.isPresent()) {
                FriendRequest request = pendingRequest.get();
                pendingFriendRequestId = request.getFriendRequestId();
                if (request.getRequester().getUserId().equals(currentUserId)) {
                    hasPendingSentRequest = true;
                } else {
                    hasPendingReceivedRequest = true;
                }
            }
        }

        return ProfileConverter.toProfileResponseDTO(
                targetUser,
                profile,
                tier.getTierName(),
                friendCount,
                isFriend,
                hasPendingSentRequest,
                hasPendingReceivedRequest,
                pendingFriendRequestId,
                badges,
                recentPostDTOs
        );
    }

    @Transactional
    public void updateProfile(Long currentUserId, ProfileUpdateRequestDTO request, MultipartFile image) {
        Profile profile = profileRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        String newImageUrl = profile.getProfileImageUrl();

        if (Boolean.TRUE.equals(request.isImageDeleted())) {
            if (newImageUrl != null) {
                s3Service.deleteFile(newImageUrl);
                newImageUrl = null;
            }
            profile.clearProfileImage();
        } else if (image != null && !image.isEmpty()) {
            fileValidatorUtil.validateImage(image);
            if (newImageUrl != null) {
                s3Service.deleteFile(newImageUrl);
            }
            newImageUrl = s3Service.uploadFile(image, "profiles");
        }

        profile.updateProfile(request.description(), request.profileVisibility(), newImageUrl);
    }
}
