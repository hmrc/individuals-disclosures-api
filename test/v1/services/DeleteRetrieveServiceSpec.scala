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

package v1.services

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.Nino
import v1.connectors.DesUri
import v1.controllers.EndpointLogContext
import v1.mocks.connectors.MockDeleteRetrieveConnector
import v1.models.domain.DesTaxYear
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.DeleteRetrieveRequest

import scala.concurrent.Future

class DeleteRetrieveServiceSpec extends ServiceSpec {

  private val nino = "AA112233A"
  private val taxYear = "2019"
  private val correlationId = "X-corr"

  private val deleteRetrieveRequest = DeleteRetrieveRequest(
    nino = Nino(nino),
    taxYear = DesTaxYear(taxYear)
  )

  trait Test extends MockDeleteRetrieveConnector {

    case class Data(field: Option[String])

    object Data {
      implicit val reads: Format[Data] = Json.format[Data]
    }

    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")
    implicit val deleteDesUri: DesUri[Unit] = DesUri[Unit](s"disc-placeholder/disclosures/$nino/$taxYear")
    implicit val retrieveDesUri: DesUri[Data] = DesUri[Data](s"disc-placeholder/disclosures/$nino/$taxYear")

    val service: DeleteRetrieveService = new DeleteRetrieveService(
      connector = mockDeleteRetrieveConnector
    )
  }

  "DeleteRetrieveService" when {
    "delete" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockDeleteRetrieveConnector.delete(deleteRetrieveRequest)
          .returns(Future.successful(outcome))

        await(service.delete(deleteRetrieveRequest)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the service" in new Test {

            MockDeleteRetrieveConnector.delete(deleteRetrieveRequest)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

            await(service.delete(deleteRetrieveRequest)) shouldBe Left(ErrorWrapper(Some(correlationId), error))
          }

        val input = Seq(
          ("INVALID_NINO", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("NOT_FOUND", NotFoundError),
          ("SERVER_ERROR", DownstreamError),
          ("SERVICE_UNAVAILABLE", DownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }

    "retrieve" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, Data(Some("value"))))

        MockDeleteRetrieveConnector.retrieve[Data](deleteRetrieveRequest)
          .returns(Future.successful(outcome))

        await(service.retrieve[Data](deleteRetrieveRequest)) shouldBe outcome
      }

      "return a NotFoundError for an empty response" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, Data(None)))

        MockDeleteRetrieveConnector.retrieve[Data](deleteRetrieveRequest)
          .returns(Future.successful(outcome))

        await(service.retrieve[Data](deleteRetrieveRequest)) shouldBe Left(ErrorWrapper(Some(correlationId), NotFoundError))
      }

      "map errors according to spec" when {

        def serviceError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the service" in new Test {

            MockDeleteRetrieveConnector.retrieve[Data](deleteRetrieveRequest)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

            await(service.retrieve[Data](deleteRetrieveRequest)) shouldBe Left(ErrorWrapper(Some(correlationId), error))
          }

        val input = Seq(
          ("INVALID_NINO", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("NOT_FOUND", NotFoundError),
          ("SERVER_ERROR", DownstreamError),
          ("SERVICE_UNAVAILABLE", DownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }
  }
}