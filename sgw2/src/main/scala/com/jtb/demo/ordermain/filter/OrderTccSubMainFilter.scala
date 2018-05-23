package com.jtb.demo.ordermain.filter

import com.jtb.tcc.client.filters.{TccSubMainFilter}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.json.FinatraObjectMapper

class OrderTccSubMainFilter(client: HttpClient, mapper: FinatraObjectMapper) extends TccSubMainFilter (client,mapper){

}

