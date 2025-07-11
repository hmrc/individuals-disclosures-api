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

package v2

import api.models.errors
import api.models.errors.{MtdError, NinoFormatError, NotFoundError, RuleOutsideAmendmentWindow, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, RuleVoluntaryClass2CannotBeChangedError, TaxYearFormatError}
import api.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import api.support.IntegrationBaseSpec
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT, SERVICE_UNAVAILABLE, UNPROCESSABLE_ENTITY}

class DeleteDisclosuresControllerHipISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456A"
    val taxYear: String = "2021-22"

    private def uri: String = s"/$nino/$taxYear"

    def HipUri: String = s"/itsd/disclosures/$nino/$taxYear"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

  "Calling the 'delete disclosures' endpoint" should {
    "return a 204 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.DELETE, HipUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().delete())
        response.status shouldBe NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {

      "validation error" when {
        def validationErrorTest(requestNino: String, requestTaxYear: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String = requestNino
            override val taxYear: String = requestTaxYear

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().delete())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = Seq(
          ("AA1123A", "2021-22", BAD_REQUEST, NinoFormatError),
          ("AA123456A", "2017-18", BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "20177", BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2015-17", BAD_REQUEST, RuleTaxYearRangeInvalidError)
        )
        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "ifs service error" when {
        def serviceErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"ifs returns an $ifsCode error and status $ifsStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.DELETE, HipUri, ifsStatus, errorBody(ifsCode))
            }

            val response: WSResponse = await(request().delete())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |   "origin": "HIP",
             |   "response": [
             |       {
             |          "errorCode": "$code",
             |          "errorDescription": "hip error"
             |       }
             |   ]
             |}
            """.stripMargin

        val input = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, errors.InternalError),
          (NOT_FOUND, "NO_DATA_FOUND", NOT_FOUND, NotFoundError),
          (UNPROCESSABLE_ENTITY, "VOLUNTARY_CLASS2_CANNOT_BE_CHANGED", BAD_REQUEST, RuleVoluntaryClass2CannotBeChangedError),
          (UNPROCESSABLE_ENTITY, "OUTSIDE_AMENDMENT_WINDOW", BAD_REQUEST, RuleOutsideAmendmentWindow),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, errors.InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, errors.InternalError)
        )
        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}
