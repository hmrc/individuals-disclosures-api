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
import api.hateoas.{HateoasLinks, HateoasWrapper, Link, MockHateoasFactory}
import api.mocks.MockIdGenerator
import api.models.domain.{Nino, TaxYear, Timestamp}
import api.models.errors.{ErrorWrapper, NinoFormatError, TaxYearFormatError}
import api.hateoas.Method._
import api.hateoas.RelType._
import api.services.{MockEnrolmentsAuthService, MockMtdIdLookupService}
import api.models.outcomes.ResponseWrapper
import config.MockAppConfig
import play.api.mvc.Result
import play.api.Configuration
import v1.controllers.validators.MockRetrieveDisclosuresValidatorFactory
import v1.fixtures.RetrieveDisclosuresControllerFixture.mtdResponseWithHateoas
import v1.models.request.retrieve.RetrieveDisclosuresRequestData
import v1.models.response.retrieveDisclosures.{Class2Nics, RetrieveDisclosuresHateoasData, RetrieveDisclosuresResponse, TaxAvoidanceItem}
import v1.services.MockRetrieveDisclosuresService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveDisclosuresControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockRetrieveDisclosuresService
    with MockHateoasFactory
    with MockRetrieveDisclosuresValidatorFactory
    with HateoasLinks
    with MockIdGenerator
    with MockAppConfig {

  val taxYear: String = "2021-22"

  val requestData: RetrieveDisclosuresRequestData = RetrieveDisclosuresRequestData(
    nino = Nino(nino),
    taxYear = TaxYear.fromMtd(taxYear)
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

  "RetrieveDisclosuresController" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

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

        runOkTest(OK, Some(mtdResponseWithHateoas(nino, taxYear)))

      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveDisclosuresService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TaxYearFormatError))))

        runErrorTest(TaxYearFormatError)
      }
    }

  }

  trait Test extends ControllerTest {

    MockedAppConfig.featureSwitches.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    val controller = new RetrieveDisclosuresController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockRetrieveDisclosuresValidatorFactory,
      service = mockRetrieveDisclosuresService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2022)
      .anyNumberOfTimes()

    override protected def callController(): Future[Result] = controller.retrieveDisclosures(nino, taxYear)(fakeGetRequest)

  }

}
