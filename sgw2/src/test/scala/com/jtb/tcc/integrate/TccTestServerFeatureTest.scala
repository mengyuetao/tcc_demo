package com.jtb.tcc.integrate

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.Mockito
import com.twitter.inject.server.FeatureTest


class TccTestServiceModule


class TccTestServerFeatureTest extends FeatureTest with Mockito {

  override protected def server: EmbeddedHttpServer =
    new EmbeddedHttpServer(twitterServer = new TestTccServer {
    override val name = "test-tcc-server"
  })


}