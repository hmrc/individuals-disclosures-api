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

package v2.models.request.create

import play.api.libs.functional.syntax.*
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import utils.JsonUtils

case class CreateMarriageAllowanceRequestBody(spouseOrCivilPartnerNino: String,
                                              spouseOrCivilPartnerFirstName: Option[String],
                                              spouseOrCivilPartnerSurname: String,
                                              spouseOrCivilPartnerDateOfBirth: Option[String])

object CreateMarriageAllowanceRequestBody extends JsonUtils {

  implicit val reads: Reads[CreateMarriageAllowanceRequestBody] = Json.reads[CreateMarriageAllowanceRequestBody]

  implicit val writes: OWrites[CreateMarriageAllowanceRequestBody] = (
    (JsPath \ "participant1Details" \ "nino").write[String] and
      (JsPath \ "participant1Details" \ "firstForeName").writeNullable[String] and
      (JsPath \ "participant1Details" \ "surname").write[String] and
      (JsPath \ "participant1Details" \ "dateOfBirth").writeNullable[String]
  )(o => Tuple.fromProductTyped(o))

}
