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

package v1.controllers.requestParsers.validators

import v1.models.errors.MtdError
import v1.models.request.RawData

import scala.annotation.tailrec

trait Validator[A <: RawData] {
  type ValidationType = A => List[List[MtdError]]

  def validate(data: A): List[MtdError]

  @tailrec
  final def run(validationSet: List[ValidationType], data: A): List[MtdError] = {
    val nextValidationResultOpt: Option[List[MtdError]] = validationSet.headOption.map(_(data).flatten)

    nextValidationResultOpt match {
      case None => List.empty[MtdError]
      case Some(errs) if errs.nonEmpty => errs
      case _ => run(validationSet.tail, data)
    }
  }
}

object Validator {

  @tailrec
  def flattenErrors(errorsToFlatten: List[List[MtdError]], flatErrors: List[MtdError] = List.empty): List[MtdError] = errorsToFlatten.flatten match {
    case Nil => flatErrors
    case item :: Nil => flatErrors :+ item
    case nextError :: tail =>
      val (matchingErrors, nonMatchingErrors) = tail.partition(_.message == nextError.message)
      val nextErrorPaths = matchingErrors.flatMap(_.paths).flatten

      def makeListOptional: List[String] => Option[List[String]] = List => if (List.isEmpty) None else Some(List)

      val newFlatError = nextError.copy(paths = makeListOptional(nextError.paths.getOrElse(List.empty) ++ nextErrorPaths))

      flattenErrors(List(nonMatchingErrors), flatErrors :+ newFlatError)
  }

  /**
   * Utility to validate a optional request body field or parameter value.
   *
   * @param optionalValue the optional value
   * @param validation    the validation to be performed if the value is set
   * @return the errors if the value with validation errors is set; an empty list otherwise
   */
  def validateOptional[A](optionalValue: Option[A])(validation: A => List[MtdError]): List[MtdError] =
    optionalValue.map(a => validation(a)).getOrElse(Nil)
}
