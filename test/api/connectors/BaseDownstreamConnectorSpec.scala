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

package api.connectors

import api.connectors.DownstreamUri.{ Ifs1Uri, Ifs2Uri }
import api.models.outcomes.ResponseWrapper
import config.AppConfig
import mocks.{ MockAppConfig, MockHttpClient }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, HttpReads }

import scala.concurrent.Future

class BaseDownstreamConnectorSpec extends ConnectorSpec {

  // WLOG
  val body        = "body"
  val outcome     = Right(ResponseWrapper(correlationId, Result(2)))
  val url         = "some/url?param=value"
  val absoluteUrl = s"$baseUrl/$url"

  // WLOG
  case class Result(value: Int)

  implicit val httpReads: HttpReads[DownstreamOutcome[Result]] = mock[HttpReads[DownstreamOutcome[Result]]]

  class Ifs1BaseTest extends MockHttpClient with MockAppConfig {

    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient     = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockAppConfig.ifs1BaseUrl returns baseUrl
    MockAppConfig.ifs1Token returns "ifs1-token"
    MockAppConfig.ifs1Environment returns "ifs1-environment"
    MockAppConfig.ifs1EnvironmentHeaders returns Some(allowedIfs1Headers)

    val qps = Seq("param1" -> "value1")
  }

  class Ifs2BaseTest extends MockHttpClient with MockAppConfig {

    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient     = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockAppConfig.ifs2BaseUrl returns baseUrl
    MockAppConfig.ifs2Token returns "ifs2-token"
    MockAppConfig.ifs2Environment returns "ifs2-environment"
    MockAppConfig.ifs2EnvironmentHeaders returns Some(allowedIfs2Headers)

    val qps = Seq("param1" -> "value1")
  }

  "for IFS1" when {
    "post" must {
      "posts with the required ifs1 headers and returns the result" in new Ifs1BaseTest {
        implicit val hc: HeaderCarrier                    = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredIfsHeadersPost: Seq[(String, String)] = requiredIfs1Headers ++ Seq("Content-Type" -> "application/json")

        MockedHttpClient
          .post(absoluteUrl,
                config = dummyHeaderCarrierConfig,
                body,
                requiredHeaders = requiredIfsHeadersPost,
                excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.post(body, Ifs1Uri[Result](url))) shouldBe outcome
      }
    }

    "get" must {
      "get with the required ifs1 headers and return the result" in new Ifs1BaseTest {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))

        MockedHttpClient
          .get(absoluteUrl,
               config = dummyHeaderCarrierConfig,
               parameters = qps,
               requiredHeaders = requiredIfs1Headers,
               excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.get(Ifs1Uri[Result](url), queryParams = qps)) shouldBe outcome
      }
    }

    "delete" must {
      "delete with the required ifs1 headers and return the result" in new Ifs1BaseTest {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))

        MockedHttpClient
          .delete(absoluteUrl,
                  config = dummyHeaderCarrierConfig,
                  requiredHeaders = requiredIfs1Headers,
                  excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.delete(Ifs1Uri[Result](url))) shouldBe outcome
      }
    }

    "put" must {
      "put with the required ifs1 headers and return result" in new Ifs1BaseTest {
        implicit val hc: HeaderCarrier                   = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredIfsHeadersPut: Seq[(String, String)] = requiredIfs1Headers ++ Seq("Content-Type" -> "application/json")

        MockedHttpClient
          .put(absoluteUrl,
               config = dummyHeaderCarrierConfig,
               body,
               requiredHeaders = requiredIfsHeadersPut,
               excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.put(body, Ifs1Uri[Result](url))) shouldBe outcome
      }
    }

    "content-type header already present and set to be passed through" must {
      "override (not duplicate) the value" when {
        testNoDuplicatedContentType("Content-Type" -> "application/user-type")
        testNoDuplicatedContentType("content-type" -> "application/user-type")

        def testNoDuplicatedContentType(userContentType: (String, String)): Unit =
          s"for user content type header $userContentType" in new Ifs1BaseTest {
            implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq(userContentType))

            MockedHttpClient
              .put(
                absoluteUrl,
                config = dummyHeaderCarrierConfig,
                body,
                requiredHeaders = requiredIfs1Headers ++ Seq("Content-Type" -> "application/json"),
                excludedHeaders = Seq(userContentType)
              )
              .returns(Future.successful(outcome))

            await(connector.put(body, Ifs1Uri[Result](url))) shouldBe outcome
          }
      }
    }
  }

  "for IFS2" when {
    "post" must {
      "posts with the required ifs2 headers and returns the result" in new Ifs2BaseTest {
        implicit val hc: HeaderCarrier                     = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredIfs2HeadersPost: Seq[(String, String)] = requiredIfs2Headers ++ Seq("Content-Type" -> "application/json")

        MockedHttpClient
          .post(absoluteUrl,
                config = dummyHeaderCarrierConfig,
                body,
                requiredHeaders = requiredIfs2HeadersPost,
                excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.post(body, Ifs2Uri[Result](url))) shouldBe outcome
      }
    }

    "get" must {
      "get with the required ifs2 headers and return the result" in new Ifs2BaseTest {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))

        MockedHttpClient
          .get(absoluteUrl,
               config = dummyHeaderCarrierConfig,
               parameters = qps,
               requiredHeaders = requiredIfs2Headers,
               excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.get(Ifs2Uri[Result](url), queryParams = qps)) shouldBe outcome
      }
    }

    "delete" must {
      "delete with the required ifs2 headers and return the result" in new Ifs2BaseTest {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))

        MockedHttpClient
          .delete(absoluteUrl,
                  config = dummyHeaderCarrierConfig,
                  requiredHeaders = requiredIfs2Headers,
                  excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.delete(Ifs2Uri[Result](url))) shouldBe outcome
      }
    }

    "put" must {
      "put with the required ifs2 headers and return result" in new Ifs2BaseTest {
        implicit val hc: HeaderCarrier                   = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredIfsHeadersPut: Seq[(String, String)] = requiredIfs2Headers ++ Seq("Content-Type" -> "application/json")

        MockedHttpClient
          .put(absoluteUrl,
               config = dummyHeaderCarrierConfig,
               body,
               requiredHeaders = requiredIfsHeadersPut,
               excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(outcome))

        await(connector.put(body, Ifs2Uri[Result](url))) shouldBe outcome
      }
    }

    "content-type header already present and set to be passed through" must {
      "override (not duplicate) the value" when {
        testNoDuplicatedContentType("Content-Type" -> "application/user-type")
        testNoDuplicatedContentType("content-type" -> "application/user-type")

        def testNoDuplicatedContentType(userContentType: (String, String)): Unit =
          s"for user content type header $userContentType" in new Ifs2BaseTest {
            implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq(userContentType))

            MockedHttpClient
              .put(
                absoluteUrl,
                config = dummyHeaderCarrierConfig,
                body,
                requiredHeaders = requiredIfs2Headers ++ Seq("Content-Type" -> "application/json"),
                excludedHeaders = Seq(userContentType)
              )
              .returns(Future.successful(outcome))

            await(connector.put(body, Ifs2Uri[Result](url))) shouldBe outcome
          }
      }
    }
  }
}
