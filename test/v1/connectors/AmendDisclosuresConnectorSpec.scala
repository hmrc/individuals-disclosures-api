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

package v1.connectors

import mocks.MockAppConfig
import uk.gov.hmrc.domain.Nino
import v1.mocks.MockHttpClient
import v1.models.outcomes.ResponseWrapper
import v1.models.request.disclosures._

import scala.concurrent.Future

class AmendDisclosuresConnectorSpec extends ConnectorSpec {

  private val nino: String = "AA111111A"
  private val taxYear: String = "2020-21"

  val amendTaxAvoidance: AmendTaxAvoidance = AmendTaxAvoidance("14211123","2020-21")
  val class2Nics: AmendClass2Nics = AmendClass2Nics(true)

  val amendDisclosuresRequest: AmendDisclosuresRequest = AmendDisclosuresRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    body = AmendDisclosuresRequestBody(Some(Seq(amendTaxAvoidance)), Some(class2Nics))
  )

  class Test extends MockHttpClient with MockAppConfig {

    val connector: AmendDisclosuresConnector = new AmendDisclosuresConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "AmendDisclosuresConnector" when {
    "amendDisclosures" must {
      "return a 204 status for a success scenario" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockedHttpClient
          .put(
            url = s"$baseUrl/income-tax/disclosures/$nino/$taxYear",
            body = amendDisclosuresRequest.body,
            requiredHeaders = "Environment" -> "des-environment", "Authorization" -> s"Bearer des-token"
          ).returns(Future.successful(outcome))

        await(connector.amendDisclosures(amendDisclosuresRequest)) shouldBe outcome
      }
    }
  }
}