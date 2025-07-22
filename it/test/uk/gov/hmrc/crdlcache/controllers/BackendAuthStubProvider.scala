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

import play.api.mvc.ControllerComponents
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import javax.inject.{Inject, Provider}
import scala.concurrent.ExecutionContext

class BackendAuthStubProvider @Inject() (stubBehaviour: StubBehaviour)(using
  cc: ControllerComponents,
  ec: ExecutionContext
) extends Provider[BackendAuthComponents] {
  override def get(): BackendAuthComponents = BackendAuthComponentsStub(stubBehaviour)
}
