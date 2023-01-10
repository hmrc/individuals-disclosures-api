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

package v1.models.request.marriageAllowance

import play.api.libs.json.{JsError, Json}
import support.UnitSpec

class CreateMarriageAllowanceBodySpec extends UnitSpec {

  private val rawJson = Json.parse(
    """
      |{
      |    "spouseOrCivilPartnerNino": "TC663795B",
      |    "spouseOrCivilPartnerFirstName": "John",
      |    "spouseOrCivilPartnerSurname": "Smith",
      |    "spouseOrCivilPartnerDateOfBirth": "1987-10-18"
      |}
    """.stripMargin
  )

  private val requestJson = Json.parse(
    """
      |{
      |    "participant1Details": {
      |        "nino": "TC663795B",
      |        "surname": "Smith",
      |        "firstForeName": "John",
      |        "dateOfBirth": "1987-10-18"
      |    }
      |}
    """.stripMargin
  )

  private val requestBodyModel = CreateMarriageAllowanceBody("TC663795B", Some("John"), "Smith", Some("1987-10-18"))

  "CreateMarriageAllowanceBody" when {
    "read from valid JSON" should {
      "produce the expected CreateMarriageAllowanceBody object" in {
        rawJson.as[CreateMarriageAllowanceBody] shouldBe requestBodyModel
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        val invalidJson = Json.parse(
          """
            |{
            |    "spouseOrCivilPartnerNationalInsuranceNumber": "TC663795B",
            |    "spouseOrCivilPartnerFirstName": "John",
            |    "spouseOrCivilPartnerSurname": "Smith",
            |    "spouseOrCivilPartnerDateOfBirth": 1987
            |}
          """.stripMargin
        )

        invalidJson.validate[CreateMarriageAllowanceBody] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(requestBodyModel) shouldBe requestJson
      }
    }
  }


}
