/*
 * Copyright 2026 HM Revenue & Customs
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

package v2.createAmendDisclosures

import api.connectors.DownstreamOutcome
import api.models.domain.{Nino, TaxYear}
import api.models.errors
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.{ServiceOutcome, ServiceSpec}
import v2.createAmendDisclosures.def2.fixture.Def2_CreateAmendDisclosuresFixture.requestBodyModel
import v2.createAmendDisclosures.def2.model.request.Def2_CreateAmendDisclosuresRequestData
import v2.createAmendDisclosures.model.request.CreateAmendDisclosuresRequestData

import scala.concurrent.Future

class CreateAmendDisclosuresServiceSpec extends ServiceSpec {

  private val requestData: CreateAmendDisclosuresRequestData = Def2_CreateAmendDisclosuresRequestData(
    nino = Nino("AA123456A"),
    taxYear = TaxYear.fromMtd("2026-27"),
    body = requestBodyModel
  )

  "CreateAmendDisclosuresService" should {
    "return correct result for a success" in new Test {
      val outcome: DownstreamOutcome[Unit] = Right(ResponseWrapper(correlationId, ()))

      MockCreateAmendDisclosuresConnector
        .createAmendDisclosures(requestData)
        .returns(Future.successful(outcome))

      val result: ServiceOutcome[Unit] = await(service.createAmendDisclosures(requestData))

      result shouldBe outcome
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockCreateAmendDisclosuresConnector
            .createAmendDisclosures(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          val result: ServiceOutcome[Unit] = await(service.createAmendDisclosures(requestData))

          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errorMap: Seq[(String, MtdError)] = Seq(
        ("1000", InternalError),
        ("1117", TaxYearFormatError),
        ("1215", NinoFormatError),
        ("1216", InternalError),
        ("4200", RuleOutsideAmendmentWindowError),
        ("5000", RuleTaxYearNotSupportedError),
        ("5003", NotFoundError.forSelfEmployment),
        ("5004", RuleVoluntaryClass2CannotBeChangedError)
      )

      errorMap.foreach(serviceError.tupled)
    }
  }

  private trait Test extends MockCreateAmendDisclosuresConnector {
    val service: CreateAmendDisclosuresService = new CreateAmendDisclosuresService(mockCreateAmendDisclosuresConnector)
  }

}
