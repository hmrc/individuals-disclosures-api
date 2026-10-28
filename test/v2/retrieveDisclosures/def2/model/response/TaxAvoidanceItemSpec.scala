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

package v2.retrieveDisclosures.def2.model.response

import play.api.libs.json.{JsError, JsObject, Json}
import support.UnitSpec
import v2.retrieveDisclosures.def2.fixture.Def2_RetrieveDisclosuresFixture.*

class TaxAvoidanceItemSpec extends UnitSpec {

  "TaxAvoidanceItem" when {
    "read from valid JSON" should {
      "produce the expected TaxAvoidanceItem object" in {
        downstreamTaxAvoidanceItemJson.as[TaxAvoidanceItem] shouldBe taxAvoidanceItemModel
      }
    }

    "read from empty JSON" should {
      "produce a JsError" in {
        JsObject.empty.validate[TaxAvoidanceItem] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(taxAvoidanceItemModel) shouldBe mtdTaxAvoidanceItemJson
      }
    }
  }

}
