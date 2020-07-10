package v1.models.request.sample

import play.api.mvc.AnyContentAsJson
import v1.models.request.RawData

case class SampleRawData(nino: String, taxYear: String, body: AnyContentAsJson) extends RawData
