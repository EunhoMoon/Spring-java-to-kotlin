package com.group.libraryapp.service.user

import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanStatus.LOANED
import com.group.libraryapp.domain.user.loanhistory.UserLoanStatus.RETURNED
import com.group.libraryapp.dto.user.request.UserCreateRequest
import com.group.libraryapp.dto.user.request.UserUpdateRequest
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("유저")
class UserServiceTest @Autowired constructor(
  private val userRepository: UserRepository,
  private val userService: UserService,
  private val userLoanHistoryRepository: UserLoanHistoryRepository
) {

  @AfterEach
  fun clean() {
    userRepository.deleteAll()
  }

  @Test
  @DisplayName("저장이 정상 동작한다.")
  fun saveUserTest() {
    // given
    val request = UserCreateRequest("Janek", null)

    // when
    userService.saveUser(request)

    // then
    val results = userRepository.findAll()
    assertThat(results).hasSize(1)
    assertThat(results[0].name).isEqualTo("Janek")
    assertThat(results[0].age).isNull()
  }

  @Test
  @DisplayName("조회가 정상 동작한다.")
  fun getUsersTest() {
    // given
    userRepository.saveAll(
      listOf(
        User("A", 20),
        User("B", null)
      )
    )

    // when
    val results = userService.getUsers()

    // then
    assertThat(results).hasSize(2)
    assertThat(results).extracting("name").containsExactlyInAnyOrder("A", "B")
    assertThat(results).extracting("age").containsExactlyInAnyOrder(20, null)
  }

  @Test
  @DisplayName("수정이 정상 동작한다.")
  fun updateUserNameTest() {
    // given
    val savedUser = userRepository.save(User("A", null))
    val request = UserUpdateRequest(savedUser.id!!, "B")

    // when
    userService.updateUserName(request)

    //then
    val result = userRepository.findAll()[0]
    assertThat(result.name).isEqualTo("B")
  }

  @Test
  @DisplayName("삭제가 정상 동작한다.")
  fun deleteUserTest() {
    // given
    userRepository.save(User("A", null))

    // when
    userService.deleteUser("A")

    //then
    assertThat(userRepository.findAll()).isEmpty()
  }

  @Test
  @DisplayName("대출 기록이 없는 유저도 응답에 포함된다.")
  fun getUserLoanHistoriesTest() {
    // given
    userRepository.save(User("A", null))

    // when
    val results = userService.getUserLoanHistories()

    //then
    assertThat(results).hasSize(1)
    assertThat(results[0].name).isEqualTo("A")
    assertThat(results[0].books).isEmpty()
  }

  @Test
  @DisplayName("대출 기록이 많은 유저의 응답이 정상 동작한다.")
  fun getUserLoanHistoriesTest2() {
    // given
    val savedUser = userRepository.save(User("A", null))
    userLoanHistoryRepository.saveAll(
      listOf(
        UserLoanHistory.fixture(savedUser, "책1", LOANED),
        UserLoanHistory.fixture(savedUser, "책2", LOANED),
        UserLoanHistory.fixture(savedUser, "책3", RETURNED),
      )
    )

    // when
    val results = userService.getUserLoanHistories()

    //then
    assertThat(results).hasSize(1)
    assertThat(results[0].name).isEqualTo("A")
    assertThat(results[0].books).hasSize(3)
    assertThat(results[0].books).extracting("name").containsExactlyInAnyOrder("책1", "책2", "책3")
    assertThat(results[0].books).extracting("isReturn").containsExactlyInAnyOrder(false, false, true)
  }

  @Test
  @DisplayName("대출 기록이 없는 유저도 응답에 포함며, 기록이 많은 유저의 응답이 정상 동작한다.")
  fun getUserLoanHistoriesTest3() {
    // given
    val savedUsers = userRepository.saveAll(
      listOf(
        User("A", null),
        User("B", null)
      ))
    userLoanHistoryRepository.saveAll(
      listOf(
        UserLoanHistory.fixture(savedUsers[0], "책1", LOANED),
        UserLoanHistory.fixture(savedUsers[0], "책2", LOANED),
        UserLoanHistory.fixture(savedUsers[0], "책3", RETURNED),
      )
    )

    // when
    val results = userService.getUserLoanHistories()

    //then
    assertThat(results).hasSize(2)

    val userAResult = results.first { it.name == "A" }
    assertThat(userAResult.books).hasSize(3)
    assertThat(userAResult.books).extracting("name").containsExactlyInAnyOrder("책1", "책2", "책3")
    assertThat(userAResult.books).extracting("isReturn").containsExactlyInAnyOrder(false, false, true)

    val userBResult = results.first { it.name == "B" }
    assertThat(userBResult.books).isEmpty()
  }
}