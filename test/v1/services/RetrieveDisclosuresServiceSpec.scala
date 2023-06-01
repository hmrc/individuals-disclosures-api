/*
 * Copyright 2023 HM Revenue & Customs
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

package v1.services

import api.controllers.EndpointLogContext
import api.models.domain.{ Nino, Timestamp }
import api.models.errors
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.connectors.MockRetrieveDisclosuresConnector
import v1.models.request.retrieve.RetrieveDisclosuresRequest
import v1.models.response.retrieveDisclosures.RetrieveDisclosuresResponse

import scala.concurrent.Future

class RetrieveDisclosuresServiceSpec extends ServiceSpec {

  private val nino    = Nino("AA112233A")
  private val taxYear = "2021-22"

  trait Test extends MockRetrieveDisclosuresConnector {
    implicit val hc: HeaderCarrier              = HeaderCarrier()
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    lazy val service = new RetrieveDisclosuresService(mockRetrieveDisclosuresConnector)
  }

  lazy val request: RetrieveDisclosuresRequest   = RetrieveDisclosuresRequest(nino, taxYear)
  lazy val response: RetrieveDisclosuresResponse = RetrieveDisclosuresResponse(None, None, Timestamp("2020-07-06T09:37:17Z"))

  "retrieve" must {
    "return correct result for a success" in new Test {
      val outcome = Right(ResponseWrapper(correlationId, response))

      MockRetrieveDisclosuresConnector
        .retrieve(request)
        .returns(Future.successful(outcome))

      await(service.retrieve(request)) shouldBe outcome
    }

    "map errors according to spec" when {

      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockRetrieveDisclosuresConnector
            .retrieve(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          await(service.retrieve(request)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val input = Seq(
        ("INVALID_NINO", NinoFormatError),
        ("INVALID_TAX_YEAR", TaxYearFormatError),
        ("INVALID_CORRELATIONID", errors.InternalError),
        ("NO_DATA_FOUND", NotFoundError),
        ("SERVER_ERROR", errors.InternalError),
        ("SERVICE_UNAVAILABLE", errors.InternalError)
      )

      val extra_error = Seq(
        ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError)
      )

      (input ++ extra_error).foreach(args => (serviceError _).tupled(args))
    }
  }
}
