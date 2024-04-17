package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants

import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.BaseRouter
import jakarta.inject.Inject

class ParticipantsRouter
@Inject constructor(
    getParticipantsRouter: GetParticipantsRouter,
    addParticipantRouter: AddParticipantRouter,
    modifyParticipantRouter: ModifyParticipantRouter,
    removeParticipantRouter: RemoveParticipantRouter,
) : BaseRouter(
    "/participants",
    listOf(
        getParticipantsRouter,
        addParticipantRouter,
        modifyParticipantRouter,
        removeParticipantRouter,
    ),
)
