package com.group.libraryapp.service.book

import com.group.libraryapp.domain.book.Book
import com.group.libraryapp.domain.book.BookRepository
import com.group.libraryapp.domain.book.BookType
import com.group.libraryapp.domain.book.BookType.*
import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanStatus.*
import com.group.libraryapp.dto.book.request.BookLoanRequest
import com.group.libraryapp.dto.book.request.BookRequest
import com.group.libraryapp.dto.book.request.BookReturnRequest
import com.group.libraryapp.dto.book.response.BookStatResponse
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@DisplayName("책")
@SpringBootTest
class BookServiceTest @Autowired constructor(
  private val bookService: BookService,
  private val bookRepository: BookRepository,
  private val userRepository: UserRepository,
  private val userLoanHistoryRepository: UserLoanHistoryRepository
) {

  @AfterEach
  fun clean() {
    bookRepository.deleteAll()
    userRepository.deleteAll()
  }

  @Test
  @DisplayName("등록이 정상 동작한다.")
  fun saveBookTest() {
    // given
    val request = BookRequest("총균쇠", COMPUTER)

    // when
    bookService.saveBook(request)

    //then
    val books = bookRepository.findAll()
    assertThat(books[0].name).isEqualTo("총균쇠")
    assertThat(books[0].type).isEqualTo(COMPUTER)
  }

  @Test
  @DisplayName("대출이 정상 동작한다.")
  fun loanBookTest() {
    // given
    bookRepository.save((Book.fixture("총균쇠")))
    val savedUser = userRepository.save(User("Janek", null))
    val request = BookLoanRequest("Janek", "총균쇠")

    // when
    bookService.loanBook(request)

    //then
    val results = userLoanHistoryRepository.findAll()
    assertThat(results).hasSize(1)
    assertThat(results[0].bookName).isEqualTo("총균쇠")
    assertThat(results[0].user.id).isEqualTo(savedUser.id)
    assertThat(results[0].status).isEqualTo(LOANED)
  }

  @Test
  @DisplayName("이미 대출되어있다면 대출 실패")
  fun loanBookFailTest() {
    // given
    bookRepository.save((Book.fixture("총균쇠")))
    val savedUser = userRepository.save(User("Janek", null))
    userLoanHistoryRepository.save(UserLoanHistory.fixture(savedUser, "총균쇠"))
    val request = BookLoanRequest("Janek", "총균쇠")

    // expect
    assertThrows<IllegalArgumentException> {
      bookService.loanBook(request)
    }.apply {
      assertThat(message).isEqualTo("진작 대출되어 있는 책입니다")
    }
  }

  @Test
  @DisplayName("반납이 정상 동작한다.")
  fun returnBookTest() {
    // given
    bookRepository.save((Book.fixture("총균쇠")))
    val savedUser = userRepository.save(User("Janek", null))
    userLoanHistoryRepository.save(UserLoanHistory.fixture(savedUser, "총균쇠"))
    val request = BookReturnRequest("Janek", "총균쇠")

    // when
    bookService.returnBook(request)

    //then
    val results = userLoanHistoryRepository.findAll()
    assertThat(results).hasSize(1)
    assertThat(results[0].status).isEqualTo(RETURNED)
  }

  @Test
  @DisplayName("책 대여 권수를 정상 확인한다.")
  fun countLoanedBookTest() {
    // given
    val savedUser = userRepository.save(User("Janek", null))
    userLoanHistoryRepository.saveAll(
      listOf(
        UserLoanHistory.fixture(savedUser, "A"),
        UserLoanHistory.fixture(savedUser, "B", RETURNED),
        UserLoanHistory.fixture(savedUser, "C", RETURNED),
      )
    )

    // when
    val result = bookService.countLoanedBook()

    //then
    assertThat(result).isEqualTo(1)
  }

  @Test
  @DisplayName("분야별 책 권수를 정상 확인한다.")
  fun getBookStatisticsTest() {
    // given
    bookRepository.saveAll(
      listOf(
        Book.fixture("A", COMPUTER),
        Book.fixture("B", COMPUTER),
        Book.fixture("C", SCIENCE),
      )
    )

    // when
    val results = bookService.getBookStatistics()

    //then
    assertThat(results).hasSize(2)
    assertCount(results, COMPUTER, 2L)
    assertCount(results, SCIENCE, 1L)
  }

}

private fun assertCount(results: List<BookStatResponse>, type: BookType, count: Long) {
  assertThat(results.first { result -> result.type == type }.count).isEqualTo(count)
}