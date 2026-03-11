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

package v2.connectors

import api.connectors.ConnectorSpec
import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v2.models.request.amend.*

import scala.concurrent.Future

class AmendDisclosuresConnectorSpec extends ConnectorSpec {
  private val nino: String    = "AA111111A"
  private val taxYear: String = "2021-22"

  "amendDisclosures" when {
    "given a valid request" must {
      "return a success response" in new HipTest with Test {
        val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        willPut(url = url"$baseUrl/itsd/disclosures/$nino/$taxYear", body = request.body)
          .returns(Future.successful(expected))

        await(connector.amendDisclosures(request)) shouldBe expected
      }
    }

    "given a request returning an error" must {
      "return an unsuccessful response with the correct correlationId and a single error" in new HipTest with Test {
        val expected: Left[ResponseWrapper[NinoFormatError.type], Nothing] = Left(ResponseWrapper(correlationId, NinoFormatError))

        willPut(url = url"$baseUrl/itsd/disclosures/$nino/$taxYear", body = request.body).returns(Future.successful(expected))

        await(connector.amendDisclosures(request)) shouldBe expected
      }
    }

    "given a request returning multiple errors" must {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new HipTest with Test {
        val expected: Left[ResponseWrapper[Seq[MtdError]], Nothing] =
          Left(ResponseWrapper(correlationId, Seq(NinoFormatError, InternalError, TaxYearFormatError)))

        willPut(url = url"$baseUrl/itsd/disclosures/$nino/$taxYear", body = request.body).returns(Future.successful(expected))

        await(connector.amendDisclosures(request)) shouldBe expected
      }
    }
  }

  trait Test {
    self: ConnectorTest =>

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
