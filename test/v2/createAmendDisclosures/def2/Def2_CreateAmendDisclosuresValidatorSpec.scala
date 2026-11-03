/*
 * Copyright 2026 HM Revenue & Customs
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

package v2.createAmendDisclosures.def2

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.utils.JsonErrorValidators
import play.api.libs.json.*
import support.UnitSpec
import v2.createAmendDisclosures.def2.fixture.Def2_CreateAmendDisclosuresFixture.*
import v2.createAmendDisclosures.def2.model.request.Def2_CreateAmendDisclosuresRequestData

class Def2_CreateAmendDisclosuresValidatorSpec extends UnitSpec with JsonErrorValidators {

  private implicit val correlationId: String = "1234"

  private val validNino: String    = "AA123456A"
  private val validTaxYear: String = "2026-27"

  private val parsedNino: Nino       = Nino(validNino)
  private val parsedTaxYear: TaxYear = TaxYear.fromMtd(validTaxYear)

  private def validate(nino: String = validNino, body: JsValue = mtdRequestBodyJson) = new Def2_CreateAmendDisclosuresValidator(
    nino = nino,
    taxYear = validTaxYear,
    body = body
  ).validateAndWrapResult()

  private def error(mtdError: MtdError) = Left(ErrorWrapper(correlationId, mtdError))

  "Def2_CreateAmendDisclosuresValidator" should {
    "return the parsed domain object" when {
      "a valid request is supplied" in {
        validate() shouldBe Right(Def2_CreateAmendDisclosuresRequestData(parsedNino, parsedTaxYear, requestBodyModel))
      }

      val testCases: Seq[(String, String)] = Seq(
        (('a' to 'z').mkString, "lowercase letters"),
        (('A' to 'Z').mkString, "uppercase letters"),
        (('0' to '9').mkString, "digits"),
        ("a b", "spaces"),
        ("""&'"()*,-./@£""", "allowed special characters"),
        ("a", "a single character"),
        ("a" * 4000, "4000 characters")
      )

      testCases.foreach { (value, description) =>
        s"a valid supplementaryInformation field is supplied with $description" in {
          val validJson: JsValue = mtdRequestBodyJson.updateArrayOrObjectField(
            "/taxAvoidance/0/supplementaryInformation",
            JsString(value)
          )

          validate(body = validJson) shouldBe Right(
            Def2_CreateAmendDisclosuresRequestData(
              parsedNino,
              parsedTaxYear,
              requestBodyModel.copy(taxAvoidance = Some(Seq(taxAvoidanceItemModel.copy(supplementaryInformation = Some(value)))))
            )
          )
        }
      }
    }

    "return NinoFormatError" when {
      "an invalid nino is supplied" in {
        validate("A12344A") shouldBe error(NinoFormatError)
      }
    }

    "return RuleIncorrectOrEmptyBodyError" when {
      "an empty JSON body is supplied" in {
        validate(body = JsObject.empty) shouldBe error(RuleIncorrectOrEmptyBodyError)
      }

      "a non-empty JSON body without any expected fields is supplied" in {
        val invalidJson: JsValue = Json.parse("""{"field": "value"}""")

        validate(body = invalidJson) shouldBe error(RuleIncorrectOrEmptyBodyError)
      }

      "an empty taxAvoidance array is supplied in the request body" in {
        val invalidJson: JsValue = mtdRequestBodyJson.update("/taxAvoidance", JsArray.empty)

        validate(body = invalidJson) shouldBe error(RuleIncorrectOrEmptyBodyError.withPath("/taxAvoidance"))
      }

      "mandatory fields are missing from a taxAvoidance item in the request body" in {
        val invalidJson: JsValue = mtdRequestBodyJson.updateArrayOrObjectField("/taxAvoidance/0", JsObject.empty)

        validate(body = invalidJson) shouldBe error(
          RuleIncorrectOrEmptyBodyError.withPaths(Seq("/taxAvoidance/0/srn", "/taxAvoidance/0/taxYear"))
        )
      }

      mtdRequestBodyJson.collectPaths().foreach { path =>
        s"an incorrect type is supplied for field: $path in the request body" in {
          val invalidJson: JsValue = mtdRequestBodyJson.updateArrayOrObjectField(path, JsNumber(1))

          validate(body = invalidJson) shouldBe error(RuleIncorrectOrEmptyBodyError.withPath(path))
        }
      }
    }

    "return SRNFormatError" when {
      "an incorrectly formatted srn is supplied in the request body" in {
        val invalidJson: JsValue = mtdRequestBodyJson.updateArrayOrObjectField("/taxAvoidance/0/srn", JsString("ABC142111235"))

        validate(body = invalidJson) shouldBe error(SRNFormatError.withPath("/taxAvoidance/0/srn"))
      }
    }

    "return TaxYearFormatError" when {
      "an incorrectly formatted tax year is supplied in the request body" in {
        val invalidJson: JsValue = mtdRequestBodyJson.updateArrayOrObjectField("/taxAvoidance/0/taxYear", JsString("2026"))

        validate(body = invalidJson) shouldBe error(TaxYearFormatError.withPath("/taxAvoidance/0/taxYear"))
      }
    }

    "return RuleTaxYearRangeInvalidError" when {
      "an invalid tax year range is supplied in the request body" in {
        val invalidJson: JsValue = mtdRequestBodyJson.updateArrayOrObjectField("/taxAvoidance/0/taxYear", JsString("2026-28"))

        validate(body = invalidJson) shouldBe error(RuleTaxYearRangeInvalidError.withPath("/taxAvoidance/0/taxYear"))
      }
    }

    "return SupplementaryInformationFormatError" when {
      val invalidCharacters: Seq[(String, String)] = """:|%\!#é""".map(char => (char.toString, s"with invalid special character '$char'"))
      val invalidLengths: Seq[(String, String)]    = Seq(0, 4001).map(length => ("a" * length, s"with an invalid length of $length"))

      val testCases: Seq[(String, String)] = invalidCharacters ++ invalidLengths

      testCases.foreach { (value, description) =>
        s"an incorrectly formatted supplementaryInformation is supplied in the request body $description" in {
          val invalidJson: JsValue = mtdRequestBodyJson.updateArrayOrObjectField("/taxAvoidance/0/supplementaryInformation", JsString(value))

          validate(body = invalidJson) shouldBe error(SupplementaryInformationFormatError.withPath("/taxAvoidance/0/supplementaryInformation"))
        }
      }
    }

    "return RuleVoluntaryClass2ValueInvalidError" when {
      "an incorrect boolean value is supplied for class2VoluntaryContributions in the request body" in {
        val invalidJson: JsValue = mtdRequestBodyJson.updateArrayOrObjectField("/class2Nics/class2VoluntaryContributions", JsBoolean(false))

        validate(body = invalidJson) shouldBe error(RuleVoluntaryClass2ValueInvalidError.withPath("/class2Nics/class2VoluntaryContributions"))
      }
    }
  }

}
