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

import api.connectors.ConnectorSpec
import api.models.domain.Nino
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import v1.models.request.create.{CreateMarriageAllowanceRequestBody, CreateMarriageAllowanceRequestData}

import scala.concurrent.Future

class CreateMarriageAllowanceConnectorSpec extends ConnectorSpec {

  private val nino: String = "AA111111A"

  "CreateMarriageAllowanceConnector" when {
    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Ifs2Test with Test {
        protected val outcome = Right(ResponseWrapper(correlationId, ()))

        willPost(url = s"$baseUrl/income-tax/marriage-allowance/claim/nino/$nino", body = request.body)
          .returns(Future.successful(outcome))

        val result = await(connector.create(request))

        result shouldBe outcome
      }
    }

    "A request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Ifs2Test with Test {

        val outcome = Left(ResponseWrapper(correlationId, NinoFormatError))

        willPost(url = s"$baseUrl/income-tax/marriage-allowance/claim/nino/$nino", body = request.body)
          .returns(Future.successful(outcome))

        await(connector.create(request)) shouldBe outcome
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Ifs2Test with Test {
        val outcome = Left(ResponseWrapper(correlationId, Seq(NinoFormatError, InternalError, TaxYearFormatError)))

        willPost(url = s"$baseUrl/income-tax/marriage-allowance/claim/nino/$nino", body = request.body)
          .returns(Future.successful(outcome))

        await(connector.create(request)) shouldBe outcome
      }
    }
  }

  trait Test {
    _: ConnectorTest =>

    protected val requestBody: CreateMarriageAllowanceRequestBody =
      CreateMarriageAllowanceRequestBody("TC663795B", None, "Smith", None)

    protected val request: CreateMarriageAllowanceRequestData = CreateMarriageAllowanceRequestData(
      nino = Nino(nino),
      body = requestBody
    )

    protected val connector: CreateMarriageAllowanceConnector = new CreateMarriageAllowanceConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

}
