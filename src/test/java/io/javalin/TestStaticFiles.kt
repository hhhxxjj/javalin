/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.staticfiles.Location
import io.javalin.util.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestStaticFiles {

    private val defaultStaticResourceApp = Javalin.create().enableStaticFiles("/public") // classpath
    private val externalStaticResourceApp = Javalin.create().enableStaticFiles("src/test/external/", Location.EXTERNAL)
    private val multiLocationStaticResourceApp = Javalin.create()
            .enableStaticFiles("src/test/external/", Location.EXTERNAL)
            .enableStaticFiles("/public/immutable")
            .enableStaticFiles("/public/protected")
            .enableStaticFiles("/public/subdir")
    private val debugLoggingApp = Javalin.create().enableStaticFiles("/public").enableDebugLogging()

    @Test
    fun `serving HTML from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains("text/html")
        assertThat(http.getBody("/html.html")).contains("HTML works")
    }

    @Test
    fun `serving JS from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/script.js").status).isEqualTo(200)
        assertThat(http.get("/script.js").headers.getFirst(Header.CONTENT_TYPE)).contains("application/javascript")
        assertThat(http.getBody("/script.js")).contains("JavaScript works")
    }

    @Test
    fun `serving CSS from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/styles.css").status).isEqualTo(200)
        assertThat(http.get("/styles.css").headers.getFirst(Header.CONTENT_TYPE)).contains("text/css")
        assertThat(http.getBody("/styles.css")).contains("CSS works")
    }

    @Test
    fun `before-handler runs before static resources`() = TestUtil.test(defaultStaticResourceApp) { app, http ->
        app.before("/protected/*") { throw UnauthorizedResponse("Protected") }
        assertThat(http.get("/protected/secret.html").status).isEqualTo(401)
        assertThat(http.getBody("/protected/secret.html")).isEqualTo("Protected")
    }

    @Test
    fun `directory root returns simple 404 if there is no welcome file`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/").status).isEqualTo(404)
        assertThat(http.getBody("/")).isEqualTo("Not found")
    }

    @Test
    fun `directory root return welcome file if there is a welcome file`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/subdir/").status).isEqualTo(200)
        assertThat(http.getBody("/subdir/")).isEqualTo("<h1>Welcome file</h1>")
    }

    @Test
    fun `expires is set to max-age=0 by default`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/script.js").headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=0")
    }

    @Test
    fun `expires is set to 1 year for files in immutable directory`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/immutable/library-1.0.0.min.js").headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=31622400")
    }

    @Test
    fun `files in external locations are found`() = TestUtil.test(externalStaticResourceApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200)
        assertThat(http.getBody("/html.html")).contains("HTML works")
    }

    @Test
    fun `one app can handle multiple static file locations`() = TestUtil.test(multiLocationStaticResourceApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200) // src/test/external/html.html
        assertThat(http.getBody("/html.html")).contains("HTML works")
        assertThat(http.get("/").status).isEqualTo(200)
        assertThat(http.getBody("/")).isEqualTo("<h1>Welcome file</h1>")
        assertThat(http.get("/secret.html").status).isEqualTo(200)
        assertThat(http.getBody("/secret.html")).isEqualTo("<h1>Secret file</h1>")
        assertThat(http.get("/styles.css").status).isEqualTo(404)
    }

    @Test
    fun `content type works in debugmmode`() = TestUtil.test(debugLoggingApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains("text/html")
        assertThat(http.getBody("/html.html")).contains("HTML works")
        assertThat(http.get("/script.js").headers.getFirst(Header.CONTENT_TYPE)).contains("application/javascript")
        assertThat(http.get("/styles.css").headers.getFirst(Header.CONTENT_TYPE)).contains("text/css")
    }

    @Test
    fun `WebJars available if enabled`() = TestUtil.test(Javalin.create().enableWebJars()) { _, http ->
        assertThat(http.get("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").status).isEqualTo(200)
        assertThat(http.get("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").headers.getFirst(Header.CONTENT_TYPE)).contains("text/css")
        assertThat(http.get("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=31622400")
    }

    @Test
    fun `WebJars not available if not enabled`() = TestUtil.test { _, http ->
        assertThat(http.get("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").status).isEqualTo(404)
    }

}
