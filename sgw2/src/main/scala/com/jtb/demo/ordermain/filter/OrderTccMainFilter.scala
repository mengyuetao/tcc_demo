package com.jtb.demo.ordermain.filter

import com.jtb.tcc.client.filters.TccMainFilter
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.json.FinatraObjectMapper

class OrderTccMainFilter(client: HttpClient, mapper: FinatraObjectMapper) extends TccMainFilter (client,mapper){

}

