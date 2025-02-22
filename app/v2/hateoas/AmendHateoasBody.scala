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

package v2.hateoas

import api.hateoas.HateoasLinks
import config.AppConfig
import play.api.libs.json.{JsValue, Json}

trait AmendHateoasBody extends HateoasLinks {

  def amendDisclosuresHateoasBody(appConfig: AppConfig, nino: String, taxYear: String): JsValue = {

    val links = Seq(
      amendDisclosures(appConfig, nino, taxYear),
      retrieveDisclosures(appConfig, nino, taxYear),
      deleteDisclosures(appConfig, nino, taxYear)
    )

    Json.obj("links" -> links)
  }

}
