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

package v1.services

import v1.controllers.EndpointLogContext
import v1.mocks.connectors.MockCreateMarriageAllowanceConnector
import v1.models.domain.Nino
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.marriageAllowance.{CreateMarriageAllowanceBody, CreateMarriageAllowanceRequest}

import scala.concurrent.Future

class CreateMarriageAllowanceServiceSpec extends ServiceSpec {

  private val nino = "AA112233A"
  private val requestBodyModel = CreateMarriageAllowanceBody("TC663795B", Some("John"), "Smith", Some("1987-10-18"))

  val createMarriageAllowanceRequest: CreateMarriageAllowanceRequest = CreateMarriageAllowanceRequest(
    nino = Nino(nino),
    body = requestBodyModel
  )

  trait Test extends MockCreateMarriageAllowanceConnector{
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: CreateMarriageAllowanceService = new CreateMarriageAllowanceService(
      connector = mockCreateMarriageAllowanceConnector
    )
  }

  "CreateMarriageAllowanceService.create" should {
    "return success response" when {
      "a valid request is supplied" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockCreateMarriageAllowanceConnector.create(createMarriageAllowanceRequest)
          .returns(Future.successful(outcome))

        await(service.create(createMarriageAllowanceRequest)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the service" in new Test {

            MockCreateMarriageAllowanceConnector.create(createMarriageAllowanceRequest)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

            await(service.create(createMarriageAllowanceRequest)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = Seq(
          ("INVALID_IDVALUE", NinoFormatError),
          ("DECEASED_PARTICIPANT", RuleDeceasedRecipientError),
          ("RELATIONSHIP_ALREADY_EXISTS", RuleActiveMarriageAllowanceClaimError),
          ("INVALID_IDTYPE", DownstreamError),
          ("END_DATE_CODE_NOT_FOUND", DownstreamError),
          ("INVALID_CORRELATIONID", DownstreamError),
          ("INVALID_PAYLOAD", DownstreamError),
          ("NINO_OR_TRN_NOT_FOUND", RuleInvalidRequest),
          ("INVALID_ACTUAL_END_DATE", DownstreamError),
          ("INVALID_PARTICIPANT_END_DATE", DownstreamError),
          ("INVALID_PARTICIPANT_START_DATE", DownstreamError),
          ("INVALID_RELATIONSHIP_CODE", DownstreamError),
          ("PARTICIPANT1_CANNOT_BE_UPDATED", DownstreamError),
          ("PARTICIPANT2_CANNOT_BE_UPDATED", DownstreamError),
          ("CONFIDENCE_CHECK_FAILED", DownstreamError),
          ("CONFIDENCE_CHECK_SURNAME_MISSED", DownstreamError),
          ("BAD_GATEWAY", DownstreamError),
          ("SERVER_ERROR", DownstreamError),
          ("SERVICE_UNAVAILABLE", DownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }
  }
}
