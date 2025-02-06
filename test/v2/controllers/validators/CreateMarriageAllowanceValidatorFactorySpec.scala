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

package v2.controllers.validators

import api.models.domain.Nino
import api.models.errors._
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v2.models.request.create.{CreateMarriageAllowanceRequestBody, CreateMarriageAllowanceRequestData}

class CreateMarriageAllowanceValidatorFactorySpec extends UnitSpec {
  private implicit val correlationId: String = "1234"

  private val validNino = "AA123456A"

  private val validBody: JsValue = Json.parse("""{
               |  "spouseOrCivilPartnerNino": "AA123456B",
               |  "spouseOrCivilPartnerFirstName": "Marge",
               |  "spouseOrCivilPartnerSurname": "Simpson",
               |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
               |}
               |""".stripMargin)

  private val invalidBodyEmpty: JsValue = Json.parse("""{}""".stripMargin)

  private val invalidBodyLongFirstName = Json.parse(s"""{
       |  "spouseOrCivilPartnerNino": "AA123456B",
       |  "spouseOrCivilPartnerFirstName": "${"1" * 36}",
       |  "spouseOrCivilPartnerSurname": "Simpson",
       |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
       |}
       |""".stripMargin)

  private val invalidBodyLongSurname = Json.parse(s"""{
       |  "spouseOrCivilPartnerNino": "AA123456B",
       |  "spouseOrCivilPartnerFirstName": "Marge",
       |  "spouseOrCivilPartnerSurname": "${"1" * 36}",
       |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
       |}
       |""".stripMargin)

  private val invalidBodyMissingField = Json.parse(s"""{
       |  "spouseOrCivilPartnerNino": "AA123456B",
       |  "spouseOrCivilPartnerFirstName": "Marge",
       |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
       |}
       |""".stripMargin)

  private val invalidBodyBadNino = Json.parse("""{
        |  "spouseOrCivilPartnerNino": "XXX",
        |  "spouseOrCivilPartnerFirstName": "Marge",
        |  "spouseOrCivilPartnerSurname": "Simpson",
        |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
        |}
        |""".stripMargin)

  private val invalidBodyBadDob = Json.parse("""{
         |  "spouseOrCivilPartnerNino": "AA123456B",
         |  "spouseOrCivilPartnerFirstName": "Marge",
         |  "spouseOrCivilPartnerSurname": "Simpson",
         |  "spouseOrCivilPartnerDateOfBirth": "XXXX"
         |}
         |""".stripMargin)

  private val invalidBodyAllFields = Json.parse(s"""{
         |  "spouseOrCivilPartnerNino": "XXX",
         |  "spouseOrCivilPartnerFirstName": "${"1" * 36}",
         |  "spouseOrCivilPartnerSurname": "${"1" * 36}",
         |  "spouseOrCivilPartnerDateOfBirth": "XXXX"
         |}
         |""".stripMargin)

  private val invalidDobRangeInBody = Json.parse("""{
      |  "spouseOrCivilPartnerNino": "AA123456B",
      |  "spouseOrCivilPartnerFirstName": "Marge",
      |  "spouseOrCivilPartnerSurname": "Simpson",
      |  "spouseOrCivilPartnerDateOfBirth": "0010-01-01"
      |}
      |""".stripMargin)

  private val parsedNino = Nino(validNino)

  private val parsedBody = CreateMarriageAllowanceRequestBody("AA123456B", Some("Marge"), "Simpson", Some("1970-01-01"))

  private val validatorFactory = new CreateMarriageAllowanceValidatorFactory()

  private def validator(nino: String, body: JsValue) = validatorFactory.validator(nino, body)

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] = validator(validNino, validBody).validateAndWrapResult()

        result shouldBe Right(CreateMarriageAllowanceRequestData(parsedNino, parsedBody))
      }
    }

    "return a single error" when {
      "passed an invalid nino" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] = validator("invalid nino", validBody).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }

      "passed an empty request body" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] = validator(validNino, invalidBodyEmpty).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "passed a request body with an incorrectly formatted Spouse or Civil Partner firstName" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] =
          validator(validNino, invalidBodyLongFirstName).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, PartnerFirstNameFormatError))
      }

      "passed a request body with an incorrectly formatted Spouse or Civil Partner surname" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] =
          validator(validNino, invalidBodyLongSurname).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, PartnerSurnameFormatError))
      }

      "passed a request body with missing field" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] =
          validator(validNino, invalidBodyMissingField).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/spouseOrCivilPartnerSurname")))))
      }

      "passed a request body with an invalid Spouse or Civil Partner nino" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] =
          validator(validNino, invalidBodyBadNino).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, PartnerNinoFormatError))
      }

      "passed a request body with an invalid Spouse or Civil Partner dob" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] =
          validator(validNino, invalidBodyBadDob).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, PartnerDoBFormatError))
      }

      "passed a request body with an invalid range of Spouse or Civil Partner dob" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] =
          validator(validNino, invalidDobRangeInBody).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, PartnerDoBFormatError))
      }
    }

    "return multiple errors" when {
      "the request body has multiple issues" in {
        val result: Either[ErrorWrapper, CreateMarriageAllowanceRequestData] =
          validator(validNino, invalidBodyAllFields).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(PartnerDoBFormatError, PartnerNinoFormatError, PartnerFirstNameFormatError, PartnerSurnameFormatError))
          )
        )
      }
    }
  }

}
