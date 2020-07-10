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

package v1.controllers.requestParsers.validators

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v1.models.errors._
import v1.models.request.sample.SampleRawData

class SampleValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2018-19"

  private val requestBodyJson: JsValue = Json.parse(
    """{
      |  "data" : "someData"
      |}
    """.stripMargin)

  private val nonValidRequestBodyJson: JsValue = Json.parse(
    """{
      |  "data" : true
      |}
    """.stripMargin)

  private val emptyRequestBodyJson: JsValue = Json.parse("""{}""")

  private val nonsenseRequestBodyJson: JsValue = Json.parse("""{"field": "value"}""")

  private val validRawRequestBody = AnyContentAsJson(requestBodyJson)
  private val emptyRawRequestBody = AnyContentAsJson(emptyRequestBodyJson)
  private val nonsenseRawRequestBody = AnyContentAsJson(nonsenseRequestBodyJson)
  private val nonValidRawRequestBody = AnyContentAsJson(nonValidRequestBodyJson)

  val validator = new SampleValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(SampleRawData(validNino, validTaxYear, validRawRequestBody)) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validator.validate(SampleRawData("A12344A", validTaxYear, validRawRequestBody)) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        validator.validate(SampleRawData(validNino, "20178", validRawRequestBody)) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an out of range tax year is supplied" in {
        validator.validate(SampleRawData(validNino, "2016-18", validRawRequestBody)) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty JSON body is submitted" in {
        validator.validate(SampleRawData(validNino, validTaxYear, emptyRawRequestBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }

      "a non-empty JSON body is submitted without any expected fields" in {
        validator.validate(SampleRawData(validNino, validTaxYear, nonsenseRawRequestBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }

      "the submitted request body is not in the correct format" in {
        validator.validate(SampleRawData(validNino, validTaxYear, nonValidRawRequestBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(SampleRawData("A12344A", "20178", validRawRequestBody)) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }
    }
  }
}