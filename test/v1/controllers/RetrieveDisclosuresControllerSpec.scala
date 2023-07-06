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

package v1.controllers

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.hateoas.HateoasLinks
import api.mocks.MockIdGenerator
import api.mocks.hateoas.MockHateoasFactory
import api.mocks.services.{MockEnrolmentsAuthService, MockMtdIdLookupService}
import api.models.domain.{Nino, Timestamp}
import api.models.errors.{ErrorWrapper, NinoFormatError, TaxYearFormatError}
import api.models.hateoas.Method._
import api.models.hateoas.RelType._
import api.models.hateoas.{HateoasWrapper, Link}
import api.models.outcomes.ResponseWrapper
import play.api.mvc.Result
import v1.fixtures.RetrieveDisclosuresControllerFixture.mtdResponseWithHateoas
import v1.mocks.requestParsers.MockRetrieveDisclosuresRequestParser
import v1.mocks.services.MockRetrieveDisclosuresService
import v1.models.request.retrieve.{RetrieveDisclosuresRawData, RetrieveDisclosuresRequest}
import v1.models.response.retrieveDisclosures.{Class2Nics, RetrieveDisclosuresHateoasData, RetrieveDisclosuresResponse, TaxAvoidanceItem}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveDisclosuresControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockRetrieveDisclosuresService
    with MockHateoasFactory
    with MockRetrieveDisclosuresRequestParser
    with HateoasLinks
    with MockIdGenerator {

  val taxYear: String = "2021-22"

  val rawData: RetrieveDisclosuresRawData = RetrieveDisclosuresRawData(
    nino = nino,
    taxYear = taxYear
  )

  val requestData: RetrieveDisclosuresRequest = RetrieveDisclosuresRequest(
    nino = Nino(nino),
    taxYear = taxYear
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

  val taxAvoidanceModel: Seq[TaxAvoidanceItem] = Seq(
    TaxAvoidanceItem(
      srn = "14211123",
      taxYear = "2020-21"
    ),
    TaxAvoidanceItem(
      srn = "34522678",
      taxYear = "2021-22"
    )
  )

  val class2NicsModel: Class2Nics = Class2Nics(class2VoluntaryContributions = Some(true))

  val retrieveDisclosuresResponseModel: RetrieveDisclosuresResponse = RetrieveDisclosuresResponse(
    taxAvoidance = Some(taxAvoidanceModel),
    class2Nics = Some(class2NicsModel),
    submittedOn = Timestamp("2020-07-06T09:37:17Z")
  )

  private val downstreamResponse = mtdResponseWithHateoas(nino, taxYear)

  trait Test extends ControllerTest {

    val controller = new RetrieveDisclosuresController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRetrieveDisclosuresRequestParser,
      service = mockRetrieveDisclosuresService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    override protected def callController(): Future[Result] = controller.retrieveDisclosures(nino, taxYear)(fakeGetRequest)

  }

  "RetrieveDisclosuresController" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {
        MockRetrieveDisclosuresRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockRetrieveDisclosuresService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveDisclosuresResponseModel))))

        MockHateoasFactory
          .wrap(retrieveDisclosuresResponseModel, RetrieveDisclosuresHateoasData(nino, taxYear))
          .returns(
            HateoasWrapper(
              retrieveDisclosuresResponseModel,
              Seq(
                amendDisclosuresLink,
                retrieveDisclosuresLink,
                deleteDisclosuresLink
              )))

        runOkTest(OK, Some(downstreamResponse))

      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        MockRetrieveDisclosuresRequestParser
          .parse(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        MockRetrieveDisclosuresRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockRetrieveDisclosuresService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TaxYearFormatError))))

        runErrorTest(TaxYearFormatError)
      }
    }

  }

}
