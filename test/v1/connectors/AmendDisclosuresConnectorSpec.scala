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

package v1.connectors

import mocks.MockAppConfig
import uk.gov.hmrc.http.HeaderCarrier
import v1.models.domain.Nino
import v1.mocks.MockHttpClient
import v1.models.outcomes.ResponseWrapper
import v1.models.request.disclosures._

import scala.concurrent.Future

class AmendDisclosuresConnectorSpec extends ConnectorSpec {
  private val nino: String = "AA111111A"
  private val taxYear: String = "2021-22"

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

  class Test extends MockHttpClient with MockAppConfig {
    val connector: AmendDisclosuresConnector = new AmendDisclosuresConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.ifs1BaseUrl returns baseUrl
    MockAppConfig.ifs1Token returns "ifs1-token"
    MockAppConfig.ifs1Environment returns "ifs1-environment"
    MockAppConfig.ifs1EnvironmentHeaders returns Some(allowedIfs1Headers)
  }

  "AmendDisclosuresConnector" when {
    "amendDisclosures" must {
      "return a 204 status for a success scenario" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredIfsHeadersPut: Seq[(String, String)] = requiredIfs1Headers ++ Seq("Content-Type" -> "application/json")

        MockedHttpClient
          .put(
            url = s"$baseUrl/income-tax/disclosures/$nino/$taxYear",
            config = dummyIfs1HeaderCarrierConfig,
            body = amendDisclosuresRequest.body,
            requiredHeaders = requiredIfsHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(outcome))

        await(connector.amendDisclosures(amendDisclosuresRequest)) shouldBe outcome
      }
    }
  }
}