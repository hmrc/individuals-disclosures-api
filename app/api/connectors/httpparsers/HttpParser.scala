/*
 * Copyright 2025 HM Revenue & Customs
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

package api.connectors.httpparsers

import api.models.errors.{BVRError, DownstreamError, DownstreamErrorCode, DownstreamErrors, InternalError, OutboundError}
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.http.HttpResponse

import scala.util.{Success, Try}

trait HttpParser {
  private val logger: Logger = Logger(this.getClass)

  implicit class KnownJsonResponse(response: HttpResponse) {

    def validateJson[T](implicit reads: Reads[T]): Option[T] = {
      Try(response.json) match {
        case Success(json: JsValue) => parseResult(json)
        case _ =>
          logger.warn("[KnownJsonResponse][validateJson] No JSON was returned")
          None
      }
    }

    def parseResult[T](json: JsValue)(implicit reads: Reads[T]): Option[T] = json.validate[T] match {
      case JsSuccess(value, _) => Some(value)
      case JsError(error) =>
        logger.warn(s"[KnownJsonResponse][validateJson] Unable to parse JSON: $error")
        None
    }

  }

  def retrieveCorrelationId(response: HttpResponse): String = response.header("CorrelationId").getOrElse("")

  private val multipleErrorReads: Reads[List[DownstreamErrorCode]] = (__ \ "failures").read[List[DownstreamErrorCode]]

  private val bvrErrorReads: Reads[List[DownstreamErrorCode]] = {
    implicit val errorIdReads: Reads[DownstreamErrorCode] = (__ \ "id").read[String].map(DownstreamErrorCode(_))
    (__ \ "bvrfailureResponseElement" \ "validationRuleFailures").read[List[DownstreamErrorCode]]
  }

  private val multipleTopLevelErrorCodesReads: Reads[Seq[DownstreamErrorCode]] =
    __.read[Seq[JsObject]].map(_.map(obj => DownstreamErrorCode((obj \ "errorCode").as[String])))

  private val multipleErrorCodesInResponseReads: Reads[Seq[DownstreamErrorCode]] =
    (__ \ "response").read[Seq[JsObject]].map(_.map(obj => DownstreamErrorCode((obj \ "errorCode").as[String])))

  def parseErrors(response: HttpResponse): DownstreamError = {
    val singleError         = response.validateJson[DownstreamErrorCode].map(err => DownstreamErrors(List(err)))
    lazy val multipleErrors = response.validateJson(multipleErrorReads).map(errs => DownstreamErrors(errs))
    lazy val multipleTopLevelErrorCodes =
      response.validateJson(multipleTopLevelErrorCodesReads).map(errs => DownstreamErrors(errs))
    lazy val multipleErrorCodesInResponse =
      response.validateJson(multipleErrorCodesInResponseReads).map(errs => DownstreamErrors(errs))
    lazy val bvrErrors = response.validateJson(bvrErrorReads).map(errs => OutboundError(BVRError, Some(errs.map(_.toMtd(BVRError.httpStatus)))))
    lazy val unableToParseJsonError = {
      logger.warn(s"unable to parse errors from response: ${response.body}")
      OutboundError(InternalError)
    }

    singleError orElse multipleErrors orElse multipleTopLevelErrorCodes orElse multipleErrorCodesInResponse orElse bvrErrors getOrElse unableToParseJsonError
  }

}
