package learntime.backend.domain.community.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import learntime.backend.domain.community.dto.request.PostCreateRequestDTO;
import learntime.backend.domain.community.dto.request.PostUpdateRequestDTO;
import learntime.backend.domain.community.enums.PostCategory;
import learntime.backend.domain.community.model.Post;
import learntime.backend.domain.community.repository.PostImageRepository;
import learntime.backend.domain.community.repository.PostLikeRepository;
import learntime.backend.domain.community.repository.PostRepository;
import learntime.backend.domain.study.error.code.StudyErrorCode;
import learntime.backend.domain.study.error.exception.StudyException;
import learntime.backend.domain.study.model.Study;
import learntime.backend.domain.study.repository.StudyRepository;
import learntime.backend.domain.study_member.enums.StudyMemberRole;
import learntime.backend.domain.study_member.enums.StudyMemberStatus;
import learntime.backend.domain.study_member.model.StudyMember;
import learntime.backend.domain.study_member.repository.StudyMemberRepository;
import learntime.backend.domain.study_progress.service.StudyQueryService;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import learntime.backend.global.infra.s3.S3Service;
import learntime.backend.global.utils.FileValidatorUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private StudyQueryService studyQueryService;

    @Mock
    private S3Service s3Service;

    @Mock
    private FileValidatorUtil fileValidatorUtil;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("게시글 생성 성공 - 공개 스터디 연결 (방장인 경우)")
    void createPost_PublicStudy_Success() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        StudyMember ownerMember = mock(StudyMember.class);
        given(ownerMember.getStudyMemberRole()).willReturn(StudyMemberRole.OWNER);
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(ownerMember));

        PostCreateRequestDTO request = new PostCreateRequestDTO(
                "게시글 제목",
                "게시글 내용",
                studyId,
                PostCategory.RECRUITMENT,
                false
        );

        Post savedPost = mock(Post.class);
        given(savedPost.getPostId()).willReturn(100L);
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        Long postId = postService.createPost(request, null, userId);

        // then
        assertThat(postId).isEqualTo(100L);
        verify(studyRepository).findById(studyId);
        verify(studyMemberRepository).findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 모집글인데 스터디 방장이 아닌 일반 멤버인 경우 예외 발생")
    void createPost_Recruitment_NotOwner_ThrowsException() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        StudyMember regularMember = mock(StudyMember.class);
        given(regularMember.getStudyMemberRole()).willReturn(StudyMemberRole.MEMBER);
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(regularMember));

        PostCreateRequestDTO request = new PostCreateRequestDTO(
                "모집글 제목",
                "모집글 내용",
                studyId,
                PostCategory.RECRUITMENT,
                false
        );

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, null, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_RECRUITMENT_OWNER_ONLY.getMessage());

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 모집글인데 해당 스터디 멤버가 아닌 경우 예외 발생")
    void createPost_Recruitment_NotMember_ThrowsException() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.empty());

        PostCreateRequestDTO request = new PostCreateRequestDTO(
                "모집글 제목",
                "모집글 내용",
                studyId,
                PostCategory.RECRUITMENT,
                false
        );

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, null, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_RECRUITMENT_OWNER_ONLY.getMessage());

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 성공 - 비모집글(일반글)에 스터디 연결 시 방장 검증 미수행")
    void createPost_GeneralCategory_NotOwner_Success() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(true);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        PostCreateRequestDTO request = new PostCreateRequestDTO(
                "일반글 제목",
                "일반글 내용",
                studyId,
                PostCategory.FREE,
                false
        );

        Post savedPost = mock(Post.class);
        given(savedPost.getPostId()).willReturn(100L);
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        Long postId = postService.createPost(request, null, userId);

        // then
        assertThat(postId).isEqualTo(100L);
        // 비모집글이므로 방장 검증을 하지 않음
        verify(studyMemberRepository, never()).findByStudy_StudyIdAndUser_UserIdAndStatus(anyLong(), anyLong(), any());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 비공개 스터디 연결 시도 시 예외 발생")
    void createPost_PrivateStudy_ThrowsException() {
        // given
        Long userId = 1L;
        Long studyId = 10L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(false);
        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        PostCreateRequestDTO request = new PostCreateRequestDTO(
                "게시글 제목",
                "게시글 내용",
                studyId,
                PostCategory.RECRUITMENT,
                false
        );

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, null, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_NOT_PUBLIC.getMessage());

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 모집글인데 스터디 ID가 없는 경우")
    void createPost_RecruitmentWithoutStudyId_ThrowsException() {
        // given
        Long userId = 1L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        PostCreateRequestDTO request = new PostCreateRequestDTO(
                "모집글 제목",
                "모집 내용",
                null,
                PostCategory.RECRUITMENT,
                false
        );

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, null, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 비공개 스터디로 변경 시도 시 예외 발생")
    void updatePost_PrivateStudy_ThrowsException() {
        // given
        Long userId = 1L;
        Long postId = 100L;
        Long privateStudyId = 20L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);

        Post post = mock(Post.class);
        given(post.getUser()).willReturn(user);
        given(postRepository.findByIdWithDetails(postId)).willReturn(Optional.of(post));

        Study study = mock(Study.class);
        given(study.getIsPublic()).willReturn(false);
        given(studyRepository.findById(privateStudyId)).willReturn(Optional.of(study));

        PostUpdateRequestDTO request = new PostUpdateRequestDTO(
                "수정된 제목",
                "수정된 내용",
                List.of(),
                privateStudyId,
                PostCategory.RECRUITMENT
        );

        // when & then
        assertThatThrownBy(() -> postService.updatePost(postId, request, null, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_NOT_PUBLIC.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 모집글의 스터디를 변경하는데 새 스터디의 방장이 아닌 경우 예외 발생")
    void updatePost_Recruitment_ChangeStudy_NotOwner_ThrowsException() {
        // given
        Long userId = 1L;
        Long postId = 100L;
        Long oldStudyId = 10L;
        Long newStudyId = 20L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);

        Post post = mock(Post.class);
        given(post.getUser()).willReturn(user);
        given(post.getStudyId()).willReturn(oldStudyId);
        given(post.getCategory()).willReturn(PostCategory.RECRUITMENT);
        given(postRepository.findByIdWithDetails(postId)).willReturn(Optional.of(post));

        Study newStudy = mock(Study.class);
        given(newStudy.getIsPublic()).willReturn(true);
        given(studyRepository.findById(newStudyId)).willReturn(Optional.of(newStudy));

        // 새 스터디의 방장이 아님 (일반 멤버)
        StudyMember regularMember = mock(StudyMember.class);
        given(regularMember.getStudyMemberRole()).willReturn(StudyMemberRole.MEMBER);
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(newStudyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(regularMember));

        PostUpdateRequestDTO request = new PostUpdateRequestDTO(
                "수정된 제목",
                "수정된 내용",
                List.of(),
                newStudyId,
                PostCategory.RECRUITMENT
        );

        // when & then
        assertThatThrownBy(() -> postService.updatePost(postId, request, null, userId))
                .isInstanceOf(StudyException.class)
                .hasMessageContaining(StudyErrorCode.STUDY_RECRUITMENT_OWNER_ONLY.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 성공 - 모집글의 스터디를 변경하는데 새 스터디의 방장인 경우 정상 수정")
    void updatePost_Recruitment_ChangeStudy_ActiveOwner_Success() {
        // given
        Long userId = 1L;
        Long postId = 100L;
        Long oldStudyId = 10L;
        Long newStudyId = 20L;

        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);

        Post post = mock(Post.class);
        given(post.getUser()).willReturn(user);
        given(post.getStudyId()).willReturn(oldStudyId);
        given(post.getCategory()).willReturn(PostCategory.RECRUITMENT);
        given(postRepository.findByIdWithDetails(postId)).willReturn(Optional.of(post));

        Study newStudy = mock(Study.class);
        given(newStudy.getIsPublic()).willReturn(true);
        given(studyRepository.findById(newStudyId)).willReturn(Optional.of(newStudy));

        // 새 스터디의 방장임
        StudyMember ownerMember = mock(StudyMember.class);
        given(ownerMember.getStudyMemberRole()).willReturn(StudyMemberRole.OWNER);
        given(studyMemberRepository.findByStudy_StudyIdAndUser_UserIdAndStatus(newStudyId, userId, StudyMemberStatus.ACTIVE))
                .willReturn(Optional.of(ownerMember));

        PostUpdateRequestDTO request = new PostUpdateRequestDTO(
                "수정된 제목",
                "수정된 내용",
                List.of(),
                newStudyId,
                PostCategory.RECRUITMENT
        );

        // when
        postService.updatePost(postId, request, null, userId);

        // then
        verify(studyMemberRepository).findByStudy_StudyIdAndUser_UserIdAndStatus(newStudyId, userId, StudyMemberStatus.ACTIVE);
        verify(post).updatePost(eq("수정된 제목"), eq("수정된 내용"), eq(newStudyId), any(), eq(PostCategory.RECRUITMENT));
    }
}
