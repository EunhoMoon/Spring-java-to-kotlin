package com.group.libraryapp.service.book

import com.group.libraryapp.domain.book.Book
import com.group.libraryapp.domain.book.BookRepository
import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.dto.book.request.BookLoanRequest
import com.group.libraryapp.dto.book.request.BookRequest
import com.group.libraryapp.dto.book.request.BookReturnRequest
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.lang.IllegalArgumentException

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
        val request = BookRequest("총균쇠")

        // when
        bookService.saveBook(request)

        //then
        val books = bookRepository.findAll()
        assertThat(books[0].name).isEqualTo("총균쇠")
    }

    @Test
    @DisplayName("대출이 정상 동작한다.")
    fun loanBookTest() {
        // given
        bookRepository.save((Book("총균쇠")))
        val savedUser = userRepository.save(User("Janek", null))
        val request = BookLoanRequest("Janek", "총균쇠")

        // when
        bookService.loanBook(request)

        //then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].bookName).isEqualTo("총균쇠")
        assertThat(results[0].user.id).isEqualTo(savedUser.id)
        assertThat(results[0].isReturn).isFalse()
    }

    @Test
    @DisplayName("이미 대출되어있다면 대출 실패")
    fun loanBookFailTest() {
        // given
        bookRepository.save((Book("총균쇠")))
        val savedUser = userRepository.save(User("Janek", null))
        userLoanHistoryRepository.save(UserLoanHistory(savedUser, "총균쇠", false))
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
        bookRepository.save((Book("총균쇠")))
        val savedUser = userRepository.save(User("Janek", null))
        userLoanHistoryRepository.save(UserLoanHistory(savedUser, "총균쇠", false))
        val request = BookReturnRequest("Janek", "총균쇠")

        // when
        bookService.returnBook(request)

        //then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].isReturn).isTrue()
    }
}