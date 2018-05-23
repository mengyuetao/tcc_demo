package com.jtb.tcc.client

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.httpclient.modules.HttpClientModule
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.app.App
import com.twitter.util.Await


object TranHttpClientMain extends TranHttpClient

class TranHttpClient extends App {
  override val name = "TranHttpClient"

  override val modules = Seq(MyHttpClientModule,FinatraJacksonModule )

  override protected def run(): Unit = {
    val  client =  injector.instance[HttpClient]

    val request = RequestBuilder.post("/tran/start").body("""{"mod_id":"abcd"}""")

    val ret = client.executeJson[JsonNode](request)

    val x=Await.result(ret)

    System.out.println(x)
  }
}


object MyHttpClientModule extends HttpClientModule {
  override val hostname = "james"
  override val dest = "127.0.0.1:8888"
}
