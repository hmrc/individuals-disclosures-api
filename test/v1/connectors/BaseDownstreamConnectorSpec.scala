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

import config.AppConfig
import mocks.MockAppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import v1.mocks.MockHttpClient
import v1.models.outcomes.ResponseWrapper

import scala.concurrent.Future

class BaseDownstreamConnectorSpec extends ConnectorSpec {
  // WLOG
  case class Result(value: Int)

  // WLOG
  val body = "body"
  val outcome = Right(ResponseWrapper(correlationId, Result(2)))

  val url = "some/url?param=value"
  val absoluteUrl = s"$baseUrl/$url"

  implicit val httpReads: HttpReads[DesOutcome[Result]] = mock[HttpReads[DesOutcome[Result]]]

  class Test extends MockHttpClient with MockAppConfig {
    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockAppConfig.desBaseUrl returns baseUrl
    MockAppConfig.desToken returns "des-token"
    MockAppConfig.desEnvironment returns "des-environment"
    MockAppConfig.desEnvironmentHeaders returns Some(allowedDesHeaders)
  }

  "BaseDownstreamConnector" when {
    "making a HTTP request to an internal service" must {
      val dummyInternalHeaderCarrierConfig: HeaderCarrier.Config =
        dummyDesHeaderCarrierConfig.copy(
          internalHostPatterns = Seq(("^" + "test-BaseUrl" + "$").r)
        )

      val requiredInternalHeaders: Seq[(String, String)] = Seq(
        "Environment" -> "des-environment",
        "Authorization" -> s"Bearer des-token",
        "User-Agent" -> "individual-disclosures-api",
        "CorrelationId" -> correlationId
      )

      testHttpMethods(dummyInternalHeaderCarrierConfig, requiredInternalHeaders)
    }

    "making a HTTP request to an external service (i.e DES)" must {
      testHttpMethods(dummyDesHeaderCarrierConfig, requiredDesHeaders)
    }
  }

  def testHttpMethods(config: HeaderCarrier.Config, requiredHeaders: Seq[(String, String)]): Unit = {
    "complete the request successfully with the required headers" when {
      "GET" in new Test {
        MockedHttpClient
          .get(absoluteUrl, config, requiredHeaders :_*)
          .returns(Future.successful(outcome))

        await(connector.get(DesUri[Result](url))) shouldBe outcome
      }

      "DELETE" in new Test {
        MockedHttpClient
          .delete(absoluteUrl, config, requiredHeaders :_*)
          .returns(Future.successful(outcome))

        await(connector.delete(DesUri[Result](url))) shouldBe outcome
      }

      "PUT" in new Test {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq("Content-Type" -> "application/json"))
        val requiredHeadersPut: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockedHttpClient.put(absoluteUrl, config, body, requiredHeadersPut:_*)
          .returns(Future.successful(outcome))

        await(connector.put(body, DesUri[Result](url))) shouldBe outcome
      }
    }
  }
}