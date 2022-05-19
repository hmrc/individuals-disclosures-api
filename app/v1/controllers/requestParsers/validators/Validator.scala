/*
 * Copyright 2022 HM Revenue & Customs
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
import scala.collection.immutable.ListSet

trait Validator[A <: RawData] {
  type ValidationType = A => ListSet[List[MtdError]]

  def validate(data: A): ListSet[MtdError]

  @tailrec
  final def run(validationSet: ListSet[ValidationType], data: A): ListSet[MtdError] = {
    val nextValidationResultOpt: Option[ListSet[MtdError]] = validationSet.headOption.map(_(data).flatten)

    nextValidationResultOpt match {
      case None => ListSet.empty[MtdError]
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
    case items =>
      val nextError: MtdError = items.head
      val nextErrorPaths = items.tail.filter(_.message == nextError.message).flatMap(_.paths).flatten

      def makeListSetOptional: ListSet[String] => Option[ListSet[String]] = listSet => if (listSet.isEmpty) None else Some(listSet)

      val newFlatError = nextError.copy(paths = makeListSetOptional(nextError.paths.getOrElse(ListSet.empty) ++ nextErrorPaths))
      val remainingErrorsToFlatten = items.filterNot(_.message == nextError.message)

      flattenErrors(List(remainingErrorsToFlatten), flatErrors :+ newFlatError)
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
