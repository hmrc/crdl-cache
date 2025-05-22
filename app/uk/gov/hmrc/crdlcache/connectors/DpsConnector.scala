/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.crdlcache.connectors

import uk.gov.hmrc.crdlcache.config.AppConfig
import uk.gov.hmrc.crdlcache.models.CodeListCode
import uk.gov.hmrc.crdlcache.models.dps.CodeListResponse
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*

import java.nio.charset.StandardCharsets
import java.util.{Base64, UUID}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DpsConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig) {
    
  private val base64Encoder = Base64.getEncoder

  def fetchCodelist(code: CodeListCode)(using
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, CodeListResponse]] = {
    val dpsUrl     = url"${appConfig.dpsUrl}/${appConfig.dpsPath}"
    val clientIdAndSecret = s"${appConfig.dpsClientId}:${appConfig.dpsClientSecret}"
    val authSecret = base64Encoder.encodeToString(clientIdAndSecret.getBytes(StandardCharsets.UTF_8))

    val headerCarrierWithAuth = hc.copy(authorization = Some(Authorization(s"Basic $authSecret")))

    httpClient
      .get(dpsUrl)(headerCarrierWithAuth)
      .setHeader("correlationId" -> UUID.randomUUID().toString)
      .transform(_.withQueryStringParameters("code_list_code" -> code.code))
      .execute[Either[UpstreamErrorResponse, CodeListResponse]]
  }
}
