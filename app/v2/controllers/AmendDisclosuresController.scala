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

package v2.controllers

import api.controllers._
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import config.AppConfig
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import utils.IdGenerator
import v2.controllers.validators.AmendDisclosuresValidatorFactory
import v2.services._

import javax.inject._
import scala.concurrent.ExecutionContext

class AmendDisclosuresController @Inject() (val authService: EnrolmentsAuthService,
                                            val lookupService: MtdIdLookupService,
                                            service: AmendDisclosuresService,
                                            validatorFactory: AmendDisclosuresValidatorFactory,
                                            auditService: AuditService,
                                            cc: ControllerComponents,
                                            val idGenerator: IdGenerator)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends AuthorisedController(cc) {

  val endpointName = "amend-disclosures"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendDisclosuresController", endpointName = "Amend Disclosures")

  def amendDisclosures(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, taxYear, request.body)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.amendDisclosures)
            .withNoContentResult(NO_CONTENT)
          .withAuditing(AuditHandler(
            auditService,
            auditType = "CreateAmendDisclosures",
            transactionName = "create-amend-disclosures",
            params = Map("nino" -> nino, "taxYear" -> taxYear),
            Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest()
    }

}
