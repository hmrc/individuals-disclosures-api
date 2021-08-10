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

package v1.controllers.requestParsers

import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v1.mocks.validators.MockCreateMarriageAllowanceValidator
import v1.models.domain.Nino
import v1.models.errors.{BadRequestError, ErrorWrapper, NinoFormatError, RuleIncorrectOrEmptyBodyError}
import v1.models.request.marriageAllowance.{CreateMarriageAllowanceBody, CreateMarriageAllowanceRawData, CreateMarriageAllowanceRequest}

class CreateMarriageAllowanceRequestParserSpec extends UnitSpec {

  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  trait Test extends MockCreateMarriageAllowanceValidator {
    lazy val parser: CreateMarriageAllowanceRequestParser = new CreateMarriageAllowanceRequestParser(
      validator = mockCreateMarriageAllowanceValidator
    )
  }

  "CreateMarriageAllowanceRequestParser" when {
    "parses successfully" must {

      "return the request" in new Test {
        val nino = "AA123456B"
        val rawData: CreateMarriageAllowanceRawData = CreateMarriageAllowanceRawData(nino, AnyContentAsJson(Json.parse(
          """{
            |  "spouseOrCivilPartnerNino": "AA123456B",
            |  "spouseOrCivilPartnerFirstName": "Marge",
            |  "spouseOrCivilPartnerSurname": "Simpson",
            |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
            |}
            |""".stripMargin)))

        MockCreateMarriageAllowanceValidator.validate(rawData) returns Nil

        parser.parseRequest(rawData) shouldBe Right(CreateMarriageAllowanceRequest(Nino(nino), CreateMarriageAllowanceBody(
          spouseOrCivilPartnerNino = "AA123456B",
          spouseOrCivilPartnerFirstName = Some("Marge"),
          spouseOrCivilPartnerSurname = "Simpson",
          spouseOrCivilPartnerDateOfBirth = Some("1970-01-01")
        )))
      }
    }

    "fails to parse" when {
      // WLOG as params and body ignored when validation fails
      val ignoredNino  = "SomeNino"
      val ignoredBody = AnyContentAsJson(JsObject.empty)

      "single error" must {
        "return the ErrorWrapper for the error" in new Test {
          val rawData: CreateMarriageAllowanceRawData = CreateMarriageAllowanceRawData(ignoredNino, ignoredBody)

          val error: NinoFormatError.type = NinoFormatError
          MockCreateMarriageAllowanceValidator.validate(rawData) returns List(error)

          parser.parseRequest(rawData) shouldBe Left(ErrorWrapper(correlationId, error))
        }
      }

      "multiple errors" must {
        "return a ErrorWrapper for a BadRequestError with the errors" in new Test {
          val rawData: CreateMarriageAllowanceRawData = CreateMarriageAllowanceRawData(ignoredNino, ignoredBody)

          val errors = List(NinoFormatError, RuleIncorrectOrEmptyBodyError)
          MockCreateMarriageAllowanceValidator.validate(rawData) returns errors

          parser.parseRequest(rawData) shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(errors)))
        }
      }
    }
  }
}