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

package v1.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class CreateMarriageAllowanceControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino1: String = "AA123456A"
    val nino2: String = "BB123456B"
    val correlationId: String = "X-123"

    val requestBodyJson: JsValue = Json.parse(
      s"""
        |{
        |  "spouseOrCivilPartnerNino": $nino2,
        |  "spouseOrCivilPartnerFirstName": "John",
        |  "spouseOrCivilPartnerSurname": "Smith",
        |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
        |}
      """.stripMargin
    )

    def uri: String = s"/marriage-allowance/$nino1"

    def desUri: String = s"/income-tax/marriage-allowance/claim/NINO/$nino1"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }
  }

  "Calling the 'amend disclosures' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino1)
          DesStub.onSuccess(DesStub.PUT, desUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.body[JsValue] shouldBe ""
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 400 with multiple errors" when {
      "all field value validations fail on the request body" in new Test {

        val allInvalidValueRequestBodyJson: JsValue = Json.parse(
          s"""
             |{
             |  "spouseOrCivilPartnerNino": ${nino2 + "r"},
             |  "spouseOrCivilPartnerFirstName": "John",
             |  "spouseOrCivilPartnerSurname": "Smith",
             |  "spouseOrCivilPartnerDateOfBirth": "1943286-04-06"
             |}
          """.stripMargin
        )

        val allInvalidValueRequestError: List[MtdError] = List(

        )

        val wrappedErrors: ErrorWrapper = ErrorWrapper(
          correlationId = correlationId,
          error = BadRequestError,
          errors = Some(allInvalidValueRequestError)
        )

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino1)
        }

        val response: WSResponse = await(request().put(allInvalidValueRequestBodyJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(wrappedErrors)
      }
    }

    "return error according to spec" when {

      val nino2: String = "BB123456B"

      val validRequestBodyJson: JsValue = Json.parse(
        s"""
          |{
          |  "spouseOrCivilPartnerNino": $nino2,
          |  "spouseOrCivilPartnerFirstName": "John",
          |  "spouseOrCivilPartnerSurname": "Smith",
          |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
          |}
        """.stripMargin
      )

      val nonsenseRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "field": "value"
          |}
        """.stripMargin
      )

      val emptyBodyJson: JsValue = Json.parse(
        """
          |{
          |}
        """.stripMargin
      )

      val invalidNinoBodyJson: JsValue = Json.parse(
        s"""
          |{
          |  "spouseOrCivilPartnerNino": ${nino2 + "r"},
          |  "spouseOrCivilPartnerFirstName": "John",
          |  "spouseOrCivilPartnerSurname": "Smith",
          |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
          |}
        """.stripMargin
      )

      val invalidDobBodyJson: JsValue = Json.parse(
        s"""
          |{
          |  "spouseOrCivilPartnerNino": $nino2,
          |  "spouseOrCivilPartnerFirstName": "John",
          |  "spouseOrCivilPartnerSurname": "Smith",
          |  "spouseOrCivilPartnerDateOfBirth": "1983256-04-06"
          |}
        """.stripMargin
      )

      "validation error" when {
        def validationErrorTest(requestNino: String, requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation $requestNino fails with ${expectedBody.code} error" in new Test {

            override val nino1: String = requestNino
            override val requestBodyJson: JsValue = requestBody

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino1)
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("AA1123A", validRequestBodyJson, BAD_REQUEST, NinoFormatError),
          ("AA123456A", nonsenseRequestBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123458A", emptyBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123457A", invalidNinoBodyJson, BAD_REQUEST, ???),
          ("AA123459A", invalidDobBodyJson, BAD_REQUEST, ???),
        )

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "des service error" when {
        def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"des returns an $desCode error and status $desStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino1)
              DesStub.onError(DesStub.PUT, desUri, desStatus, errorBody(desCode))
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |   "code": "$code",
             |   "reason": "des message"
             |}
            """.stripMargin

        val input = Seq(
          (BAD_REQUEST, "INVALID_IDTYPE", BAD_REQUEST, DownstreamError),
          (BAD_REQUEST, "INVALID_IDVALUE", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "END_DATE_CODE_NOT_FOUND", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "NINO_OR_TRN_NOT_FOUND", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_ACTUAL_END_DATE", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_PARTICIPANT_END_DATE", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_PARTICIPANT_START_DATE", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "DECEASED_PARTICIPANT", INTERNAL_SERVER_ERROR, RuleDeceasedRecipientError),
          (BAD_REQUEST, "INVALID_RELATIONSHIP_CODE", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "PARTICIPANT1_CANNOT_BE_UPDATED", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "PARTICIPANT2_CANNOT_BE_UPDATED", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "RELATIONSHIP_ALREADY_EXISTS", INTERNAL_SERVER_ERROR, RuleActiveMarriageAllowanceClaimError),
          (BAD_REQUEST, "CONFIDENCE_CHECK_FAILED", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "CONFIDENCE_CHECK_SURNAME_MISSED", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "SERVER_ERROR", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "BAD_GATEWAY", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, DownstreamError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}