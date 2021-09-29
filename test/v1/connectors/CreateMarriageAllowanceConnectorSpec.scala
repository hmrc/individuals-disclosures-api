/*
 * Copyright 2021 HM Revenue & Customs
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
import v1.mocks.MockHttpClient
import v1.models.domain.Nino
import v1.models.outcomes.ResponseWrapper
import v1.models.request.marriageAllowance.{CreateMarriageAllowanceBody, CreateMarriageAllowanceRequest}

import scala.concurrent.Future

class CreateMarriageAllowanceConnectorSpec extends ConnectorSpec {

  private val nino: String = "AA111111A"

  private val requestBodyModel = CreateMarriageAllowanceBody("TC663795B", Some("John"), "Smith", Some("1987-10-18"))

  val createMarriageAllowanceRequest: CreateMarriageAllowanceRequest = CreateMarriageAllowanceRequest(
      nino = Nino(nino),
      body = requestBodyModel
  )

  class Test extends MockHttpClient with MockAppConfig {
    val connector: CreateMarriageAllowanceConnector = new CreateMarriageAllowanceConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.ifs2BaseUrl returns baseUrl
    MockAppConfig.ifs2Token returns "ifs2-token"
    MockAppConfig.ifs2Environment returns "ifs2-environment"
    MockAppConfig.ifs2EnvironmentHeaders returns Some(allowedIfs2Headers)
  }

  "CreateMarriageAllowanceConnector.create" should {
    "return a 201 status" when {
      "a valid request is supplied" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredIfsHeadersPost: Seq[(String, String)] = requiredIfs2Headers ++ Seq("Content-Type" -> "application/json")

        MockedHttpClient
          .post(
            url = s"$baseUrl/income-tax/marriage-allowance/claim/nino/$nino",
            config = dummyIfs2HeaderCarrierConfig,
            body = createMarriageAllowanceRequest.body,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(outcome))

        await(connector.create(createMarriageAllowanceRequest)) shouldBe outcome
      }
    }
  }
}
