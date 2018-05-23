package com.jtb.tcc.client.filters


import com.twitter.finagle.http.{RequestBuilder, Response, Status}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.httpclient.test.InMemoryHttpService
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, Test}
import com.twitter.io.Buf

class TestTccMainFilter extends Test {

  val orderService = new InMemoryHttpService()
  val subTranService = new InMemoryHttpService()
  val injector: Injector = TestInjector(modules = Seq(FinatraJacksonModule)).create


  test("TestOrderMainFilter") {
    //val httpClient: HttpClient = new HttpClient(orderService)
    val mockResponse = Response(Status.Ok)
    val mockResponse2 = Response(Status.Ok)
    val mockResponse3 = Response(Status.Ok)

    mockResponse.setContentString("""{"tran_id":"test_tran_id","mod_id":"test_mod_id"}""")

    subTranService.mockPost("/tran/start",  andReturn = mockResponse)
    subTranService.mockPost("/tran/close",  andReturn = mockResponse2)
    orderService.mockPost("/hello",  andReturn = mockResponse3)

    val httpClient=new HttpClient(hostname="",
      httpService=subTranService,mapper=injector.instance[FinatraObjectMapper])

    val filter= new TccMainFilter(httpClient,injector.instance[FinatraObjectMapper])

    val s=filter.andThen(orderService)


    val r = RequestBuilder().url("http://www.hello.com/hello")
    s(r.buildPost(Buf.Empty))

  }


  override def afterEach(): Unit = {
    orderService.reset()
    subTranService.reset()
  }

}
