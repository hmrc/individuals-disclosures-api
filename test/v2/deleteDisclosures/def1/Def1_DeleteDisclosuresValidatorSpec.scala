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

package v2.deleteDisclosures.def1

import api.models.domain.{Nino, TaxYear}
import api.models.errors.{ErrorWrapper, NinoFormatError}
import support.UnitSpec
import v2.deleteDisclosures.def1.model.request.Def1_DeleteDisclosuresRequestData

class Def1_DeleteDisclosuresValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino: String    = "AA123456A"
  private val validTaxYear: String = "2026-27"

  private val parsedNino: Nino       = Nino(validNino)
  private val parsedTaxYear: TaxYear = TaxYear.fromMtd(validTaxYear)

  private def validate(nino: String) = new Def1_DeleteDisclosuresValidator(nino, validTaxYear).validateAndWrapResult()

  "Def1_DeleteDisclosuresValidator" should {
    "return the parsed domain object" when {
      "a valid request is supplied" in {
        validate(validNino) shouldBe Right(Def1_DeleteDisclosuresRequestData(parsedNino, parsedTaxYear))
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validate("A12344A") shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }
  }

}
