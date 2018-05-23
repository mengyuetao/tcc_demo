package com.jtb.tcc.client.filters

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging
import com.twitter.util.Future

case class Mod(modId: String)

case class TranContext(tranId: String, modId: String)
case class TranCloseRequest(tranId: String, modId: String, tryCommit: Boolean)


class TccMainFilter(client: HttpClient, mapper: FinatraObjectMapper) extends SimpleFilter[Request, Response] with Logging {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {


    val r = RequestBuilder.post("/tran/start").body(mapper.writeValueAsString(Mod(request.path)))

    val ret = client.executeJson[TranContext](r)

    ret.onFailure(
      e => error(e)
    )
    ret.flatMap { tranContext =>
      val tranCtx =  TranInfo.tranCtx
      Contexts.local.let(tranCtx, TranId(tranContext.tranId)) {
        service(request).onSuccess { _ =>

          val r=RequestBuilder.post("/tran/close").body(mapper.writeValueAsString(
            TranCloseRequest(tranContext.tranId,request.path,true)))
           client.execute(r)
        }
          .onFailure{ _ =>

            val r=RequestBuilder.post("/tran/close").body(mapper.writeValueAsString(
              TranCloseRequest(tranContext.tranId,request.path,false)))
            client.execute(r)
          }
      }
    }
  }

}
