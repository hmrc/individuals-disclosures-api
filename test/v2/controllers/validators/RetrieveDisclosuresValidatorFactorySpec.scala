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

import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import config.MockAppConfig
import support.UnitSpec
import v2.models.request.retrieve.RetrieveDisclosuresRequestData

class RetrieveDisclosuresValidatorFactorySpec extends UnitSpec with MockAppConfig {
  private implicit val correlationId: String = "1234"

  private val validNino    = "AA123456A"
  private val validTaxYear = "2023-24"

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  private val validatorFactory = new RetrieveDisclosuresValidatorFactory(mockAppConfig)

  private def validator(nino: String, taxYear: String) = validatorFactory.validator(nino, taxYear)

  class SetUp {

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2022)
      .anyNumberOfTimes()

  }

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request" in new SetUp {
        val result: Either[ErrorWrapper, RetrieveDisclosuresRequestData] = validator(validNino, validTaxYear).validateAndWrapResult()

        result shouldBe Right(RetrieveDisclosuresRequestData(parsedNino, parsedTaxYear))
      }
    }

    "return a single error" when {
      "passed an invalid nino" in new SetUp {
        val result: Either[ErrorWrapper, RetrieveDisclosuresRequestData] = validator("invalid nino", validTaxYear).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }

      "passed an incorrectly formatted taxYear" in new SetUp {
        val result: Either[ErrorWrapper, RetrieveDisclosuresRequestData] = validator(validNino, "202324").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }

      "passed an unsupported taxYear" in new SetUp {
        val result: Either[ErrorWrapper, RetrieveDisclosuresRequestData] = validator(validNino, "2020-21").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }

      "passed a taxYear spanning an invalid tax year range" in new SetUp {
        val result: Either[ErrorWrapper, RetrieveDisclosuresRequestData] = validator(validNino, "2020-22").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }

    "return multiple errors" when {
      "the request has multiple issues (path parameters)" in new SetUp {
        val result: Either[ErrorWrapper, RetrieveDisclosuresRequestData] =
          validator("invalid", "invalid").validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(NinoFormatError, TaxYearFormatError))
          )
        )
      }
    }
  }

}
