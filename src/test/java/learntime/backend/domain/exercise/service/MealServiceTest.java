package learntime.backend.domain.exercise.service;

import learntime.backend.domain.exercise.dto.request.MealRequestDTO;
import learntime.backend.domain.exercise.dto.response.MealResponseDTO;
import learntime.backend.domain.user.model.User;
import learntime.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MealServiceTest {

    @Autowired
    private MealService mealService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveMeal() {
        // given
        User user = User.builder()
                .email("meal_test@test.com")
                .name("식단테스트")
                .password("password")
                .build();
        userRepository.save(user);

        MealRequestDTO request = new MealRequestDTO("닭가슴살 100g");

        // when
        try {
            MealResponseDTO response = mealService.saveMeal(user.getUserId(), request);
            System.out.println("====== SUCCESS ======");
            System.out.println("response = " + response);
            assertThat(response).isNotNull();
        } catch (Exception e) {
            System.out.println("====== ERROR ======");
            e.printStackTrace();
            throw e;
        }
    }
}
