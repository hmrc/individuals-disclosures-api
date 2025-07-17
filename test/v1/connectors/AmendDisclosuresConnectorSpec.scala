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

package v1.connectors

import api.connectors.ConnectorSpec
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import config.MockAppConfig
import mocks.MockHttpClient
import play.api.Configuration
import uk.gov.hmrc.http.StringContextOps
import v1.models.request.amend._

import scala.concurrent.Future

class AmendDisclosuresConnectorSpec extends ConnectorSpec {
  private val nino: String    = "AA111111A"
  private val taxYear: String = "2021-22"

  "amendDisclosures" when {
    "given a valid request" must {
      "return a success response when feature switch is disabled (IFS enabled)" in new Ifs1Test with Test {
        MockedAppConfig.featureSwitches.returns(Configuration("ifs_hip_migration_1638.enabled" -> false))

        val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        willPut(url = url"$baseUrl/income-tax/disclosures/$nino/$taxYear", body = request.body).returns(Future.successful(expected))

        await(connector.amendDisclosures(request)) shouldBe expected
      }

      "return a success response when feature switch is enabled (HIP enabled)" in new HipTest with Test {
        MockedAppConfig.featureSwitches.returns(Configuration("ifs_hip_migration_1638.enabled" -> true))

        val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        willPut(url = url"$baseUrl/itsd/disclosures/$nino/$taxYear", body = request.body)
          .returns(Future.successful(expected))

        await(connector.amendDisclosures(request)) shouldBe expected
      }
    }

    "given a request returning an error" must {
      "return an unsuccessful response with the correct correlationId and a single error" in new HipTest with Test {
        MockedAppConfig.featureSwitches.returns(Configuration("ifs_hip_migration_1638.enabled" -> true))

        val expected: Left[ResponseWrapper[NinoFormatError.type], Nothing] = Left(ResponseWrapper(correlationId, NinoFormatError))

        willPut(url = url"$baseUrl/itsd/disclosures/$nino/$taxYear", body = request.body).returns(Future.successful(expected))

        await(connector.amendDisclosures(request)) shouldBe expected
      }
    }

    "given a request returning multiple errors" must {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new HipTest with Test {
        MockedAppConfig.featureSwitches.returns(Configuration("ifs_hip_migration_1638.enabled" -> true))

        val expected: Left[ResponseWrapper[Seq[MtdError]], Nothing] =
          Left(ResponseWrapper(correlationId, Seq(NinoFormatError, InternalError, TaxYearFormatError)))

        willPut(url = url"$baseUrl/itsd/disclosures/$nino/$taxYear", body = request.body).returns(Future.successful(expected))

        await(connector.amendDisclosures(request)) shouldBe expected
      }
    }
  }

  trait Test extends MockHttpClient with MockAppConfig {

    val requestBody: AmendDisclosuresRequestBody =
      AmendDisclosuresRequestBody(None, None)

    protected val request: AmendDisclosuresRequestData = AmendDisclosuresRequestData(
      nino = Nino(nino),
      taxYear = TaxYear.fromMtd(taxYear),
      body = requestBody
    )

    val connector: AmendDisclosuresConnector = new AmendDisclosuresConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

}
