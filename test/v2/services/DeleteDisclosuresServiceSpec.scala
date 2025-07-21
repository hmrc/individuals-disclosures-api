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

package v2.services

import api.controllers.EndpointLogContext
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.MockDeleteDisclosuresConnector
import v2.models.request.delete.DeleteDisclosuresRequestData

import scala.concurrent.Future

class DeleteDisclosuresServiceSpec extends ServiceSpec {

  private val nino    = Nino("AA112233A")
  private val taxYear = TaxYear.fromMtd("2021-22")

  trait Test extends MockDeleteDisclosuresConnector {
    implicit val hc: HeaderCarrier              = HeaderCarrier()
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    lazy val service = new DeleteDisclosuresService(mockDeleteDisclosuresConnector)
  }

  lazy val request: DeleteDisclosuresRequestData = DeleteDisclosuresRequestData(nino, taxYear)

  "DeleteDisclosuresService" when {
    "return correct result for a success" in new Test {
      val outcome = Right(ResponseWrapper(correlationId, ()))

      MockDeleteDisclosuresConnector
        .delete(request)
        .returns(Future.successful(outcome))

      await(service.delete(request)) shouldBe outcome
    }

    "map errors according to spec" when {

      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockDeleteDisclosuresConnector
            .delete(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          await(service.delete(request)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val ifsInput = Seq(
        ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
        ("INVALID_TAX_YEAR", TaxYearFormatError),
        ("INVALID_CORRELATIONID", InternalError),
        ("NO_DATA_FOUND", NotFoundError),
        ("OUTSIDE_AMENDMENT_WINDOW", RuleOutsideAmendmentWindowError),
        ("SERVER_ERROR", InternalError),
        ("SERVICE_UNAVAILABLE", InternalError)
      )

      val hipInput = Seq(
        ("1215", NinoFormatError),
        ("1117", TaxYearFormatError),
        ("5010", NotFoundError),
        ("4200", RuleOutsideAmendmentWindowError),
        ("5000", RuleTaxYearNotSupportedError)
      )

      (ifsInput ++ hipInput).foreach(args => (serviceError _).tupled(args))
    }
  }

}
