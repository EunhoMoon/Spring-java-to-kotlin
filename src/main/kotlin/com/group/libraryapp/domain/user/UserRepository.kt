package com.group.libraryapp.domain.user

import com.group.libraryapp.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, Long> {

    fun findByName(name: String): Optional<User>

}