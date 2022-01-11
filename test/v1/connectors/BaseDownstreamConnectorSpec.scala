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

package v1.connectors

import config.AppConfig
import mocks.MockAppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import v1.connectors.DownstreamUri.{Ifs1Uri, Ifs2Uri}
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

  implicit val httpReads: HttpReads[DownstreamOutcome[Result]] = mock[HttpReads[DownstreamOutcome[Result]]]

  class Ifs1Test(ifs1EnvironmentHeaders: Option[Seq[String]]) extends MockHttpClient with MockAppConfig {
    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockAppConfig.ifs1BaseUrl returns baseUrl
    MockAppConfig.ifs1Token returns "ifs1-token"
    MockAppConfig.ifs1Environment returns "ifs1-environment"
    MockAppConfig.ifs1EnvironmentHeaders returns ifs1EnvironmentHeaders
  }

  "BaseDownstreamConnector" when {
    val requiredHeaders: Seq[(String, String)] = Seq(
      "Environment" -> "ifs1-environment",
      "Authorization" -> s"Bearer ifs1-token",
      "User-Agent" -> "individual-disclosures-api",
      "CorrelationId" -> correlationId,
      "Gov-Test-Scenario" -> "DEFAULT"
    )

    val excludedHeaders: Seq[(String, String)] = Seq(
      "AnotherHeader" -> "HeaderValue"
    )

    "making a HTTP request to a downstream service (i.e DES)" must {
      ifs1TestHttpMethods(dummyIfs1HeaderCarrierConfig, requiredHeaders, excludedHeaders, Some(allowedIfs1Headers))

      "exclude all `otherHeaders` when no external service header allow-list is found" should {
        val requiredHeaders: Seq[(String, String)] = Seq(
          "Environment" -> "ifs1-environment",
          "Authorization" -> s"Bearer ifs1-token",
          "User-Agent" -> "individual-disclosures-api",
          "CorrelationId" -> correlationId,
        )

        ifs1TestHttpMethods(dummyIfs1HeaderCarrierConfig, requiredHeaders, otherHeaders, None)
      }
    }
}

  def ifs1TestHttpMethods(config: HeaderCarrier.Config,
                      requiredHeaders: Seq[(String, String)],
                      excludedHeaders: Seq[(String, String)],
                      ifs1EnvironmentHeaders: Option[Seq[String]]): Unit = {

    "complete the request successfully with the required headers" when {
      "GET" in new Ifs1Test(ifs1EnvironmentHeaders) {
        MockedHttpClient
          .get(absoluteUrl, config, requiredHeaders, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.get(Ifs1Uri[Result](url))) shouldBe outcome
      }

      "DELETE" in new Ifs1Test(ifs1EnvironmentHeaders) {
        MockedHttpClient
          .delete(absoluteUrl, config, requiredHeaders, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.delete(Ifs1Uri[Result](url))) shouldBe outcome
      }

      "PUT" in new Ifs1Test(ifs1EnvironmentHeaders) {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPut: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockedHttpClient.put(absoluteUrl, config, body, requiredHeadersPut, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.put(body, Ifs1Uri[Result](url))) shouldBe outcome
      }
    }
  }

  class Ifs2Test(ifs2EnvironmentHeaders: Option[Seq[String]]) extends MockHttpClient with MockAppConfig {
    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockAppConfig.ifs2BaseUrl returns baseUrl
    MockAppConfig.ifs2Token returns "ifs2-token"
    MockAppConfig.ifs2Environment returns "ifs2-environment"
    MockAppConfig.ifs2EnvironmentHeaders returns ifs2EnvironmentHeaders
  }

  "BaseDownstreamConnector" when {
    val requiredHeaders: Seq[(String, String)] = Seq(
      "Environment" -> "ifs2-environment",
      "Authorization" -> s"Bearer ifs2-token",
      "User-Agent" -> "individual-disclosures-api",
      "CorrelationId" -> correlationId,
      "Gov-Test-Scenario" -> "DEFAULT"
    )

    val excludedHeaders: Seq[(String, String)] = Seq(
      "AnotherHeader" -> "HeaderValue"
    )

    "making a HTTP request to a downstream service (i.e DES)" must {
      ifs2TestHttpMethods(dummyIfs1HeaderCarrierConfig, requiredHeaders, excludedHeaders, Some(allowedIfs1Headers))

      "exclude all `otherHeaders` when no external service header allow-list is found" should {
        val requiredHeaders: Seq[(String, String)] = Seq(
          "Environment" -> "ifs2-environment",
          "Authorization" -> s"Bearer ifs2-token",
          "User-Agent" -> "individual-disclosures-api",
          "CorrelationId" -> correlationId,
        )

        ifs2TestHttpMethods(dummyIfs1HeaderCarrierConfig, requiredHeaders, otherHeaders, None)
      }
    }
  }

  def ifs2TestHttpMethods(config: HeaderCarrier.Config,
                      requiredHeaders: Seq[(String, String)],
                      excludedHeaders: Seq[(String, String)],
                      ifs2EnvironmentHeaders: Option[Seq[String]]): Unit = {

    "complete the request successfully with the required headers" when {
      "POST" in new Ifs2Test(ifs2EnvironmentHeaders) {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPost: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockedHttpClient.post(absoluteUrl, config, body, requiredHeadersPost, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.post(body, Ifs2Uri[Result](url))) shouldBe outcome
      }
    }
  }
}