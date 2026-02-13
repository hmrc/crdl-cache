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

package uk.gov.hmrc.crdlcache.models

import play.api.libs.json.*
import play.api.mvc.QueryStringBindable

import java.time.{Instant, LocalDate}
import java.time.format.DateTimeFormatter

object Binders {
  private val formatter          = DateTimeFormatter.ISO_INSTANT
  private val localDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
  private val KeysParam          = "keys"
  private val ActiveAtParam      = "activeAt"
  private val PhaseParam         = "phase"
  private val DomainParam        = "domain"

  given bindableInstant: QueryStringBindable[Instant] = new QueryStringBindable.Parsing[Instant](
    Instant.parse,
    formatter.format,
    (key, e) => s"Cannot parse parameter $key as Instant: ${e.getMessage}"
  )

  given bindableLocalDate: QueryStringBindable[LocalDate] =
    new QueryStringBindable.Parsing[LocalDate](
      LocalDate.parse(_, localDateFormatter),
      localDateFormatter.format,
      (key, e) => s"Cannot parse parameter $key as LocalDate: ${e.getMessage}"
    )

  given bindableSet[A: QueryStringBindable]: QueryStringBindable[Set[A]] =
    new QueryStringBindable[Set[A]] {
      private val bindableSeq = summon[QueryStringBindable[Seq[A]]]

      override def unbind(key: String, value: Set[A]): String =
        bindableSeq.unbind(key, value.toSeq)

      override def bind(
        key: String,
        params: Map[String, Seq[String]]
      ): Option[Either[String, Set[A]]] =
        params.get(key).flatMap { paramValues =>
          val values = paramValues.flatMap(_.split(",")).filterNot(_.isEmpty)
          bindableSeq.bind(key, Map(key -> values)).map(_.map(_.toSet))
        }
    }

  given bindableJsValueMap: QueryStringBindable[Map[String, JsValue]] =
    new QueryStringBindable[Map[String, JsValue]] {
      private def parsePropValue(propValue: String) = {
        propValue match {
          case "true"  => JsTrue
          case "false" => JsFalse
          case "null"  => JsNull
          case _       => JsString(propValue)
        }
      }

      override def bind(
        key: String,
        params: Map[String, Seq[String]]
      ): Option[Either[String, Map[String, JsValue]]] = {
        val parsedProps = for {
          (propName, propValues) <- params
            .removed(KeysParam)
            .removed(ActiveAtParam) // Ignore the "keys" and "activeAt" query parameters
            .removed(PhaseParam)
            .removed(DomainParam)
          propValue <- propValues.headOption
          if propValue.nonEmpty
        } yield propName -> parsePropValue(propValue)

        if parsedProps.nonEmpty
        then Some(Right(parsedProps))
        else None
      }

      override def unbind(key: String, value: Map[String, JsValue]): String =
        throw new UnsupportedOperationException(
          "Rendering reference data properties into query string is not supported"
        )
    }
}
