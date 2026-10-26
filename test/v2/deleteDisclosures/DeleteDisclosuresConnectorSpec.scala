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

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.models.domain.{Nino, TaxYear}
import api.models.errors.{DownstreamErrorCode, DownstreamErrors}
import api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v2.deleteDisclosures.def1.model.request.Def1_DeleteDisclosuresRequestData
import v2.deleteDisclosures.model.request.DeleteDisclosuresRequestData

import scala.concurrent.Future

class DeleteDisclosuresConnectorSpec extends ConnectorSpec {

  private val nino: Nino       = Nino("AA123456A")
  private val taxYear: TaxYear = TaxYear.fromMtd("2026-27")

  private val requestData: DeleteDisclosuresRequestData = Def1_DeleteDisclosuresRequestData(
    nino = nino,
    taxYear = taxYear
  )

  "DeleteDisclosuresConnector" when {
    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new HipTest with Test {
        val outcome: DownstreamOutcome[Unit] = Right(ResponseWrapper(correlationId, ()))

        willDelete(url"$baseUrl/itsd/disclosures/$nino/${taxYear.asMtd}").returns(Future.successful(outcome))

        await(connector.deleteDisclosures(requestData)) shouldBe outcome
      }
    }

    "a request supplied returns a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new HipTest with Test {
        val downstreamErrorResponse: DownstreamErrors = DownstreamErrors.single(DownstreamErrorCode("SOME_ERROR"))
        val outcome: DownstreamOutcome[Unit]          = Left(ResponseWrapper(correlationId, downstreamErrorResponse))

        willDelete(url"$baseUrl/itsd/disclosures/$nino/${taxYear.asMtd}").returns(Future.successful(outcome))

        await(connector.deleteDisclosures(requestData)) shouldBe outcome
      }
    }

    "a request supplied returns multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new HipTest with Test {
        val downstreamErrorResponse: DownstreamErrors = DownstreamErrors(
          List(DownstreamErrorCode("SOME_ERROR_1"), DownstreamErrorCode("SOME_ERROR_2"))
        )
        val outcome: DownstreamOutcome[Unit] = Left(ResponseWrapper(correlationId, downstreamErrorResponse))

        willDelete(url"$baseUrl/itsd/disclosures/$nino/${taxYear.asMtd}").returns(Future.successful(outcome))

        await(connector.deleteDisclosures(requestData)) shouldBe outcome
      }
    }
  }

  private trait Test extends ConnectorTest {

    val connector: DeleteDisclosuresConnector = new DeleteDisclosuresConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

}
