package com.jtb.tcc.integrate.test

import com.jtb.tcc.integrate.TestTccServer
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import com.twitter.finagle.http.Status._

class TestTccServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    twitterServer = new TestTccServer
  )

  test("Server#startup") {
    server.assertHealthy()
  }

  test("test /tran/start") {
    server.httpPost("/tran/start", postBody = """{"mod_id":"abcd"}""", andExpect = Ok)

  }
  }