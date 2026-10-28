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

package v2.deleteDisclosures

import api.connectors.DownstreamOutcome
import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.{ServiceOutcome, ServiceSpec}
import v2.deleteDisclosures.def1.model.request.Def1_DeleteDisclosuresRequestData
import v2.deleteDisclosures.model.request.DeleteDisclosuresRequestData

import scala.concurrent.Future

class DeleteDisclosuresServiceSpec extends ServiceSpec {

  private val requestData: DeleteDisclosuresRequestData = Def1_DeleteDisclosuresRequestData(
    nino = Nino("AA123456A"),
    taxYear = TaxYear.fromMtd("2026-27")
  )

  "DeleteDisclosuresService" should {
    "return correct result for a success" in new Test {
      val outcome: DownstreamOutcome[Unit] = Right(ResponseWrapper(correlationId, ()))

      MockDeleteDisclosuresConnector
        .deleteDisclosures(requestData)
        .returns(Future.successful(outcome))

      val result: ServiceOutcome[Unit] = await(service.delete(requestData))

      result shouldBe outcome
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockDeleteDisclosuresConnector
            .deleteDisclosures(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          val result: ServiceOutcome[Unit] = await(service.delete(requestData))

          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errorMap: Seq[(String, MtdError)] = Seq(
        ("1215", NinoFormatError),
        ("1117", TaxYearFormatError),
        ("5010", NotFoundError),
        ("4200", RuleOutsideAmendmentWindowError),
        ("5000", RuleTaxYearNotSupportedError)
      )

      errorMap.foreach(serviceError.tupled)
    }
  }

  private trait Test extends MockDeleteDisclosuresConnector {
    val service: DeleteDisclosuresService = new DeleteDisclosuresService(mockDeleteDisclosuresConnector)
  }

}
