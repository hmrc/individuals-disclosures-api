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

package v1.controllers

import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.MockIdGenerator
import v1.mocks.requestParsers.MockCreateMarriageAllowanceRequestParser
import v1.mocks.services.{MockAuditService, MockCreateMarriageAllowanceService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import v1.models.domain.Nino
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.marriageAllowance.{CreateMarriageAllowanceBody, CreateMarriageAllowanceRawData, CreateMarriageAllowanceRequest}

import scala.collection.immutable.ListSet
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NoStackTrace

class CreateMarriageAllowanceControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockCreateMarriageAllowanceService
    with MockCreateMarriageAllowanceRequestParser
    with MockAuditService
    with MockIdGenerator {

  val nino1: String         = "AA123456A"
  val nino2: String         = "BB123456B"
  val correlationId: String = "X-123"

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new CreateMarriageAllowanceController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      requestParser = mockCreateMarriageAllowanceRequestParser,
      service = mockCreateMarriageAllowanceService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino1).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockAppConfig.apiGatewayContext.returns("baseUrl").anyNumberOfTimes()
    MockIdGenerator.generateCorrelationId.returns(correlationId)
  }

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

  val rawData: CreateMarriageAllowanceRawData = CreateMarriageAllowanceRawData(
    nino = nino1,
    body = AnyContentAsJson(requestBodyJson)
  )

  val createMarriageAllowanceRequestBody: CreateMarriageAllowanceBody = CreateMarriageAllowanceBody(
    nino2,
    Some("John"),
    "Smith",
    Some("1986-04-06")
  )

  val requestData: CreateMarriageAllowanceRequest = CreateMarriageAllowanceRequest(
    nino = Nino(nino1),
    body = createMarriageAllowanceRequestBody
  )

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "CreateMarriageAllowanceClaim",
      transactionName = "create-marriage-allowance-claim",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino1),
        request = Some(requestBodyJson),
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  "CreateMarriageAllowanceController" should {
    "return OK" when {
      "happy path" in new Test {

        MockCreateMarriageAllowanceRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockCreateMarriageAllowanceService
          .createMarriageAllowance(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        val result: Future[Result] = controller.createMarriageAllowance(nino1)(fakePutRequest(requestBodyJson))

        status(result) shouldBe CREATED
        contentAsString(result) shouldBe ""
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(CREATED, None, None)
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once()
      }

      "audit fails" in new Test {
        MockCreateMarriageAllowanceRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockCreateMarriageAllowanceService
          .createMarriageAllowance(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        val result: Future[Result] = controller.createMarriageAllowance(nino1)(fakePutRequest(requestBodyJson))

        status(result) shouldBe CREATED
        contentAsString(result) shouldBe ""
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(CREATED, None, None)
        MockedAuditService.verifyAuditEvent(event(auditResponse), Future.failed(new RuntimeException with NoStackTrace)).once()
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockCreateMarriageAllowanceRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(correlationId, error, None)))

            val result: Future[Result] = controller.createMarriageAllowance(nino1)(fakePutRequest(requestBodyJson))

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(ListSet(AuditError(error.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once()
          }
        }

        val input = Seq(
          (BadRequestError, BAD_REQUEST),
          (NinoFormatError, BAD_REQUEST),
          (PartnerFirstNameFormatError, BAD_REQUEST),
          (PartnerSurnameFormatError, BAD_REQUEST),
          (PartnerNinoFormatError, BAD_REQUEST),
          (PartnerDoBFormatError, BAD_REQUEST),
          (RuleIncorrectOrEmptyBodyError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockCreateMarriageAllowanceRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockCreateMarriageAllowanceService
              .createMarriageAllowance(requestData)
              .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

            val result: Future[Result] = controller.createMarriageAllowance(nino1)(fakePutRequest(requestBodyJson))

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(ListSet(AuditError(mtdError.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once()
          }
        }

        val input = Seq(
          (NinoFormatError, BAD_REQUEST),
          (RuleDeceasedRecipientError, FORBIDDEN),
          (RuleActiveMarriageAllowanceClaimError, FORBIDDEN),
          (RuleInvalidRequestError, FORBIDDEN),
          (InternalError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}
