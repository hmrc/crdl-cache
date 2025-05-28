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

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import uk.gov.hmrc.crdlcache.config.AppConfig
import uk.gov.hmrc.crdlcache.models.CodeListCode
import uk.gov.hmrc.crdlcache.models.dps.RelationType.Next
import uk.gov.hmrc.crdlcache.models.dps.{CodeListResponse, Relation}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, StringContextOps, UpstreamErrorResponse}

import java.net.{URI, URL, URLDecoder}
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.{Base64, UUID}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DpsConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(using
  mat: Materializer
) {

  private val base64Encoder = Base64.getEncoder
  private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

  private val baseUrl = url"${appConfig.dpsUrl}/${appConfig.dpsPath}"
  private val baseUri = baseUrl.toURI

  private def baseUrlWithQuery(relation: Relation) = {
    val decodedQuery = URLDecoder.decode(relation.href, StandardCharsets.UTF_8)
    new URI(
      baseUri.getScheme,
      baseUri.getUserInfo,
      baseUri.getAuthority,
      baseUri.getPort,
      baseUri.getPath,
      // Drop the leading `?` from the DPS next relation
      decodedQuery.substring(1),
      null
    ).toURL
  }

  private def headerCarrierWithAuth(hc: HeaderCarrier) = {
    val clientIdAndSecret =
      s"${appConfig.dpsClientId}:${appConfig.dpsClientSecret}"
    val authSecret =
      base64Encoder.encodeToString(clientIdAndSecret.getBytes(StandardCharsets.UTF_8))

    hc.copy(authorization = Some(Authorization(s"Basic $authSecret")))
  }

  def fetchCodelistSnapshots(code: CodeListCode, lastUpdatedDate: ZonedDateTime)(
    processResponse: CodeListResponse => Future[Unit]
  )(using ec: ExecutionContext): Future[Unit] = {
    // Produce the URL for the first page
    val queryParams = Map(
      "code_list_code"    -> code.code,
      "last_updated_date" -> dateFormatter.format(lastUpdatedDate),
      "$start_index"      -> 0,
      "$count"            -> 10,
      "$orderby"          -> "snapshotversion ASC"
    )

    val initialDpsUrl = url"$baseUrl?$queryParams"

    Source
      .unfoldAsync[URL, Unit](initialDpsUrl) { dpsUrl =>
        // Fetch each page
        httpClient
          .get(dpsUrl)(headerCarrierWithAuth(HeaderCarrier()))
          .setHeader("correlationId" -> UUID.randomUUID().toString)
          .execute[Either[UpstreamErrorResponse, CodeListResponse]]
          .flatMap {
            case Left(err) =>
              // Rethrow errors
              Future.failed(err)
            case Right(response) =>
              // Provide the page to our processing function
              processResponse(response).map { _ =>
                // If we find a "next" link, we can produce a URL for the next page
                val nextLink = response.links.find(_.rel == Next)
                nextLink.map(link => (baseUrlWithQuery(link), ()))
              }
          }
      }
      .run()
      .map(_ => ())
  }

  def fetchCodelist(code: CodeListCode)(using
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
