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

package v2.connectors

import api.connectors.ConnectorSpec
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import v2.models.request.delete.DeleteDisclosuresRequestData

import scala.concurrent.Future

class DeleteDisclosuresConnectorSpec extends ConnectorSpec {
  private val nino: String    = "AA111111A"
  private val taxYear: String = "2021-22"

  "DeleteDisclosuresConnector" when {
    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Ifs1Test with Test {

        val expected = Right(ResponseWrapper(correlationId, ()))

        MockedHttpClient
          .delete(
            url = s"$baseUrl/income-tax/disclosures/$nino/$taxYear",
            config = dummyHeaderCarrierConfig,
            requiredHeaders = requiredIfs1Headers,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        await(connector.deleteDisclosures(request)) shouldBe expected
      }
    }

    "A request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Ifs1Test with Test {

        val expected = Left(ResponseWrapper(correlationId, NinoFormatError))

        MockedHttpClient
          .delete(
            url = s"$baseUrl/income-tax/disclosures/$nino/$taxYear",
            config = dummyHeaderCarrierConfig,
            requiredHeaders = requiredIfs1Headers,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        await(connector.deleteDisclosures(request)) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Ifs1Test with Test {
        val expected = Left(ResponseWrapper(correlationId, Seq(NinoFormatError, InternalError, TaxYearFormatError)))

        MockedHttpClient
          .delete(
            url = s"$baseUrl/income-tax/disclosures/$nino/$taxYear",
            config = dummyHeaderCarrierConfig,
            requiredHeaders = requiredIfs1Headers,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        await(connector.deleteDisclosures(request)) shouldBe expected
      }
    }
  }

  trait Test {
    _: ConnectorTest =>

    protected val request: DeleteDisclosuresRequestData = DeleteDisclosuresRequestData(
      nino = Nino(nino),
      taxYear = TaxYear.fromMtd(taxYear)
    )

    val connector: DeleteDisclosuresConnector = new DeleteDisclosuresConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

}
