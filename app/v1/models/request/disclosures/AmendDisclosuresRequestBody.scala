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

package v1.models.request.disclosures

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}
import utils.JsonUtils

case class AmendDisclosuresRequestBody(taxAvoidance: Option[Seq[AmendTaxAvoidance]], class2Nics: Option[Class2Nics])

object AmendDisclosuresRequestBody extends JsonUtils {
  val empty = AmendDisclosuresRequestBody(None, None)

  implicit val reads: Reads[AmendDisclosuresRequestBody] = (
    (JsPath \ "taxAvoidance").readNullable[Seq[AmendTaxAvoidance]].mapEmptySeqToNone and
      (JsPath \ "class2Nics").readNullable[Class2Nics]
    )(AmendDisclosuresRequestBody.apply _)

  implicit val writes: OWrites[AmendDisclosuresRequestBody] = (
    (JsPath \ "taxAvoidance").writeNullable[Seq[AmendTaxAvoidance]] and
      (JsPath \ "class2Nics").writeNullable[Class2Nics]
    ) (unlift(AmendDisclosuresRequestBody.unapply))

}
