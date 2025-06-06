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

package uk.gov.hmrc.crdlcache.controllers

import controllers.AssetsFinder
import org.mockito.Mockito.when
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.crdlcache.views

class DocumentationControllerSpec extends AnyFlatSpec with Matchers with MockitoSugar {
  private val assets        = mock[AssetsFinder]
  private val view          = views.html.OpenApi(assets)
  private val docController = new DocumentationController(Helpers.stubControllerComponents(), view)

  "DocumentationController" should "return a documentation page for the API version requested" in {
    val version = "1.0"

    when(assets.path(s"api/$version/openapi.yaml")).thenReturn(s"/api/$version/openapi.yaml")

    val fakeRequest = FakeRequest(GET, s"/api/$version")
    val result      = docController.docs(version)(fakeRequest)

    contentAsString(result) mustBe
      """<!DOCTYPE html>
        |<html lang="en">
        |  <head>
        |    <meta charset="UTF-8">
        |    <meta name="viewport" content="width=device-width, initial-scale=1">
        |    <title>CRDL Reference Data API 1.0</title>
        |  </head>
        |  <body>
        |    <redoc spec-url="/api/1.0/openapi.yaml" disable-search="true"></redoc>
        |    <script src="https://cdn.redoc.ly/redoc/latest/bundles/redoc.standalone.js"> </script>
        |  </body>
        |</html>""".stripMargin

    status(result) mustBe OK
  }
}
