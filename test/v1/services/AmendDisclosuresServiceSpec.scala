/*
 * Copyright 2022 HM Revenue & Customs
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

import v1.controllers.EndpointLogContext
import v1.mocks.connectors.MockAmendDisclosuresConnector
import v1.models.domain.Nino
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.disclosures._

import scala.concurrent.Future

class AmendDisclosuresServiceSpec extends ServiceSpec {

  private val nino    = "AA112233A"
  private val taxYear = "2021-22"

  val taxAvoidanceModel: Seq[AmendTaxAvoidanceItem] = Seq(
    AmendTaxAvoidanceItem(
      srn = "14211123",
      taxYear = "2020-21"
    )
  )

  val class2NicsModel: AmendClass2Nics = AmendClass2Nics(class2VoluntaryContributions = Some(true))

  val amendDisclosuresRequest: AmendDisclosuresRequest = AmendDisclosuresRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    body = AmendDisclosuresRequestBody(
      taxAvoidance = Some(taxAvoidanceModel),
      class2Nics = Some(class2NicsModel)
    )
  )

  trait Test extends MockAmendDisclosuresConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: AmendDisclosuresService = new AmendDisclosuresService(
      connector = mockAmendDisclosuresConnector
    )
  }

  "AmendDisclosuresService" when {
    "amendDisclosures" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockAmendDisclosuresConnector
          .amendDisclosures(amendDisclosuresRequest)
          .returns(Future.successful(outcome))

        await(service.amendDisclosures(amendDisclosuresRequest)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(ifsErrorCode: String, error: MtdError): Unit =
          s"a $ifsErrorCode error is returned from the service" in new Test {

            MockAmendDisclosuresConnector
              .amendDisclosures(amendDisclosuresRequest)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(ifsErrorCode))))))

            await(service.amendDisclosures(amendDisclosuresRequest)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = Seq(
          ("INVALID_NINO", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("INVALID_CORRELATIONID", InternalError),
          ("INVALID_PAYLOAD", InternalError),
          ("INCOME_SOURCE_NOT_FOUND", NotFoundError),
          ("VOLUNTARY_CLASS2_CANNOT_BE_CHANGED", RuleVoluntaryClass2CannotBeChangedError),
          ("SERVER_ERROR", InternalError),
          ("SERVICE_UNAVAILABLE", InternalError)
        )

        val extra_error = Seq(
          ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError)
        )

        (input ++ extra_error).foreach(args => (serviceError _).tupled(args))
      }
    }
  }
}
