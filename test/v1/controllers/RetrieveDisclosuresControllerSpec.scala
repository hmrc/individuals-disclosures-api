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
import v1.fixtures.RetrieveDisclosuresControllerFixture
import v1.hateoas.HateoasLinks
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockDeleteRetrieveRequestParser
import v1.mocks.services.{MockDeleteRetrieveService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.domain.DesTaxYear
import v1.models.errors._
import v1.models.hateoas.Method.{DELETE, GET, PUT}
import v1.models.hateoas.RelType.{AMEND_DISCLOSURES, DELETE_DISCLOSURES, SELF}
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.disclosures.Class2Nics
import v1.models.request.{DeleteRetrieveRawData, DeleteRetrieveRequest}
import v1.models.response.retrieveDisclosures.{RetrieveDisclosuresHateoasData, RetrieveDisclosuresResponse, TaxAvoidanceItem}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveDisclosuresControllerSpec extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockDeleteRetrieveService
  with MockHateoasFactory
  with MockDeleteRetrieveRequestParser
  with HateoasLinks {

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

  val amendDisclosuresLink: Link =
    Link(
      href = s"/individuals/disclosures/$nino/$taxYear",
      method = PUT,
      rel = AMEND_DISCLOSURES
    )

  val retrieveDisclosuresLink: Link =
    Link(
      href = s"/individuals/disclosures/$nino/$taxYear",
      method = GET,
      rel = SELF
    )

  val deleteDisclosuresLink: Link =
    Link(
      href = s"/individuals/disclosures/$nino/$taxYear",
      method = DELETE,
      rel = DELETE_DISCLOSURES
    )

  private val taxAvoidanceModel = Seq(
    TaxAvoidanceItem(
      srn = "14211123",
      taxYear = "2020-21"
    ),
    TaxAvoidanceItem(
      srn = "34522678",
      taxYear = "2021-22"
    )
  )

  val class2Nics: Class2Nics = Class2Nics(true)

  private val retrieveDisclosuresResponseModel = RetrieveDisclosuresResponse(
    taxAvoidance = Some(taxAvoidanceModel), class2Nics = Some(class2Nics), submittedOn = "2020-07-06T09:37:17Z"
  )

  private val mtdResponse = RetrieveDisclosuresControllerFixture.mtdResponseWithHateoas(nino, taxYear)

  trait Test {
    val hc = HeaderCarrier()

    val controller = new RetrieveDisclosuresController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      requestParser = mockDeleteRetrieveRequestParser,
      service = mockDeleteRetrieveService,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "RetrieveDisclosuresController" should {
    "return OK" when {
      "happy path" in new Test {

        MockDeleteRetrieveRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockDeleteRetrieveService
          .retrieve[RetrieveDisclosuresResponse](requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveDisclosuresResponseModel))))

        MockHateoasFactory
          .wrap(retrieveDisclosuresResponseModel, RetrieveDisclosuresHateoasData(nino, taxYear))
          .returns(HateoasWrapper(retrieveDisclosuresResponseModel,
            Seq(
              amendDisclosuresLink,
              retrieveDisclosuresLink,
              deleteDisclosuresLink
            )
          ))

        val result: Future[Result] = controller.retrieveDisclosures(nino, taxYear)(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe mtdResponse
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockDeleteRetrieveRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.retrieveDisclosures(nino, taxYear)(fakeGetRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
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
              .retrieve[RetrieveDisclosuresResponse](requestData)
              .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

            val result: Future[Result] = controller.retrieveDisclosures(nino, taxYear)(fakeGetRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
          }
        }

        val input = Seq(
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}