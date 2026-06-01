package learntime.backend.domain.profile.service;

import learntime.backend.domain.profile.dto.request.ProfileUpdateRequestDTO;
import learntime.backend.domain.profile.model.Profile;
import learntime.backend.domain.profile.repository.ProfileRepository;
import learntime.backend.global.error.code.AuthErrorCode;
import learntime.backend.global.error.exception.AuthException;
import learntime.backend.global.infra.s3.event.ImageDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileStoreService {

    private final ProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public String getProfileImageUrl(Long userId) {
        return profileRepository.findByUser_UserId(userId)
                .map(Profile::getProfileImageUrl)
                .orElse(null);
    }

    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequestDTO request, String newImageUrl, boolean isImageChanged) {
        Profile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        String oldImageUrl = profile.getProfileImageUrl();

        if (Boolean.TRUE.equals(request.isImageDeleted())) {
            profile.clearProfileImage();
            if (oldImageUrl != null) {
                eventPublisher.publishEvent(new ImageDeletedEvent(oldImageUrl));
            }
        } else if (isImageChanged) {
            profile.updateProfile(request.description(), request.profileVisibility(), newImageUrl);
            if (oldImageUrl != null) {
                eventPublisher.publishEvent(new ImageDeletedEvent(oldImageUrl));
            }
        } else {
            // 이미지 변경이 없으면 설명과 공개 여부만 업데이트
            profile.updateProfile(request.description(), request.profileVisibility(), null);
        }
    }
}
