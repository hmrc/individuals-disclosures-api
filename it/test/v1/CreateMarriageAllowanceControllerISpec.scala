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

package v1

import api.models.errors
import api.models.errors.*
import api.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import api.support.IntegrationBaseSpec
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.json.{JsResult, JsSuccess, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*

class CreateMarriageAllowanceControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino1: String         = "AA123456A"
    private val nino2: String = "BB123456B"
    val invalidNino: String   = "BB123456Br"
    val correlationId: String = "X-123"

    val requestBodyJson: JsValue = Json.parse(
      s"""
         |{
         |  "spouseOrCivilPartnerNino": "$nino2",
         |  "spouseOrCivilPartnerFirstName": "John",
         |  "spouseOrCivilPartnerSurname": "Smith",
         |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
         |}
      """.stripMargin
    )

    private def uri: String = s"/marriage-allowance/$nino1"

    def ifs2Uri: String = s"/income-tax/marriage-allowance/claim/nino/$nino1"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

  "Calling the 'create marriage allowance' endpoint" should {
    "return a 201 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino1)
          DownstreamStub.onSuccess(DownstreamStub.POST, ifs2Uri, NO_CONTENT)
        }

        val response: WSResponse = await(request().post(requestBodyJson))
        response.status shouldBe CREATED
        response.body[String] shouldBe ""
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return a 400 with multiple errors" when {
      "all field value validations fail on the request body" in new Test {

        val allInvalidValueRequestBodyJson: JsValue = Json.parse(
          s"""
             |{
             |  "spouseOrCivilPartnerNino": "$invalidNino",
             |  "spouseOrCivilPartnerFirstName": "Johny1n4|y8nx34tij8",
             |  "spouseOrCivilPartnerSurname": "Smith47cyw-i|teqytya",
             |  "spouseOrCivilPartnerDateOfBirth": "1943286-04-06"
             |}
          """.stripMargin
        )

        val allInvalidValueRequestError: List[MtdError] = List(
          PartnerDoBFormatError,
          PartnerNinoFormatError,
          PartnerFirstNameFormatError,
          PartnerSurnameFormatError
        )

        val wrappedErrors: ErrorWrapper = ErrorWrapper(
          correlationId = correlationId,
          error = BadRequestError,
          errors = Some(allInvalidValueRequestError)
        )

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino1)
        }

        val response: WSResponse = await(request().post(allInvalidValueRequestBodyJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(wrappedErrors)
      }
    }

    "return error according to spec" when {

      val nino2: String       = "BB123456B"
      val invalidNino: String = "BB123456Br"

      val validRequestBodyJson: JsValue = Json.parse(
        s"""
           |{
           |  "spouseOrCivilPartnerNino": "$nino2",
           |  "spouseOrCivilPartnerFirstName": "John",
           |  "spouseOrCivilPartnerSurname": "Smith",
           |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
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
           |  "spouseOrCivilPartnerNino": "$invalidNino",
           |  "spouseOrCivilPartnerFirstName": "John",
           |  "spouseOrCivilPartnerSurname": "Smith",
           |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
           |}
        """.stripMargin
      )

      val invalidFirstNameBodyJson: JsValue = Json.parse(
        s"""
           |{
           |  "spouseOrCivilPartnerNino": "$nino2",
           |  "spouseOrCivilPartnerFirstName": "Jo37uwfuwjgqof87?@£%£&^*%hn",
           |  "spouseOrCivilPartnerSurname": "Smith",
           |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
           |}
        """.stripMargin
      )

      val invalidSurnameBodyJson: JsValue = Json.parse(
        s"""
           |{
           |  "spouseOrCivilPartnerNino": "$nino2",
           |  "spouseOrCivilPartnerFirstName": "John",
           |  "spouseOrCivilPartnerSurname": "Smi37uwfuwjgqof87?@£%£&^*%th",
           |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
           |}
        """.stripMargin
      )

      val invalidDobBodyJson: JsValue = Json.parse(
        s"""
           |{
           |  "spouseOrCivilPartnerNino": "$nino2",
           |  "spouseOrCivilPartnerFirstName": "John",
           |  "spouseOrCivilPartnerSurname": "Smith",
           |  "spouseOrCivilPartnerDateOfBirth": "1983256-04-06"
           |}
        """.stripMargin
      )

      "validation error" when {
        def validationErrorTest(requestNino: String, requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation $requestNino fails with ${expectedBody.code} error" in new Test {

            override val nino1: String            = requestNino
            override val requestBodyJson: JsValue = requestBody

            override def setupStubs(): StubMapping = {
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino1)
            }

            val response: WSResponse = await(request().post(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("AA1123A", validRequestBodyJson, BAD_REQUEST, NinoFormatError),
          ("AA123458A", emptyBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123457A", invalidNinoBodyJson, BAD_REQUEST, PartnerNinoFormatError),
          ("AA123457A", invalidFirstNameBodyJson, BAD_REQUEST, PartnerFirstNameFormatError),
          ("AA123457A", invalidSurnameBodyJson, BAD_REQUEST, PartnerSurnameFormatError),
          ("AA123459A", invalidDobBodyJson, BAD_REQUEST, PartnerDoBFormatError)
        )
        input.foreach(validationErrorTest.tupled)

        "with complex body format errors" in new Test {
          val nonsenseBodyPaths: List[String] = List("/spouseOrCivilPartnerNino", "/spouseOrCivilPartnerSurname")

          val nonsenseRequestBodyJson: JsValue = Json.parse(
            """
              |{
              |   "field": "value"
              |}
            """.stripMargin
          )

          override def setupStubs(): StubMapping = {
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino1)
          }

          val response: WSResponse = await(request().post(nonsenseRequestBodyJson))
          response.status shouldBe BAD_REQUEST

          val responseErrorCode: JsResult[String]       = (response.json \ "code").validate[String]
          val responseErrorMessage: JsResult[String]    = (response.json \ "message").validate[String]
          val responseErrorPaths: JsResult[Seq[String]] = (response.json \ "paths").validate[Seq[String]]

          responseErrorCode shouldBe a[JsSuccess[?]]
          responseErrorMessage shouldBe a[JsSuccess[?]]
          responseErrorPaths shouldBe a[JsSuccess[?]]

          responseErrorCode.get shouldBe RuleIncorrectOrEmptyBodyError.code
          responseErrorMessage.get shouldBe RuleIncorrectOrEmptyBodyError.message
          responseErrorPaths.get should contain.allElementsOf(nonsenseBodyPaths)
        }
      }

      "ifs service error" when {
        def serviceErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"ifs returns an $ifsCode error and status $ifsStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino1)
              DownstreamStub.onError(DownstreamStub.POST, ifs2Uri, ifsStatus, errorBody(ifsCode))
            }

            val response: WSResponse = await(request().post(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |   "code": "$code",
             |   "reason": "ifs2 message"
             |}
            """.stripMargin

        val input = Seq(
          (BAD_REQUEST, "INVALID_IDTYPE", INTERNAL_SERVER_ERROR, errors.InternalError),
          (BAD_REQUEST, "INVALID_IDVALUE", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, errors.InternalError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, errors.InternalError),
          (NOT_FOUND, "END_DATE_CODE_NOT_FOUND", INTERNAL_SERVER_ERROR, errors.InternalError),
          (NOT_FOUND, "NINO_OR_TRN_NOT_FOUND", BAD_REQUEST, RuleInvalidRequestError),
          (UNPROCESSABLE_ENTITY, "INVALID_ACTUAL_END_DATE", INTERNAL_SERVER_ERROR, errors.InternalError),
          (UNPROCESSABLE_ENTITY, "INVALID_PARTICIPANT_END_DATE", INTERNAL_SERVER_ERROR, errors.InternalError),
          (UNPROCESSABLE_ENTITY, "INVALID_PARTICIPANT_START_DATE", INTERNAL_SERVER_ERROR, errors.InternalError),
          (UNPROCESSABLE_ENTITY, "DECEASED_PARTICIPANT", BAD_REQUEST, RuleDeceasedRecipientError),
          (UNPROCESSABLE_ENTITY, "INVALID_RELATIONSHIP_CODE", INTERNAL_SERVER_ERROR, errors.InternalError),
          (UNPROCESSABLE_ENTITY, "PARTICIPANT1_CANNOT_BE_UPDATED", INTERNAL_SERVER_ERROR, errors.InternalError),
          (UNPROCESSABLE_ENTITY, "PARTICIPANT2_CANNOT_BE_UPDATED", INTERNAL_SERVER_ERROR, errors.InternalError),
          (UNPROCESSABLE_ENTITY, "RELATIONSHIP_ALREADY_EXISTS", BAD_REQUEST, RuleActiveMarriageAllowanceClaimError),
          (UNPROCESSABLE_ENTITY, "CONFIDENCE_CHECK_FAILED", INTERNAL_SERVER_ERROR, errors.InternalError),
          (UNPROCESSABLE_ENTITY, "CONFIDENCE_CHECK_SURNAME_MISSED", INTERNAL_SERVER_ERROR, errors.InternalError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, errors.InternalError),
          (BAD_GATEWAY, "BAD_GATEWAY", INTERNAL_SERVER_ERROR, errors.InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, errors.InternalError)
        )
        input.foreach(serviceErrorTest.tupled)
      }
    }
  }

}
