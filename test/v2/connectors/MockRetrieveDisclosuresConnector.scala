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

package v2.connectors

import api.connectors.DownstreamOutcome
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier
import v2.models.request.retrieve.RetrieveDisclosuresRequestData
import v2.models.response.retrieveDisclosures.RetrieveDisclosuresResponse

import scala.concurrent.{ExecutionContext, Future}

trait MockRetrieveDisclosuresConnector extends TestSuite with MockFactory {

  val mockRetrieveDisclosuresConnector: RetrieveDisclosuresConnector = mock[RetrieveDisclosuresConnector]

  object MockRetrieveDisclosuresConnector {

    def retrieve(request: RetrieveDisclosuresRequestData): CallHandler[Future[DownstreamOutcome[RetrieveDisclosuresResponse]]] = {
      (mockRetrieveDisclosuresConnector
        .retrieve(_: RetrieveDisclosuresRequestData)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(request, *, *, *)
    }

  }

}
