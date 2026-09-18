# 스터디 토론방 및 삭제 정책

Java 패키지: `domain/study_forum`. 별도 방 테이블 없이 스터디당 하나의 텍스트 대화 공간을 제공한다.
첨부파일, 수정, 답글, 읽음 표시, 알림, WebSocket은 포함하지 않는다.

## API

JWT 인증 필요. 작성자 ID는 인증 정보에서 결정한다.

| 메서드 | 경로 | 응답 |
|---|---|---|
| POST | /api/study/{studyId}/forum/messages | 201, 생성된 메시지 |
| GET | /api/study/{studyId}/forum/messages?beforeId=123&size=30 | 200, CursorResponse |
| DELETE | /api/study/{studyId}/forum/messages/{messageId} | 204 |

POST 본문: `{"content":"오늘 학습한 내용에 대한 의견입니다."}`
본문은 공백 불가, 최대 1,000자이며 앞뒤 공백을 제거한다.

GET은 messageId 내림차순. beforeId 미지정 시 최신 목록, 지정 시 해당 ID 미만을 반환한다.
size 기본 30, 허용 1~100. 응답은 content, nextCursor, hasNext.
메시지 필드는 messageId, authorStudyMemberId, authorUserId, authorName, content, createdAt.
양방향 사용자 차단을 SQL에서 필터링한 뒤 페이지를 구성한다.
프런트엔드는 본문을 텍스트로 렌더링하고, 토론 화면이 열려 있을 때 최신 페이지를 갱신한다.
이는 최신 페이지 조회이며 모든 신규 메시지의 실시간 전달을 보장하는 스트림 API는 아니다.

| 멤버 상태 | 조회 | 작성 | 본인 삭제 |
|---|---|---|---|
| ACTIVE | 가능 | 가능 | 가능 |
| COMPLETED | 가능 | 불가 | 가능 |
| WITHDRAWN / 비회원 | 불가 | 불가 | 불가 |

방장도 다른 사람의 메시지를 삭제할 수 없다. 공개 스터디도 토론은 멤버만 접근한다.
메시지 삭제는 hard delete. 작성 시 사용자 → 스터디 → 멤버 순서로 잠금을 얻고 상태를 검증한다.
계정 탈퇴는 사용자 잠금, 스터디 삭제는 스터디 잠금을 사용한다.

## 삭제 정책

- 계정 탈퇴: 대기 중 가입 요청 취소, ACTIVE/COMPLETED 방장 승계, 기존 개인 데이터 정리.
  쪽지는 탈퇴자 측 삭제 플래그를 설정하고 사용자 FK를 NULL 처리한다.
  토론 메시지도 작성 멤버 FK를 NULL 처리하며 내용은 보존한다.
- 회원 hard delete: 180일 경과 후 최대 100명씩 별도 트랜잭션으로 반복 처리.
  해당 회원의 퀴즈 답안/이력/문항/퀴즈, 필기/피드백/진도/멤버 콘텐츠, 스터디 멤버,
  초대/가입 요청 및 개인 테이블을 자식부터 정리한다.
  스터디 본체, 다른 회원의 자료, 상대방 쪽지함과 공유 게시글/댓글/토론 메시지는 보존한다.
  이때 필기는 계정의 최종 삭제 대상이다. 스터디 탈퇴만 한 사용자의 개인 자산 보존은 유지한다.
- 스터디 삭제: ACTIVE/COMPLETED 방장 허용. 토론 메시지를 먼저 삭제한다.
  기존 필기의 SET NULL 보존 정책은 변경하지 않았다.
- 쪽지: 양쪽 모두 삭제된 시각(completelyDeletedAt)부터 1개월 후 영구 삭제.
  읽음 여부는 조건에 포함하지 않는다. 한쪽만 삭제한 쪽지는 유지한다.
- 이미지: 부모 게시글 삭제와 독립적으로, 개별 soft delete 90일 경과 이미지 행을 정리한다.
  기존 이미지 삭제 이벤트를 발행하여 S3 정리 경로를 재사용한다.

## 기존 DB 변경

`docs/sql/20260914-study-forum.sql`을 기존 스키마에 **애플리케이션 쓰기를 중단한 상태에서 배포 전에 한 번** 적용한다.
이 파일은 자동 실행되지 않는다. 이미 적용한 DB에 재실행하지 않는다.
기존 FK 이름 변경은 필요 없지만, 먼저 SHOW CREATE TABLE로 실제 컬럼 타입과 제약을 확인한다.

변경: message 송수신 FK nullable, completely_deleted_at 추가, 정리 인덱스 3개,
study_forum_message 신규 테이블 및 study_member 복합 유니크 키. 기존 양쪽 삭제 쪽지는 정확한 삭제일을 알 수 없으므로
적용 시각부터 새 보존 기간을 부여한다. 과거 탈퇴자의 쪽지 참조와 가입 요청도 정리한다.
ddl-auto=update만으로 기존 행 보정과 토론방 복합 FK가 생성되지는 않는다.
신규 설치도 기본 JPA 스키마 생성 후 아래 복합 FK를 별도로 적용해야 한다.

토론 메시지는 Study와 StudyMember를 참조한다. study_id는 멤버를 통해서도 알 수 있지만
스터디별 조회·삭제와 복합 FK 검증에 사용한다. 잘못된 스터디의 멤버를 연결하면 DB에서 거부한다.
단일 study_member_id FK는 JPA에서도 생성하며, 배포 SQL에서 복합 FK를 추가해 같은 스터디인지도 보장한다.
복합 FK에는 필수 study_id가 포함되므로 ON DELETE SET NULL은 사용하지 않는다.
회원 hard delete는 멤버 삭제 전에 메시지의 study_member_id만 명시적으로 NULL 처리한다.

```sql
ALTER TABLE study_forum_message ADD CONSTRAINT fk_forum_member_study
FOREIGN KEY (study_id, study_member_id) REFERENCES study_member (study_id, study_member_id);
```

## 테스트

서비스 단위 테스트:
```powershell
.\gradlew.bat test --tests '*StudyForumServiceTest' --tests '*StudyManagementServiceTest'
```

MySQL 통합 테스트는 **폐기 가능한 전용 MySQL 스키마**에서 실행한다.
스키마를 생성/삭제하므로 기존 개발/운영 DB 주소를 사용하지 않는다.
TEST_MYSQL_URL이 없으면 해당 테스트는 건너뛴다. 외부 서비스와 스케줄러는 로드하지 않는다.

```powershell
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:33379/forum_test?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Seoul'
$env:TEST_MYSQL_USER='root'
$env:TEST_MYSQL_PASSWORD=''
.\gradlew.bat test --tests '*ForumDeletionMySqlTest' --rerun-tasks
```

로컬 검증은 별도 datadir과 포트 33379의 MySQL 8.0 인스턴스를 사용한다.

