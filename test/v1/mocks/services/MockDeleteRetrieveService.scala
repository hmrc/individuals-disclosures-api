/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.mocks.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{Format, Reads}
import uk.gov.hmrc.http.HeaderCarrier
import v1.connectors.DesUri
import v1.controllers.EndpointLogContext
import v1.models.errors.ErrorWrapper
import v1.models.outcomes.ResponseWrapper
import v1.models.request.DeleteRetrieveRequest
import v1.services.DeleteRetrieveService

import scala.concurrent.{ExecutionContext, Future}

trait MockDeleteRetrieveService extends MockFactory {

  val mockDeleteRetrieveService: DeleteRetrieveService = mock[DeleteRetrieveService]

  object MockDeleteRetrieveService {

    def delete(requestData: DeleteRetrieveRequest): CallHandler[Future[Either[ErrorWrapper, ResponseWrapper[Unit]]]] = {
      (mockDeleteRetrieveService
        .delete(_: DeleteRetrieveRequest)(_: HeaderCarrier, _: ExecutionContext, _: EndpointLogContext, _: DesUri[Unit]))
        .expects(requestData, *, *, *, *)
    }

    def retrieve[Resp: Reads](requestData: DeleteRetrieveRequest): CallHandler[Future[Either[ErrorWrapper, ResponseWrapper[Resp]]]] = {
      (mockDeleteRetrieveService
        .retrieve[Resp](_: DeleteRetrieveRequest)(_: Format[Resp], _: HeaderCarrier, _: ExecutionContext, _: EndpointLogContext, _: DesUri[Resp]))
        .expects(requestData, *, *, *, *, *)
    }
  }

}