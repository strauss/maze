package de.dreamcube.mazegame.web_ui

import js.objects.jso
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLInputElement
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

@Serializable
@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("LoginResponse")
data class LoginResponse(val token: String, val expires: Long)

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("UiActions")
object UiActions {
    fun onLoginClick() {
        console.log("Login-Click-HYPE")
        val user: HTMLInputElement? = (window.document.getElementById("username")) as? HTMLInputElement
        val pass: HTMLInputElement? = (window.document.getElementById("password")) as? HTMLInputElement
        val basic = window.btoa("${user?.value}:${pass?.value}")

        val headers = Headers().apply {
            append("Authorization", "Basic $basic")
        }

        val init = jso<RequestInit> {
            this.method  = "POST"
            this.headers = headers
        }

        window.fetch("/login", init)
            .then {it.text()}
            .then { json ->
                val resp = Json.decodeFromString<LoginResponse>(json)
                window.sessionStorage.setItem("jwt", resp.token)
                window.location.replace("/hello")
            }

    }

}