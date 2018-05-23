
package com.jtb.tcc.client.filters

import com.twitter.finagle.Service
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http.{Request, RequestBuilder, Response, Status}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.httpclient.test.InMemoryHttpService
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, Test}
import com.twitter.util.Future

class TccSubMainFilterTest extends Test  {


  val orderService = new InMemoryHttpService()
  val subTranService = new InMemoryHttpService()
  val injector: Injector = TestInjector(modules = Seq(FinatraJacksonModule)).create

    test("TccSubMainFilterTest") {

      val mockResponse = Response(Status.Ok)
      val mockResponse2 = Response(Status.Ok)
      mockResponse.setContentString("""{"tran_id":"tranId-123456","mod_id":"/hello"}""")

      subTranService.mockPost("/tran/sub/start",  andReturn = mockResponse)
      subTranService.mockPost("/tran/sub/close",  andReturn = mockResponse2)
      val httpClient=new HttpClient(hostname="",
        httpService=subTranService,mapper=injector.instance[FinatraObjectMapper])



       val tranCtx = new Contexts.local.Key[TranId]
       val filter = new TccSubMainFilter(httpClient,injector.instance[FinatraObjectMapper])
       val service =  Service.mk[Request,Response] { req =>

         assert(req.headerMap("xx-tranId") == "tranId-123456")
         assert(req.headerMap("xx-subTranId") == "/hello")
         Future(Response())
       }

      Contexts.local.let(tranCtx,TranId("tranId-123456")){
           val r = RequestBuilder().url("http://www.hello.com/hello")
           val ss = filter.andThen(service)
           val resp = ss(r.buildGet())
      }

    }

  override def afterEach(): Unit = {
    orderService.reset()
    subTranService.reset()
  }


}