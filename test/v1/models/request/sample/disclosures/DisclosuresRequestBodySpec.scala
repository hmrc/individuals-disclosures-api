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

package v1.models.request.sample.disclosures

import play.api.libs.json._
import support.UnitSpec
import v1.models.request.disclosures.{DisclosuresRequestBody, TaxAvoidance}
import v1.models.request.sample.SampleRequestBody
import v1.models.utils.JsonErrorValidators

class DisclosuresRequestBodySpec extends UnitSpec with JsonErrorValidators {
  "reads" when {
    "passed valid JSON" should {
      val inputJson = Json.parse(
        """
          |{
          |   "taxAvoidance": [{"srn":"123","taxYear":"12-12"}]
          |}
        """.stripMargin
      )

      "return a valid model" in {
        DisclosuresRequestBody(Some(Seq(TaxAvoidance("123","12-12")))) shouldBe inputJson.as[DisclosuresRequestBody]
      }

      testPropertyType[DisclosuresRequestBody](inputJson)(
        path = "/taxAvoidance",
        replacement = 12344.toJson,
        expectedError = JsonError.JSARRAY_FORMAT_EXCEPTION
      )
    }
  }
}
