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

package v2.retrieveDisclosures.def1.fixture

import api.models.domain.Timestamp
import play.api.libs.json.{JsValue, Json}
import v2.retrieveDisclosures.def1.model.response.*

object Def1_RetrieveDisclosuresFixture {

  val taxAvoidanceItemModel: TaxAvoidanceItem = TaxAvoidanceItem(
    srn = "14211123",
    taxYear = "2025-26"
  )

  val class2NicsModel: Class2Nics = Class2Nics(class2VoluntaryContributions = Some(true))

  val responseBodyModel: Def1_RetrieveDisclosuresResponse = Def1_RetrieveDisclosuresResponse(
    taxAvoidance = Some(Seq(taxAvoidanceItemModel)),
    class2Nics = Some(class2NicsModel),
    submittedOn = Timestamp("2025-07-06T09:37:17.976Z")
  )

  val downstreamTaxAvoidanceItemJson: JsValue = Json.parse(
    """
      |{
      |  "SRN": "14211123",
      |  "taxYear": "2025-26"
      |}
    """.stripMargin
  )

  val class2NicsJson: JsValue = Json.parse(
    """
      |{
      |  "class2VoluntaryContributions": true
      |}
    """.stripMargin
  )

  val downstreamResponseBodyJson: JsValue = Json.parse(
    s"""
      |{
      |  "taxAvoidance": [$downstreamTaxAvoidanceItemJson],
      |  "class2Nics": $class2NicsJson,
      |  "submittedOn": "2025-07-06T09:37:17.976Z"
      |}
    """.stripMargin
  )

  val mtdTaxAvoidanceItemJson: JsValue = Json.parse(
    """
      |{
      |  "srn": "14211123",
      |  "taxYear": "2025-26"
      |}
    """.stripMargin
  )

  val mtdResponseBodyJson: JsValue = Json.parse(
    s"""
      |{
      |  "taxAvoidance": [$mtdTaxAvoidanceItemJson],
      |  "class2Nics": $class2NicsJson,
      |  "submittedOn": "2025-07-06T09:37:17.976Z"
      |}
    """.stripMargin
  )

}
