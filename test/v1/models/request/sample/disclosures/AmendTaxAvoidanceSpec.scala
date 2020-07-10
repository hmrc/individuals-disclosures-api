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

import play.api.libs.json.{JsError, JsValue, Json}
import support.UnitSpec
import v1.models.request.disclosures.AmendTaxAvoidance

class AmendTaxAvoidanceSpec extends UnitSpec {

  private val json: JsValue = Json.parse(
    """
      |{
      |   "srn":"123","taxYear":"12-12"
      |}
        """.stripMargin
  )

  private val taxAvoidanceModel: AmendTaxAvoidance = AmendTaxAvoidance("123","12-12")

  "taxAvoidanceModel" when {
    "read from valid JSON" should {
      "produce the expected AmendDividendsRequestBody object" in {
        json.as[AmendTaxAvoidance] shouldBe taxAvoidanceModel
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        val error = Json.parse(
          """
            |{"a":"b"}
            |""".stripMargin
        )

        error.validate[AmendTaxAvoidance] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(taxAvoidanceModel) shouldBe json
      }
    }
  }
}
