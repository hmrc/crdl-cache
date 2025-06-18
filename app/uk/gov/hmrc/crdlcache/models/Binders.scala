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

import play.api.mvc.QueryStringBindable

import java.time.Instant
import java.time.format.DateTimeFormatter

object Binders {
  private val formatter = DateTimeFormatter.ISO_INSTANT

  given bindableInstant: QueryStringBindable[Instant] = new QueryStringBindable.Parsing[Instant](
    Instant.parse,
    formatter.format,
    (key, e) => s"Cannot parse parameter $key as Instant: ${e.getMessage}"
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
}
