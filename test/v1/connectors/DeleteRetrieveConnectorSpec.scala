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
import play.api.libs.json.{Json, Reads}
import v1.connectors.DownstreamUri.Ifs1Uri
import v1.mocks.MockHttpClient
import v1.models.outcomes.ResponseWrapper

import scala.concurrent.Future

class DeleteRetrieveConnectorSpec extends ConnectorSpec {
  val nino: String = "AA111111A"
  val taxYear: String = "2021-22"

  class Test extends MockHttpClient with MockAppConfig {
    val connector: DeleteRetrieveConnector = new DeleteRetrieveConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.ifs1BaseUrl returns baseUrl
    MockAppConfig.ifs1Token returns "ifs1-token"
    MockAppConfig.ifs1Environment returns "ifs1-environment"
    MockAppConfig.ifs1EnvironmentHeaders returns Some(allowedIfs1Headers)
  }

  "DeleteRetrieveConnector" when {
    "delete" must {
      "return a 204 status for a success scenario" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))
        implicit val ifs1Uri: Ifs1Uri[Unit] = Ifs1Uri[Unit](s"income-tax/disclosures/$nino/$taxYear")

        MockedHttpClient
          .delete(
            url = s"$baseUrl/income-tax/disclosures/$nino/$taxYear",
            config = dummyIfs1HeaderCarrierConfig,
            requiredHeaders = requiredIfs1Headers,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(outcome))

        await(connector.delete()) shouldBe outcome
      }
    }

    "retrieve" must {
      "return a 200 status for a success scenario" in new Test {
        case class Data(field: String)

        object Data {
          implicit val reads: Reads[Data] = Json.reads[Data]
        }

        val outcome = Right(ResponseWrapper(correlationId, Data("value")))
        implicit val ifs1Uri: Ifs1Uri[Data] = Ifs1Uri[Data](s"income-tax/disclosures/$nino/$taxYear")

        MockedHttpClient
          .get(
            url = s"$baseUrl/income-tax/disclosures/$nino/$taxYear",
            config = dummyIfs1HeaderCarrierConfig,
            requiredHeaders = requiredIfs1Headers,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(outcome))

        await(connector.retrieve[Data]()) shouldBe outcome
      }
    }
  }
}