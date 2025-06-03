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

import com.typesafe.config.Config
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl
import org.apache.pekko.stream.scaladsl.Source
import uk.gov.hmrc.crdlcache.config.AppConfig
import uk.gov.hmrc.crdlcache.models.CodeListCode
import uk.gov.hmrc.crdlcache.models.dps.CodeListResponse
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.UpstreamErrorResponse.{Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{
  Authorization,
  HeaderCarrier,
  HttpReads,
  Retries,
  StringContextOps,
  UpstreamErrorResponse
}

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.{Base64, UUID}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DpsConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(using
  system: ActorSystem
) extends Retries {
  override protected def actorSystem: ActorSystem = system
  override protected def configuration: Config    = appConfig.config.underlying

  private val base64Encoder = Base64.getEncoder
  private val dateFormatter = DateTimeFormatter.ISO_INSTANT

  private val baseUrl = url"${appConfig.dpsUrl}/${appConfig.dpsPath}"

  private def headerCarrierWithAuth(hc: HeaderCarrier) = {
    val clientIdAndSecret =
      s"${appConfig.dpsClientId}:${appConfig.dpsClientSecret}"
    val authSecret =
      base64Encoder.encodeToString(clientIdAndSecret.getBytes(StandardCharsets.UTF_8))

    hc.copy(authorization = Some(Authorization(s"Basic $authSecret")))
  }

  private def fetchCodeListSnapshot(
    code: CodeListCode,
    lastUpdatedDate: Instant,
    startIndex: Int
  )(using ec: ExecutionContext): Future[CodeListResponse] = {
    val queryParams = Map(
      "code_list_code"    -> code.code,
      "last_updated_date" -> dateFormatter.format(lastUpdatedDate),
      "$start_index"      -> startIndex,
      "$count"            -> 10,
      "$orderby"          -> "snapshotversion ASC"
    )

    val dpsUrl = url"$baseUrl?$queryParams"

    retryFor(s"fetch of $code snapshots at index $startIndex as of $lastUpdatedDate") {
      // No point in retrying if our request is wrong
      case Upstream4xxResponse(_) => false
      // Attempt to recover from intermittent connectivity issues
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(dpsUrl)(headerCarrierWithAuth(HeaderCarrier()))
        .setHeader("correlationId" -> UUID.randomUUID().toString)
        .execute[CodeListResponse](using throwOnFailure(readEitherOf[CodeListResponse]), ec)
    }
  }

  def fetchCodeListSnapshots(code: CodeListCode, lastUpdatedDate: Instant)(using
    ec: ExecutionContext
  ): Source[CodeListResponse, NotUsed] = Source
    .unfoldAsync[Int, CodeListResponse](0) { startIndex =>
      fetchCodeListSnapshot(code, lastUpdatedDate, startIndex).map { response =>
        if (response.elements.isEmpty)
          None
        else
          Some((startIndex + 10, response))
      }
    }

  def fetchCodeList(code: CodeListCode)(using
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, CodeListResponse]] = {
    val queryParams = Map("code_list_code" -> code.code)
    val dpsUrl      = url"${appConfig.dpsUrl}/${appConfig.dpsPath}?$queryParams"
    httpClient
      .get(dpsUrl)(headerCarrierWithAuth(hc))
      .setHeader("correlationId" -> UUID.randomUUID().toString)
      .execute[Either[UpstreamErrorResponse, CodeListResponse]]
  }
}
