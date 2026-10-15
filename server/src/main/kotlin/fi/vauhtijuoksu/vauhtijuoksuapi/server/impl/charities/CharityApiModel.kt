package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.charities

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Charity
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.util.UUID

data class CharityApiModel(
    val id: UUID,
    val name: String,
    val url: String?,
    val goal: Int?,
    @JsonProperty("img_filename")
    val imgFilename: String?,
) : ApiModel<Charity> {
    companion object {
        fun from(charity: Charity): CharityApiModel {
            return CharityApiModel(
                charity.id,
                charity.name,
                charity.url,
                charity.goal,
                charity.imgFilename,
            )
        }
    }

    override fun toModel(): Charity {
        return Charity(
            id,
            name,
            url,
            goal,
            imgFilename,
        )
    }

    override fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }
}
