package com.appham.mockinizer

import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse

val mocks: Map<RequestFilter, MockResponse> = mapOf(
    RequestFilter(path = "/typicode/demo/mocked") to MockResponse().apply {
        setResponseCode(200)
    },

    RequestFilter(path = "/typicode/demo/foo") to MockResponse().apply {
        setResponseCode(200)
    },

    RequestFilter(path = "/typicode/demo/error500") to MockResponse().apply {
        setResponseCode(500)
    },

    RequestFilter(path = "/typicode/demo/private") to MockResponse().apply {
        setResponseCode(403)
    },

    RequestFilter(path = "/typicode/demo/delete", method = Method.DELETE) to MockResponse().apply {
        setResponseCode(200)
    },

    RequestFilter(
        path = "/typicode/demo/post",
        method = Method.POST,
        body = """{"hey":"ya"}"""
    ) to MockResponse().apply {
        setResponseCode(200)
        setBody("""{"foo":"bar"}""")
    },

    RequestFilter(
        path = "/typicode/demo/header",
        headers = Headers.headersOf("name", "value")
    ) to MockResponse().apply {
        setResponseCode(200)
    },

    RequestFilter(
        path = "/typicode/demo/headers",
        headers = Headers.headersOf("name", "value", "foo", "bar")
    ) to MockResponse().apply {
        setResponseCode(200)
    }
)

