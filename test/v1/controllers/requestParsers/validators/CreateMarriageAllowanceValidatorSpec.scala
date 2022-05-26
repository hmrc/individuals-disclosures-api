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

package v1.controllers.requestParsers.validators

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v1.models.errors._
import v1.models.request.marriageAllowance.CreateMarriageAllowanceRawData


class CreateMarriageAllowanceValidatorSpec extends UnitSpec {
  val validator = new CreateMarriageAllowanceValidator
  val nino = "AA123456A"

  val body: AnyContentAsJson = AnyContentAsJson(Json.parse("""{
      |  "spouseOrCivilPartnerNino": "AA123456B",
      |  "spouseOrCivilPartnerFirstName": "Marge",
      |  "spouseOrCivilPartnerSurname": "Simpson",
      |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
      |}
      |""".stripMargin))

  "CreateMarriageAllowanceValidator" when {
    "valid request" must {
      "return no errors" in {
        validator.validate(CreateMarriageAllowanceRawData(nino, body)) shouldBe List.empty[MtdError]
      }
    }

    "bad nino" must {
      "return FORMAT_NINO" in {
        validator.validate(CreateMarriageAllowanceRawData("BADNINO", body)) shouldBe List(NinoFormatError)
      }
    }

    "good nino" when {
      "bad body format" must {
        "return RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED" in {
          val badBody = AnyContentAsJson(Json.parse("""{
              |  "XXX": 123,
              |  "spouseOrCivilPartnerNino": "AA123456B",
              |  "spouseOrCivilPartnerFirstName": "Marge",
              |  "spouseOrCivilPartnerSurname": "Simpson",
              |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
              |}
              |""".stripMargin))

          validator.validate(CreateMarriageAllowanceRawData(nino, badBody)) shouldBe List.empty[MtdError]
        }
      }

      "good body json format" when {
        "bad spouse first name" must {
          "return FORMAT_SPOUSE_OR_CIVIL_PARTNERS_FIRST_NAME" in {
            val bodyWithLongFirstName = AnyContentAsJson(Json.parse(s"""{
                 |  "spouseOrCivilPartnerNino": "AA123456B",
                 |  "spouseOrCivilPartnerFirstName": "${"1" * 36}",
                 |  "spouseOrCivilPartnerSurname": "Simpson",
                 |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
                 |}
                 |""".stripMargin))

            validator.validate(CreateMarriageAllowanceRawData(nino, bodyWithLongFirstName)) shouldBe
              List(PartnerFirstNameFormatError)
          }
        }

        "bad partner surname" must {
          "return FORMAT_SPOUSE_OR_CIVIL_PARTNERS_SURNAME" in {
            val bodyWithLongFirstName = AnyContentAsJson(Json.parse(s"""{
                 |  "spouseOrCivilPartnerNino": "AA123456B",
                 |  "spouseOrCivilPartnerFirstName": "Marge",
                 |  "spouseOrCivilPartnerSurname": "${"1" * 36}",
                 |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
                 |}
                 |""".stripMargin))

            validator.validate(CreateMarriageAllowanceRawData(nino, bodyWithLongFirstName)) shouldBe
              List(PartnerSurnameFormatError)
          }
        }

        "partner surname not present" must {
          "return RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED with affected path" in {
            val bodyWithNoSurname = AnyContentAsJson(Json.parse(s"""{
                 |  "spouseOrCivilPartnerNino": "AA123456B",
                 |  "spouseOrCivilPartnerFirstName": "Marge",
                 |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
                 |}
                 |""".stripMargin))

            validator.validate(CreateMarriageAllowanceRawData(nino, bodyWithNoSurname)) shouldBe List(
              RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/spouseOrCivilPartnerSurname"))))
          }
        }

        "bad partner nino" must {
          "return FORMAT_SPOUSE_OR_CIVIL_PARTNERS_NINO" in {
            val bodyWithBadNino = AnyContentAsJson(Json.parse("""{
                |  "spouseOrCivilPartnerNino": "XXX",
                |  "spouseOrCivilPartnerFirstName": "Marge",
                |  "spouseOrCivilPartnerSurname": "Simpson",
                |  "spouseOrCivilPartnerDateOfBirth": "1970-01-01"
                |}
                |""".stripMargin))

            validator.validate(CreateMarriageAllowanceRawData(nino, bodyWithBadNino)) shouldBe
              List(PartnerNinoFormatError)
          }
        }

        "bad partner dob" must {
          "return FORMAT_SPOUSE_OR_CIVIL_PARTNERS_DATE_OF_BIRTH" in {
            val bodyWithBadDob = AnyContentAsJson(Json.parse("""{
                |  "spouseOrCivilPartnerNino": "AA123456B",
                |  "spouseOrCivilPartnerFirstName": "Marge",
                |  "spouseOrCivilPartnerSurname": "Simpson",
                |  "spouseOrCivilPartnerDateOfBirth": "XXXX"
                |}
                |""".stripMargin))

            validator.validate(CreateMarriageAllowanceRawData(nino, bodyWithBadDob)) shouldBe
              List(PartnerDoBFormatError)
          }
        }

        "multiple body errors" must {
          "return all of them" in {
            val badBody = AnyContentAsJson(Json.parse(s"""{
                 |  "spouseOrCivilPartnerNino": "XXX",
                 |  "spouseOrCivilPartnerFirstName": "${"1" * 36}",
                 |  "spouseOrCivilPartnerSurname": "${"1" * 36}",
                 |  "spouseOrCivilPartnerDateOfBirth": "XXXX"
                 |}
                 |""".stripMargin))

            validator.validate(CreateMarriageAllowanceRawData(nino, badBody)) should contain.allElementsOf(
              List(
                PartnerNinoFormatError,
                PartnerFirstNameFormatError,
                PartnerSurnameFormatError,
                PartnerDoBFormatError,
              )
            )
          }
        }
      }
    }
  }
}
