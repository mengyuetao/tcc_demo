package com.jtb.demo.ordermain.modules

import com.google.inject.{Provides, Singleton}
import com.jtb.demo.ordermain.anno.{Order, Tcc, Warehouse}
import com.jtb.demo.ordermain.filter.{OrderTccMainFilter, OrderTccSubMainFilter}
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.TwitterModule
import com.twitter.util.Future


object OrderMainModule extends OrderMainModule


class OrderMainModule extends TwitterModule {


  @Singleton
  @Provides
  def orderTccMainFilterProvide(
                                 @Tcc tcc: Service[Request, Response],
                                 mapper: FinatraObjectMapper
                               ): OrderTccMainFilter = {

    val jsonclient = new HttpClient("hostname", tcc, None, Map(), mapper)
    new OrderTccMainFilter(jsonclient, mapper)

  }

  @Singleton
  @Provides
  def orderTccSubMainFilterProvide(
                                    @Tcc tcc: Service[Request, Response],
                                    mapper: FinatraObjectMapper
                                  )

  : OrderTccSubMainFilter = {
    val jsonclient = new HttpClient("hostname", tcc, None, Map(), mapper)
    new OrderTccSubMainFilter(jsonclient, mapper)

  }

  @Singleton
  @Provides
  @Order
  def orderService(filter: OrderTccSubMainFilter ): Service[Request, Response] = {

    val client = Http.client.newService("127.0.0.1:28888", "")
    filter.andThen(client)
  }


  @Singleton
  @Provides
  @Warehouse
  def warehouseService(filter: OrderTccSubMainFilter ): Service[Request, Response] = {

    val client = Http.client.newService("127.0.0.1:18888", "")
    filter.andThen(client)

  }


  @Singleton
  @Provides
  @Tcc
  def tccService(): Service[Request, Response] = {

    val client = Http.client.newService("127.0.0.1:48888", "")
    client

  }

}
