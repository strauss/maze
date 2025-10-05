package de.dreamcube.mazegame.server.control

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.security.MessageDigest
import java.util.*

object UserService {
    val salt: String = UUID.randomUUID().toString()

    val users: Map<String, String> = mapOf("master" to hashPassword("${System.getProperty("masterPassword", "retsam")}:$salt"))

    fun checkCredentials(name: String, password: String): Boolean {
        val storedPassword: String? = users[name]
        return if (storedPassword == null) {
            return false
        } else {
            storedPassword == hashPassword("$password:$salt")
        }
    }

    private fun hashPassword(password: String): String =
        MessageDigest.getInstance("SHA-512").digest(password.toByteArray()).joinToString("") { "%2x".format(it) }

}

object JwtService {
    private val secret: String = UUID.randomUUID().toString()
    private val algorithm = Algorithm.HMAC256(secret)
    private val verifier = JWT.require(algorithm).build()

    fun issue(subject: String, ttlSec: Long): String =
        JWT.create()
            .withSubject(subject)
            .withExpiresAt(Date(System.currentTimeMillis() + ttlSec * 1000L))
            .sign(algorithm)

    fun verify(token: String): DecodedJWT? = runCatching { verifier.verify(token) }.getOrNull()
}