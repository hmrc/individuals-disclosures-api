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

package v1.controllers

import api.controllers._
import api.hateoas.HateoasFactory
import api.services.{ AuditService, EnrolmentsAuthService, MtdIdLookupService }
import config.AppConfig
import play.api.libs.json.JsValue
import play.api.mvc.{ Action, AnyContentAsJson, ControllerComponents }
import utils.IdGenerator
import v1.controllers.requestParsers.AmendDisclosuresRequestParser
import v1.models.request.amend.AmendDisclosuresRawData
import v1.models.response.amendDisclosures.AmendDisclosuresHateoasData
import v1.models.response.amendDisclosures.AmendDisclosuresResponse.AmendLinksFactory
import v1.services._

import javax.inject._
import scala.concurrent.ExecutionContext

class AmendDisclosuresController @Inject()(val authService: EnrolmentsAuthService,
                                           val lookupService: MtdIdLookupService,
                                           service: AmendDisclosuresService,
                                           parser: AmendDisclosuresRequestParser,
                                           hateoasFactory: HateoasFactory,
                                           auditService: AuditService,
                                           appConfig: AppConfig,
                                           cc: ControllerComponents,
                                           val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendDisclosuresController", endpointName = "Amend Disclosures")

  def amendDisclosures(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData: AmendDisclosuresRawData = AmendDisclosuresRawData(
        nino = nino,
        taxYear = taxYear,
        body = AnyContentAsJson(request.body)
      )

      val requestHandler =
        RequestHandler
          .withParser(parser)
          .withService(service.amendDisclosures)
          .withHateoasResult(hateoasFactory)(AmendDisclosuresHateoasData(nino, taxYear))
          .withAuditing(AuditHandler(
            auditService,
            auditType = "CreateAmendDisclosures",
            transactionName = "create-amend-disclosures",
            params = Map("nino" -> nino, "taxYear" -> taxYear),
            Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest(rawData)
    }
}
