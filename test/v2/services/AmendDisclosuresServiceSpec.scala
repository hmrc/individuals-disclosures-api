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
import api.models.errors
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v2.connectors.MockAmendDisclosuresConnector
import v2.models.request.amend._

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

  val amendDisclosuresRequest: AmendDisclosuresRequestData = AmendDisclosuresRequestData(
    nino = Nino(nino),
    taxYear = TaxYear.fromMtd(taxYear),
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

        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockAmendDisclosuresConnector
              .amendDisclosures(amendDisclosuresRequest)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            await(service.amendDisclosures(amendDisclosuresRequest)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = Seq(
          ("INVALID_NINO", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("INVALID_CORRELATIONID", errors.InternalError),
          ("INVALID_PAYLOAD", errors.InternalError),
          ("INCOME_SOURCE_NOT_FOUND", NotFoundError),
          ("VOLUNTARY_CLASS2_CANNOT_BE_CHANGED", RuleVoluntaryClass2CannotBeChangedError),
          ("OUTSIDE_AMENDMENT_WINDOW", RuleOutsideAmendmentWindow),
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

}
