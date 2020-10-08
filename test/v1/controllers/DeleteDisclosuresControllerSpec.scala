/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.requestParsers.MockDeleteRetrieveRequestParser
import v1.mocks.services.{MockAuditService, MockDeleteRetrieveService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import v1.models.domain.DesTaxYear
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.{DeleteRetrieveRawData, DeleteRetrieveRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteDisclosuresControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockDeleteRetrieveService
    with MockDeleteRetrieveRequestParser
    with MockAuditService {

  val nino: String = "AA123456A"
  val taxYear: String = "2017-18"
  val correlationId: String = "X-123"

  val rawData: DeleteRetrieveRawData = DeleteRetrieveRawData(
    nino = nino,
    taxYear = taxYear
  )

  val requestData: DeleteRetrieveRequest = DeleteRetrieveRequest(
    nino = Nino(nino),
    taxYear = DesTaxYear.fromMtd(taxYear)
  )

  trait Test {
    val hc = HeaderCarrier()

    val controller = new DeleteDisclosuresController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      requestParser = mockDeleteRetrieveRequestParser,
      service = mockDeleteRetrieveService,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "DeleteDisclosures",
      transactionName = "delete-disclosures",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino, "taxYear" -> taxYear),
        request = None,
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  "DeleteDisclosuresController" should {
    "return NO_content" when {
      "happy path" in new Test {

        MockDeleteRetrieveRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockDeleteRetrieveService
          .delete()
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        val result: Future[Result] = controller.deleteDisclosures(nino, taxYear)(fakeDeleteRequest)

        status(result) shouldBe NO_CONTENT
        contentAsString(result) shouldBe ""
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(NO_CONTENT, None, None)
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockDeleteRetrieveRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.deleteDisclosures(nino, taxYear)(fakeDeleteRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
          }
        }

        val input = Seq(
          (BadRequestError, BAD_REQUEST),
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockDeleteRetrieveRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockDeleteRetrieveService
              .delete()
              .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

            val result: Future[Result] = controller.deleteDisclosures(nino, taxYear)(fakeDeleteRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(mtdError.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
          }
        }

        val input = Seq(
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (NotFoundError, NOT_FOUND),
          (RuleVoluntaryClass2CannotBeChanged, FORBIDDEN),
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}