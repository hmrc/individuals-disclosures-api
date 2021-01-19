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

package v1.controllers.requestParsers.validators

import config.AppConfig
import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v1.models.errors._
import v1.models.request.disclosures.AmendDisclosuresRawData

class AmendDisclosuresValidatorSpec extends UnitSpec with MockAppConfig {

  private val validNino = "AA123456A"
  private val validTaxYear = "2021-22"

  private val validRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "14211123",
      |      "taxYear": "2020-21"
      |    },
      |    {
      |      "srn": "34522678",
      |      "taxYear": "2021-22"
      |    }
      |  ],
      |  "class2Nics": {
      |   "class2VoluntaryContributions": true
      |  }
      |}
    """.stripMargin
  )

  private val emptyRequestBodyJson: JsValue = Json.parse("""{}""")

  private val nonsenseRequestBodyJson: JsValue = Json.parse("""{"field": "value"}""")

  private val nonValidRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": true,
      |      "taxYear": "2020-21"
      |    }
      |  ],
      |  "class2Nics": {
      |  }
      |}
    """.stripMargin
  )

  private val invalidSRNRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "ABC142111235",
      |      "taxYear": "2020-21"
      |    }
      |  ],
      |  "class2Nics": {
      |     "class2VoluntaryContributions": true
      |  }
      |}
    """.stripMargin
  )

  private val invalidTaxYearRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "14211123",
      |      "taxYear": "2020"
      |    }
      |  ],
      |  "class2Nics": {
      |     "class2VoluntaryContributions": true
      |  }
      |}
    """.stripMargin
  )

  private val invalidTaxYearRangeRuleRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "14211123",
      |      "taxYear": "2020-22"
      |    }
      |  ],
      |  "class2Nics": {
      |     "class2VoluntaryContributions": true
      |  }
      |}
    """.stripMargin
  )

  private val missingClass2RequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "14211123",
      |      "taxYear": "2020-22"
      |    }
      |  ],
      |  "class2Nics": {
      |
      |  }
      |}
    """.stripMargin
  )

  private val allInvalidValueRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "ABC142111235D",
      |      "taxYear": "2020"
      |    },
      |    {
      |      "srn": "CDE345226789F",
      |      "taxYear": "2020-22"
      |    }
      |  ],
      |  "class2Nics": {
      |     "class2VoluntaryContributions": true
      |  }
      |}
    """.stripMargin
  )

  private val validRawRequestBody = AnyContentAsJson(validRequestBodyJson)
  private val emptyRawRequestBody = AnyContentAsJson(emptyRequestBodyJson)
  private val nonsenseRawRequestBody = AnyContentAsJson(nonsenseRequestBodyJson)
  private val nonValidRawRequestBody = AnyContentAsJson(nonValidRequestBodyJson)
  private val invalidSRNRawRequestBody = AnyContentAsJson(invalidSRNRequestBodyJson)
  private val invalidTaxYearRawRequestBody = AnyContentAsJson(invalidTaxYearRequestBodyJson)
  private val invalidTaxYearRangeRuleRawRequestBody = AnyContentAsJson(invalidTaxYearRangeRuleRequestBodyJson)
  private val missingClass2RawRequestBody = AnyContentAsJson(missingClass2RequestBodyJson)
  private val allInvalidValueRawRequestBody = AnyContentAsJson(allInvalidValueRequestBodyJson)

  implicit val appConfig: AppConfig = mockAppConfig

  val validator = new AmendDisclosuresValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, validTaxYear, validRawRequestBody)) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {

        validator.validate(AmendDisclosuresRawData("A12344A", validTaxYear, validRawRequestBody)) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {

        validator.validate(AmendDisclosuresRawData(validNino, "20178", validRawRequestBody)) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty JSON body is submitted" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, validTaxYear, emptyRawRequestBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }


      "a non-empty JSON body is submitted without any expected fields" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, validTaxYear, nonsenseRawRequestBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }

      "the submitted request body is not in the correct format" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, validTaxYear, nonValidRawRequestBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/taxAvoidance/0/srn", "/class2Nics/class2VoluntaryContributions"))))
      }
    }

    "return SRNFormatError error" when {
      "an incorrectly formatted srn is submitted" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, validTaxYear, invalidSRNRawRequestBody)) shouldBe
          List(SRNFormatError.copy(paths = Some(List("/taxAvoidance/0/srn"))))
      }
    }

    "return TaxYearFormatError error" when {
      "an incorrectly formatted tax year is submitted in the request body" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, validTaxYear, invalidTaxYearRawRequestBody)) shouldBe
          List(TaxYearFormatError.copy(paths = Some(List("/taxAvoidance/0/taxYear"))))
      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an invalid tax year range is submitted in the request body" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, validTaxYear, invalidTaxYearRangeRuleRawRequestBody)) shouldBe
          List(RuleTaxYearRangeInvalidError.copy(paths = Some(List("/taxAvoidance/0/taxYear"))))
      }
    }

    "return an IncorrectBody error" when {
      "an empty body for class2Nics is submitted in the request body" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, validTaxYear, missingClass2RawRequestBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/class2Nics/class2VoluntaryContributions"))))
      }
    }

    "return a TaxYearNotSupported error" when {
      "an unsupported date is submitted in the request body" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, "2019-20", missingClass2RawRequestBody)) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return multiple errors (multiple failures)" when {
      "multiple fields fail value validation" in {
        MockedAppConfig.minimumPermittedTaxYear.returns(2021)

        validator.validate(AmendDisclosuresRawData(validNino, validTaxYear, allInvalidValueRawRequestBody)) shouldBe
          List(
            RuleTaxYearRangeInvalidError.copy(
              paths = Some(List(
                "/taxAvoidance/1/taxYear"
              ))
            ),
            TaxYearFormatError.copy(
              paths = Some(List(
                "/taxAvoidance/0/taxYear"
              ))
            ),
            SRNFormatError.copy(
              paths = Some(List(
                "/taxAvoidance/0/srn",
                "/taxAvoidance/1/srn"
              ))
            )
          )
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors (path parameters)" in {
        validator.validate(AmendDisclosuresRawData("A12344A", "20178", emptyRawRequestBody)) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }
    }
  }
}