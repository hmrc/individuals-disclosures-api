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

package v2.createAmendDisclosures.def1.model.request

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Json, OWrites, Reads}

case class TaxAvoidanceItem(srn: String, taxYear: String)

object TaxAvoidanceItem {
  implicit val reads: Reads[TaxAvoidanceItem] = Json.reads[TaxAvoidanceItem]

  implicit val writes: OWrites[TaxAvoidanceItem] = (
    (JsPath \ "SRN").write[String] and
      (JsPath \ "taxYear").write[String]
  )(o => Tuple.fromProductTyped(o))

}
