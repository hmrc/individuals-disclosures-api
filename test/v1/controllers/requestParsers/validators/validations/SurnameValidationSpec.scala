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

package v1.controllers.requestParsers.validators.validations

import org.scalatest.Inspectors
import support.UnitSpec

// Note that these tests are sanity checks of the implementation of a specified regex rather than being definitive.
class SurnameValidationSpec extends UnitSpec with Inspectors {
  val validator: SurnameValidation.type = SurnameValidation

  "SurnameValidation" must {
    "allow alphanumeric values with arbitrary capitalization" in {
      forAll(Seq("Smith", "McDougal", "jones")) { name =>
        validator.isValid(name) shouldBe true
      }
    }

    "allow hyphenated and spaces and dots" in {
      forAll(Seq("Smith-Jones", "O'Leary", "Smith Jones", "St. John")) { name =>
        validator.isValid(name) shouldBe true
      }
    }

    // Re-instate if reqd once spec finalized
    //    "allow diacritics" in {
    //      forAll(Seq("Courtès", "Héroux", "Frêche")) { name =>
    //        validator.isValid(name) shouldBe true
    //      }
    //    }

    "allow ordinals" in {
      forAll(Seq("Jones 3rd")) { name =>
        validator.isValid(name) shouldBe true
      }
    }

    "disallow special characters and emojis" in {
      forAll(Seq("{}", "_", "\uD83D\uDC4D", "; drop table students; --")) { name =>
        validator.isValid(name) shouldBe false
      }
    }

    "allow values at both ends of length limit" in {
      forAll(Seq("A", "a".repeat(35))) { name =>
        validator.isValid(name) shouldBe true
      }
    }

    "disallow values that are too short or too long" in {
      forAll(Seq("", "a".repeat(36))) { name =>
        validator.isValid(name) shouldBe false
      }
    }
  }
}
