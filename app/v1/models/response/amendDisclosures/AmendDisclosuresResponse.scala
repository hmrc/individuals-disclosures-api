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

package v1.models.response.amendDisclosures

import config.AppConfig
import api.hateoas.{ HateoasLinks, HateoasLinksFactory }
import api.models.hateoas.{ HateoasData, Link }

object AmendDisclosuresResponse extends HateoasLinks {

  implicit object AmendLinksFactory extends HateoasLinksFactory[Unit, AmendDisclosuresHateoasData] {

    override def links(appConfig: AppConfig, data: AmendDisclosuresHateoasData): Seq[Link] = {
      import data._
      Seq(
        amendDisclosures(appConfig, nino, taxYear),
        retrieveDisclosures(appConfig, nino, taxYear),
        deleteDisclosures(appConfig, nino, taxYear)
      )
    }

  }

}

case class AmendDisclosuresHateoasData(nino: String, taxYear: String) extends HateoasData
