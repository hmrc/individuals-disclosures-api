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

package v1.controllers.validators

import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import config.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v1.models.request.amend.{AmendClass2Nics, AmendDisclosuresRequestBody, AmendDisclosuresRequestData, AmendTaxAvoidanceItem}

class AmendDisclosuresValidatorFactorySpec extends UnitSpec with MockAppConfig {
  private implicit val correlationId: String = "1234"

  private val validNino    = "AA123456A"
  private val validTaxYear = "2023-24"

  private val validRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "taxAvoidance": [
      |      {
      |         "srn": "14211123",
      |         "taxYear": "2020-21"
      |      },
      |      {
      |         "srn": "34522678",
      |         "taxYear": "2021-22"
      |      }
      |   ],
      |   "class2Nics": {
      |      "class2VoluntaryContributions": true
      |   }
      |}
    """.stripMargin
  )

  private val emptyRequestBodyJson: JsValue = Json.parse("""{}""")

  private val nonsenseRequestBodyJson: JsValue = Json.parse("""{"field": "value"}""")

  private val nonValidRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "taxAvoidance": [
      |      {
      |         "srn": true,
      |         "taxYear": "2020-21"
      |      }
      |   ],
      |   "class2Nics": {
      |      "class2VoluntaryContributions": "no"
      |   }
      |}
    """.stripMargin
  )

  private val invalidSRNRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "taxAvoidance": [
      |      {
      |         "srn": "ABC142111235",
      |         "taxYear": "2020-21"
      |      }
      |   ],
      |   "class2Nics": {
      |      "class2VoluntaryContributions": true
      |   }
      |}
    """.stripMargin
  )

  private val invalidTaxYearRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "taxAvoidance": [
      |      {
      |         "srn": "14211123",
      |         "taxYear": "2020"
      |      }
      |   ],
      |   "class2Nics": {
      |      "class2VoluntaryContributions": true
      |   }
      |}
    """.stripMargin
  )

  private val invalidTaxYearRangeRuleRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "taxAvoidance": [
      |      {
      |         "srn": "14211123",
      |         "taxYear": "2020-22"
      |      }
      |   ]
      |}
    """.stripMargin
  )

  private val invalidClass2ValueRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "class2Nics": {
      |      "class2VoluntaryContributions": false
      |   }
      |}
    """.stripMargin
  )

  private val allInvalidValueRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "taxAvoidance": [
      |      {
      |         "srn": "ABC142111235D",
      |         "taxYear": "2020"
      |      },
      |      {
      |         "srn": "CDE345226789F",
      |         "taxYear": "2020-22"
      |      }
      |   ],
      |   "class2Nics": {
      |      "class2VoluntaryContributions": false
      |   }
      |}
    """.stripMargin
  )

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  private val parsedRequestBody: AmendDisclosuresRequestBody = AmendDisclosuresRequestBody(
    Some(
      List(
        AmendTaxAvoidanceItem("14211123", "2020-21"),
        AmendTaxAvoidanceItem("34522678", "2021-22")
      )
    ),
    Some(
      AmendClass2Nics(Some(true))
    )
  )

  private val validatorFactory = new AmendDisclosuresValidatorFactory(mockAppConfig)

  private def validator(nino: String, taxYear: String, body: JsValue) = validatorFactory.validator(nino, taxYear, body)

  class SetUp {

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2022)
      .anyNumberOfTimes()

  }

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator(validNino, validTaxYear, validRequestBodyJson).validateAndWrapResult()

        result shouldBe Right(AmendDisclosuresRequestData(parsedNino, parsedTaxYear, parsedRequestBody))
      }
    }

    "return a single error" when {
      "passed an invalid nino" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator("invalid nino", validTaxYear, validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }

      "passed an incorrectly formatted taxYear" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] = validator(validNino, "202324", validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }

      "passed an unsupported taxYear" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] = validator(validNino, "2020-21", validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }

      "passed a taxYear spanning an invalid tax year range" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] = validator(validNino, "2020-22", validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }

      "passed an empty JSON body" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator(validNino, "2021-22", emptyRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "passed a non-empty JSON body without any expected fields" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator(validNino, "2021-22", nonsenseRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "passed incorrectly formatted request body" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator(validNino, "2021-22", nonValidRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/class2Nics/class2VoluntaryContributions", "/taxAvoidance/0/srn")))))
      }

      "passed an incorrectly formatted srn in the request body" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator(validNino, "2021-22", invalidSRNRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, SRNFormatError.copy(paths = Some(List("/taxAvoidance/0/srn")))))
      }

      "passed an incorrectly formatted tax year in the request body" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator(validNino, "2021-22", invalidTaxYearRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError.copy(paths = Some(List("/taxAvoidance/0/taxYear")))))
      }

      "passed an invalid tax year range in the request body" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator(validNino, "2021-22", invalidTaxYearRangeRuleRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError.copy(paths = Some(List("/taxAvoidance/0/taxYear")))))
      }

      "passed an incorrect boolean value for class2VoluntaryContributions in the request body" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator(validNino, "2021-22", invalidClass2ValueRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleVoluntaryClass2ValueInvalidError.copy(paths = Some(List("/class2Nics/class2VoluntaryContributions")))))
      }
    }

    "return multiple errors" when {
      "the request has multiple issues (path parameters)" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator("invalid", "invalid", validRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(
              List(
                NinoFormatError,
                TaxYearFormatError
              ))
          )
        )
      }

      "the request body has multiple issues" in new SetUp {
        val result: Either[ErrorWrapper, AmendDisclosuresRequestData] =
          validator(validNino, validTaxYear, allInvalidValueRequestBodyJson).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(
              TaxYearFormatError.copy(paths = Some(List("/taxAvoidance/0/taxYear"))),
              SRNFormatError.copy(paths = Some(List("/taxAvoidance/0/srn", "/taxAvoidance/1/srn"))),
              RuleTaxYearRangeInvalidError.copy(paths = Some(List("/taxAvoidance/1/taxYear"))),
              RuleVoluntaryClass2ValueInvalidError.copy(paths = Some(List("/class2Nics/class2VoluntaryContributions")))
            ))
          )
        )
      }
    }
  }

}
